package com.smartpresence.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger / OpenAPI pour la documentation interactive de l'API.
 */
@Configuration
public class OpenApiConfig {

    private static final String JWT_SCHEME = "Bearer JWT";
    private static final String API_KEY_SCHEME = "ESP32 API Key";

    @Bean
    public OpenAPI smartPresenceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartPresence API")
                        .description("Backend de gestion automatisée de présence universitaire — "
                                + "Mémoire de Master. Authentification JWT (Flutter) et clé d'API (ESP32).")
                        .version("v1")
                        .contact(new Contact()
                                .name("SmartPresence")
                                .email("contact@smartpresence.local"))
                        .license(new License().name("Academic Project")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
                                .name(JWT_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                                .name("X-API-KEY")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)));
    }
}
