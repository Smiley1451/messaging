package com.notification.service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.model.dto.JobCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobEventProducer {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.job-create}")
    private String topic;

    public Mono<Void> sendJobCreatedEvent(JobCreateEvent event) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(jsonPayload -> {
                    SenderRecord<String, String, Integer> record = SenderRecord.create(
                            topic,
                            null,
                            System.currentTimeMillis(),
                            event.getRequesterWhatsapp(), // Key
                            jsonPayload, // Value (JSON String)
                            null
                    );
                    return kafkaSender.send(Mono.just(record)).next();
                })
                .doOnSuccess(r -> log.info("Produced JobCreateEvent for: {}", event.getJobTitle()))
                .then();
    }
}