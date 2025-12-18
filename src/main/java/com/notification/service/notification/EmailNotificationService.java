package com.notification.service.notification;

import com.notification.model.dto.NotificationEvent;
import com.notification.model.dto.NotificationStatus;
import com.notification.service.logging.NotificationLoggingService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

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
        String email = event.getDestination() != null ? event.getDestination().getEmail() : null;

        if (email == null || email.isBlank()) {
            return Mono.error(new IllegalArgumentException("Email address is required"));
        }

        return sendEmail(event)
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

    private Mono<Void> sendEmail(NotificationEvent event) {
        return Mono.fromRunnable(() -> {
                    try {
                        MimeMessage mimeMessage = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                        helper.setFrom(fromEmail);
                        helper.setTo(event.getDestination().getEmail());
                        helper.setSubject(event.getSubject());

                        Map<String, Object> meta = event.getMetadata() != null ? event.getMetadata() : Collections.emptyMap();

                        String jobTitle = Objects.toString(meta.get("job_title"), "");
                        if (jobTitle.isBlank()) jobTitle = Objects.toString(meta.get("jobTitle"), "");
                        String wage = Objects.toString(meta.get("wage"), "");
                        String location = Objects.toString(meta.get("location"), "");
                        String providerName = Objects.toString(meta.get("providerName"), "");
                        String providerPhone = Objects.toString(meta.get("providerPhone"), "");
                        String providerEmail = Objects.toString(meta.get("providerEmail"), "");
                        String jobId = Objects.toString(meta.get("jobId"), "");

                        String htmlContent = String.format(
                                "<div style='font-family: sans-serif; max-width: 600px; border: 1px solid #eee; padding: 20px;'>" +
                                        "<h2 style='color: #007bff;'>New Job Match Found!</h2>" +
                                        "<p>Hello <b>%s</b>, a new job matches your skills.</p>" +
                                        "<div style='background: #f8f9fa; border-left: 5px solid #007bff; padding: 15px; margin: 20px 0;'>" +
                                        "<h3 style='margin:0;'>%s</h3>" +
                                        "<p style='margin:0;'><b>Offered Wage:</b> %s</p>" +
                                        "<p style='margin:0;'><b>Location:</b> %s</p>" +
                                        "</div>" +
                                        "<h4 style='border-bottom: 1px solid #eee; padding-bottom: 10px;'>Contact the Provider</h4>" +
                                        "<p><b>Name:</b> %s</p>" +
                                        "<p><b>Phone:</b> %s</p>" +
                                        "<p><b>Email:</b> %s</p>" +
                                        "<br><a href='#' style='display: inline-block; background: #28a745; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px;'>Accept Job</a>" +
                                        "<div style='margin-top:20px; padding:12px; background:#f1f1f1; border-top:1px solid #e0e0e0;'>" +
                                        "<strong>Contact %s</strong> at <a href='tel:%s'>%s</a> or reply to <a href='mailto:%s'>%s</a>" +
                                        "</div>" +
                                        "</div>",
                                event.getUserName(), jobTitle, wage, location, providerName, providerPhone, providerEmail,
                                providerName, providerPhone, providerPhone, providerEmail, providerEmail
                        );

                        helper.setText(htmlContent, true);
                        mailSender.send(mimeMessage);
                        log.info("Rich HTML Email sent to {}", event.getDestination().getEmail());
                    } catch (Exception e) {
                        log.error("Email delivery failed", e);
                        throw new RuntimeException(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
