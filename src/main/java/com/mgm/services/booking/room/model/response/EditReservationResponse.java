package com.mgm.services.booking.room.model.response;

import com.mgm.services.booking.room.model.reservation.RoomReservation;

import lombok.Data;

/**
 * EditReservationResponse
 * @author nitpande0
 *
 */
public @Data class EditReservationResponse {

    private RoomReservation modifiedReservation;
}
