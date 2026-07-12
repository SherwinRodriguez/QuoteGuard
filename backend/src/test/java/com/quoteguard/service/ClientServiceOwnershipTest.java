package com.quoteguard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.quoteguard.dto.ClientRequest;
import com.quoteguard.entity.Client;
import com.quoteguard.entity.User;
import com.quoteguard.exception.ResourceNotFoundException;
import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.UserRepository;

/**
 * Pure unit tests (Mockito, no database, no Spring context) proving the
 * ownership rule enforced by ClientService.findOwnedClient: an owner can
 * read/update/delete their own client, a non-owner is rejected with
 * AccessDeniedException (-> 403 at the web layer), and a missing client
 * is rejected with ResourceNotFoundException (-> 404).
 */
@ExtendWith(MockitoExtension.class)
class ClientServiceOwnershipTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    private ClientService clientService;

    private User owner;
    private Client client;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository, userRepository);

        owner = User.builder().id(1L).email("owner@example.com").name("Owner").role("USER").build();

        client = new Client();
        client.setId(10L);
        client.setName("Acme Corp");
        client.setUser(owner);
    }

    @Test
    void getClientById_ownerCanReadTheirOwnClient() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));

        var response = clientService.getClientById(10L, 1L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Acme Corp");
    }

    @Test
    void getClientById_nonOwnerIsRejectedWithAccessDenied() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.getClientById(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getClientById_missingClientIsRejectedWithNotFound() {
        when(clientRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(404L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateClient_nonOwnerIsRejectedBeforeAnyMutation() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        ClientRequest request = new ClientRequest("New Name", "new@example.com", null, null);

        assertThatThrownBy(() -> clientService.updateClient(10L, request, 999L))
                .isInstanceOf(AccessDeniedException.class);

        // the original client object must be untouched - proves the ownership
        // check happens before any field is mutated, not after
        assertThat(client.getName()).isEqualTo("Acme Corp");
    }

    @Test
    void deleteClient_nonOwnerIsRejected() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.deleteClient(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
