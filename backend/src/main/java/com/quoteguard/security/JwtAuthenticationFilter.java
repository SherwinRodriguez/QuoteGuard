package com.quoteguard.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Reads the "Authorization: Bearer &lt;token&gt;" header, validates it, and -
 * if valid - populates the SecurityContext so downstream code can resolve
 * the authenticated user.
 *
 * This filter never rejects a request itself. A missing or invalid token
 * simply leaves the SecurityContext unauthenticated; whether that's
 * acceptable for the requested path is decided by the authorization rules
 * in SecurityConfig (e.g. /api/auth/** and /api/invoices/verify/** stay
 * public and never need a token in the first place).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            Long userId = jwtService.extractUserId(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserById(userId);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // Invalid, expired, or forged token: leave the SecurityContext empty.
            // authorizeHttpRequests() rejects this as unauthenticated on any
            // protected route. The response shape here is intentionally the
            // Spring Boot default for now - Phase B's global exception handler
            // will standardize every error response into a consistent shape.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
