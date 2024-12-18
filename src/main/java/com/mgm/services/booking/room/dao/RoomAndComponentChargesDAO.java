package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.reservation.RoomReservation;

/**
 * Interface provides methods to implement for features related to room 
 * reservation charges
 * @author uttam
 *
 */
public interface RoomAndComponentChargesDAO {

      
    /**
     * Method to implement for calculating room reservation charges with component charges
     * @param roomReservation - RoomReservation object
     * @return
     *         RoomReservation object
     */
    RoomReservation calculateRoomAndComponentCharges(RoomReservation roomReservation);
}
