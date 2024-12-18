package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class UserAddress {

    private String street1;
    private String street2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
