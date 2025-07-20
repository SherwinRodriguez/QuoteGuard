package com.quoteguard.service;

import com.quoteguard.entity.Client;
import com.quoteguard.entity.User;
import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    // 🔐 Add client for specific user
    public String addClient(Client client, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        client.setUser(user);
        clientRepository.save(client);
        return "Client saved";
    }

    // 🧾 Get all clients (admin use only, not recommended for normal users)
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    // 🔍 Get single client by ID
    public Optional<Client> getClientById(Long id) {
        return clientRepository.findById(id);
    }

    // ❌ Delete only if no invoices exist
    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        if (client.getInvoices() != null && !client.getInvoices().isEmpty()) {
            throw new IllegalStateException("Cannot delete client with existing invoices.");
        }

        clientRepository.deleteById(id);
    }

    // 🔁 Update client data
    public Client updateClient(Long id, Client updatedClient) {
        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        existing.setName(updatedClient.getName());
        existing.setEmail(updatedClient.getEmail());
        existing.setPhone(updatedClient.getPhone());
        existing.setGstin(updatedClient.getGstin());

        return clientRepository.save(existing);
    }

    // 🔐 List clients belonging to specific user
    public List<Client> getClientsByUser(Long userId) {
        return clientRepository.findByUserId(userId);
    }
}