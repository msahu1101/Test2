package com.mgm.services.booking.room.model.request;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.mgm.services.booking.room.annotations.ValidateDateFormat;
import com.mgm.services.booking.room.annotations.ValidateTripParams;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.model.BaseRequest;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@ValidateTripParams(startDate = "checkInDate", endDate = "checkOutDate", numberOfAdults="numAdults")
public @Data class ApplicableProgramsRequest extends BaseRequest {

    @NotNull(message = "_invalid_property")
    private String propertyId;

    private String roomTypeId;

    @ValidateDateFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT, errorMessage = "_invalid_book_date_")
    private String bookDate;

    @ValidateDateFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT, errorMessage = "_invalid_travel_date_")
    private String travelDate;

    private boolean filterBookable;
    private boolean filterViewable;

    private String checkInDate;
    private String checkOutDate;

    private int numAdults;

    private int numChildren;

    @AssertTrue(message = "_invalid_property")
    public boolean isValidPropertyId() {
        return ValidationUtil.isUuid(propertyId);
    }

    @AssertTrue(message = "_invalid_roomtype")
    public boolean isValidRoomTypeId() {
        return (roomTypeId == null ? true : ValidationUtil.isUuid(roomTypeId));
    }
}
