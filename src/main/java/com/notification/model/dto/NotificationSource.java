package com.notification.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationSource {
    WHATSAPP("WHATSAPP"),
    EMAIL("EMAIL"),
    REALTIME("REALTIME");

    private final String value;

    NotificationSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NotificationSource fromValue(String value) {
        for (NotificationSource source : NotificationSource.values()) {
            if (source.value.equalsIgnoreCase(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Invalid notification source: " + value);
    }

}
