package com.megamart.useradminserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userAdminOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MegaMart User Admin API")
                        .description("Comprehensive API for user and admin management in MegaMart application. Includes authentication, user management, admin operations, address management, and payment methods.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MegaMart Team")
                                .email("support@megamart.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Development Server")
                ));
    }
}