package com.quoteguard.service;

import com.quoteguard.entity.Client;
import com.quoteguard.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

}
