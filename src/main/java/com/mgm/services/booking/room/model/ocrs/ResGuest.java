package com.mgm.services.booking.room.model.ocrs;

import lombok.Data;

@Data
public class ResGuest {
    private String profileRPHs;
    private String reservationID;
    private String confirmationID;
    private ReservationReferences reservationReferences;
    private long arrivalTime;
    private long departureTime;
}
