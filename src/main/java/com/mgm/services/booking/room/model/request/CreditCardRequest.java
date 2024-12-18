package com.mgm.services.booking.room.model.request;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CreditCardRequest {

    private String cardHolder;
    private String firstName;
    private String lastName;
    private String cardNumber;
    private String ccToken;
    private String maskedNumber;
    private double amount;
    private String cvv;
    private String expiry;
    private String type;
    private double fxAmount;
    private String fxCurrencyISOCode;
    private String fxCurrencyCode;
    private double fxExchangeRate;
    private String fxFlag;
    private String authId;
    private List<String> authIds;
    private Date txnDateAndTime;

}
