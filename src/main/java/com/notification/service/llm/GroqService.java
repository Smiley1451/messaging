package com.notification.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.model.dto.JobCreateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    public Mono<JobCreateEvent> extractJobDetails(String userMessage, String userPhone) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("User message is empty. Cannot extract job details."));
        }

        String systemPrompt = """
                You are a data extraction assistant. Extract job details from the user's message into a strict JSON format.
                The JSON must have these fields: job_title, description, location, wage, contact_number.
                If a field is missing, use "Not specified".
                Return ONLY the JSON object.
                """;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object") // Forces JSON output
        );

        return webClientBuilder.build()
                .post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Groq API Error: {}", errorBody);
                                    return Mono.error(new RuntimeException("Groq API Error: " + errorBody));
                                })
                )
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    try {
                        String content = response.get("choices").get(0).get("message").get("content").asText();
                        content = content.replace("```json", "").replace("```", "").trim();

                        JobCreateEvent event = objectMapper.readValue(content, JobCreateEvent.class);
                        event.setRequesterWhatsapp(userPhone);
                        return event;
                    } catch (Exception e) {
                        log.error("Failed to parse LLM response", e);
                        throw new RuntimeException("Failed to parse LLM response", e);
                    }
                });
    }
}