package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.model.BaseRequest;
import com.mgm.services.common.model.Customer;
import com.mgm.services.common.model.RedemptionValidationResponse;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class ResortPriceRequest extends BaseRequest {
    
    private List<String> auroraItineraryIds;

    @NotNull(
            message = "_invalid_dates")
    private LocalDate checkInDate;
    @NotNull(
            message = "_invalid_dates")
    private LocalDate checkOutDate;
    private int numGuests = ServiceConstant.DEFAULT_GUESTS;
    private String programId;
    private String propertyId;
    private boolean perpetualPricing;
    private List<String> propertyIds;
    private Map<String, RedemptionValidationResponse> myVegasRedemptionItems;
    private Customer customer;

    @AssertTrue(
            message = "_invalid_dates")
    public boolean isDatesValid() {
        return ValidationUtil.isTripDatesValid(checkInDate, checkOutDate);
    }
}
