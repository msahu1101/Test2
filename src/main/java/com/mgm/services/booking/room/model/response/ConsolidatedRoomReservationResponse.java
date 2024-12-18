package com.mgm.services.booking.room.model.response;

import java.util.ArrayList;
import java.util.List;

import com.mgm.services.common.model.Message;

import lombok.Data;

/**
 * Consolidated response of failed and successful bookings
 * @author nitpande0
 *
 */
public @Data class ConsolidatedRoomReservationResponse {
    private List<RoomReservationResponse> booked = new ArrayList<>();
    private List<FailedRoomReservationResponse> failed = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();
}
