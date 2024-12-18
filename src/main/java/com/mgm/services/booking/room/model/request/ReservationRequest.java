package com.mgm.services.booking.room.model.request;

import com.mgm.services.booking.room.model.BillingInfo;
import com.mgm.services.booking.room.model.UserProfile;
import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=false)
public @Data class ReservationRequest extends BaseRequest {
    private String inAuthTransactionId;
    private UserProfile profile;
    private BillingInfo billing;
    private boolean fullPayment;
    private String iata;
    private boolean eligibleForAccountCreation;
}
