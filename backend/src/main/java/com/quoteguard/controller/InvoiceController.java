package com.quoteguard.controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.quoteguard.dto.InvoiceDetailResponse;
import com.quoteguard.dto.InvoiceRequest;
import com.quoteguard.dto.InvoiceResponse;
import com.quoteguard.dto.RevokeInvoiceRequest;
import com.quoteguard.dto.VerificationResponse;
import com.quoteguard.service.InvoiceService;

import lombok.RequiredArgsConstructor;

/**
 * Invoice Controller
 * 
 * AUTHENTICATED ENDPOINTS (Freelancers only):
 * - POST   /api/invoices              → Create invoice
 * - GET    /api/invoices              → List user's invoices
 * - GET    /api/invoices/{id}         → Get invoice details
 * - POST   /api/invoices/{uuid}/revoke → Revoke invoice
 * - GET    /api/invoices/pdf/{id}     → Download PDF
 * 
 * PUBLIC ENDPOINTS (No auth required):
 * - GET    /api/invoices/verify/{uuid} → Verify invoice authenticity
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    /**
     * Create a new invoice (AUTHENTICATED)
     * 
     * Invoice becomes IMMUTABLE after creation.
     * A unique UUID is generated for verification.
     */
    @PostMapping
    public ResponseEntity<String> createInvoice(@RequestBody InvoiceRequest request) {
        String response = invoiceService.createInvoice(request);
        return ResponseEntity.ok(response);
    }

    /**
     * PUBLIC VERIFICATION ENDPOINT
     * 
     * Anyone can verify an invoice by UUID.
     * NO AUTHENTICATION required.
     * 
     * Returns:
     * - VERIFIED: Invoice is authentic and active
     * - REVOKED: Invoice was revoked by issuer
     * - MODIFIED: Invoice has been tampered with
     * - NOT_FOUND: Invoice does not exist (possibly fake)
     */
    @GetMapping("/verify/{uuid}")
    public ResponseEntity<VerificationResponse> verifyInvoice(@PathVariable String uuid) {
        VerificationResponse result = invoiceService.verifyInvoice(uuid);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all invoices for authenticated user
     */
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByUser(@RequestParam Long userId) {
        List<InvoiceResponse> invoices = invoiceService.getAllInvoicesByUser(userId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get invoice details by internal ID (AUTHENTICATED)
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    /**
     * Revoke an invoice (AUTHENTICATED)
     * 
     * Rules:
     * - Only owner can revoke
     * - Status changes to REVOKED
     * - Original data unchanged (audit trail)
     * - Revoked invoices fail verification
     */
    @PostMapping("/{uuid}/revoke")
    public ResponseEntity<String> revokeInvoice(
            @PathVariable String uuid,
            @RequestParam Long userId,
            @RequestBody RevokeInvoiceRequest request) {
        try {
            String response = invoiceService.revokeInvoice(uuid, userId, request);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Download invoice PDF (AUTHENTICATED)
     */
    @GetMapping("/pdf/{invoiceId}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long invoiceId) {
        try {
            String filePath = "generated/invoices/invoice-" + invoiceId + ".pdf";
            File file = new File(filePath);

            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            // Log error - in production use proper logging framework
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * DELETE ENDPOINT REMOVED
     * 
     * Invoices must NEVER be deleted (audit trail requirement).
     * Use POST /{uuid}/revoke instead.
     */
}
