package com.mgm.services.booking.room.model.event;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * Reservation event request wrapper class to wrap original request with other
 * needed parameters
 * 
 * @author vararora
 *
 */

public @Data class ReservationEventRequestWrapper {
    
    private String callbackUrl;
    private Map<String,String> headers = new HashMap<>();
    private EventBody body;
    private DataGovernance dataGovernance;
    
    @Data
    public class EventBody {
        private RoomReservationEventRequest roomReservation;
    }

}