package com.notification.service.notification;

import com.notification.model.dto.NotificationEvent;
import com.notification.model.dto.NotificationSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRouterService {

    private final WhatsAppNotificationService whatsAppService;
    private final EmailNotificationService emailService;
    private final RealtimeNotificationService realtimeService;

    public Mono<Void> routeNotification(NotificationEvent event) {
        log.info("Routing notification for user: {} via {}", event.getUsername(), event.getSource());

        return switch (event.getSource()) {
            case WHATSAPP -> whatsAppService.sendNotification(event);
            case EMAIL -> emailService.sendNotification(event);
            case REALTIME -> realtimeService.sendNotification(event);
            default -> Mono.error(new IllegalArgumentException("Unsupported notification source: " + event.getSource()));
        };
    }
}
