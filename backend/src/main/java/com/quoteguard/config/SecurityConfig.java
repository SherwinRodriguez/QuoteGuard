package com.quoteguard.config;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security Configuration
 * 
 * PUBLIC ENDPOINTS (No Auth):
 * - /api/invoices/verify/{uuid} - Invoice verification
 * - /verify/{uuid} - Alternative verification route
 * - /api/auth/** - Authentication endpoints
 * 
 * PROTECTED ENDPOINTS (JWT Auth):
 * - All other /api/** endpoints
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public verification endpoints (CRITICAL: No auth required)
                        .requestMatchers(
                                "/api/invoices/verify/**",
                                "/verify/**",
                                "/api/auth/**"
                        ).permitAll()
                        
                        // Temporary: Allow all for development
                        // TODO: Enable JWT auth for production
                        .requestMatchers("/api/**").permitAll()
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // basic auth for now

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
