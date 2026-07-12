package com.quoteguard.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quoteguard.dto.ClientRequest;
import com.quoteguard.dto.ClientResponse;
import com.quoteguard.entity.Client;
import com.quoteguard.entity.User;
import com.quoteguard.exception.ResourceNotFoundException;
import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Every method here takes the caller's authenticated user ID explicitly and
 * enforces it: clients are only ever created under that user, and every
 * by-ID lookup is scoped to resources owned by that user.
 *
 * Explicit @Transactional boundaries (rather than relying on Spring Boot's
 * default Open-Session-In-View) matter here specifically because
 * deleteClient reads client.getInvoices(), a LAZY collection - with OSIV
 * disabled (see application.properties), that access needs an open
 * session/transaction at the point it happens, not just at the point the
 * repository call returns.
 */
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClientResponse addClient(ClientRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists: " + currentUserId));

        Client client = new Client();
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setGstin(request.getGstin());
        client.setPhone(request.getPhone());
        client.setUser(user);

        return toResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> getClientsByUser(Long currentUserId) {
        return clientRepository.findByUserId(currentUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id, Long currentUserId) {
        return toResponse(findOwnedClient(id, currentUserId));
    }

    @Transactional
    public void deleteClient(Long id, Long currentUserId) {
        Client client = findOwnedClient(id, currentUserId);

        if (client.getInvoices() != null && !client.getInvoices().isEmpty()) {
            throw new IllegalStateException("Cannot delete client with existing invoices.");
        }

        clientRepository.delete(client);
    }

    @Transactional
    public ClientResponse updateClient(Long id, ClientRequest request, Long currentUserId) {
        Client existing = findOwnedClient(id, currentUserId);

        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setGstin(request.getGstin());

        return toResponse(clientRepository.save(existing));
    }

    /**
     * Loads a client and enforces ownership in exactly one place: a
     * ResourceNotFoundException (-> 404) if the row doesn't exist at all,
     * an AccessDeniedException (-> 403) if it exists but belongs to
     * someone else. Both are translated centrally by GlobalExceptionHandler.
     */
    private Client findOwnedClient(Long id, Long currentUserId) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));

        if (!client.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not own this client");
        }

        return client;
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getGstin(),
                client.getPhone()
        );
    }
}
