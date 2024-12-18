package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Calendar price request class
 * 
 * @author laknaray
 *
 */
@EqualsAndHashCode(
        callSuper = false)
public @Data class CalendarPriceV3Request extends BasePriceRequest {

    private static final long serialVersionUID = 97908245503670629L;

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
    private LocalDate startDate;
    @NotNull(
            message = "_invalid_dates")
    @DateTimeFormat(
    		pattern = ServiceConstant.ISO_8601_DATE_FORMAT)

    private LocalDate endDate;
    private int numAdults;
    private int numChildren;

    @NotNull(
            message = "_invalid_property")
    private String propertyId;
    private List<String> roomTypeIds;
    private boolean ignoreChannelMargins;
    private int numRooms;
    private String operaConfirmationNumber;
    private String customerDominantPlay;
    
    private int customerRank;
    @JsonProperty
    private boolean isGroupCode;

    private int totalNights;

    public boolean getIsGroupCode() {
        return isGroupCode;
    }

    public void setIsGroupCode(boolean isGroupCode) {
        this.isGroupCode = isGroupCode;
    }

    @AssertTrue(
            message = "_invalid_dates")
    public boolean isDatesValid() {
        // Null check is added to avoid adding multiple _invalid_dates error codes
        return startDate == null || endDate == null || ValidationUtil.isTripDatesValid(startDate, endDate);
    }

    @AssertTrue(
            message = "_invalid_property")
    public boolean isPropertyValid() {
        // Null check is added to avoid adding multiple _invalid_dates error codes
        return propertyId == null || ValidationUtil.isUuid(propertyId);
    }
    
    @AssertTrue(
            message = "_invalid_num_adults")
    public boolean isNumAdultsValid() {
        return  numAdults > 0;
    }
}
