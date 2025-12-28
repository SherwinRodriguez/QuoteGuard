package com.quoteguard.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {
    private String invoiceNumber; // Optional - will be auto-generated if not provided
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String currency; // e.g., "INR", "USD"
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private Long clientId;
    private Long userId;
    private List<InvoiceItemRequest> items;
    
    // Legacy field - can be removed
    @Deprecated
    private boolean paid;
}
