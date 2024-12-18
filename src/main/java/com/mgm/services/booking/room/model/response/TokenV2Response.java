package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Token V2 Response class
 * 
 * @author nitpande0
 *
 */
public @Data class TokenV2Response {
    private String id;
    private String login;
    private String userId;
    private String status;
    private Profile profile;
    @JsonProperty("preferred_username")
    private String emailId;
    @JsonProperty("access_token")
    private String accessToken;
    private boolean transientFlag;
    
    public static @Data class Profile {
        @JsonProperty("mlifeNumber")
        private String mlifeNumber;
        
    }
}


