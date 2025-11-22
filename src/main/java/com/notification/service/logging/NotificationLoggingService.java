package com.notification.service.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.model.dto.NotificationEvent;
import com.notification.model.dto.NotificationStatus;
import com.notification.model.entity.NotificationLog;
import com.notification.repository.NotificationLogRepository;
import io.r2dbc.postgresql.codec.Json; // Import R2DBC Json type
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationLoggingService {

    private final NotificationLogRepository repository;
    private final ObjectMapper objectMapper;

    public Mono<NotificationLog> logNotification(
            NotificationEvent event,
            NotificationStatus status,
            String errorMessage,
            int retryCount) {

        return Mono.fromCallable(() -> {
                    String destination = getDestinationString(event);


                    String metadataString = event.getMetadata() != null
                            ? objectMapper.writeValueAsString(event.getMetadata())
                            : null;

                    Json metadataJson = metadataString != null
                            ? Json.of(metadataString)
                            : null;

                    return NotificationLog.builder()
                            .userName(event.getUserName())
                            .username(event.getUsername())
                            .subject(event.getSubject())
                            .source(event.getSource().getValue())
                            .destination(destination)
                            .message(event.getMessage())
                            .metadata(metadataJson) // Pass the Json object
                            .status(status.name())
                            .retryCount(retryCount)
                            .errorMessage(errorMessage)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                })
                .flatMap(repository::save)
                .doOnSuccess(saved -> log.debug("Logged notification with ID: {}", saved.getId()))
                .doOnError(error -> log.error("Failed to log notification", error));
    }

    private String getDestinationString(NotificationEvent event) {
        return switch (event.getSource()) {
            case WHATSAPP -> event.getDestination().getWhatsappNumber();
            case EMAIL -> event.getDestination().getEmail();
            case REALTIME -> event.getDestination().getUserId();
        };
    }
}