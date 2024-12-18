package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class PaymentInfo {

    private String cardHolder;
    private String cardNumber;
    private String cvv;
    private String expiry;
    private String type;
    private String paymentToken;
    private double amount;
}
