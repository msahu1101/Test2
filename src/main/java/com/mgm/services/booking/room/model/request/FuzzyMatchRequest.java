package com.mgm.services.booking.room.model.request;

import java.util.List;

import lombok.Data;

/**
 * This is the request to invoke ID's Utility Service to perform fuzzy match.
 * 
 * @author laknaray
 *
 */
public @Data class FuzzyMatchRequest {

    private String firstName;
    private String lastName;
    private List<FuzzyNamesRequest> names;
    
}
