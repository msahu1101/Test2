package com.mgm.services.booking.room.model.response;

import java.util.List;

import lombok.Data;

/**
 * Response from ACRS Organization Search
 * 
 */
public @Data class OrganizationSearchV2Response {
    private String fullName;

    private List<String> iataCode;

    private String shortName;   
}
