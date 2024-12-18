package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.reservation.RoomReservation;

/**
 * Interface provides methods to implement for features related to room 
 * reservation charges
 * @author swakulka
 *
 */
public interface RoomReservationChargesDAOStrategy {

    /**
     * Method to implement for calculating room reservation charges
     * @param roomReservation - RoomReservation object
     * @return
     *         RoomReservation object
     */
    RoomReservation calculateRoomReservationCharges(RoomReservation roomReservation);
}
