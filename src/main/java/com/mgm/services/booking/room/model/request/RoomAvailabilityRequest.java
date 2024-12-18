package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class RoomAvailabilityRequest extends BasePriceRequest {

    private static final long serialVersionUID = 308253072912083497L;

    private List<String> auroraItineraryIds;

    @NotNull(
            message = "_invalid_dates")
    private LocalDate checkInDate;
    @NotNull(
            message = "_invalid_dates")
    private LocalDate checkOutDate;
    private int numGuests = ServiceConstant.DEFAULT_GUESTS;

    @NotNull(
            message = "_invalid_property")
    private String propertyId;

    @AssertTrue(
            message = "_invalid_dates")
    public boolean isDatesValid() {
        return ValidationUtil.isTripDatesValid(checkInDate, checkOutDate);
    }

    @AssertTrue(
            message = "_invalid_property")
    public boolean isPropertyValid() {
        return ValidationUtil.isUuid(propertyId);
    }
}
