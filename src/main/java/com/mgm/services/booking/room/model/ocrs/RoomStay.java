package com.mgm.services.booking.room.model.ocrs;

import lombok.Data;

@Data
public class RoomStay {
    private String roomInventoryCode;
    private String reservationStatusType;
    private GuestCounts guestCounts;
}
