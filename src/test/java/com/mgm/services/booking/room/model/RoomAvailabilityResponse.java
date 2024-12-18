package com.mgm.services.booking.room.model;

import java.util.ArrayList;
import java.util.List;

import com.mgm.services.booking.room.model.response.RatePlanV2Response;
import com.mgm.services.booking.room.model.response.RoomAvailabilityV2Response;
import com.mgm.services.booking.room.model.response.TripPricingMetadata;

import lombok.Data;

public @Data class RoomAvailabilityResponse {

 // availability object is deprecated in v3 trip api. To be removed once v2 is dropped
    private List<RoomAvailabilityV2Response> availability;
    private List<RatePlanV2Response> ratePlans = new ArrayList<>();
    private TripPricingMetadata metadata;
}
