package com.mgm.services.booking.room.model.request;

import lombok.Data;

public @Data class TokenizeRequest {

    private String creditCard;
    private int expirationMonth;
    private int expirationYear;
}
