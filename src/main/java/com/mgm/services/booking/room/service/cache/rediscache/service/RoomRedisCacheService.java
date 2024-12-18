package com.mgm.services.booking.room.service.cache.rediscache.service;

import com.mgm.services.booking.room.model.phoenix.Room;

/**
 * Service interface exposing services for room cache services.
 *
 */
public interface RoomRedisCacheService {

    /**
     * Return room data from cache.
     * 
     * @param roomTypeId
     *            Room type id
     * @return Room information
     */
    Room getRoom(String roomTypeId);

    /**
     * Return room data from cache.
     * 
     * @param roomCode
     *            Opera Room Code
     * @return Room information
     */
    Room getRoomByRoomCode(String roomCode);
}
