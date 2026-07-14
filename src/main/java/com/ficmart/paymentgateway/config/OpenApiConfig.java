package com.ficmart.paymentgateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Ficmart Payment Gateway API",
                version = "1.0.0",
                description = "REST API for payment authorization, capture, void, refund, idempotency, and payment retrieval.",
                contact = @Contact(
                        name = "Chukwuebuka Christopher",
                        email = "victorchris73@gmail.com.com"
                ),
                license = @License(
                        name = "MIT License"
                )
        )
)
public class OpenApiConfig {
}