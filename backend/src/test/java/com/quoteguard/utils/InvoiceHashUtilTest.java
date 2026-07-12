package com.quoteguard.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.quoteguard.entity.Client;
import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceItems;
import com.quoteguard.entity.InvoiceStatus;
import com.quoteguard.entity.User;

/**
 * This is the test that matters most for QuoteGuard's actual value
 * proposition: it proves the tamper-detection guarantee genuinely works,
 * not just that the code compiles. Also covers the BigDecimal migration
 * done in this phase - the hash must remain deterministic and
 * order-independent after switching unitPrice from double to BigDecimal
 * and the formatting mechanism from locale-sensitive String.format to
 * BigDecimal.toPlainString().
 */
class InvoiceHashUtilTest {

    private final InvoiceHashUtil hashUtil = new InvoiceHashUtil();

    @Test
    void generateHash_isDeterministic_forIdenticalInvoices() {
        Invoice a = buildInvoice();
        Invoice b = buildInvoice();

        assertThat(hashUtil.generateHash(a)).isEqualTo(hashUtil.generateHash(b));
    }

    @Test
    void generateHash_isIndependentOfLineItemOrder() {
        Invoice a = buildInvoice();
        Invoice b = buildInvoice();

        List<InvoiceItems> reversed = new ArrayList<>(b.getItems());
        Collections.reverse(reversed);
        b.setItems(reversed);

        assertThat(hashUtil.generateHash(a)).isEqualTo(hashUtil.generateHash(b));
    }

    @Test
    void verifyHash_passesForUntamperedInvoice() {
        Invoice invoice = buildInvoice();
        invoice.setInvoiceHash(hashUtil.generateHash(invoice));

        assertThat(hashUtil.verifyHash(invoice)).isTrue();
    }

    @Test
    void verifyHash_detectsTamperingWithTotalAmount() {
        Invoice invoice = buildInvoice();
        invoice.setInvoiceHash(hashUtil.generateHash(invoice));

        invoice.setTotalAmount(invoice.getTotalAmount().add(BigDecimal.ONE));

        assertThat(hashUtil.verifyHash(invoice)).isFalse();
    }

    @Test
    void verifyHash_detectsTamperingWithLineItemPrice() {
        Invoice invoice = buildInvoice();
        invoice.setInvoiceHash(hashUtil.generateHash(invoice));

        invoice.getItems().get(0).setUnitPrice(new BigDecimal("999.99"));

        assertThat(hashUtil.verifyHash(invoice)).isFalse();
    }

    @Test
    void verifyHash_detectsTamperingWithInvoiceNumber() {
        Invoice invoice = buildInvoice();
        invoice.setInvoiceHash(hashUtil.generateHash(invoice));

        invoice.setInvoiceNumber("INV-1-FORGED");

        assertThat(hashUtil.verifyHash(invoice)).isFalse();
    }

    private Invoice buildInvoice() {
        User user = User.builder().id(1L).name("Freelancer").email("freelancer@example.com").build();
        Client client = new Client();
        client.setId(2L);

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-1-1")
                .issueDate(LocalDate.of(2026, 1, 1))
                .dueDate(LocalDate.of(2026, 1, 31))
                .currency("INR")
                .subtotal(new BigDecimal("300.00"))
                .tax(new BigDecimal("54.00"))
                .totalAmount(new BigDecimal("354.00"))
                .status(InvoiceStatus.ACTIVE)
                .user(user)
                .client(client)
                .build();

        InvoiceItems item1 = InvoiceItems.builder()
                .product("Widget").quantity(2).unitPrice(new BigDecimal("100.00")).invoice(invoice).build();
        InvoiceItems item2 = InvoiceItems.builder()
                .product("Gadget").quantity(1).unitPrice(new BigDecimal("100.00")).invoice(invoice).build();

        invoice.setItems(List.of(item1, item2));
        return invoice;
    }
}
