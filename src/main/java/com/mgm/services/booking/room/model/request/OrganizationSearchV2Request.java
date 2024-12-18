package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=false)
public @Data class OrganizationSearchV2Request extends BaseRequest {
    private String iataCode;

    private String orgName;   
}
