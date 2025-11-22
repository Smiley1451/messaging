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
                    return objectMapper.readValue(record.value(), NotificationEvent.class);
                })
                .flatMap(notificationRouterService::routeNotification)
                .doOnSuccess(v -> record.receiverOffset().acknowledge())
                .doOnError(error -> log.error("Error processing record", error))
                .onErrorResume(error -> handleError(record, error));
    }

    private Mono<Void> handleError(ReceiverRecord<String, String> record, Throwable error) {
        log.error("Failed to process message, sending to DLT: {}", error.getMessage());
        return deadLetterService.sendToDeadLetterTopic(record.value(), error.getMessage())
                .doFinally(signal -> record.receiverOffset().acknowledge())
                .then();
    }
}
