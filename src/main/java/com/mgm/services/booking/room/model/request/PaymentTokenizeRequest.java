package com.mgm.services.booking.room.model.request;

import lombok.Data;

/**
 * Tokenization request
 * @author nitpande0
 *
 */
public @Data class PaymentTokenizeRequest {
    
    private String creditCard;
    private String expirationMonth;
    private String expirationYear;
    private String cardType;
    private String cvv;

}
