/**
 * 
 */
package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.dto.SourceRoomReservationBasicInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;

/**
 * DAO interface to expose services for reservation lookups.
 *
 */
public interface FindReservationDAOStrategy {

    /**
     * Finds room reservation based on confirmation number.
     * 
     * @param reservationRequest
     *            Find reservation request
     * @return Room reservation object
     */
    RoomReservation findRoomReservation(FindReservationRequest reservationRequest);
    
    /**
     * Finds room reservation based on confirmation number.
     * 
     * @param reservationRequest
     *            Find reservation request
     * @return Room reservation object
     */
    RoomReservation findRoomReservation(FindReservationV2Request reservationRequest);
    
    /**
     * Retrieves the list of reservation basic details under single party
     * reservation.
     * 
     * @param request - to get reservation base info.
     * @return SourceRoomReservationBasicInfoResponse response.
     */
    ReservationsBasicInfoResponse getRoomReservationsBasicInfoList(
            SourceRoomReservationBasicInfoRequestDTO request);
    
    /**
     * Searches a room reservation based on confirmation number.
     * @param reservationRequest
     *  		  Find reservation request
     * @return String
     */
    String searchRoomReservationByExternalConfirmationNo(FindReservationV2Request reservationRequest);
}
