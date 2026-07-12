package com.quoteguard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import com.quoteguard.dto.ClientResponse;
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
import com.quoteguard.exception.DuplicateResourceException;
import com.quoteguard.exception.ResourceNotFoundException;
import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.InvoiceRepository;
import com.quoteguard.repository.UserRepository;
import com.quoteguard.utils.InvoiceHashUtil;
import com.quoteguard.utils.PDFGenerator;

import lombok.RequiredArgsConstructor;

/**
 * Invoice Service
 *
 * CRITICAL RULES:
 * 1. Invoices are IMMUTABLE after creation
 * 2. Hash is generated ONCE and NEVER updated
 * 3. Invoices are NEVER deleted (use revoke instead)
 * 4. Verification is public (no authentication)
 * 5. Only ACTIVE invoices with a valid hash are VERIFIED
 * 6. subtotal is ALWAYS computed server-side from line items and is never
 *    trusted from the client; totalAmount is validated against
 *    subtotal + tax and rejected if inconsistent.
 *
 * DISCLOSED LIMITATION: tax is accepted as submitted, not independently
 * re-derived. The current domain model has no per-client/jurisdiction tax
 * rate configuration, so the server has no ground truth to check the tax
 * AMOUNT against - only that the arithmetic (subtotal + tax = total) is
 * internally consistent. Full tax correctness would require accepting and
 * validating a tax RATE against a server-held configuration, which does
 * not exist in this domain model and is out of scope here (see
 * QUOTEGUARD_ARCHITECTURE.md Section 11.8 for the full discussion).
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private static final String DEFAULT_CURRENCY = "INR";
    private static final String INVOICE_NUMBER_PREFIX = "INV-";

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final PDFGenerator pdfGenerator;
    private final InvoiceHashUtil hashUtil;

    /**
     * Create a new invoice (IMMUTABLE after this point).
     */
    @Transactional
    public String createInvoice(InvoiceRequest request, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists: " + currentUserId));

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + request.getClientId()));

        // Ownership check on the CLIENT, not just the acting user's own
        // identity: without this, a caller could attach an invoice to a
        // client record they don't own. This was missed in the earlier
        // ownership-validation pass and caught during this review.
        if (!client.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not own this client");
        }

        String invoiceNumber = request.getInvoiceNumber();
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            invoiceNumber = generateInvoiceNumber(user.getId());
        }

        if (invoiceRepository.existsByUserIdAndInvoiceNumber(user.getId(), invoiceNumber)) {
            throw new DuplicateResourceException("Invoice number already exists: " + invoiceNumber);
        }

        BigDecimal computedSubtotal = computeSubtotal(request);
        BigDecimal tax = request.getTax().setScale(2, RoundingMode.HALF_UP);
        BigDecimal computedTotal = computedSubtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        BigDecimal submittedTotal = request.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

        if (computedTotal.compareTo(submittedTotal) != 0) {
            throw new IllegalArgumentException(
                    "totalAmount does not match subtotal + tax computed from line items. Expected "
                            + computedTotal + " but received " + submittedTotal);
        }

        String uuid = UUID.randomUUID().toString();

        Invoice invoice = Invoice.builder()
                .uuid(uuid)
                .invoiceNumber(invoiceNumber)
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .currency(request.getCurrency() != null ? request.getCurrency() : DEFAULT_CURRENCY)
                .subtotal(computedSubtotal)
                .tax(tax)
                .totalAmount(computedTotal)
                .status(InvoiceStatus.ACTIVE)
                .user(user)
                .client(client)
                .build();

        List<InvoiceItems> items = request.getItems().stream().map(itemReq ->
                InvoiceItems.builder()
                        .product(itemReq.getProduct())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(itemReq.getUnitPrice().setScale(2, RoundingMode.HALF_UP))
                        .invoice(invoice)
                        .build()
        ).collect(Collectors.toList());

        invoice.setItems(items);

        // CRITICAL: hash computed from server-derived values, BEFORE saving.
        // This hash becomes IMMUTABLE after save.
        String hash = hashUtil.generateHash(invoice);
        invoice.setInvoiceHash(hash);

        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);
        log.info("Created invoice {} (uuid={}) for user {}", savedInvoice.getInvoiceNumber(), savedInvoice.getUuid(), currentUserId);

        try {
            String pdfPath = pdfGenerator.buildPdfPath(savedInvoice.getId());
            pdfGenerator.generateInvoicePdf(savedInvoice, pdfPath);
        } catch (Exception e) {
            // PDF generation failure does not fail the transaction: the
            // invoice record and its hash are the source of truth, and the
            // PDF is a regenerable artifact. This is a real durability gap
            // (a failed PDF is currently unrecoverable without a retry
            // mechanism) - see QUOTEGUARD_ARCHITECTURE.md Section 11.4/11.5.
            log.error("Failed to generate PDF for invoice {}", savedInvoice.getId(), e);
        }

        return "Invoice created with UUID: " + uuid;
    }

    private BigDecimal computeSubtotal(InvoiceRequest request) {
        return request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * PUBLIC VERIFICATION ENDPOINT - called by anyone who scans the QR code, no auth.
     *
     * @Transactional(readOnly = true) is required here, not optional: with
     * spring.jpa.open-in-view=false, hashUtil.verifyHash() below accesses
     * invoice.getItems(), a LAZY collection. Without an open
     * transaction/session at this point that access throws
     * LazyInitializationException - i.e. disabling OSIV without this
     * annotation would have silently broken the single most important
     * endpoint in the application.
     */
    @Transactional(readOnly = true)
    public VerificationResponse verifyInvoice(String uuid) {
        Invoice invoice = invoiceRepository.findByUuid(uuid).orElse(null);

        if (invoice == null) {
            return VerificationResponse.notFound();
        }

        if (invoice.getStatus() == InvoiceStatus.REVOKED) {
            return VerificationResponse.revoked(
                    invoice.getUser().getName(),
                    invoice.getInvoiceNumber(),
                    invoice.getIssueDate(),
                    invoice.getRevokedAt(),
                    invoice.getRevokedReason()
            );
        }

        boolean hashValid = hashUtil.verifyHash(invoice);

        if (!hashValid) {
            return VerificationResponse.modified(invoice.getUser().getName(), invoice.getInvoiceNumber());
        }

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
     * Revoke an invoice. Only the owner can revoke; status ACTIVE -> REVOKED
     * only; original data is never altered (audit trail).
     */
    @Transactional
    public String revokeInvoice(String uuid, Long currentUserId, RevokeInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not own this invoice");
        }

        if (invoice.getStatus() == InvoiceStatus.REVOKED) {
            throw new IllegalStateException("Invoice is already revoked");
        }

        invoice.setStatus(InvoiceStatus.REVOKED);
        invoice.setRevokedAt(LocalDateTime.now());
        invoice.setRevokedReason(request.getReason());

        invoiceRepository.save(invoice);
        log.info("Revoked invoice {} (uuid={}) by user {}", invoice.getInvoiceNumber(), uuid, currentUserId);

        return "Invoice revoked successfully";
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoicesByUser(Long userId) {
        return invoiceRepository.findByUser_Id(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get invoice by internal ID (for the freelancer dashboard). Enforces
     * ownership: 404 for invoices that don't exist, 403 for invoices that
     * exist but belong to someone else. Previously this method had no
     * ownership check at all.
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id, Long currentUserId) {
        return toResponse(findOwnedInvoice(id, currentUserId));
    }

    /**
     * Confirms the invoice exists and is owned by the caller, without
     * paying for a full DTO mapping. Used by the PDF download endpoint,
     * which previously had NO ownership check at all.
     */
    @Transactional(readOnly = true)
    public void assertOwnership(Long invoiceId, Long currentUserId) {
        findOwnedInvoice(invoiceId, currentUserId);
    }

    private Invoice findOwnedInvoice(Long id, Long currentUserId) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));

        if (!invoice.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not own this invoice");
        }

        return invoice;
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        Client client = invoice.getClient();

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getUuid(),
                invoice.getInvoiceNumber(),
                new ClientResponse(client.getId(), client.getName(), client.getEmail(), client.getGstin(), client.getPhone()),
                invoice.getCurrency(),
                invoice.getSubtotal(),
                invoice.getTax(),
                invoice.getTotalAmount(),
                invoice.getStatus(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getCreatedAt().toLocalDate(),
                invoice.getRevokedAt(),
                invoice.getRevokedReason(),
                invoice.getItems().stream()
                        .map(item -> new ItemResponse(item.getProduct(), item.getQuantity(), item.getUnitPrice()))
                        .collect(Collectors.toList())
        );
    }

    private String generateInvoiceNumber(Long userId) {
        return INVOICE_NUMBER_PREFIX + userId + "-" + System.currentTimeMillis();
    }

    /**
     * Resolves the on-disk path for an invoice's PDF, delegating to
     * PDFGenerator's single source of truth so this and the download
     * endpoint can never disagree on where a file lives.
     */
    public String getPdfPath(Long invoiceId) {
        return pdfGenerator.buildPdfPath(invoiceId);
    }

    /**
     * DEPRECATED: Invoices should NEVER be deleted. Use revokeInvoice() instead.
     */
    @Deprecated
    public void deleteInvoice(Long id) {
        throw new UnsupportedOperationException(
                "Invoices cannot be deleted. Use revoke instead for audit trail compliance."
        );
    }
}
