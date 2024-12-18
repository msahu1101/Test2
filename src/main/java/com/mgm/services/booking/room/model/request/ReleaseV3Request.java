package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.AssertTrue;

/**
 * Data Object for Release flow
 *
 */

public @Data class ReleaseV3Request extends BaseRequest{

    private String confirmationNumber;
    private String propertyId;
    private String holdId;
    private boolean f1Package;

    @AssertTrue(message = "_invalid_property_id")
    public boolean isValidRequest() {
        return StringUtils.isNotEmpty(propertyId);
    }
}
