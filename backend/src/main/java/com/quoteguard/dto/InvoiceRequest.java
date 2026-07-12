package com.quoteguard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {

    private String invoiceNumber; // Optional - auto-generated if blank

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g. INR, USD)")
    private String currency; // e.g., "INR", "USD" - defaults to INR server-side if omitted

    // NOTE: subtotal is intentionally accepted here but NEVER trusted.
    // InvoiceService.createInvoice always recomputes it from `items` and
    // discards whatever value is submitted - see QUOTEGUARD_ARCHITECTURE.md
    // Section 11.8, the exact gap this closes.
    private BigDecimal subtotal;

    @NotNull(message = "Tax is required (use 0 if not applicable)")
    @DecimalMin(value = "0.0", message = "Tax cannot be negative")
    private BigDecimal tax;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", message = "Total amount cannot be negative")
    private BigDecimal totalAmount;

    @NotNull(message = "Client is required")
    private Long clientId;

    // NOTE: userId intentionally removed - the acting user is resolved
    // server-side from the authenticated JWT principal, never from the
    // client. See QUOTEGUARD_ARCHITECTURE.md Section 11.2.

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<InvoiceItemRequest> items;
}
