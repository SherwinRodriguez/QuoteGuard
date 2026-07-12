package com.quoteguard.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quoteguard.dto.InvoiceRequest;
import com.quoteguard.dto.InvoiceResponse;
import com.quoteguard.dto.RevokeInvoiceRequest;
import com.quoteguard.dto.VerificationResponse;
import com.quoteguard.service.InvoiceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Invoice Controller
 *
 * AUTHENTICATED ENDPOINTS (ownership-checked, identity from JWT):
 * - POST   /api/invoices              -> Create invoice
 * - GET    /api/invoices              -> List caller's invoices
 * - GET    /api/invoices/{id}         -> Get invoice details
 * - POST   /api/invoices/{uuid}/revoke -> Revoke invoice
 * - GET    /api/invoices/pdf/{id}     -> Download PDF
 *
 * PUBLIC ENDPOINT (no auth):
 * - GET    /api/invoices/verify/{uuid} -> Verify invoice authenticity
 *
 * Error handling: exceptions thrown by InvoiceService (ResourceNotFoundException,
 * DuplicateResourceException, AccessDeniedException, IllegalStateException,
 * IllegalArgumentException) are handled centrally by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<String> createInvoice(
            @Valid @RequestBody InvoiceRequest request,
            @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(request, currentUserId));
    }

    /**
     * PUBLIC VERIFICATION ENDPOINT - no authentication.
     * Returns VERIFIED / REVOKED / MODIFIED / NOT_FOUND.
     */
    @GetMapping("/verify/{uuid}")
    public ResponseEntity<VerificationResponse> verifyInvoice(@PathVariable String uuid) {
        return ResponseEntity.ok(invoiceService.verifyInvoice(uuid));
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByUser(
            @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        return ResponseEntity.ok(invoiceService.getAllInvoicesByUser(currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id, currentUserId));
    }

    @PostMapping("/{uuid}/revoke")
    public ResponseEntity<String> revokeInvoice(
            @PathVariable String uuid,
            @AuthenticationPrincipal(expression = "id") Long currentUserId,
            @Valid @RequestBody RevokeInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.revokeInvoice(uuid, currentUserId, request));
    }

    /**
     * Download invoice PDF (AUTHENTICATED, ownership-checked).
     *
     * assertOwnership is called before any file I/O and its exceptions are
     * deliberately NOT caught here - they flow to GlobalExceptionHandler
     * (404 for missing, 403 for not-yours). Only genuine file-I/O failures
     * are handled locally, since those are specific to this endpoint.
     */
    @GetMapping("/pdf/{invoiceId}")
    public ResponseEntity<Resource> downloadPdf(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal(expression = "id") Long currentUserId) {
        invoiceService.assertOwnership(invoiceId, currentUserId);

        String filePath = invoiceService.getPdfPath(invoiceId);
        File file = new File(filePath);

        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * DELETE ENDPOINT REMOVED - Invoices must NEVER be deleted (audit trail
     * requirement). Use POST /{uuid}/revoke instead.
     */
}
