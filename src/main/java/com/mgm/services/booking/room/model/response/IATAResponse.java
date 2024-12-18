package com.mgm.services.booking.room.model.response;

import lombok.Data;

/**
 * Response from IATA
 * @author nitpande0
 *
 */
public @Data class IATAResponse {
    private String address1;

    private String address2;

    private String city;

    private String country;

    private String id;

    private String lastModifiedByTime;

    private String lastModifiedByUser;

    private String operaNameId;

    private String primaryContactNumber;

    private String primaryEmailAddress;

    private String state;

    private String travelAgentId;

    private String travelAgentName;

    private String zip;

}
