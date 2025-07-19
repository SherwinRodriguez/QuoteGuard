package com.quoteguard.controller;

import com.quoteguard.dto.InvoiceDetailResponse;
import com.quoteguard.dto.InvoiceRequest;
import com.quoteguard.dto.InvoiceResponse;
import com.quoteguard.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

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

    @GetMapping("/verify/{qrToken}")
    public ResponseEntity<String> verifyInvoice(@PathVariable String qrToken) {
        String result = invoiceService.verifyInvoice(qrToken);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        List<InvoiceResponse> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/pdf/{invoiceId}")
    public ResponseEntity<Resource> getInvoicePdf(@PathVariable Long invoiceId) throws IOException {
        String pdfPath = "invoices/invoice-" + invoiceId + ".pdf";
        File file = new File(pdfPath);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
