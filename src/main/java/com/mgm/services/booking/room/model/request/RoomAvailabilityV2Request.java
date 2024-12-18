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

@EqualsAndHashCode(
        callSuper = true)
public @Data class RoomAvailabilityV2Request extends BasePriceRequest {

    private static final long serialVersionUID = 308253072912083497L;

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
    private int numChildren;
    private boolean enableMrd;
    private boolean ignoreChannelMargins;
    private int numRooms;
    private String operaConfirmationNumber;
    private String customerDominantPlay;
    private int customerRank;
    private boolean includeSoldOutRooms;
    private boolean excludeNonOffer;
    @JsonProperty
    private boolean isGroupCode;
    @JsonProperty
    private boolean perpetualPricing;
    private boolean packageFlow;
    public boolean getIsGroupCode() {
        return isGroupCode;
    }

    public void setIsGroupCode(boolean isGroupCode) {
        this.isGroupCode = isGroupCode;
    }

    public boolean getPerpetualPricing() {
        return perpetualPricing;
    }

    public void setPerpetualPricing(boolean perpetualPricing) {
        this.perpetualPricing = perpetualPricing;
    }
    
    @NotNull(
            message = "_invalid_property")
    private String propertyId;

    @AssertTrue(
            message = "_invalid_dates")
    public boolean isDatesValid() {
        // Null check is added to avoid adding multiple _invalid_dates error codes
        return checkInDate == null || checkOutDate == null
                || ValidationUtil.isTripDatesValid(checkInDate, checkOutDate);
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
        return numAdults > 0;
    }
}
