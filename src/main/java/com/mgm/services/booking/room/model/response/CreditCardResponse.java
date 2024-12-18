package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class CreditCardResponse {

    private String cardHolder;
    private String firstName;
    private String lastName;
    private String ccToken;
    private String encryptedccToken;
    private String maskedNumber;
    private double amount;
    private String cvv;
    private String expiry;
    private String type;

}
