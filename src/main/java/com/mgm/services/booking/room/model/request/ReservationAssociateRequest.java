package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data class ReservationAssociateRequest extends BaseRequest {
    private String confirmationNumber;
}
