package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data Object for cancellation flow
 * @author nitpande0
 *
 */

@EqualsAndHashCode(
        callSuper = false)
public @Data class CancelRequest extends BaseRequest {

    private String confirmationNumber;
    private String firstName;
    private String lastName;
    private String itineraryId;
    private String reservationId;

}
