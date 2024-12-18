package com.mgm.services.booking.room.model.ocrs;

import lombok.Data;

@Data
public class AdditionalGuest {

    private String firstName;
    private String lastName;
    protected String mgmId;
    protected String reservationID;

}
