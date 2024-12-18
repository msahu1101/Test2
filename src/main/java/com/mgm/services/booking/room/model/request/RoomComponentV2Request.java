package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.AssertTrue;

import org.springframework.format.annotation.DateTimeFormat;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.model.BaseRequest;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=false)
public @Data class RoomComponentV2Request extends BaseRequest {

    private String propertyId;
    private String roomTypeId;
    private String programId;
    private List<String> componentIds;
    @DateTimeFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate checkInDate;
    @DateTimeFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate checkOutDate;
    
    @AssertTrue(
            message = "_invalid_dates")
    public boolean isDatesValid() {
        // Null check is added to avoid adding multiple _invalid_dates error codes
        return checkInDate == null || checkOutDate == null || ValidationUtil.isTripDatesValid(checkInDate, checkOutDate);
    }
}
