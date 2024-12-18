package com.mgm.services.booking.room.model.response;

import lombok.Data;

@Data
public class BillingAddressResponse {

    private String street1;
    private String street2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
