package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Token Response class
 *
 */
public @Data class ACRSAuthTokenResponse{

    @JsonProperty("token")
    private String token;

}
