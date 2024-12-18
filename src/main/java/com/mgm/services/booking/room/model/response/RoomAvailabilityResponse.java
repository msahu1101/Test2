package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mgm.services.booking.room.model.RoomTripPrice;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class RoomAvailabilityResponse implements Comparable<RoomAvailabilityResponse> {

    private String roomTypeId;
    private double resortFee;
    private RoomTripPrice price;
    private RoomTripPrice memberPrice;

    /**
     * Overriding compareTo function to sort responses based on price in the
     * order of low to high
     */
    @Override
    public int compareTo(RoomAvailabilityResponse response) {
        int returnValue = -1;

        if (null != this.price.getDiscountedAveragePrice() && null != response.price.getDiscountedAveragePrice()
                && this.price.getDiscountedAveragePrice() >= response.price.getDiscountedAveragePrice()) {
            returnValue = 1;
        }

        return returnValue;
    }

}
