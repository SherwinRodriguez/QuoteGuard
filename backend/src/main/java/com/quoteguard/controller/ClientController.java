package com.quoteguard.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quoteguard.dto.ClientRequest;
import com.quoteguard.dto.ClientResponse;
import com.quoteguard.service.ClientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Client Controller
 *
 * Every endpoint operates on the AUTHENTICATED user's own clients only. The
 * acting user ID is resolved from the validated JWT principal via
 * {@code @AuthenticationPrincipal(expression = "id")} - never from a
 * request parameter or body. By-ID endpoints verify ownership in
 * ClientService. All failure cases (not found, not yours, validation,
 * conflict) are handled centrally by GlobalExceptionHandler - no
 * per-endpoint try/catch is needed here.
 */
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody ClientRequest request,
            @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.addClient(request, currentUserId));
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getClientsByUser(
            @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        return ResponseEntity.ok(clientService.getClientsByUser(currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        return ResponseEntity.ok(clientService.getClientById(id, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        clientService.deleteClient(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request,
            @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        return ResponseEntity.ok(clientService.updateClient(id, request, currentUserId));
    }
}
