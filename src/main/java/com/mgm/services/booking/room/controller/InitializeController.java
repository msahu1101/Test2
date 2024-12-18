package com.mgm.services.booking.room.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.service.TpsInitializationService;

/**
 * Controller to re-initialize connection.
 * 
 * @author laknaray
 */
@RestController
@RequestMapping(value = "/v1")
public class InitializeController {

    @Autowired
    private TpsInitializationService tpsService;
    
    /**
     * Re-initiate tps connections on demand.
     * 
     * @return 
     */
    @PostMapping("/initialize")
    public String initializeTpsConnection() {
        return tpsService.reinitializeConnections();
    }
}
