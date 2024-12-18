package com.mgm.services.booking.room.model.response;

import lombok.Data;

/**
 * Payment Tokenize response 
 * @author nitpande0
 *
 */
public @Data class PaymentTokenizeResponse {
    
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
