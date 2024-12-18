package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.content.Room;

/**
 * DAO interface to expose services for retrieving room marketing content.
 *
 */
public interface RoomContentDAO {

    /**
     * Gets and returns room marketing content for the requested room type id.
     * Service relies on content api end points to fetch required attributes.
     * 
     * @param roomId
     *            Room type id
     * @return Basic room information
     */
    Room getRoomContent(String roomId);

	Room getRoomContent(String operaCode, String operaPropertyCode, boolean isPrimary);
}
