package com.mgm.services.booking.room.dao;

import java.util.List;

import com.mgm.services.booking.room.model.phoenix.RoomProgram;

/**
 * DAO interface to expose service to retrieve all room programs from Phoenix.
 *
 */
public interface PhoenixRoomProgramDAO {

    /**
     * Get the room programs configuration information from phoenix
     *
     * @return List of room programs
     */
    List<RoomProgram> getRoomPrograms();
}
