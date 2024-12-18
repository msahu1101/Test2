package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;

import lombok.Data;

public @Data class PointAmount implements Serializable {

    private static final long serialVersionUID = 4264130962835304597L;

    private double dollarValue;
    private int pointValue;
}
