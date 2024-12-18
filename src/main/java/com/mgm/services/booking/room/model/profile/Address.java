package com.mgm.services.booking.room.model.profile;

import lombok.Data;

public @Data class Address {

    private AddressType type;
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
}
