package com.notification.service.kafka;

import com.notification.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaDeadLetterService {

    private final KafkaSender<String, String> kafkaSender;
    private final KafkaConfig kafkaConfig;

    public Mono<Void> sendToDeadLetterTopic(String message, String errorMessage) {
        String dlTopic = kafkaConfig.getConsumer().getDeadLetterTopic();
        
        ProducerRecord<String, String> record = new ProducerRecord<>(
                dlTopic,
                null,
                System.currentTimeMillis(),
                null,
                message
        );
        
        record.headers().add("error", errorMessage.getBytes());
        record.headers().add("original-topic", kafkaConfig.getConsumer().getTopic().getBytes());

        return kafkaSender.send(Mono.just(SenderRecord.create(record, null)))
                .doOnNext(result -> log.info("Sent message to DLT: {}", dlTopic))
                .doOnError(error -> log.error("Failed to send to DLT", error))
                .then();
    }
}
