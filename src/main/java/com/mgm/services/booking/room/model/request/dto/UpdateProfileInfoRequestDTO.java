package com.mgm.services.booking.room.model.request.dto;

import com.mgm.services.booking.room.model.request.UserProfileRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public @Builder(toBuilder=true) @Getter @Setter @AllArgsConstructor @NoArgsConstructor  class UpdateProfileInfoRequestDTO {
    private String source;
    private String reservationId;
    private String itineraryId;
    private String confirmationNumber;
    private String propertyId;
    private UserProfileRequest userProfile;
    private RoomReservation originalReservation;
    private boolean moveItinerary;
    private boolean isAssociateFlow;
}
