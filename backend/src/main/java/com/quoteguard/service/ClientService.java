package com.quoteguard.service;

import com.quoteguard.entity.Client;
import com.quoteguard.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;

    public String addClient(Client client) {
        clientRepository.save(client);
        return "Client saved";
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Optional<Client> getClientById(Long id) {
        return clientRepository.findById(id);
    }

    public Client updateClient(Long id, Client updatedClient) {
        return clientRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedClient.getName());
                    existing.setEmail(updatedClient.getEmail());
                    existing.setPhone(updatedClient.getPhone());
                    existing.setGstin(updatedClient.getGstin());
                    return clientRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));
    }

    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new RuntimeException("Client not found with id: " + id);
        }
        clientRepository.deleteById(id);
    }

}
