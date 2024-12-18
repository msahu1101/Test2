package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
public @Data class PartnerAuthTokenResponse {
	
	@JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private int expiresIn;
	
}
