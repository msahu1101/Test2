package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.model.BaseRequest;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class ResortPriceV2Request extends BaseRequest {
    
    private List<String> auroraItineraryIds;

    @JsonSetter("itineraryIds")
    public void setItineraryIds(List<String> auroraItineraryIds) {
        this.auroraItineraryIds = auroraItineraryIds;
    }

    @JsonGetter("itineraryIds")
    public List<String> getItineraryIds() {
        return auroraItineraryIds;
    }

    @NotNull(
            message = "_invalid_dates")
    @DateTimeFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate checkInDate;
    @NotNull(
            message = "_invalid_dates")
    @DateTimeFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate checkOutDate;
    private int numAdults;
    private String programId;
    private String segment;
    private String propertyId;
    private List<String> propertyIds;
    private String redemptionCode;
    private String promo;
    private boolean participatingResortsOnly;
    private String groupCode;
    private boolean ignoreSO;
    protected boolean packageFlow;

    @AssertTrue(
            message = "_invalid_dates")
    public boolean isDatesValid() {
        // Null check is added to avoid adding multiple _invalid_dates error codes
        return checkInDate == null || checkOutDate == null
                || ValidationUtil.isTripDatesValid(checkInDate, checkOutDate);
    }

    @AssertTrue(
            message = "_invalid_num_adults")
    public boolean isNumAdultsValid() {
        return  numAdults > 0;
    }

}
