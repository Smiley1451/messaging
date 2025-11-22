package com.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@EnableR2dbcRepositories
@EnableAsync
public class NotificationServiceApplication {

    public static void main(String[] args) {
        // Enable Reactor debugging (disable in production)
        Hooks.onOperatorDebug();
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
