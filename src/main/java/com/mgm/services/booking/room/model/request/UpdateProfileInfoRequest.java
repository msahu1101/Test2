package com.mgm.services.booking.room.model.request;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public @Data class UpdateProfileInfoRequest extends BaseRequest {
    private String reservationId;
    private String itineraryId;
    private String confirmationNumber;
    private String propertyId;
    private UserProfileRequest userProfile;
    private RoomReservation originalReservation;
    private boolean moveItinerary;
}
