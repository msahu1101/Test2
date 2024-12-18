package com.mgm.services.booking.room.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class ACRSTokenRequest extends TokenRequest {

    @JsonProperty("grant_type")
    private String grantType;

}
