package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Calendar price request class
 * 
 * @author nitpande0
 *
 */
@EqualsAndHashCode(
        callSuper = false)
public @Data class CalendarPriceRequest extends BasePriceRequest {

    private static final long serialVersionUID = 9118151118970631119L;

    private List<String> auroraItineraryIds;
    private boolean excludeNonOffer;

    @NotNull(
            message = "_invalid_dates")
    private LocalDate startDate;
    @NotNull(
            message = "_invalid_dates")
    private LocalDate endDate;
    private int numGuests = ServiceConstant.DEFAULT_GUESTS;
    @NotNull(
            message = "_invalid_property")
    private String propertyId;

    @AssertTrue(
            message = "_invalid_dates")
    public boolean isDatesValid() {
        return CommonUtil.isCalendarDatesValid(startDate, endDate);
    }

    @AssertTrue(
            message = "_invalid_property")
    public boolean isPropertyValid() {
        return ValidationUtil.isUuid(propertyId);
    }
}
