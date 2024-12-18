package com.mgm.services.booking.room.model.ocrs;

import lombok.Data;

@Data
public class Profile {
    private String mfResortProfileID;
    private String profileType;
    private IndividualName individualName;
    private PhoneNumbers phoneNumbers = new PhoneNumbers();
    private ElectronicAddresses electronicAddresses = new ElectronicAddresses();
    private PostalAddresses postalAddresses = new PostalAddresses();
    private Memberships memberships = new Memberships();

}
