package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = false)
public @Data class FindReservationV2Request extends BaseRequest {

    private String confirmationNumber;
    private String firstName;
    private String lastName;
    private boolean cacheOnly;
    private String propertyId;
    private String operaConfNumber;
    private boolean isTcolvReservation;
    private String acrsConfirmationNumber;
    private boolean tcolvTravelClickResv;
    private String propertyCode;

}
