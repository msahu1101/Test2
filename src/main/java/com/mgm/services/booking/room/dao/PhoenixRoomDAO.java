/**
 * 
 */
package com.mgm.services.booking.room.dao;

import java.util.List;

import com.mgm.services.booking.room.model.phoenix.Room;

/**
 * DAO interface to expose services to retrieve room data from phoenix.
 *
 */
public interface PhoenixRoomDAO {

    /**
     * Returns rooms data by property from phoenix.
     * 
     * @param propertyId
     *            Property Identifier
     * @return Returns Rooms
     */
    List<Room> getRoomsByProperty(String propertyId);
}
