package com.quoteguard.service;

import com.quoteguard.dto.InvoiceRequest;
import com.quoteguard.entity.Client;
import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceItems;
import com.quoteguard.entity.User;
import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.InvoiceRepository;
import com.quoteguard.repository.UserRepository;
import com.quoteguard.utils.PDFGenerator;
import com.quoteguard.utils.QRCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    public String createInvoice(InvoiceRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Invoice invoice = Invoice.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .issueDate(request.getIssueDate())
                .paid(request.isPaid())
                .totalAmount((request.getTotalAmount()))
                .qrToken(UUID.randomUUID().toString())
                .user(user)
                .client(client)
                .build();

        List<InvoiceItems> items = request.getItems().stream().map(itemReq ->
            InvoiceItems.builder()
                    .product(itemReq.getProduct())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .build()
        ).collect(Collectors.toList());

        invoice.setItems(items);
        Invoice savedInvoice =invoiceRepository.save(invoice);

        try {
            String pdfPath = "invoices/invoice-" + savedInvoice.getId() + ".pdf";
            PDFGenerator.generateInvoicePdf(savedInvoice, pdfPath);

            // 6. Generate QR Code
            String qrPath = "qrcodes/invoice-" + savedInvoice.getId() + ".png";
            QRCodeGenerator.generateQrCode(savedInvoice.getQrToken(), qrPath);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Invoice created";
    }
    public String verifyInvoice(String qrToken) {
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getQrToken().equals(qrToken))
                .findFirst()
                .map(invoice -> "✅ Invoice is valid for: " + invoice.getClient().getName())
                .orElse("❌ Invalid or fake invoice");
    }

}
