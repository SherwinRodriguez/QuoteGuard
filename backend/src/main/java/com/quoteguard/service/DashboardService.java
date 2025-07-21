package com.quoteguard.service;

import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;

    public Map<String, Object> getStats(Long userId) {
        long clientCount = clientRepository.countByUserId(userId);
        long invoiceCount = invoiceRepository.countByUserId(userId);
        long pendingCount = invoiceRepository.countByUserIdAndPaidFalse(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("clients", clientCount);
        stats.put("invoices", invoiceCount);
        stats.put("pending", pendingCount);

        return stats;
    }
}