package com.mgm.services.booking.room.model.response;

import com.mgm.services.common.model.Customer;

import lombok.Data;

/**
 * The Class CreateCustomerResponse.
 */
public @Data class CreateCustomerResponse {
    private boolean accountcreated;
    private String verificationCode;
    private Customer customer;
    private boolean inactiveWebProfile;
    private boolean webProfileNotFound;
}
