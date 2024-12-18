package com.mgm.services.booking.room.model.request;

import lombok.Data;

@Data
public class UpdateRoomReservationRequest {

    private boolean isCreditCardUpdated;

    private RoomReservationRequest roomReservation;
}
