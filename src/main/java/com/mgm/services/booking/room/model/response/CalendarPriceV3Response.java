package com.mgm.services.booking.room.model.response;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.AvailabilityStatus;

import lombok.Data;

/**
 * Calendar Price Response class
 * 
 * @author laknaray
 *
 */
@JsonInclude(Include.NON_NULL)
public @Data class CalendarPriceV3Response {

    private AvailabilityStatus status;
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date date;
    private String roomTypeId;
    private List<TripDetailsV3Response> tripDetails;
    @JsonProperty("isPOApplicable")
    private boolean isPOApplicable;
    private PricingModes pricingMode;
    private int totalCompNights;
    private Double totalNightlyTripBasePrice;
    private Double averageNightlyTripBasePrice;
    private Double totalNightlyTripPrice;
    private Double averageNightlyTripPrice;
    private String promo;

}
