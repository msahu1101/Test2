package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.reservation.PartyRoomReservation;
import com.mgm.services.booking.room.model.reservation.RoomReservation;

/**
 * DAO interface to expose services for pre-reserve related functionalities for
 * creating/updating room reservation object with charges/taxes.
 *
 */
public interface ReservationDAO {

    /**
     * Creates room reservation object by retreiving prices for selected room,
     * and invoking GSE's updateRoomReservation API. Reservation object contains
     * all the breakdown of charges and taxes.
     * 
     * @param request
     *            Pre-reserve request
     * @return Returns room reservation object
     */
    RoomReservation prepareRoomCartItem(RoomCartRequest request);

    /**
     * Updates room reservation object with selected room requests.
     * 
     * @param reservation
     *            Room reservation object
     * @return Updates room reservation object
     */
    RoomReservation updateRoomReservation(RoomReservation reservation);

    /**
     * Complete room reservation object with selected room requests.
     * 
     * @param reservation
     *            Room reservation object
     * @return Confirmed room reservation object
     */
    RoomReservation makeRoomReservation(RoomReservation reservation);
    
    /**
     * Save room reservation object as itinerary into aurora.
     * 
     * @param reservation
     *            Room reservation object
     * @return Save room reservation object
     */
    RoomReservation saveRoomReservation(RoomReservation reservation);

    /**
     * Make room reservation with the given roomReservation object for v2.
     * 
     * @param reservation
     *            Room reservation object
     * @return Confirmed room reservation object
     */
    RoomReservation makeRoomReservationV2(RoomReservation reservation);
    RoomReservation makeRoomReservationV4(RoomReservation reservation);

    /**
     * Make party room reservation with the given roomReservation object for v2.
     * 
     * @param reservation
     *            Room reservation object
     * @param splitCreditCardDetails
     *            Indicate whether source system to use same payment profile 
     *            for all the individual reservations or not.
     * @return Confirmed room reservation object
     */
    PartyRoomReservation makePartyRoomReservation(RoomReservation reservation, boolean splitCreditCardDetails);
    PartyRoomReservation makePartyRoomReservationV4(RoomReservation reservation, boolean splitCreditCardDetails);
}
