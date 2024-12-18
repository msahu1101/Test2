package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The Class ProfileRequest.
 */
@EqualsAndHashCode(
        callSuper = false)
public @SuperBuilder @Data @AllArgsConstructor @NoArgsConstructor class ProfileRequest {

    private String source;
    private long customerId;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String phoneNumber;
    private String phoneType;
    private String addressType;
    private String patronType;
    private String customerEmail;
    private Integer mlifeNo;

}
