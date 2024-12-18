package com.mgm.services.booking.room.dao;

import java.util.List;

import com.mgm.services.booking.room.model.phoenix.Property;

/**
 * DAO interface to expose service to retrieve all property info from Phoenix.
 *
 */
public interface PhoenixPropertyDAO {

    /**
     * Get the property configuration information from phoenix
     *
     * @return List of properties
     */
    List<Property> getProperties();
}
