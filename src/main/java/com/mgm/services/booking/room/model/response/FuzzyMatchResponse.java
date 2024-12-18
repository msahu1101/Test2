package com.mgm.services.booking.room.model.response;

import java.util.List;

import lombok.Data;

/**
 * This is the response object returned by ID's Utility Service fuzzy match.
 * 
 * @author laknaray
 *
 */
public @Data class FuzzyMatchResponse {

    private String firstName;
    private String lastName;
    private List<FuzzyNamesResponse> names;
    
}
