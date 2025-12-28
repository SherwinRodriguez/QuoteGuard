package com.quoteguard.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokeInvoiceRequest {
    private String reason; // Required: why is this invoice being revoked
}
