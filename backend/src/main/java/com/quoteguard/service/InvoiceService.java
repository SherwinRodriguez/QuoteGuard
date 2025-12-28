package com.quoteguard.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quoteguard.dto.ClientResponse;
import com.quoteguard.dto.InvoiceDetailResponse;
import com.quoteguard.dto.InvoiceRequest;
import com.quoteguard.dto.InvoiceResponse;
import com.quoteguard.dto.ItemResponse;
import com.quoteguard.dto.RevokeInvoiceRequest;
import com.quoteguard.dto.VerificationResponse;
import com.quoteguard.entity.Client;
import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceItems;
import com.quoteguard.entity.InvoiceStatus;
import com.quoteguard.entity.User;
import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.InvoiceRepository;
import com.quoteguard.repository.UserRepository;
import com.quoteguard.utils.InvoiceHashUtil;
import com.quoteguard.utils.PDFGenerator;

import lombok.RequiredArgsConstructor;

/**
 * Invoice Service - Production-Grade Implementation
 * 
 * CRITICAL RULES:
 * 1. Invoices are IMMUTABLE after creation
 * 2. Hash is generated ONCE and NEVER updated
 * 3. Invoices are NEVER deleted (use revoke instead)
 * 4. Verification is public (no authentication)
 * 5. Only ACTIVE invoices with valid hash are VERIFIED
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final PDFGenerator pdfGenerator;
    private final InvoiceHashUtil hashUtil;

    /**
     * Create a new invoice (IMMUTABLE after this point)
     * 
     * Steps:
     * 1. Validate user and client
     * 2. Generate invoice number if not provided
     * 3. Build invoice entity with UUID
     * 4. Compute SHA-256 hash
     * 5. Save to database (hash becomes immutable)
     * 6. Generate PDF with QR code
     */
    @Transactional
    public String createInvoice(InvoiceRequest request) {
        System.out.println("🔐 Creating invoice for user ID: " + request.getUserId());

        // Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate client exists
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Generate invoice number if not provided
        String invoiceNumber = request.getInvoiceNumber();
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            invoiceNumber = generateInvoiceNumber(user.getId());
        }
        
        // Check for duplicate invoice number
        if (invoiceRepository.existsByUserIdAndInvoiceNumber(user.getId(), invoiceNumber)) {
            throw new IllegalArgumentException("Invoice number already exists: " + invoiceNumber);
        }

        // Generate UUID for public identification
        String uuid = UUID.randomUUID().toString();

        // Build invoice entity
        Invoice invoice = Invoice.builder()
                .uuid(uuid)
                .invoiceNumber(invoiceNumber)
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .subtotal(request.getSubtotal())
                .tax(request.getTax())
                .totalAmount(request.getTotalAmount())
                .status(InvoiceStatus.ACTIVE)
                .user(user)
                .client(client)
                .build();

        // Build line items
        List<InvoiceItems> items = request.getItems().stream().map(itemReq ->
                InvoiceItems.builder()
                        .product(itemReq.getProduct())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(itemReq.getUnitPrice())
                        .invoice(invoice)
                        .build()
        ).collect(Collectors.toList());

        invoice.setItems(items);

        // CRITICAL: Compute hash BEFORE saving
        // This hash will become IMMUTABLE after save
        String hash = hashUtil.generateHash(invoice);
        invoice.setInvoiceHash(hash);

        // Save invoice (hash is now immutable)
        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);

        System.out.println("✅ Saved invoice: " + savedInvoice.getInvoiceNumber());
        System.out.println("📌 UUID: " + savedInvoice.getUuid());
        System.out.println("🔒 Hash: " + savedInvoice.getInvoiceHash());

        // Generate PDF with QR code
        try {
            String pdfPath = "generated/invoices/invoice-" + savedInvoice.getId() + ".pdf";
            pdfGenerator.generateInvoicePdf(savedInvoice, pdfPath);
            System.out.println("✅ PDF generated at: " + pdfPath);
        } catch (Exception e) {
            System.out.println("❌ Failed to generate PDF: " + e.getMessage());
            // Log error but don't fail the transaction - PDF generation is not critical
        }

        return "Invoice created with UUID: " + uuid;
    }

    /**
     * PUBLIC VERIFICATION ENDPOINT
     * 
     * This is called by ANYONE who scans the QR code.
     * NO AUTHENTICATION required.
     * 
     * Verification logic:
     * 1. Find invoice by UUID
     * 2. If not found -> NOT_FOUND
     * 3. If found but REVOKED -> REVOKED
     * 4. Recompute hash from stored fields
     * 5. If hash matches -> VERIFIED
     * 6. If hash doesn't match -> MODIFIED
     */
    public VerificationResponse verifyInvoice(String uuid) {
        System.out.println("🔍 Verifying invoice: " + uuid);
        
        // Find invoice by UUID
        Invoice invoice = invoiceRepository.findByUuid(uuid).orElse(null);
        
        if (invoice == null) {
            return VerificationResponse.notFound();
        }
        
        // Check if revoked
        if (invoice.getStatus() == InvoiceStatus.REVOKED) {
            return VerificationResponse.revoked(
                invoice.getUser().getName(),
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getRevokedAt(),
                invoice.getRevokedReason()
            );
        }
        
        // Verify hash (check for tampering)
        boolean hashValid = hashUtil.verifyHash(invoice);
        
        if (!hashValid) {
            return VerificationResponse.modified(
                invoice.getUser().getName(),
                invoice.getInvoiceNumber()
            );
        }
        
        // Invoice is valid
        return VerificationResponse.verified(
            invoice.getUser().getName(),
            invoice.getInvoiceNumber(),
            invoice.getIssueDate(),
            invoice.getDueDate(),
            invoice.getCurrency(),
            invoice.getTotalAmount()
        );
    }

    /**
     * Revoke an invoice
     * 
     * RULES:
     * - Only the invoice owner can revoke
     * - Invoice status changes from ACTIVE to REVOKED
     * - Original data remains unchanged (audit trail)
     * - Revoked invoices fail verification
     */
    @Transactional
    public String revokeInvoice(String uuid, Long userId, RevokeInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        // Verify ownership
        if (!invoice.getUser().getId().equals(userId)) {
            throw new SecurityException("You do not own this invoice");
        }
        
        // Check if already revoked
        if (invoice.getStatus() == InvoiceStatus.REVOKED) {
            throw new IllegalStateException("Invoice is already revoked");
        }
        
        // Revoke invoice
        invoice.setStatus(InvoiceStatus.REVOKED);
        invoice.setRevokedAt(LocalDateTime.now());
        invoice.setRevokedReason(request.getReason());
        
        invoiceRepository.save(invoice);
        
        return "Invoice revoked successfully";
    }

    /**
     * Get all invoices for a user
     */
    public List<InvoiceResponse> getAllInvoicesByUser(Long userId) {
        System.out.println("📥 Fetching invoices for user ID: " + userId);
        List<Invoice> invoices = invoiceRepository.findByUser_Id(userId);
        System.out.println("📊 Invoices fetched: " + invoices.size());

        return invoices.stream()
                .map(invoice -> new InvoiceResponse(
                        invoice.getId(),
                        new ClientResponse(
                                invoice.getClient().getId(),
                                invoice.getClient().getName(),
                                invoice.getClient().getEmail(),
                                invoice.getClient().getGstin(),
                                invoice.getClient().getPhone()
                        ),
                        invoice.getTotalAmount(),
                        invoice.getCreatedAt().toLocalDate(),
                        invoice.getItems().stream()
                                .map(item -> new ItemResponse(
                                        item.getProduct(),
                                        item.getQuantity(),
                                        BigDecimal.valueOf(item.getUnitPrice())
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get invoice by internal ID (for freelancer dashboard)
     */
    public InvoiceDetailResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        Client client = invoice.getClient();

        return new InvoiceDetailResponse(
                invoice.getId(),
                new ClientResponse(
                        client.getId(),
                        client.getName(),
                        client.getEmail(),
                        client.getGstin(),
                        client.getPhone()
                ),
                invoice.getTotalAmount(),
                invoice.getCreatedAt().toLocalDate(),
                invoice.getItems().stream().map(item -> new ItemResponse(
                        item.getProduct(),
                        item.getQuantity(),
                        BigDecimal.valueOf(item.getUnitPrice())
                )).collect(Collectors.toList())
        );
    }

    /**
     * Generate unique invoice number
     */
    private String generateInvoiceNumber(Long userId) {
        return "INV-" + userId + "-" + System.currentTimeMillis();
    }

    /**
     * DEPRECATED: Invoices should NEVER be deleted
     * Use revokeInvoice() instead
     */
    @Deprecated
    public void deleteInvoice(Long id) {
        throw new UnsupportedOperationException(
            "Invoices cannot be deleted. Use revoke instead for audit trail compliance."
        );
    }
}