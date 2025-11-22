package com.notification.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCreateEvent {
    @JsonProperty("job_title")
    private String jobTitle;

    @JsonProperty("description")
    private String description;

    @JsonProperty("location")
    private String location;

    @JsonProperty("wage")
    private String wage;

    @JsonProperty("contact_number")
    private String contactNumber;

    @JsonProperty("requester_whatsapp")
    private String requesterWhatsapp;
}