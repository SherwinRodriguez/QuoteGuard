package com.quoteguard.controller;

import com.quoteguard.dto.LoginRequest;
import com.quoteguard.repository.UserRepository;
import com.quoteguard.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody LoginRequest loginRequest) {
        String result = authService.RegisterUser(loginRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> result = authService.LoginUser(loginRequest);

        // If login failed — return 401
        if ("user not found".equals(result.get("message")) || "Password Mismatch".equals(result.get("message"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        // If login successful — return 200 with userId + message
        return ResponseEntity.ok(result);
    }


}
