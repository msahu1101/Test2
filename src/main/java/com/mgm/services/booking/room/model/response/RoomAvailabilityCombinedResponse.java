package com.mgm.services.booking.room.model.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class RoomAvailabilityCombinedResponse {

    // availability object is deprecated in v3 trip api. To be removed once v2 is dropped
    private Set<RoomAvailabilityV2Response> availability;
    private List<RatePlanV2Response> ratePlans = new ArrayList<>();
    private TripPricingMetadata metadata;

}
