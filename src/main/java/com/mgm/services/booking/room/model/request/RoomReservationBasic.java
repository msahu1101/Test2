package com.mgm.services.booking.room.model.request;

import lombok.Data;

@Data
public class RoomReservationBasic {
    
    private int numRooms;
    private int numAdults;
    private int numChildren;
    private String operaState;
    private String checkInDate;
    private String roomTypeId;
    private String confirmationNumber;
    private String checkOutDate;
    private boolean nrgStatus;
    private String oTAConfirmationNumber;
    private String operaConfirmationNumber;
    private String propertyId;
    private String state;
}
