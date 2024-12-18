package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

/**
 * Service to expose functionality for pre-reserve actions like add room to
 * cart, add room requests and compute charges and totals.
 * 
 */
public interface RoomCartService {

    /**
     * Service to reprice selected room, create room reservation object and
     * update computed charges, taxes and totals.
     * 
     * @param request
     *            Pre reserve request
     * @return Room reservation response
     */
    RoomReservation prepareRoomCartItem(RoomCartRequest request);
    
    /**
     * Service to save room to Aurora and generate a itinerary id using which Aurora is
     * aware what is added to the User's cart
     * 
     * @param request
     *            Pre reserve request
     * @return Room reservation response
     */
    RoomReservation saveRoomCartItemInAurora(RoomReservation request);

    /**
     * Service to update room reservation charges/taxes with selected room
     * requests.
     * 
     * @param reservation
     *            Room reservation object
     * @return Updated room reservation object
     */
    RoomReservation addRoomRequests(RoomReservation reservation);
}
