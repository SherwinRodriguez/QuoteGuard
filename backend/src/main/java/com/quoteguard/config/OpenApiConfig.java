package com.quoteguard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Registers a JWT "bearer" security scheme so Swagger UI shows an
 * "Authorize" button: paste an access token from POST /api/auth/login once,
 * and every subsequent "Try it out" call on a protected endpoint carries it
 * automatically. Without this, exercising any authenticated endpoint from
 * Swagger UI would require manually adding the Authorization header on
 * every single request.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI quoteGuardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("QuoteGuard API")
                        .description("Tamper-proof invoice and quotation verification platform. "
                                + "Every invoice is hashed (SHA-256) at issuance, embedded in a QR-coded "
                                + "PDF pointing at the public verification endpoint, and can be revoked "
                                + "but never deleted.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
