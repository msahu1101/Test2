package com.mgm.services.booking.room.service.cache;

import com.mgm.services.booking.room.model.phoenix.RoomComponent;

import java.util.List;

/**
 * The Interface PhoenixComponentsCacheService.
 *
 * @author laknaray
 *
 */
public interface PhoenixComponentsCacheService {

    /**
     * Gets the component.
     *
     * @param compId
     *            the comp id
     * @return the component
     */
    public RoomComponent getComponent(String compId);

    /**
     * Return component data from cache.
     *
     * @param externalCode
     *            External Code
     * @return List of RoomComponent information
     */
    public List<RoomComponent> getComponentsByExternalCode(String externalCode);
}
