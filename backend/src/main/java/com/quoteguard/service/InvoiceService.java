package com.quoteguard.service;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.quoteguard.dto.ClientResponse;
import com.quoteguard.dto.InvoiceDetailResponse;
import com.quoteguard.dto.InvoiceRequest;
import com.quoteguard.dto.InvoiceResponse;
import com.quoteguard.dto.ItemResponse;
import com.quoteguard.entity.Client;
import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceItems;
import com.quoteguard.entity.User;
import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.InvoiceRepository;
import com.quoteguard.repository.UserRepository;
import com.quoteguard.utils.PDFGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceService {
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final PDFGenerator pdfGenerator;

    public String createInvoice(InvoiceRequest request) {
        System.out.println("🔐 Creating invoice for user ID: " + request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        String generatedInvoiceNumber = "INV-" + System.currentTimeMillis();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generatedInvoiceNumber)
                .issueDate(request.getIssueDate())
                .paid(request.isPaid())
                .totalAmount(BigDecimal.valueOf(request.getTotalAmount()))
                .qrToken(UUID.randomUUID().toString())
                .user(user)
                .client(client)
                .build();

        List<InvoiceItems> items = request.getItems().stream().map(itemReq ->
                InvoiceItems.builder()
                        .product(itemReq.getProduct())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(itemReq.getUnitPrice())
                        .invoice(invoice)
                        .build()
        ).collect(Collectors.toList());

        invoice.setItems(items);

        Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice); // 🔥 important

        System.out.println("✅ Saved invoice for user: " + request.getUserId());
        System.out.println("✅ Invoice ID: " + savedInvoice.getId());
        System.out.println("📌 Saved invoice user: " + savedInvoice.getUser());
        System.out.println("📌 Saved invoice user ID: " + savedInvoice.getUser().getId());

        try {
            String pdfPath = "generated/invoices/invoice-" + savedInvoice.getId() + ".pdf";

            pdfGenerator.generateInvoicePdf(savedInvoice, pdfPath);

            System.out.println("✅ PDF generated at: " + pdfPath);
            System.out.println("✅ Exists: " + new File(pdfPath).exists());
            System.out.println("✅ PDF + QR successfully generated for invoice ID: " + savedInvoice.getId());
        } catch (Exception e) {
            System.out.println("❌ Failed to generate PDF or QR: " + e.getMessage());
            e.printStackTrace();
        }

        return "Invoice created";
    }

    public List<InvoiceResponse> getAllInvoicesByUser(Long userId) {
        System.out.println("📥 Fetching invoices for user ID: " + userId);
        List<Invoice> invoices = invoiceRepository.findByUser_Id(userId);
        System.out.println("📊 Invoices fetched: " + invoices.size());

        for (Invoice invoice : invoices) {
            System.out.println("🧾 Invoice ID: " + invoice.getId() + ", User ID: " + invoice.getUser().getId());
        }

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
                        invoice.getCreatedAt(),
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
                invoice.getCreatedAt(),
                invoice.getItems().stream().map(item -> new ItemResponse(
                        item.getProduct(),
                        item.getQuantity(),
                        BigDecimal.valueOf(item.getUnitPrice())
                )).collect(Collectors.toList())
        );
    }

    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        invoiceRepository.delete(invoice);

        String baseDir = System.getProperty("user.dir");
        String pdfPath = baseDir + "/invoices/invoice-" + id + ".pdf";
        new File(pdfPath).delete();
    }

    public String verifyInvoice(String qrToken) {
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getQrToken().equals(qrToken))
                .findFirst()
                .map(invoice -> "✅ Invoice is valid for: " + invoice.getClient().getName())
                .orElse("❌ Invalid or fake invoice");
    }
}