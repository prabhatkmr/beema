package com.beema.kernel.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration.
 *
 * Access documentation at: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kernelOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Beema Unified Agreement Kernel API")
                .description("""
                    Bitemporal, metadata-driven insurance agreement system.

                    Features:
                    - Bitemporal tracking (valid time + transaction time)
                    - JSONB flex-schema for market-specific attributes
                    - Multi-context support (RETAIL, COMMERCIAL, LONDON_MARKET)
                    - Multi-tenancy with row-level security
                    - Metadata-driven validation
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Beema Platform Team")
                    .email("platform@beema.com")
                    .url("https://beema.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://beema.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local development"),
                new Server()
                    .url("https://api.beema.com")
                    .description("Production")
            ));
    }
}
