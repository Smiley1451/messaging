package com.notification.service.notification;

import com.notification.model.dto.NotificationEvent;
import com.notification.model.dto.NotificationSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRouterService {

    private final WhatsAppNotificationService whatsAppService;
    private final EmailNotificationService emailService;
    private final RealtimeNotificationService realtimeService;

    public Mono<Void> routeNotification(NotificationEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        log.info("Routing notification for user: {} via {}", event.getUsername(), event.getSource());

        return switch (event.getSource()) {
            case WHATSAPP -> whatsAppService.sendNotification(event);
            case EMAIL -> emailService.sendNotification(event);
            case REALTIME -> realtimeService.sendNotification(event);
            case ALL -> routeToAllChannels(event);
            default -> Mono.error(new IllegalArgumentException("Unsupported notification source: " + event.getSource()));
        };
    }

    private Mono<Void> routeToAllChannels(NotificationEvent event) {
        if (event.getDestination() == null) {
            return Mono.error(new IllegalArgumentException("Destination is required for ALL notifications"));
        }

        List<Mono<Void>> monos = new ArrayList<>();

        var dest = event.getDestination();
        if (dest.getWhatsappNumber() != null && !dest.getWhatsappNumber().isBlank()) {
            monos.add(whatsAppService.sendNotification(event));
        } else {
            log.debug("Skipping WhatsApp: no whatsapp number for user {}", event.getUsername());
        }

        if (dest.getEmail() != null && !dest.getEmail().isBlank()) {
            monos.add(emailService.sendNotification(event));
        } else {
            log.debug("Skipping Email: no email for user {}", event.getUsername());
        }

        if (dest.getUserId() != null && !dest.getUserId().isBlank()) {
            monos.add(realtimeService.sendNotification(event));
        } else {
            log.debug("Skipping Realtime: no userId for user {}", event.getUsername());
        }

        if (monos.isEmpty()) {
            return Mono.error(new IllegalArgumentException("No valid destination found for ALL notification"));
        }

        // run all selected publishers in parallel and complete when all finish
        return Mono.when(monos)
                .doOnSuccess(v -> log.info("Multi-channel notification sent for {}", event.getUsername()));
    }
}
