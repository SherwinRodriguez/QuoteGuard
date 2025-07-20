package com.quoteguard.controller;

import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/stats")
    public Map<String, Long> getDashboardStats() {
        long clientCount = clientRepository.count();
        long invoiceCount = invoiceRepository.count();

        Map<String, Long> stats = new HashMap<>();
        stats.put("clients", clientCount);
        stats.put("invoices", invoiceCount);

        return stats;
    }
}