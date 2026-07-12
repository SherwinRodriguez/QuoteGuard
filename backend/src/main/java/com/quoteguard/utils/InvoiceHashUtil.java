package com.quoteguard.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceItems;

/**
 * Utility class for generating deterministic SHA-256 hashes for invoices.
 *
 * CRITICAL RULES:
 * 1. Hash is computed ONCE at invoice creation
 * 2. Hash uses IMMUTABLE fields only
 * 3. Line items are sorted deterministically before hashing
 * 4. Formatting changes must NOT affect hash
 *
 * Money is formatted via BigDecimal.setScale(2, HALF_UP).toPlainString(),
 * not String.format("%.2f", ...). The previous String.format call used the
 * JVM's default Locale, which is locale-sensitive (some locales render a
 * decimal comma instead of a decimal point) - meaning the SAME invoice data
 * could hash differently depending purely on the server's locale
 * configuration, not on the invoice's actual content. toPlainString() is
 * always locale-independent.
 *
 * NOTE: changing this formatting mechanism changes the canonical string
 * for otherwise-identical invoices, which means it changes the resulting
 * hash. That is safe today only because there is no production data yet.
 * A live system would need a hashVersion field per invoice to dispatch to
 * the correct historical algorithm - see QUOTEGUARD_ARCHITECTURE.md
 * Section 10, a gap that remains open after this change.
 */
@Component
public class InvoiceHashUtil {

    public String generateHash(Invoice invoice) {
        try {
            String canonical = buildCanonicalString(invoice);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String buildCanonicalString(Invoice invoice) {
        StringBuilder sb = new StringBuilder();

        sb.append("user_id:").append(invoice.getUser().getId()).append("|");
        sb.append("invoice_number:").append(invoice.getInvoiceNumber()).append("|");
        sb.append("issue_date:").append(invoice.getIssueDate().toString()).append("|");
        sb.append("due_date:").append(invoice.getDueDate().toString()).append("|");
        sb.append("currency:").append(invoice.getCurrency()).append("|");
        sb.append("subtotal:").append(formatMoney(invoice.getSubtotal())).append("|");
        sb.append("tax:").append(formatMoney(invoice.getTax())).append("|");
        sb.append("total_amount:").append(formatMoney(invoice.getTotalAmount())).append("|");

        sb.append("items:[");
        List<InvoiceItems> sortedItems = invoice.getItems().stream()
                .sorted(Comparator.comparing(InvoiceItems::getProduct))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedItems.size(); i++) {
            InvoiceItems item = sortedItems.get(i);
            sb.append("{product:").append(item.getProduct())
              .append(",qty:").append(item.getQuantity())
              .append(",price:").append(formatMoney(item.getUnitPrice()))
              .append("}");
            if (i < sortedItems.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");

        return sb.toString();
    }

    private String formatMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

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
     * Verify if invoice has been tampered with. Recomputes the hash and
     * compares with the stored hash.
     *
     * @return true if hash matches (invoice is authentic), false if tampered
     */
    public boolean verifyHash(Invoice invoice) {
        String computedHash = generateHash(invoice);
        return computedHash.equals(invoice.getInvoiceHash());
    }
}
