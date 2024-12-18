package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Token Response class
 * @author nitpande0
 *
 */
public @Data class IDMSTokenResponse extends TokenResponse {

    @JsonProperty("scope")
    private String scope;

}
