package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.CreatePartyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.CreateRoomReservationRequest;
import com.mgm.services.booking.room.model.request.SaveReservationRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CreatePartyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.SaveReservationResponse;

/**
 * Service to provide room booking or checkout functionality.
 */
public interface ReservationService {

    /**
     * Confirm room reservation with source system and return updated room
     * reservation object.
     * 
     * @param reservation Room Reservation object
     * @return Updated room reservation object with confirmation number
     */
    RoomReservation makeRoomReservation(RoomReservation reservation);

    /**
     * Confirm room reservation with source system and return updated room
     * reservation object for v2 services.
     * 
     * @param createRoomReservationRequest Create Room Reservation Request object
     * @param skipMyVegasConfirm whether to skip myvegas confirmation
     * @return create room reservation object with confirmation number
     */
    CreateRoomReservationResponse makeRoomReservationV2(CreateRoomReservationRequest createRoomReservationRequest,
            String skipMyVegasConfirm);

    /**
     * Confirm party room reservation with source system and return updated room
     * reservation object for v2 services.
     * 
     * @param createPartyRoomReservationRequest Create Party Room Reservation Request object
     * @param skipMyVegasConfirm whether to skip myvegas confirmation
     * @return create party room reservation object with confirmation number
     */
    CreatePartyRoomReservationResponse makePartyRoomReservation(
            CreatePartyRoomReservationRequest createPartyRoomReservationRequest, String skipMyVegasConfirm);

    CreatePartyRoomReservationResponse makePartyRoomReservationV4(
            CreatePartyRoomReservationRequest createPartyRoomReservationRequest, String skipMyVegasConfirm);

    /**
     * Save the room reservation in source system and return the room reservation.
     * 
     * @param saveReservationRequest room reservation request.
     * @return room reservation response.
     */
    SaveReservationResponse saveRoomReservation(SaveReservationRequest saveReservationRequest);
}
