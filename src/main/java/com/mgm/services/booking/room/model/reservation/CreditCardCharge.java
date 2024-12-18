package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;

public @Data class CreditCardCharge implements Serializable {

    private static final long serialVersionUID = 6251096262232803551L;
    private double amount;
    private String cvv;
    private String ccToken;
    private Date expiry;
    private String holder;
    private String number;
    private String decryptedNumber;
    private String maskedNumber;
    private String type;
    private String currencyCode;
    private CardHolderProfile holderProfile;

    //Additional fields to support CRS
    private double fxAmount;
    private String fxCurrencyISOCode;
    private double fxExchangeRate;
    private String fxFlag;
    private String authId;
    private List<String> authIds;
    private Date   txnDateAndTime;
}
