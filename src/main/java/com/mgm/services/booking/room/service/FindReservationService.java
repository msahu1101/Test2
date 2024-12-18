package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.RoomReservationBasicInfoRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.GetRoomReservationResponse;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;

/**
 * Service class to find room reservation
 */
public interface FindReservationService {

    /**
     * Finds room reservation based on confirmation number.
     * 
     * @param reservationRequest
     *            Find reservation request
     * @return Room reservation object
     */
    RoomReservation findRoomReservation(FindReservationRequest reservationRequest);
    
    /**
     * Finds reservation based on confirmation number and respond with reservation object.
     * 
     * @param reservationRequest reservation request
     * @param validate Flag to validate or not
     * @return GetRoomReservationResponse response v2 object.
     */
    RoomReservation findRoomReservation(FindReservationV2Request reservationRequest, boolean validate);
    
    /**
     * Finds reservation based on confirmation number and respond with v2 version attributes.
     * 
     * @param reservationRequest reservation request
     * @return GetRoomReservationResponse response v2 object.
     */
    GetRoomReservationResponse findRoomReservationResponse(FindReservationV2Request reservationRequest);
    
    /**
     * Returns list of reservations with basic details under a single party
     * reservation or share-with group of reservations.
     * 
     * @param request RoomReservationBasicInfoRequest.
     * @return ReservationAdditionalInfoResponse reservation response.
     */
    ReservationsBasicInfoResponse getReservationBasicInfoList(RoomReservationBasicInfoRequest request);

}
