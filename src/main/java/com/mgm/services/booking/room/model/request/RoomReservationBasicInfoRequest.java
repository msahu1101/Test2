package com.mgm.services.booking.room.model.request;

import javax.validation.constraints.AssertTrue;

import org.apache.commons.lang3.StringUtils;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data class RoomReservationBasicInfoRequest extends BaseRequest {
    private String confirmationNumber;
    private String operaPartyCode;

    @AssertTrue(message = "_invalid_party_reservation_request")
    public boolean isValidRequest() {
        return (StringUtils.isEmpty(confirmationNumber) && StringUtils.isNotEmpty(operaPartyCode))
                || (StringUtils.isNotEmpty(confirmationNumber) && StringUtils.isEmpty(operaPartyCode));
    }
}
