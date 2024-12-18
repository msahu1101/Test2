package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class RoomAvailabilityV3Request extends BasePriceRequest {

    private static final long serialVersionUID = 308253072912083497L;

    private List<String> auroraItineraryIds;
    
    private List<String> programIds = new ArrayList<>();

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
    private boolean ignoreChannelMargins;
    private int numRooms;
    private String operaConfirmationNumber;
    private String confirmationNumber;
    private String customerDominantPlay;
    private int customerRank;
    private boolean includeSoldOutRooms;
    private boolean includeDefaultRatePlans;
    @JsonIgnore
    private boolean poProgramRequested;
    @JsonProperty
    private boolean isGroupCode;

    private boolean displaySoPrice;
    private String groupCnfNumber;
    public boolean getIsGroupCode() {
        return isGroupCode;
    }

    public void setIsGroupCode(boolean isGroupCode) {
        this.isGroupCode = isGroupCode;
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
    
    @AssertTrue(
            message = "_invalid_program_ids")
    public boolean isProgramIdsValid() {
        return programIds.size() <= 3;
    }
}
