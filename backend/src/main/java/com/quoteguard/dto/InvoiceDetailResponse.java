package com.quoteguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceDetailResponse {
    private Long id;
    private ClientResponse client;
    private BigDecimal totalAmount;
    private LocalDate createdAt;
    private List<ItemResponse> items;
}