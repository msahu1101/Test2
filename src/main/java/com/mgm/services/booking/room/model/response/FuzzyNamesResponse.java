package com.mgm.services.booking.room.model.response;

import java.util.List;

import lombok.Data;

/**
 * This is the response object returned as part of FuzzyMatchResponse.
 * 
 * @author laknaray
 *
 */
public @Data class FuzzyNamesResponse {

    private String id;
    private String firstName;
    private String lastName;
    private boolean match;
    private List<String> score;
    private List<String> result;
    
}
