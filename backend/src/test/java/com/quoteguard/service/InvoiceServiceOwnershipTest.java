package com.quoteguard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.quoteguard.entity.Client;
import com.quoteguard.entity.Invoice;
import com.quoteguard.entity.InvoiceStatus;
import com.quoteguard.entity.User;
import com.quoteguard.exception.ResourceNotFoundException;
import com.quoteguard.repository.ClientRepository;
import com.quoteguard.repository.InvoiceRepository;
import com.quoteguard.repository.UserRepository;
import com.quoteguard.utils.InvoiceHashUtil;
import com.quoteguard.utils.PDFGenerator;

/**
 * Pure unit tests (Mockito, no database, no Spring context) proving the
 * ownership rule enforced by InvoiceService.findOwnedInvoice, used by both
 * getInvoiceById (dashboard detail view) and assertOwnership (PDF download
 * gate). Before this feature, neither of those two code paths checked
 * ownership at all.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceOwnershipTest {

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
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(clientRepository, userRepository, invoiceRepository, pdfGenerator, hashUtil);

        User owner = User.builder().id(1L).email("owner@example.com").name("Owner").role("USER").build();
        Client client = new Client();
        client.setId(5L);
        client.setName("Acme Corp");
        client.setEmail("client@example.com");

        invoice = Invoice.builder()
                .id(100L)
                .uuid("11111111-1111-1111-1111-111111111111")
                .invoiceNumber("INV-1-1")
                .totalAmount(BigDecimal.valueOf(500))
                .createdAt(LocalDateTime.now())
                .status(InvoiceStatus.ACTIVE)
                .items(Collections.emptyList())
                .user(owner)
                .client(client)
                .build();
    }

    @Test
    void getInvoiceById_ownerCanReadTheirOwnInvoice() {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        var response = invoiceService.getInvoiceById(100L, 1L);

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void getInvoiceById_nonOwnerIsRejectedWithAccessDenied() {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.getInvoiceById(100L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getInvoiceById_missingInvoiceIsRejectedWithNotFound() {
        when(invoiceRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(404L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assertOwnership_passesSilentlyForOwner() {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        invoiceService.assertOwnership(100L, 1L); // no exception = pass
    }

    @Test
    void assertOwnership_rejectsNonOwner_thisGatesThePdfDownloadEndpoint() {
        when(invoiceRepository.findById(100L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.assertOwnership(100L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
