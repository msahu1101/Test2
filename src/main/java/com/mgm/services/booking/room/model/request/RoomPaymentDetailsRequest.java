package com.mgm.services.booking.room.model.request;

import lombok.Data;

@Data
public class RoomPaymentDetailsRequest {

    private CreditCardRequest payment;
    private BillingAddressRequest address;

}
