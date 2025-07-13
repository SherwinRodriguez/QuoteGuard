package com.quoteguard.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {
    private String invoiceNumber;
    private LocalDate issueDate;
    private boolean paid;
    private double totalAmount;
    private Long clientId;
    private Long userId;
    private List<InvoiceItemRequest> items;
}
