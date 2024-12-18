package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class TokenV2Request extends BaseRequest {
    
    private String accessToken;
    private String oktaSessionId;
    private boolean transientFlag;

}
