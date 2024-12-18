package com.mgm.services.booking.room.model.response;

import com.mgm.services.booking.room.model.request.AbstractBaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The Class ActivateCustomerResponse.
 */
@EqualsAndHashCode(
        callSuper = false)
public @Data class ActivateCustomerResponse extends AbstractBaseRequest {
    private static final long serialVersionUID = 1949146205656411440L;

    private boolean accountActivated;
}
