package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class CartSummary {

    private double reservationTotal;
    private double depositDue;
    private double balanceUponCheckIn;
}
