package com.mgm.services.booking.room.model.ocrs;

import lombok.Data;

@Data
public class PostalAddress {
    private String addressType;
    private String mfPrimaryYN;
    private String address1;
    private String address2;
    private String city;
    private String stateCode;
    private String countryCode;
    private String postalCode;

}
