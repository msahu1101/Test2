package com.mgm.services.booking.room.model.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * The Class LoginCredentials.
 */
public @Data class LoginCredentials {
    private Password password;
    @JsonProperty("recovery_question")
    private RecoveryQuestion recoveryQuestion;
}
