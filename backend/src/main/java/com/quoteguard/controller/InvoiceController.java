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
import com.quoteguard.service.InvoiceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<String> createInvoice(@RequestBody InvoiceRequest request) {
        String response = invoiceService.createInvoice(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<?> verifyInvoice(@PathVariable String token) {
        try {
            InvoiceResponse response = invoiceService.verifyAndFetchInvoice(token);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ " + e.getMessage());
        }
    }
    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByUser(@RequestParam Long userId) {
        List<InvoiceResponse> invoices = invoiceService.getAllInvoicesByUser(userId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteInvoice(@PathVariable Long id) {
        try {
            invoiceService.deleteInvoice(id);
            return ResponseEntity.ok("Invoice deleted successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<String> markInvoiceAsPaid(@PathVariable Long id) {
        try {
            invoiceService.markAsPaid(id);
            return ResponseEntity.ok("Invoice marked as paid");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
