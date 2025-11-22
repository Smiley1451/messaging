package com.notification.service.notification;

import com.notification.model.dto.NotificationEvent;
import com.notification.model.dto.NotificationStatus;
import com.notification.service.logging.NotificationLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeNotificationService {

    private final WebSocketNotificationHandler webSocketHandler;
    private final NotificationLoggingService loggingService;

    public Mono<Void> sendNotification(NotificationEvent event) {
        String userId = event.getDestination().getUserId();

        if (userId == null || userId.isBlank()) {
            return Mono.error(new IllegalArgumentException("User ID is required for real-time notification"));
        }

        return webSocketHandler.sendNotificationToUser(userId, event)
                .flatMap(sent -> {
                    if (sent) {
                        return loggingService.logNotification(event, NotificationStatus.SUCCESS, null, 0);
                    } else {
                        return loggingService.logNotification(event, NotificationStatus.FAILED, "User not connected", 0)
                                .then(Mono.error(new RuntimeException("User not connected via WebSocket")));
                    }
                })
                .doOnSuccess(v -> log.info("Real-time notification sent successfully to user {}", userId))
                .onErrorResume(error -> {
                    log.error("Failed to send real-time notification to user {}: {}", userId, error.getMessage());
                    return Mono.error(error);
                })
                .then();
    }
}
