package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.reservation.RoomReservation;

public interface CommonService {
    public void checkBookingLimitApplied(RoomReservation roomReservation);
}
