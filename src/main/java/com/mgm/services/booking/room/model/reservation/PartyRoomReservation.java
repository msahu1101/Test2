/**
 * 
 */
package com.mgm.services.booking.room.model.reservation;

import java.util.List;

import com.mgm.services.booking.room.model.response.FailedReservation;

import lombok.Data;

/**
 * @author laknaray
 *
 */
public @Data class PartyRoomReservation {

    private List<RoomReservation> roomReservations;
    private List<FailedReservation> failedReservations;
}
