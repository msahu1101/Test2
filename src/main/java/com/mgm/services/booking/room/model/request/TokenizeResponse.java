package com.mgm.services.booking.room.model.request;

import lombok.Data;

public @Data class TokenizeResponse {

    private String token;
    private String accountNumberMasked;
    private String cardExpirationMonth;
    private String cardExpirationYear;
    private String cardIssuerName;
    private String posCardType;
    private String cardType;
    private String tokenExpiration;
    private String posData;
    private String newToken;
}
