package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class PaymentBasic {

    private double chargeAmount;
    private String cardType;
    private String cardMaskedNumber;
}
