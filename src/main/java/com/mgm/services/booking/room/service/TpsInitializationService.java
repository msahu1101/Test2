package com.mgm.services.booking.room.service;

/**
 * Service to call AuroraBaseDAO.
 * 
 * @author laknaray
 *
 */
public interface TpsInitializationService {

    /**
     * Re-initializes the tps connections.
     * 
     * @return status message as string.
     */
    String reinitializeConnections();    
}
