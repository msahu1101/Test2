package com.mgm.services.booking.room.model;

import com.mgm.services.booking.room.model.request.TripParams;

import lombok.Data;

public @Data class CustomerItineraryCreationRequest {
    private String itineraryName;
    private long customerId;
    private TripParams tripParams;
}
