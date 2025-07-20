package com.quoteguard.service;

import com.quoteguard.dto.LoginRequest;
import com.quoteguard.entity.User;
import com.quoteguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Register flow
    public String RegisterUser(LoginRequest request) {
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

    // Login flow — returns User now
    public Map<String, Object> LoginUser(LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            response.put("message", "user not found");
            return response;
        }

        boolean isPasswordMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!isPasswordMatch) {
            response.put("message", "Password Mismatch");
            return response;
        }

        response.put("message", "Successfully logged in");
        response.put("userId", user.getId());
        return response;
    }
}