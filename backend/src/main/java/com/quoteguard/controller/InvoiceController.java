package com.quoteguard.controller;

import com.quoteguard.dto.InvoiceRequest;
import com.quoteguard.dto.InvoiceResponse;
import com.quoteguard.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
