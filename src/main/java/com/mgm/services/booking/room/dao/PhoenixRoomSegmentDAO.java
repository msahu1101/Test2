/**
 * 
 */
package com.mgm.services.booking.room.dao;

import java.util.List;

import com.mgm.services.booking.room.model.phoenix.RoomSegment;

/**
 * DAO interface to expose service to retrieve all room segments from Phoenix.
 *
 */
public interface PhoenixRoomSegmentDAO {

    /**
     * Gets the segments configuration information from phoenix.
     * 
     * @return Room segments
     */
    List<RoomSegment> getRoomSegments();

}
