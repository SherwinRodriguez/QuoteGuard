package com.quoteguard.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

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

@Component
public class PDFGenerator {

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

        // Header
        document.add(new Paragraph("Invoice", bold));
        document.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber(), regular));
        document.add(new Paragraph("Client: " + invoice.getClient().getName(), regular));
        document.add(new Paragraph("Email: " + invoice.getClient().getEmail(), regular));
        document.add(new Paragraph("Issue Date: " + invoice.getIssueDate(), regular));
        document.add(new Paragraph("Total Amount: ₹" + invoice.getTotalAmount(), regular));
        document.add(new Paragraph(" "));

        // Items Table
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);

        table.addCell("Product");
        table.addCell("Quantity");
        table.addCell("Unit Price");

        List<InvoiceItems> items = invoice.getItems();
        for (InvoiceItems item : items) {
            table.addCell(item.getProduct());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell("₹" + item.getUnitPrice());
        }

        document.add(table);
        document.add(new Paragraph(" "));

        // Generate and embed QR Code
        BufferedImage qrImage = generateQrCodeImage(invoice.getQrToken());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        Image qr = Image.getInstance(baos.toByteArray());
        qr.scaleToFit(100, 100);
        qr.setAlignment(Element.ALIGN_RIGHT);

        document.add(new Paragraph("Scan to verify invoice:"));
        document.add(qr);

        document.close();
    }

    private BufferedImage generateQrCodeImage(String text) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }
}
