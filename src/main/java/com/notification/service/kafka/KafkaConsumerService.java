package com.notification.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.model.dto.NotificationEvent;
import com.notification.service.notification.NotificationRouterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final NotificationRouterService notificationRouterService;
    private final KafkaDeadLetterService deadLetterService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void startConsuming() {
        kafkaReceiver.receive()
                .flatMap(this::processRecord)
                .subscribe(
                        result -> log.info("Successfully processed notification"),
                        error -> log.error("Error in Kafka consumer stream", error)
                );
    }

    private Mono<Void> processRecord(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> {
                    log.info("Received message: key={}, partition={}, offset={}",
                            record.key(), record.partition(), record.offset());
                    NotificationEvent event = objectMapper.readValue(record.value(), NotificationEvent.class);

                    // Normalize metadata keys to make downstream processing consistent.
                    Map<String, Object> meta = event.getMetadata();
                    if (meta == null) {
                        meta = new HashMap<>();
                    } else {
                        meta = new HashMap<>(meta); // copy to mutate safely
                    }

                    // Support both camelCase and snake_case incoming keys
                    normalizeKey(meta, "providerName", "provider_name");
                    normalizeKey(meta, "providerPhone", "provider_phone", "providerPhone");
                    normalizeKey(meta, "providerEmail", "provider_email", "providerEmail");
                    normalizeKey(meta, "wage", "wage");
                    normalizeKey(meta, "jobId", "job_id", "jobId");

                    event.setMetadata(meta);

                    // Log provider info for observability
                    String providerName = String.valueOf(meta.getOrDefault("providerName", ""));
                    String providerPhone = String.valueOf(meta.getOrDefault("providerPhone", ""));
                    String providerEmail = String.valueOf(meta.getOrDefault("providerEmail", ""));

                    log.info("Notification for user {} (userId={}), provider: {} / {} / {}",
                            event.getUsername(), event.getDestination() != null ? event.getDestination().getUserId() : null,
                            providerName, providerPhone, providerEmail);

                    return event;
                })
                .flatMap(notificationRouterService::routeNotification)
                .doOnSuccess(v -> record.receiverOffset().acknowledge())
                .doOnError(error -> log.error("Error processing record", error))
                .onErrorResume(error -> handleError(record, error));
    }

    private void normalizeKey(Map<String, Object> meta, String canonicalKey, String... possibleKeys) {
        // If canonicalKey already present, nothing to do
        if (meta.containsKey(canonicalKey) && meta.get(canonicalKey) != null) return;

        for (String k : possibleKeys) {
            if (k.equals(canonicalKey)) continue;
            if (meta.containsKey(k) && meta.get(k) != null) {
                meta.put(canonicalKey, meta.get(k));
                break;
            }
        }
    }

    private Mono<Void> handleError(ReceiverRecord<String, String> record, Throwable error) {
        log.error("Failed to process message, sending to DLT: {}", error.getMessage());
        return deadLetterService.sendToDeadLetterTopic(record.value(), error.getMessage())
                .doFinally(signal -> record.receiverOffset().acknowledge())
                .then();
    }
}
