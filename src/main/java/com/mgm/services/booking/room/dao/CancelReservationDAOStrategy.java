package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.CancelRequest;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.ReleaseV2Request;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

/**
 * DAO interface to expose services for room reservation cancellation.
 *
 */
public interface CancelReservationDAOStrategy {

    /**
     * Cancels room reservation associated with the confirmation number.
     * 
     * @param request
     *            Cancel request
     * @return Room reservation object
     */
    RoomReservation cancelReservation(CancelRequest request, String propertyId);
    

    /**
     * Cancels room reservation associated with the confirmation number or itinerary
     * id and reservation id combination for V2 API.
     * 
     * @param request Cancel request
     * @return Room reservation object
     */
    RoomReservation cancelReservation(CancelV2Request request);

    /**
     * Ignores room reservation associated with the confirmation number or itinerary
     * id and reservation id combination for V2 API.
     *
     * @param request Cancel request
     * @return boolean
     */
    boolean ignoreReservation(ReleaseV2Request request);
}
