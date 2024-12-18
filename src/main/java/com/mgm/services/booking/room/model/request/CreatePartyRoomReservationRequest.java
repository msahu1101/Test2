/**
 * Request object for creating party room reservations.
 */
package com.mgm.services.booking.room.model.request;

import lombok.Data;

/**
 * @author laknaray
 *
 */
public @Data class CreatePartyRoomReservationRequest {

    private boolean splitCreditCardDetails;
    private RoomReservationRequest roomReservation;

}
