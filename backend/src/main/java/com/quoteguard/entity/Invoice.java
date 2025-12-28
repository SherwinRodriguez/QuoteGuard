package com.quoteguard.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Invoice entity - IMMUTABLE after issuance
 * 
 * CRITICAL CONSTRAINTS:
 * 1. uuid is the PUBLIC identifier (used in verification URLs)
 * 2. invoiceHash is computed ONCE and NEVER updated
 * 3. Core fields (invoiceNumber, amounts, dates, items) are immutable
 * 4. Status can only transition from ACTIVE to REVOKED
 * 5. Invoices are NEVER deleted (audit trail requirement)
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    
    // Internal database ID (not exposed publicly)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Public UUID identifier (used in QR codes and verification URLs)
    @Column(nullable = false, unique = true, updatable = false, columnDefinition = "VARCHAR(36)")
    private String uuid;
    
    // Invoice number (must be unique per freelancer)
    @Column(nullable = false, updatable = false)
    private String invoiceNumber;
    
    // Dates
    @Column(nullable = false, updatable = false)
    private LocalDate issueDate;
    
    @Column(nullable = false, updatable = false)
    private LocalDate dueDate;
    
    // Financial fields
    @Column(nullable = false, updatable = false, length = 3)
    private String currency; // e.g., "INR", "USD"
    
    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;
    
    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal tax;
    
    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    
    // SHA-256 hash for integrity verification (IMMUTABLE)
    @Column(nullable = false, updatable = false, length = 64)
    private String invoiceHash;
    
    // Status (can only go from ACTIVE -> REVOKED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;
    
    // Revocation tracking
    @Column
    private LocalDateTime revokedAt;
    
    @Column(length = 500)
    private String revokedReason;
    
    // Audit timestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        
        // Generate UUID if not set
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
        
        // Set default status
        if (this.status == null) {
            this.status = InvoiceStatus.ACTIVE;
        }
    }

    // Relationships
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private Client client;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItems> items;
    
    // Legacy field - can be removed after migration
    @Deprecated
    @Column
    private boolean paid;
}
