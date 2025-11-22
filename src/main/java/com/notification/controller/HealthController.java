package com.notification.controller;

import com.notification.service.notification.WebSocketNotificationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final WebSocketNotificationHandler webSocketHandler;

    @GetMapping
    public Mono<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "unified-notification-service");
        health.put("activeWebSocketConnections", webSocketHandler.getActiveConnectionsCount());
        health.put("timestamp", System.currentTimeMillis());
        
        return Mono.just(health);
    }
}
