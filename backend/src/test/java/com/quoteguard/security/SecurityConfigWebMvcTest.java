package com.quoteguard.security;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.quoteguard.config.SecurityConfig;
import com.quoteguard.controller.ClientController;
import com.quoteguard.controller.InvoiceController;
import com.quoteguard.dto.VerificationResponse;
import com.quoteguard.entity.User;
import com.quoteguard.service.ClientService;
import com.quoteguard.service.InvoiceService;

/**
 * Verifies the authorization rules configured in SecurityConfig at the web
 * layer, without requiring a live database:
 * - protected endpoints must reject requests with no token, or an invalid one
 * - protected endpoints must accept requests with a valid token
 * - explicitly public endpoints must remain reachable without any token
 *
 * Full end-to-end integration tests (real DB, real login flow) belong to
 * Phase E - this slice test only needs the web + security layers, so it has
 * no external infrastructure dependency and runs in any environment,
 * including CI, with no setup.
 */
@WebMvcTest(controllers = { ClientController.class, InvoiceController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class })
class SecurityConfigWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService userDetailsService;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/clients").param("userId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/clients").param("userId", "1")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_isAuthenticatedAndReturnsOk() throws Exception {
        String token = jwtService.generateToken(1L, "user@example.com");
        UserDetails principal = new CustomUserDetails(
                User.builder().id(1L).email("user@example.com").role("USER").build());

        when(userDetailsService.loadUserById(1L)).thenReturn(principal);
        when(clientService.getClientsByUser(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/clients").param("userId", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void publicVerifyEndpoint_isReachableWithoutToken() throws Exception {
        when(invoiceService.verifyInvoice(anyString())).thenReturn(VerificationResponse.notFound());

        mockMvc.perform(get("/api/invoices/verify/some-uuid"))
                .andExpect(status().isOk());
    }

    /**
     * Proves the ownership-check wiring end to end at the web layer: when a
     * service method throws AccessDeniedException (as ClientService/
     * InvoiceService now do for cross-user access), the response must be
     * 403, not 500 and not 200. This specifically guards against the
     * catch-ordering bug where a broad "catch (Exception e)" in a
     * controller would silently swallow AccessDeniedException - see the
     * comments in InvoiceController.downloadPdf/revokeInvoice.
     */
    @Test
    void ownershipDenied_returns403NotAWhitelabel500() throws Exception {
        String token = jwtService.generateToken(1L, "user@example.com");
        UserDetails principal = new CustomUserDetails(
                User.builder().id(1L).email("user@example.com").role("USER").build());

        when(userDetailsService.loadUserById(1L)).thenReturn(principal);
        when(clientService.getClientById(anyLong(), anyLong()))
                .thenThrow(new AccessDeniedException("You do not own this client"));

        mockMvc.perform(get("/api/clients/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
