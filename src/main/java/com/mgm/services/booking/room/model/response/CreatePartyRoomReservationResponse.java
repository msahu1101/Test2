/**
 * Response object of creating party room reservations.
 */
package com.mgm.services.booking.room.model.response;

import java.util.List;

import lombok.Data;

/**
 * @author laknaray
 *
 */
public @Data class CreatePartyRoomReservationResponse {

    private List<RoomReservationV2Response> roomReservations;
    private List<FailedReservation> failedReservations;
}
