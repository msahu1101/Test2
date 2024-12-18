package com.mgm.services.booking.room.model.response;

import java.util.Date;

import com.mgm.services.booking.room.model.AvailabilityStatus;

import lombok.Data;

/**
 * Aurora Price Response class
 * @author nitpande0
 *
 */

public @Data class AuroraPriceResponse {

    private Date date;
    private boolean comp;
    private AvailabilityStatus status;
    private boolean closeToArrival;
    private String propertyId;
    private String roomTypeId;
    private double basePrice;
    private double discountedPrice;
    private double baseMemberPrice;
    private double discountedMemberPrice;
    private String programId;
    private String memberProgramId;
    private double resortFee;
    private String unavailabilityReason;
    private String pricingRuleId;
    private boolean programIdIsRateTable;
    private boolean pOApplicable;
    private String promo;
    private double amtAftTax;
    private double baseAmtAftTax;
}
