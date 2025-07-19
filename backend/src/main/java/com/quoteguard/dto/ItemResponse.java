package com.quoteguard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemResponse {
    private String product;
    private int quantity;
    private BigDecimal unitPrice;
}