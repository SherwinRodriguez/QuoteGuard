package com.quoteguard.utils;

import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceItems;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for generating deterministic SHA-256 hashes for invoices.
 * 
 * CRITICAL RULES:
 * 1. Hash is computed ONCE at invoice creation
 * 2. Hash uses IMMUTABLE fields only
 * 3. Line items are sorted deterministically before hashing
 * 4. Formatting changes must NOT affect hash
 */
@Component
public class InvoiceHashUtil {

    /**
     * Generate SHA-256 hash for invoice verification.
     * 
     * Hash inputs (in order):
     * - freelancer_id (user_id)
     * - invoice_number
     * - issue_date
     * - due_date
     * - currency
     * - subtotal
     * - tax
     * - total_amount
     * - normalized line items (sorted by product name)
     */
    public String generateHash(Invoice invoice) {
        try {
            // Build canonical string representation
            String canonical = buildCanonicalString(invoice);
            
            // Compute SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            return bytesToHex(hashBytes);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Build canonical string from invoice immutable fields.
     * Line items are sorted alphabetically by product name for determinism.
     */
    private String buildCanonicalString(Invoice invoice) {
        StringBuilder sb = new StringBuilder();
        
        // User ID (freelancer)
        sb.append("user_id:").append(invoice.getUser().getId()).append("|");
        
        // Invoice number
        sb.append("invoice_number:").append(invoice.getInvoiceNumber()).append("|");
        
        // Issue date (ISO format)
        sb.append("issue_date:").append(invoice.getIssueDate().toString()).append("|");
        
        // Due date (ISO format)
        sb.append("due_date:").append(invoice.getDueDate().toString()).append("|");
        
        // Currency
        sb.append("currency:").append(invoice.getCurrency()).append("|");
        
        // Subtotal (normalized to 2 decimal places)
        sb.append("subtotal:").append(String.format("%.2f", invoice.getSubtotal())).append("|");
        
        // Tax (normalized to 2 decimal places)
        sb.append("tax:").append(String.format("%.2f", invoice.getTax())).append("|");
        
        // Total amount (normalized to 2 decimal places)
        sb.append("total_amount:").append(String.format("%.2f", invoice.getTotalAmount())).append("|");
        
        // Line items (sorted by product name for determinism)
        sb.append("items:[");
        List<InvoiceItems> sortedItems = invoice.getItems().stream()
                .sorted(Comparator.comparing(InvoiceItems::getProduct))
                .collect(Collectors.toList());
        
        for (int i = 0; i < sortedItems.size(); i++) {
            InvoiceItems item = sortedItems.get(i);
            sb.append("{product:").append(item.getProduct())
              .append(",qty:").append(item.getQuantity())
              .append(",price:").append(String.format("%.2f", item.getUnitPrice()))
              .append("}");
            if (i < sortedItems.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        
        return sb.toString();
    }

    /**
     * Convert byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Verify if invoice has been tampered with.
     * Recomputes hash and compares with stored hash.
     * 
     * @return true if hash matches (invoice is authentic), false if tampered
     */
    public boolean verifyHash(Invoice invoice) {
        String computedHash = generateHash(invoice);
        return computedHash.equals(invoice.getInvoiceHash());
    }
}
