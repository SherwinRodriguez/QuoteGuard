package com.quoteguard.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.quoteguard.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Standard Spring Security contract - used by the AuthenticationManager
     * during login, where the caller only has an email/password pair.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
    }

    /**
     * Used by JwtAuthenticationFilter on every subsequent request. The JWT
     * subject is the numeric user ID rather than the email, so token
     * validation resolves the principal by ID instead of by username.
     */
    public UserDetails loadUserById(Long id) {
        return userRepository.findById(id)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user with id: " + id));
    }
}
