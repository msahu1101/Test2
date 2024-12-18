package com.mgm.services.booking.room.model;

import com.mgm.services.booking.room.model.request.TripDetailsRequest;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import lombok.Data;

import java.util.List;

@Data
public class HoldReservationBasicInfo {
    private String propertyId;
    private long mlifeNumber;
    private long customerId;
    private TripDetailsRequest tripDetails;
    private List<RoomPrice> bookings;
}
