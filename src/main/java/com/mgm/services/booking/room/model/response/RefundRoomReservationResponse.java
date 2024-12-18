package com.mgm.services.booking.room.model.response;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import lombok.Data;

@Data
public class RefundRoomReservationResponse {
    private RoomReservation roomReservation;
}
