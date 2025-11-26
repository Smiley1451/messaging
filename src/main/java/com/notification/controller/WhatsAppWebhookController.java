package com.notification.controller;

import com.notification.service.conversation.WhatsAppConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppConversationService conversationService;

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> handleIncomingMessage(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String from = formData.getFirst("From");
                    String body = formData.getFirst("Body");

                    log.info("Received WhatsApp message from {}: {}", from, body);

                    if (from != null && body != null) {
                        return conversationService.processIncomingMessage(from, body);
                    }
                    return Mono.empty();
                })
                .thenReturn("<Response></Response>");
    }
}