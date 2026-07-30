package com.granttrack.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Swagger / OpenAPI configuration with a global JWT bearer security scheme. */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI grantTrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GrantTrack API")
                        .description("Research Grant & Academic Output Management System — REST API")
                        .version("v1")
                        .contact(new Contact().name("GrantTrack Engineering").email("support@granttrack.example"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(new Server().url("/").description("Default")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token returned by /api/v1/auth/login")));
    }
}
