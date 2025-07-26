package com.quoteguard.repository;

import com.quoteguard.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUser_Id(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndPaidFalse(Long userId);
    Optional<Invoice> findByQrToken(String qrToken);
}
