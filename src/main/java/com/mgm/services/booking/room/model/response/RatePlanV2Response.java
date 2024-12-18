package com.mgm.services.booking.room.model.response;

import java.util.Set;

import com.mgm.services.booking.room.model.ProgramStartingPrice;

import lombok.Data;

public @Data class RatePlanV2Response {

    private String programId;
    private String promo;    
    private ProgramStartingPrice startingPrice;
    private Set<RoomAvailabilityV2Response> rooms;
}
