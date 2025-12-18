package com.notification.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.model.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String userId = extractUserId(session);

        if (userId != null) {
            userSessions.put(userId, session);
            log.info("WebSocket connection established for user: {}", userId);
        }

        return session.receive()
                .doOnNext(message -> {
                })
                .doFinally(sig -> {
                    if (userId != null) {
                        userSessions.remove(userId);
                        log.info("WebSocket connection closed for user: {}", userId);
                    }
                })
                .then();
    }

    public Mono<Boolean> sendNotificationToUser(String userId, NotificationEvent event) {
        WebSocketSession session = userSessions.get(userId);

        if (session == null || !session.isOpen()) {
            log.warn("No active WebSocket session for user: {}", userId);
            return Mono.just(false);
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("subject", event.getSubject());
            payload.put("message", event.getMessage());
            payload.put("username", event.getUsername());
            payload.put("userName", event.getUserName());
            payload.put("destination", event.getDestination());
            payload.put("metadata", event.getMetadata());
            // explicit provider fields for convenience
            Map<String, Object> meta = event.getMetadata() != null ? event.getMetadata() : Map.of();
            payload.put("providerName", meta.getOrDefault("providerName", ""));
            payload.put("providerPhone", meta.getOrDefault("providerPhone", ""));
            payload.put("providerEmail", meta.getOrDefault("providerEmail", ""));
            payload.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(payload);

            return session.send(Mono.just(session.textMessage(jsonMessage)))
                    .thenReturn(true)
                    .onErrorResume(e -> {
                        log.error("Failed to send WebSocket message to user: {}", userId, e);
                        return Mono.just(false);
                    });

        } catch (Exception e) {
            log.error("Error serializing message for user: {}", userId, e);
            return Mono.just(false);
        }
    }

    private String extractUserId(WebSocketSession session) {
        try {
            String query = session.getHandshakeInfo().getUri().getQuery();
            if (query != null && query.contains("userId=")) {
                return query.split("userId=")[1].split("&")[0];
            }
        } catch (Exception e) {
            log.warn("Failed to extract userId from session", e);
        }
        return null;
    }

    public int getActiveConnectionsCount() {
        return userSessions.size();
    }
}