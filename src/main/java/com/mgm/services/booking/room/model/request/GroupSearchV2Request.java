package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import com.mgm.services.booking.room.annotations.ValidateDateFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = false)
public @Data class GroupSearchV2Request extends BaseRequest {

    @NotNull(message = "_invalid_property")
    private String propertyId;

    @NotNull(message = "_invalid_travel_date_")
    @ValidateDateFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT, errorMessage = "_invalid_travel_date_")
    private String startDate;

    @NotNull(message = "_invalid_travel_date_")
    @ValidateDateFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT, errorMessage = "_invalid_travel_date_")
    private String endDate;

    private String groupName;
   
    private String id;
}
