package com.quoteguard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.quoteguard.dto.InvoiceItemRequest;
import com.quoteguard.dto.InvoiceRequest;
import com.quoteguard.dto.RevokeInvoiceRequest;
import com.quoteguard.entity.Client;
import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceStatus;
import com.quoteguard.entity.User;
import com.quoteguard.exception.DuplicateResourceException;
import com.quoteguard.exception.ResourceNotFoundException;
import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.InvoiceRepository;
import com.quoteguard.repository.UserRepository;
import com.quoteguard.utils.InvoiceHashUtil;
import com.quoteguard.utils.PDFGenerator;

/**
 * Pure unit tests (Mockito, no database) for the financial-integrity and
 * ownership rules enforced in InvoiceService.createInvoice/revokeInvoice.
 * See InvoiceServiceOwnershipTest for read-path (getInvoiceById/
 * assertOwnership) ownership coverage - this class covers the write path.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceCreationTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PDFGenerator pdfGenerator;
    @Mock
    private InvoiceHashUtil hashUtil;

    private InvoiceService invoiceService;
    private User owner;
    private Client ownedClient;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(clientRepository, userRepository, invoiceRepository, pdfGenerator, hashUtil);

        owner = User.builder().id(1L).email("owner@example.com").name("Owner").role("USER").build();

        ownedClient = new Client();
        ownedClient.setId(5L);
        ownedClient.setName("Acme Corp");
        ownedClient.setUser(owner);
    }

    private InvoiceRequest.InvoiceRequestBuilder validRequestBuilder() {
        InvoiceItemRequest item = InvoiceItemRequest.builder()
                .product("Consulting")
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .build();

        // subtotal = 2 * 50.00 = 100.00, tax = 10.00, so a correct totalAmount is 110.00
        return InvoiceRequest.builder()
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .currency("INR")
                .clientId(5L)
                .tax(new BigDecimal("10.00"))
                .items(List.of(item));
    }

    @Test
    void createInvoice_computesSubtotalServerSideAndIgnoresClientSubmittedValue() {
        InvoiceRequest request = validRequestBuilder()
                .subtotal(new BigDecimal("999999.00")) // must be ignored entirely
                .totalAmount(new BigDecimal("110.00"))  // matches the SERVER-computed 100.00 + 10.00
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(clientRepository.findById(5L)).thenReturn(Optional.of(ownedClient));
        when(invoiceRepository.existsByUserIdAndInvoiceNumber(any(), any())).thenReturn(false);
        when(hashUtil.generateHash(any())).thenReturn("deadbeef");
        when(invoiceRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(100L);
            i.setCreatedAt(LocalDateTime.now());
            return i;
        });

        String result = invoiceService.createInvoice(request, 1L);

        assertThat(result).contains("Invoice created");
    }

    @Test
    void createInvoice_rejectsMismatchedTotalAmount() {
        InvoiceRequest request = validRequestBuilder()
                .totalAmount(new BigDecimal("999.99")) // does not equal 100.00 + 10.00
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(clientRepository.findById(5L)).thenReturn(Optional.of(ownedClient));

        assertThatThrownBy(() -> invoiceService.createInvoice(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void createInvoice_rejectsClientOwnedBySomeoneElse() {
        User someoneElse = User.builder().id(2L).email("other@example.com").role("USER").build();
        Client notMyClient = new Client();
        notMyClient.setId(5L);
        notMyClient.setUser(someoneElse);

        InvoiceRequest request = validRequestBuilder().totalAmount(new BigDecimal("110.00")).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(clientRepository.findById(5L)).thenReturn(Optional.of(notMyClient));

        assertThatThrownBy(() -> invoiceService.createInvoice(request, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createInvoice_rejectsDuplicateInvoiceNumberForSameUser() {
        InvoiceRequest request = validRequestBuilder()
                .invoiceNumber("INV-1-CUSTOM")
                .totalAmount(new BigDecimal("110.00"))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(clientRepository.findById(5L)).thenReturn(Optional.of(ownedClient));
        when(invoiceRepository.existsByUserIdAndInvoiceNumber(1L, "INV-1-CUSTOM")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.createInvoice(request, 1L))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void revokeInvoice_ownerCanRevokeAnActiveInvoice() {
        Invoice invoice = Invoice.builder()
                .id(100L).uuid("uuid-1").invoiceNumber("INV-1-1")
                .status(InvoiceStatus.ACTIVE).user(owner).client(ownedClient)
                .build();
        when(invoiceRepository.findByUuid("uuid-1")).thenReturn(Optional.of(invoice));

        String result = invoiceService.revokeInvoice("uuid-1", 1L, new RevokeInvoiceRequest("Client requested cancellation"));

        assertThat(result).contains("revoked");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REVOKED);
        assertThat(invoice.getRevokedReason()).isEqualTo("Client requested cancellation");
    }

    @Test
    void revokeInvoice_nonOwnerIsRejected() {
        Invoice invoice = Invoice.builder()
                .id(100L).uuid("uuid-1").invoiceNumber("INV-1-1")
                .status(InvoiceStatus.ACTIVE).user(owner).client(ownedClient)
                .build();
        when(invoiceRepository.findByUuid("uuid-1")).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.revokeInvoice("uuid-1", 999L, new RevokeInvoiceRequest("not mine")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void revokeInvoice_alreadyRevokedIsRejected() {
        Invoice invoice = Invoice.builder()
                .id(100L).uuid("uuid-1").invoiceNumber("INV-1-1")
                .status(InvoiceStatus.REVOKED).user(owner).client(ownedClient)
                .build();
        when(invoiceRepository.findByUuid("uuid-1")).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.revokeInvoice("uuid-1", 1L, new RevokeInvoiceRequest("again")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void revokeInvoice_missingInvoiceIsRejectedWithNotFound() {
        when(invoiceRepository.findByUuid("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.revokeInvoice("ghost", 1L, new RevokeInvoiceRequest("n/a")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
