package com.mgm.services.booking.room.model.request;

import javax.validation.constraints.AssertTrue;

import org.apache.commons.lang3.StringUtils;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;

/**
 * Data Object for Release flow
 * 
 * @author ksammandan
 *
 */

public @Data class ReleaseV2Request extends BaseRequest{

    private String confirmationNumber;
    private String propertyId;
    private boolean f1Package;

    @AssertTrue(message = "_invalid_property_id_confirmation_number")
    public boolean isValidRequest() {
        return StringUtils.isNotEmpty(confirmationNumber) && StringUtils.isNotEmpty(propertyId);
    }
}
