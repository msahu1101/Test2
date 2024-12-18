package com.mgm.services.booking.room.model.response;

import java.util.Date;
import java.util.List;

import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.TripDetailsV3;

import lombok.Data;

/**
 * Aurora Price Response class
 * 
 * @author laknaray
 *
 */

public @Data class AuroraPriceV3Response {

    private AvailabilityStatus status;
    private Date date;
    private String roomTypeId;
    private List<TripDetailsV3> tripDetails;
    private boolean pOApplicable;
    private PricingModes pricingMode;
    private Double totalNightlyTripPrice;
    private Double totalNightlyTripBasePrice;
    private String unavailabilityReason;
    private String promo;

}
