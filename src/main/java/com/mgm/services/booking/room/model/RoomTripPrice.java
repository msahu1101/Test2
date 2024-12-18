package com.mgm.services.booking.room.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class RoomTripPrice {

    private Double baseAveragePrice;
    private Double discountedAveragePrice;
    private Double baseSubtotal;
    private Double discountedSubtotal;
    @JsonProperty("isComp")
    private boolean comp;
    private List<PriceItemized> itemized;

}
