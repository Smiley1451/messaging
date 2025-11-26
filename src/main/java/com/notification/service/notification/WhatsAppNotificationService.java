
package com.notification.service.notification;

import com.notification.model.dto.NotificationEvent;
import com.notification.model.dto.NotificationStatus;
import com.notification.service.logging.NotificationLoggingService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationService {

    private final NotificationLoggingService loggingService;

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;

    @Value("${notification.retry.max-attempts}")
    private int maxRetries;

    @Value("${notification.retry.backoff-delay}")
    private long backoffDelay;

    @PostConstruct
    public void init() {

        Twilio.init(accountSid, authToken);
        log.info("Twilio SDK initialized with Account SID: {}", accountSid);
    }

    public Mono<Void> sendNotification(NotificationEvent event) {
        String toWhatsAppNumber = event.getDestination().getWhatsappNumber();

        if (toWhatsAppNumber == null || toWhatsAppNumber.isBlank()) {
            return Mono.error(new IllegalArgumentException("WhatsApp number is required"));
        }

        String formattedToNumber = toWhatsAppNumber.startsWith("whatsapp:")
                ? toWhatsAppNumber
                : "whatsapp:" + toWhatsAppNumber;

        return sendTwilioMessage(formattedToNumber, event.getMessage())
                .flatMap(response -> loggingService.logNotification(event, NotificationStatus.SUCCESS, null, 0))
                .doOnSuccess(v -> log.info("WhatsApp notification sent successfully to {}", toWhatsAppNumber))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(backoffDelay))
                        .doBeforeRetry(signal -> log.warn("Retrying WhatsApp notification, attempt: {}", signal.totalRetries() + 1)))
                .onErrorResume(error -> {
                    log.error("Failed to send WhatsApp notification to {}: {}", toWhatsAppNumber, error.getMessage());
                    return loggingService.logNotification(event, NotificationStatus.FAILED, error.getMessage(), maxRetries)
                            .then(Mono.error(error));
                })
                .then();
    }

    private Mono<Message> sendTwilioMessage(String to, String messageBody) {
        return Mono.fromCallable(() -> {
                    return Message.creator(
                            new PhoneNumber(to),
                            new PhoneNumber(fromNumber),
                            messageBody
                    ).create();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}