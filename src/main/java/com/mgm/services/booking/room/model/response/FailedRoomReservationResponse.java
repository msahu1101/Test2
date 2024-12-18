package com.mgm.services.booking.room.model.response;

import com.mgm.services.booking.room.model.FailureReason;

import lombok.Data;

/**
 * Model to hold failed room booking details
 * @author nitpande0
 *
 */
public @Data class FailedRoomReservationResponse {
    private String itemId;
    private FailureReason failure;
}
