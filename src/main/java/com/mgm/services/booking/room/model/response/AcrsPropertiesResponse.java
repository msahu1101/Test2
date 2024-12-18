package com.mgm.services.booking.room.model.response;

import lombok.Data;

import java.util.List;

/**
 *  Health Check response returning list of properties configured under ACRS
 *
 */
public @Data class AcrsPropertiesResponse {
    private List<String> acrsProperties;
}
