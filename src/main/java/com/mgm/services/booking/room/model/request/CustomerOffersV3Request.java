package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Customer programs search request.
 * 
 */
@EqualsAndHashCode(callSuper = true)
public @Data class CustomerOffersV3Request extends BaseRequest {

    private String propertyId;
    private String region;
    private boolean onlyPoPrograms;
    private boolean onlyPoPatronPrograms;
    private boolean patronFirst;
    private boolean perpetualPricing;
    private boolean includeNonBookableOnline;
    private boolean resortPricing;
}
