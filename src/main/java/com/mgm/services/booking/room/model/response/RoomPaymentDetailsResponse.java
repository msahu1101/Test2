package com.mgm.services.booking.room.model.response;

import lombok.Data;

@Data
public class RoomPaymentDetailsResponse {

    private CreditCardResponse payment;
    private BillingAddressResponse address;

}
