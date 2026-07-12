package com.quoteguard.exception;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.quoteguard.config.SecurityConfig;
import com.quoteguard.controller.ClientController;
import com.quoteguard.entity.User;
import com.quoteguard.security.AppUserDetailsService;
import com.quoteguard.security.CustomUserDetails;
import com.quoteguard.security.JwtAuthenticationFilter;
import com.quoteguard.security.JwtService;
import com.quoteguard.service.ClientService;

/**
 * Proves GlobalExceptionHandler is wired up correctly: Bean Validation
 * failures return 400 with a field-level error map, and domain exceptions
 * return RFC 9457 application/problem+json bodies with the expected status
 * and detail - not a whitelabel error page, and not a raw stack trace.
 *
 * GlobalExceptionHandler is NOT in the @Import list below: @WebMvcTest
 * auto-detects @ControllerAdvice/@RestControllerAdvice beans as part of
 * its standard web-layer slice, the same way it auto-detects @Controller
 * beans. Explicitly importing it too would risk a duplicate bean
 * definition.
 */
@WebMvcTest(controllers = ClientController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class })
class GlobalExceptionHandlerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService userDetailsService;

    @MockitoBean
    private ClientService clientService;

    private String bearerTokenForUser1() {
        String token = jwtService.generateToken(1L, "user@example.com");
        UserDetails principal = new CustomUserDetails(
                User.builder().id(1L).email("user@example.com").role("USER").build());
        when(userDetailsService.loadUserById(1L)).thenReturn(principal);
        return "Bearer " + token;
    }

    @Test
    void invalidRequestBody_returns400ProblemDetailWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .header("Authorization", bearerTokenForUser1())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"email\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void resourceNotFound_returns404ProblemDetailWithDetailMessage() throws Exception {
        when(clientService.getClientById(anyLong(), anyLong()))
                .thenThrow(new ResourceNotFoundException("Client not found with id: 404"));

        mockMvc.perform(get("/api/clients/404")
                        .header("Authorization", bearerTokenForUser1()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Client not found with id: 404"));
    }

    @Test
    void duplicateResource_returns409ProblemDetail() throws Exception {
        when(clientService.getClientById(anyLong(), anyLong()))
                .thenThrow(new DuplicateResourceException("Invoice number already exists: INV-1-1"));

        mockMvc.perform(get("/api/clients/1")
                        .header("Authorization", bearerTokenForUser1()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
