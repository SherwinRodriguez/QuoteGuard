package com.quoteguard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Replaces the previous overloaded use of LoginRequest for registration.
 * Login only ever needed email+password; register also needs name - a
 * shared DTO couldn't express that "name is required here, irrelevant
 * there" without weakening validation for one of the two use cases.
 * Password STRENGTH (not just non-blank) is enforced separately by
 * PasswordPolicy in AuthService, not duplicated here as a @Pattern.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
