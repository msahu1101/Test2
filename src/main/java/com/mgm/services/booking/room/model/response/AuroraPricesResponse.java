package com.mgm.services.booking.room.model.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Aurora Price Response class
 * @author nitpande0
 *
 */

public @Data class AuroraPricesResponse {

    @JsonProperty("isMrdPricing")
    private boolean mrdPricing;
    private List<AuroraPriceResponse> auroraPrices;

}
