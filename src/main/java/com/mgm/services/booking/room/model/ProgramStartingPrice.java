package com.mgm.services.booking.room.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

public @Data class ProgramStartingPrice {

    private double resortFee;
    private Double baseAveragePrice;
    private double discountedAveragePrice;
    private Double baseSubtotal;
    private Double discountedSubtotal;
    @JsonProperty("isComp")
    private boolean comp;
}
