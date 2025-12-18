package com.notification.config;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI messagingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Messaging / Notification Service API")
                        .version("v1")
                        .description("APIs for sending notifications to workers and managing templates")
                        .contact(new Contact().name("Prohands")
                                .email("support@prohands.example")));
    }
}

