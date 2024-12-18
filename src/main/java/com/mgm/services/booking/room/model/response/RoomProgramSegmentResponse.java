package com.mgm.services.booking.room.model.response;

import java.util.List;

import lombok.Data;

/**
 * Response class for segment 
 * @author nitpande0
 *
 */
public @Data class RoomProgramSegmentResponse {
    private String segmentId;
    private List<String> programIds;

}
