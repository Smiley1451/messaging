package com.notification.service.notification;

import com.notification.model.dto.NotificationEvent;
import com.notification.model.dto.NotificationStatus;
import com.notification.service.logging.NotificationLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final NotificationLoggingService loggingService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${notification.retry.max-attempts}")
    private int maxRetries;

    @Value("${notification.retry.backoff-delay}")
    private long backoffDelay;

    public Mono<Void> sendNotification(NotificationEvent event) {
        String email = event.getDestination().getEmail();

        if (email == null || email.isBlank()) {
            return Mono.error(new IllegalArgumentException("Email address is required"));
        }

        return sendEmail(email, event.getSubject(), event.getMessage(), event.getUserName())
                .flatMap(v -> loggingService.logNotification(event, NotificationStatus.SUCCESS, null, 0))
                .doOnSuccess(v -> log.info("Email notification sent successfully to {}", email))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(backoffDelay))
                        .doBeforeRetry(signal -> log.warn("Retrying email notification, attempt: {}", signal.totalRetries() + 1)))
                .onErrorResume(error -> {
                    log.error("Failed to send email notification to {}: {}", email, error.getMessage());
                    return loggingService.logNotification(event, NotificationStatus.FAILED, error.getMessage(), maxRetries)
                            .then(Mono.error(error));
                })
                .then();
    }

    private Mono<Void> sendEmail(String to, String subject, String message, String userName) {
        return Mono.fromRunnable(() -> {
                    try {
                        SimpleMailMessage mailMessage = new SimpleMailMessage();
                        mailMessage.setFrom(fromEmail);
                        mailMessage.setTo(to);
                        mailMessage.setSubject(subject);
                        mailMessage.setText(buildEmailBody(userName, message));
                        
                        mailSender.send(mailMessage);
                        log.debug("Email sent successfully to: {}", to);
                    } catch (MailException e) {
                        log.error("Failed to send email", e);
                        throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private String buildEmailBody(String userName, String message) {
        return String.format("""
                Hi %s,
                
                %s
                
                Best regards,
                Notification Service
                """, userName, message);
    }
}
