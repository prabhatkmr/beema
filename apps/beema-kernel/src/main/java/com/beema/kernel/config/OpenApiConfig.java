package com.beema.kernel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI beemaKernelOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Beema Kernel API")
                        .description("Beema Unified Insurance Platform - Kernel Service API")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Beema Platform Team")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token authentication"))
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl("${OAUTH2_AUTH_URL:https://auth.beema.local/realms/beema/protocol/openid-connect/auth}")
                                                .tokenUrl("${OAUTH2_TOKEN_URL:https://auth.beema.local/realms/beema/protocol/openid-connect/token}")))))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
