package com.quoteguard.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Public verification response DTO
 * 
 * This is returned to ANYONE who scans the QR code.
 * Do NOT include sensitive internal data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResponse {
    
    /**
     * Verification result status
     */
    public enum VerificationStatus {
        VERIFIED,   // Hash matches, invoice is ACTIVE
        REVOKED,    // Invoice was revoked by issuer
        MODIFIED,   // Hash mismatch - invoice has been tampered with
        NOT_FOUND   // Invoice UUID does not exist
    }
    
    private VerificationStatus status;
    private String message; // Human-readable message
    
    // Invoice details (only if found)
    private String freelancerName;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String currency;
    private BigDecimal totalAmount;
    
    // Revocation info (only if revoked)
    private LocalDateTime revokedAt;
    private String revokedReason;
    
    // Verification metadata
    private LocalDateTime verificationTimestamp;
    
    /**
     * Create a NOT_FOUND response
     */
    public static VerificationResponse notFound() {
        return VerificationResponse.builder()
                .status(VerificationStatus.NOT_FOUND)
                .message("Invoice not found. This may be a fake invoice.")
                .verificationTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create a REVOKED response
     */
    public static VerificationResponse revoked(String freelancerName, String invoiceNumber, 
                                               LocalDate issueDate, LocalDateTime revokedAt, 
                                               String revokedReason) {
        return VerificationResponse.builder()
                .status(VerificationStatus.REVOKED)
                .message("This invoice has been revoked by the issuer.")
                .freelancerName(freelancerName)
                .invoiceNumber(invoiceNumber)
                .issueDate(issueDate)
                .revokedAt(revokedAt)
                .revokedReason(revokedReason)
                .verificationTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create a MODIFIED response
     */
    public static VerificationResponse modified(String freelancerName, String invoiceNumber) {
        return VerificationResponse.builder()
                .status(VerificationStatus.MODIFIED)
                .message("WARNING: Invoice has been modified after issuance. Do not trust this invoice.")
                .freelancerName(freelancerName)
                .invoiceNumber(invoiceNumber)
                .verificationTimestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create a VERIFIED response
     */
    public static VerificationResponse verified(String freelancerName, String invoiceNumber,
                                                LocalDate issueDate, LocalDate dueDate,
                                                String currency, BigDecimal totalAmount) {
        return VerificationResponse.builder()
                .status(VerificationStatus.VERIFIED)
                .message("Invoice is valid and has not been tampered with.")
                .freelancerName(freelancerName)
                .invoiceNumber(invoiceNumber)
                .issueDate(issueDate)
                .dueDate(dueDate)
                .currency(currency)
                .totalAmount(totalAmount)
                .verificationTimestamp(LocalDateTime.now())
                .build();
    }
}
