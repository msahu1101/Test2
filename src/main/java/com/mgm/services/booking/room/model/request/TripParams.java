package com.mgm.services.booking.room.model.request;

import lombok.Data;

@Data
public class TripParams {

    private int numAdults;
    private int numChildren;
    private String arrivalDate;
    private String departureDate;

}
