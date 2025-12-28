package com.quoteguard.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceItems;

/**
 * PDF Generator with QR Code
 * 
 * CRITICAL QR CODE RULE:
 * QR must contain ONLY the verification URL:
 * https://<domain>/verify/{invoice_uuid}
 * 
 * DO NOT encode:
 * - Invoice amounts
 * - Client info
 * - Hash values
 * - Any other data
 */
@Component
public class PDFGenerator {

    @Value("${app.verification.base-url:http://localhost:3000}")
    private String baseUrl;

    public void generateInvoicePdf(Invoice invoice, String filePath) throws Exception {
        // Ensure the parent folders exist
        java.io.File file = new java.io.File(filePath);
        java.io.File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new Exception("Failed to create directory for PDF: " + parent.getAbsolutePath());
        }

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // Fonts
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font regular = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 10);

        // Header
        document.add(new Paragraph("INVOICE", bold));
        document.add(new Paragraph(" "));
        
        // Freelancer info
        document.add(new Paragraph("From: " + invoice.getUser().getName(), regular));
        document.add(new Paragraph("Email: " + invoice.getUser().getEmail(), regular));
        document.add(new Paragraph(" "));
        
        // Client info
        document.add(new Paragraph("Bill To:", regular));
        document.add(new Paragraph(invoice.getClient().getName(), regular));
        document.add(new Paragraph("Email: " + invoice.getClient().getEmail(), regular));
        if (invoice.getClient().getGstin() != null && !invoice.getClient().getGstin().isEmpty()) {
            document.add(new Paragraph("GSTIN: " + invoice.getClient().getGstin(), regular));
        }
        document.add(new Paragraph(" "));
        
        // Invoice details
        document.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber(), regular));
        document.add(new Paragraph("Issue Date: " + invoice.getIssueDate(), regular));
        document.add(new Paragraph("Due Date: " + invoice.getDueDate(), regular));
        document.add(new Paragraph(" "));

        // Items Table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setWidths(new float[]{4f, 1f, 2f, 2f});

        table.addCell("Product");
        table.addCell("Qty");
        table.addCell("Unit Price");
        table.addCell("Total");

        List<InvoiceItems> items = invoice.getItems();
        for (InvoiceItems item : items) {
            table.addCell(item.getProduct());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(invoice.getCurrency() + " " + String.format("%.2f", item.getUnitPrice()));
            table.addCell(invoice.getCurrency() + " " + String.format("%.2f", item.getLineTotal()));
        }

        document.add(table);
        document.add(new Paragraph(" "));

        // Totals
        document.add(new Paragraph("Subtotal: " + invoice.getCurrency() + " " + invoice.getSubtotal(), regular));
        document.add(new Paragraph("Tax: " + invoice.getCurrency() + " " + invoice.getTax(), regular));
        document.add(new Paragraph("Total Amount: " + invoice.getCurrency() + " " + invoice.getTotalAmount(), bold));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // QR Code Section
        String verificationUrl = baseUrl + "/verify/" + invoice.getUuid();
        BufferedImage qrImage = generateQrCodeImage(verificationUrl);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        Image qr = Image.getInstance(baos.toByteArray());
        qr.scaleToFit(120, 120);
        qr.setAlignment(Element.ALIGN_CENTER);

        document.add(new Paragraph("Scan to verify invoice authenticity:", small));
        document.add(qr);
        document.add(new Paragraph(verificationUrl, small));

        document.close();
    }

    /**
     * Generate QR code image from verification URL
     * 
     * CRITICAL: The text parameter MUST be the full verification URL:
     * https://<domain>/verify/{uuid}
     */
    private BufferedImage generateQrCodeImage(String text) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 250, 250);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }
}
