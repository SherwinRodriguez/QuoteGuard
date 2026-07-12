package com.quoteguard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.quoteguard.entity.InvoiceStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Used for BOTH the invoice list endpoint and the single-invoice detail
 * endpoint. Previously these were two near-identical classes
 * (InvoiceResponse / InvoiceDetailResponse) that had also both drifted out
 * of sync with what the frontend actually expects: neither ever included
 * uuid, invoiceNumber, status, subtotal, tax, currency, issueDate, dueDate,
 * revokedAt, or revokedReason, which meant the "Verify" link, the
 * ACTIVE/REVOKED badge, and the subtotal/tax breakdown in the dashboard UI
 * were reading undefined fields from day one - a real, previously
 * unflagged bug, not a hypothetical one. Consolidated into one DTO rather
 * than fixing the same bug twice in two structurally-identical classes.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String uuid;
    private String invoiceNumber;
    private ClientResponse client;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate createdAt;
    private LocalDateTime revokedAt;
    private String revokedReason;
    private List<ItemResponse> items;
}
