package com.quoteguard.service;

import com.quoteguard.dto.LoginRequest;
import com.quoteguard.entity.User;
import com.quoteguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //Register flow
    public String RegisterUser(LoginRequest request){
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "User already exists";
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();
        userRepository.save(user);
        return "User registered successfully";
    }


    //Login Flow
    public String LoginUser(LoginRequest request){
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if(user == null){
            return "user not found";
        }

        boolean isPasswordmatch = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if(!isPasswordmatch){
            return "Password Mismatch";
        }
        return "Successfully logged in";
    }

}
