package com.mgm.services.booking.room.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Aurora Price Request class
 * @author nitpande0
 *
 */
public @SuperBuilder(toBuilder = true) @Getter @NoArgsConstructor class AuroraPriceV3Request extends AuroraPriceRequest{
    // Added for Calendar LOS based Pricing
    private int tripLength;
}
