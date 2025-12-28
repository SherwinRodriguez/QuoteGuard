package com.quoteguard.repository;

import com.quoteguard.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUser_Id(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndPaidFalse(Long userId);
    
    // Find invoice by public UUID (used for verification)
    Optional<Invoice> findByUuid(String uuid);
    
    // Check if invoice with number already exists for user
    boolean existsByUserIdAndInvoiceNumber(Long userId, String invoiceNumber);
}
