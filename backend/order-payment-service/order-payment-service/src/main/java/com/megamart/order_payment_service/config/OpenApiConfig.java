package com.megamart.order_payment_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Payment Service API")
                        .version("1.0.0")
                        .description("API for managing orders and payments in MegaMart")
                        .contact(new Contact()
                                .name("MegaMart Team")
                                .email("support@megamart.com")));
    }
}