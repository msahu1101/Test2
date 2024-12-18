package com.mgm.services.booking.room.model.request;

import javax.validation.constraints.NotNull;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = false)
public @Data class FindReservationRequest extends BaseRequest {

    @NotNull(
            message = "_invalid_confirmation_number")
    private String confirmationNumber;
    @NotNull(
            message = "_invalid_first_name")
    private String firstName;
    @NotNull(
            message = "_invalid_last_name")
    private String lastName;
}
