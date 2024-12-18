package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.ResortPrice;

import lombok.Data;

@JsonInclude(Include.NON_EMPTY)
public @Data class ResortPriceResponse {

    @JsonProperty("isComp")
    private boolean comp;
    private AvailabilityStatus status;
    private String propertyId;
    private String perpetualProgramId;
    private String programId;
    private String roomTypeId;
    private String ratePlanCode;
    private ResortPrice price;
    private double resortFee;
    private PricingModes pricingMode;
    private String promo;
    private double f1TicketPrice;
    private double amtAftTax;
    private double baseAmtAftTax;
}
