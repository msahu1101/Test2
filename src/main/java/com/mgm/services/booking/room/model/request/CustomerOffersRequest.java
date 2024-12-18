package com.mgm.services.booking.room.model.request;

import javax.validation.constraints.AssertTrue;

import com.mgm.services.common.model.BaseRequest;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Customer programs search request.
 * 
 * @author jayveera
 *
 */
@EqualsAndHashCode(callSuper = true)
public @Data class CustomerOffersRequest extends BaseRequest {

    private String propertyId;
    private boolean notRolledToSegments;
    private boolean notSorted;
    
    @AssertTrue(message = "_invalid_property")
    public boolean isValidPropertyId() {
        return isNullOrValidUuid(propertyId);
    }
    
    private boolean isNullOrValidUuid(String uuid) {
        return (uuid == null ? true : ValidationUtil.isUuid(uuid));
    }

}
