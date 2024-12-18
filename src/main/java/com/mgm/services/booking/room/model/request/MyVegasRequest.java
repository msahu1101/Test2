package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;
import com.mgmresorts.myvegas.jaxb.Customer;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = true)
public @Data class MyVegasRequest extends BaseRequest {

    private String redemptionCode;
    private String reservationDate;
    private String confirmationNumber;
    private String couponCode;
    private Customer customer;
    private boolean skipCache;

}
