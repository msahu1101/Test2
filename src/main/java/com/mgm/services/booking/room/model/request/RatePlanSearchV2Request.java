package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = false)
public @Data class RatePlanSearchV2Request extends BaseRequest {

    @NotNull(
            message = "_invalid_property")
    private String propertyId;

    private String ratePlanCode;

    private LocalDate startDate;

    private LocalDate endDate;

}


