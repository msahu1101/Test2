package com.mgm.services.booking.room.model.request;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The Class ActivateCustomerRequest.
 */
@EqualsAndHashCode(
        callSuper = false)
public @SuperBuilder @Getter @AllArgsConstructor @NoArgsConstructor class ActivateCustomerRequest {

    private String source;
    private long customerId;
    private String mlifeNumber;
    private String customerEmail;

}
