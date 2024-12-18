package com.mgm.services.booking.room.dao;

import java.util.List;

import com.mgm.services.booking.room.model.content.Property;

/**
 * DAO interface to expose services for retrieving property marketing content.
 *
 */
public interface PropertyContentDAO {

    /**
     * Gets and returns basic property marketing content for all properties in
     * DMP. Service relies on the content api end points for retrieving this
     * information.
     * 
     * @return Returns basic property information of all properties
     */
    List<Property> getAllPropertiesContent();
}
