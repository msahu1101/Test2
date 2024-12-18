package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class ResortPrice {

    private double baseAveragePrice;
    private double discountedAveragePrice;
    private double baseSubtotal;
    private double discountedSubtotal;
}
