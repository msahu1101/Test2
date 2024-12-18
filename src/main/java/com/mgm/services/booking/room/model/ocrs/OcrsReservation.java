package com.mgm.services.booking.room.model.ocrs;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;

@Data
public class OcrsReservation {
    private HotelReference hotelReference;
    private String reservationID;
    private ResGuests resGuests = new ResGuests();
    private ResProfiles resProfiles = new ResProfiles();
    private MgmProfile mgmProfile = new MgmProfile();
    private RoomStays roomStays = new RoomStays();
    @JsonFormat(
            pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date originalBookingDate;
    private MfImage mfImage;
    private SelectedMemberships selectedMemberships = new SelectedMemberships();
}
