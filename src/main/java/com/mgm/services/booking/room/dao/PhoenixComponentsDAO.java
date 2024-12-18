package com.mgm.services.booking.room.dao;

import java.util.Map;

import com.mgm.services.booking.room.model.phoenix.RoomComponent;

/**
 * DAO interface to expose service to retrieve all components from Phoenix.
 * 
 * @author laknaray
 *
 */
public interface PhoenixComponentsDAO {

    /**
     * Gets the room components.
     *
     * @return the room components
     */
    Map<String, RoomComponent> getRoomComponents();
}
