package com.mgm.services.booking.room.model.response;

import java.util.Set;

import com.mgm.services.booking.room.model.ProgramStartingPrice;

import lombok.Data;

public @Data class RatePlanResponse {

    private String programId;
    private ProgramStartingPrice startingPrice;
    private Set<RoomAvailabilityResponse> rooms;
}
