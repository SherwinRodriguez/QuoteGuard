package com.quoteguard.utils;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceItems;

import java.io.FileOutputStream;
import java.util.List;

public class PDFGenerator {
    public static void generateInvoicePdf(Invoice invoice, String outputPath) throws Exception {
        PdfWriter writer = new PdfWriter(new FileOutputStream(outputPath));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc);

        doc.add(new Paragraph("Invoice: " + invoice.getInvoiceNumber()).setBold().setFontSize(16));
        doc.add(new Paragraph("Date: " + invoice.getIssueDate()));
        doc.add(new Paragraph("Client: " + invoice.getClient().getName()));
        doc.add(new Paragraph("GSTIN: " + invoice.getClient().getGstin()));
        doc.add(new Paragraph(" "));

        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2, 2}))
                .useAllAvailableWidth();

        table.addHeaderCell("Product");
        table.addHeaderCell("Quantity");
        table.addHeaderCell("Unit Price");
        table.addHeaderCell("Total");

        List<InvoiceItems> items = invoice.getItems();
        for (InvoiceItems item : items) {
            table.addCell(item.getProduct());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(String.valueOf(item.getUnitPrice()));
            table.addCell(String.valueOf(item.getLineTotal()));
        }

        doc.add(table);

        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Total Amount: ₹" + invoice.getTotalAmount()).setBold());

        doc.close();
}
}