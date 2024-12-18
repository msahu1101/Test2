package com.mgm.services.booking.room.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class TokenRequest extends BaseRequest {

    @JsonProperty("client_id")
    private String clientId;
    @JsonProperty("client_secret")
    private String clientSecret;
    @JsonProperty("transient")
    private boolean transientFlag;

}
