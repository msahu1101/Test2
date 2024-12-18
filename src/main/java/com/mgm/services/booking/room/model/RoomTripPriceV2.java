package com.mgm.services.booking.room.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class RoomTripPriceV2 {

    private Double baseAveragePrice;
    private Double discountedAveragePrice;
    private Double baseSubtotal;
    private Double discountedSubtotal;
    private Double discountsTotal;
    private Double resortFeeTotal;
    @JsonProperty("isDiscounted")
    private Boolean discounted;
    private Double tripSubtotal;
    @JsonProperty("isComp")
    private boolean comp;
    private List<PriceV2Itemized> itemized;
    private double f1TicketPrice;
    private double averageNightlyF1TripPrice;
}
