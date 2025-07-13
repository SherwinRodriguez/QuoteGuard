package com.quoteguard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemRequest {
    private String product;
    private int quantity;
    private double unitPrice;

}
