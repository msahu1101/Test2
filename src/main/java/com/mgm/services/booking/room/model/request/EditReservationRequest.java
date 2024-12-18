package com.mgm.services.booking.room.model.request;

import java.util.Date;

import lombok.Data;

public @Data class EditReservationRequest {

    private String confirmationNumber;
    private Date checkInDate;
    private Date checkOutDate;
    private String[] componentIds;
    private boolean removeAllComponents;
}
