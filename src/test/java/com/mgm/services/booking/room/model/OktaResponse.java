package com.mgm.services.booking.room.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

public @Data class OktaResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("id")
    private String oktaSessionId;

}
