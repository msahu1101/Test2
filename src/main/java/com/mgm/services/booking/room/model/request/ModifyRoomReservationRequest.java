/**
 * Request object for creating party room reservations.
 */
package com.mgm.services.booking.room.model.request;

import lombok.Data;

/**
 * @author vararora
 *
 */
public @Data class ModifyRoomReservationRequest {

    private boolean creditCardUpdated;
    private RoomReservationRequest roomReservation;

}
