package com.quoteguard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for creating/updating a client. Replaces accepting the raw
 * Client JPA entity as the request body (see ClientController Javadoc).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    private String gstin;
    private String phone;
}
