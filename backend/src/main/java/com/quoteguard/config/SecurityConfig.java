package com.quoteguard.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.quoteguard.security.AppUserDetailsService;
import com.quoteguard.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Security Configuration
 *
 * PUBLIC ENDPOINTS (No Auth):
 * - POST /api/auth/register, /api/auth/login, /api/auth/refresh, /api/auth/logout
 * - GET  /api/invoices/verify/{uuid} - public invoice verification
 * - /verify/** - frontend verification route passthrough
 * - /actuator/health/** - orchestrator/Docker liveness+readiness probes.
 *   Deliberately public: an orchestrator checking whether a container is
 *   alive cannot be expected to hold a JWT, and only "health" is exposed
 *   via management.endpoints.web.exposure.include (not the full Actuator
 *   surface - env/beans/threaddump remain unreachable regardless of auth).
 * - /swagger-ui/**, /v3/api-docs/** - API documentation. Not sensitive on
 *   its own; protected endpoints still require a real token to actually
 *   invoke from the Swagger "Try it out" UI (see OpenApiConfig).
 *
 * PROTECTED ENDPOINTS (JWT Bearer token required):
 * - All other /api/** endpoints
 *
 * Stateless: no HTTP session is created or used - every request must carry
 * its own bearer token (see JwtAuthenticationFilter). CSRF protection is
 * disabled deliberately, not carelessly: CSRF exploits rely on the browser
 * automatically attaching credentials (cookies) to a forged request. A
 * bearer token in an Authorization header is never attached automatically
 * by the browser, so the CSRF threat model does not apply to this API.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AppUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Unauthorized: a valid bearer token is required")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register", "/api/auth/login",
                                "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/invoices/verify/**").permitAll()
                        .requestMatchers("/verify/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JwtAuthenticationFilter is a @Component bean, so Spring Boot would
     * otherwise also register it directly with the servlet container as a
     * generic Filter (on top of it already running inside the security
     * filter chain via addFilterBefore() above). That duplicate copy runs
     * outside the chain built by HttpSecurity, ahead of
     * SecurityContextHolderFilter - so any authentication it sets gets wiped
     * out before AuthorizationFilter ever sees it. Disabling the servlet
     * registration leaves the filter active only where it's explicitly
     * wired in: inside the SecurityFilterChain.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        // DaoAuthenticationProvider's no-arg constructor and setUserDetailsService()
        // setter were removed in the Spring Security version pulled in by
        // spring-boot-starter-parent:4.0.0-SNAPSHOT - the UserDetailsService is now
        // a required constructor argument instead.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer(@org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:3000}") String[] allowedOrigins) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
