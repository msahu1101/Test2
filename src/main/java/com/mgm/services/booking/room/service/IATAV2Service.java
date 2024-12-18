package com.mgm.services.booking.room.service;

import java.util.List;

import com.mgm.services.booking.room.model.request.OrganizationSearchV2Request;
import com.mgm.services.booking.room.model.response.OrganizationSearchV2Response;

/**
 * Service interface that exposes service for validating IATA code
 * 
 */
public interface IATAV2Service {

    /**
     * Service method to validate the iata code
     * 
     * @param iataCode
     *            the code
     * @return Void response or error
     */
    void validateCode(String iataCode);
        
    /**
     * Service to find organization by iata code and organization Name.
     * 
     * @param organizationSearchRequest
     *            organization search Request
     * @return List of OrganizationSearchResponse
     */
    List<OrganizationSearchV2Response> organizationSearch(OrganizationSearchV2Request organizationSearchRequest);

}
