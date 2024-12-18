package com.mgm.services.booking.room.model.request;

import lombok.Data;

/**
 * This is the request object used as part of FuzzyMatchRequest.
 * 
 * @author laknaray
 *
 */
public @Data class FuzzyNamesRequest {

    private String id;
    private String firstName;
    private String lastName;
    
}
