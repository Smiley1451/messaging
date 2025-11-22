package com.notification.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder

public class NotificationEvent {

    @NotBlank(message = "User name is required")
    @JsonProperty("user_name")
    private String userName;

    @NotBlank(message = "Username is required")
    @JsonProperty("username")
    private String username;

    @NotBlank(message = "Subject is required")
    @JsonProperty("subject")
    private String subject;

    @NotNull(message = "Source is required")
    @JsonProperty("source")
    private NotificationSource source;

    @NotNull(message = "Destination is required")
    @Valid
    @JsonProperty("destination")
    private Destination destination;

    @NotBlank(message = "Message is required")
    @JsonProperty("message")
    private String message;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Destination {
        @JsonProperty("whatsapp_number")
        private String whatsappNumber;

        @JsonProperty("email")
        private String email;

        @JsonProperty("user_id")
        private String userId;
    }

    public NotificationEvent(String userName, String username, String subject, NotificationSource source, Destination destination, String message, Map<String, Object> metadata) {
        this.userName = userName;
        this.username = username;
        this.subject = subject;
        this.source = source;
        this.destination = destination;
        this.message = message;
        this.metadata = metadata;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public NotificationSource getSource() {
        return source;
    }

    public void setSource(NotificationSource source) {
        this.source = source;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public NotificationEvent() {
    }
}
