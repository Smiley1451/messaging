package com.notification.service.conversation;

import com.notification.model.dto.NotificationEvent;
import com.notification.model.dto.NotificationSource;
import com.notification.service.kafka.JobEventProducer;
import com.notification.service.llm.GroqService;
import com.notification.service.notification.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppConversationService {

    private final WhatsAppNotificationService whatsAppService;
    private final GroqService groqService;
    private final JobEventProducer jobEventProducer;
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String STATE_KEY_PREFIX = "whatsapp:state:";

    private static final Duration SESSION_TIMEOUT = Duration.ofHours(24);

    private static final String STATE_IDLE = "IDLE";
    private static final String STATE_AWAITING_CONFIRMATION = "AWAITING_CONFIRMATION";
    private static final String STATE_AWAITING_DETAILS = "AWAITING_DETAILS";

    public Mono<Void> processIncomingMessage(String fromNumber, String messageBody) {
        String redisKey = STATE_KEY_PREFIX + fromNumber;
        String input = messageBody.trim();

        if (isResetCommand(input)) {
            return resetConversation(fromNumber, redisKey);
        }

        return redisTemplate.opsForValue().get(redisKey)
                .defaultIfEmpty(STATE_IDLE)
                .flatMap(state -> {
                    return redisTemplate.expire(redisKey, SESSION_TIMEOUT)
                            .then(dispatchState(state, fromNumber, input, redisKey));
                });
    }

    private Mono<Void> dispatchState(String state, String from, String input, String redisKey) {
        return switch (state) {
            case STATE_IDLE -> handleMainMenu(from, input, redisKey);
            case STATE_AWAITING_CONFIRMATION -> handleConfirmation(from, input, redisKey);
            case STATE_AWAITING_DETAILS -> handleJobDetails(from, input, redisKey);
            default -> resetConversation(from, redisKey);
        };
    }


    private Mono<Void> handleMainMenu(String from, String input, String redisKey) {
        if (input.equals("1") || input.toLowerCase().contains("job") || input.toLowerCase().contains("hi")) {
            return updateState(redisKey, STATE_AWAITING_CONFIRMATION)
                    .then(reply(from, """
                            👷 *Job Posting Service*
                            
                            Do you want to post a new job?
                            
                            *1.* Yes
                            *2.* No
                            
                            _Reply with a number._"""));
        }

        return reply(from, """
                👋 Welcome! How can I help you?
                
                *1.* Post a Job
                *0.* Clear Memory / Reset
                
                _Reply with a number._""");
    }

    private Mono<Void> handleConfirmation(String from, String input, String redisKey) {
        if (input.equals("1") || input.equalsIgnoreCase("yes")) {
            return updateState(redisKey, STATE_AWAITING_DETAILS)
                    .then(reply(from, """
                            ✅ Great! Please describe the job details.
                            
                            Include:
                            - Role (e.g., Painter)
                            - Location
                            - Wage/Salary
                            - Description
                            
                            *Example:* "Need a plumber in Whitefield, 500rs/hour to fix a leak."
                            
                            _Type your message below:_"""));
        } else if (input.equals("2") || input.equalsIgnoreCase("no")) {
            return resetConversation(from, redisKey, "👌 No problem. Type *1* anytime to start again.");
        }

        return reply(from, "❌ Invalid option. Please reply *1* for Yes or *2* for No.");
    }

    private Mono<Void> handleJobDetails(String from, String input, String redisKey) {
        if (input.length() < 10) {
            return reply(from, "⚠️ That seems too short. Please provide more details about the job.");
        }

        return reply(from, "⏳ Processing details... please wait.")
                .then(groqService.extractJobDetails(input, from))
                .flatMap(jobEvent -> jobEventProducer.sendJobCreatedEvent(jobEvent).thenReturn(jobEvent))
                .flatMap(jobEvent -> {
                    String successMsg = String.format("""
                            🎉 *Job Created Successfully!*
                            
                            👷 **Role:** %s
                            📍 **Location:** %s
                            💰 **Wage:** %s
                            
                            We are notifying available workers now. You will receive updates shortly.
                            
                            _Type *1* to post another job._""",
                            jobEvent.getJobTitle(), jobEvent.getLocation(), jobEvent.getWage());

                    return updateState(redisKey, STATE_IDLE)
                            .then(reply(from, successMsg));
                })
                .onErrorResume(e -> {
                    log.error("Error processing job", e);
                    return reply(from, "⚠️ Sorry, I couldn't understand that. Please try describing the job again.");
                });
    }


    private Mono<Void> updateState(String key, String newState) {
        return redisTemplate.opsForValue().set(key, newState, SESSION_TIMEOUT).then();
    }

    private Mono<Void> resetConversation(String from, String key) {
        return resetConversation(from, key, "🔄 Memory cleared. Type *1* to start.");
    }

    private Mono<Void> resetConversation(String from, String key, String message) {
        return redisTemplate.delete(key)
                .then(reply(from, message));
    }

    private boolean isResetCommand(String input) {
        return input.equals("0") || input.equalsIgnoreCase("reset") || input.equalsIgnoreCase("clear");
    }

    private Mono<Void> reply(String to, String message) {
        NotificationEvent event = NotificationEvent.builder()
                .source(NotificationSource.WHATSAPP)
                .destination(new NotificationEvent.Destination(to, null, null))
                .message(message)
                .userName("System")
                .username("system")
                .subject("Reply")
                .build();
        return whatsAppService.sendNotification(event);
    }
}