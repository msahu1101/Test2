package com.mgm.services.booking.room.model.response;

import com.mgm.services.booking.room.model.reservation.RoomReservation;

import lombok.Data;

public @Data class SaveReservationResponse {
    private RoomReservation roomReservation;
}
