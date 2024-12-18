package com.mgm.services.booking.room.dao;

import java.util.List;

import com.mgm.services.booking.room.model.request.OrganizationSearchV2Request;
import com.mgm.services.booking.room.model.response.IATAResponse;
import com.mgm.services.booking.room.model.response.OrganizationSearchV2Response;

/**
 * DAO interface for validating the iata code
 *
 */
public interface IATADAO {

    /**
     * Service method to validate the iata code
     * 
     * @param iataCode
     *            the code
     * @return the response from IATA
     */
    boolean validateCode(String iataCode);
    
    /**
     * Service to find organization by iata code and organization Name.
     * 
     * @param organizationSearchRequest
     *            organization search Request
     * @return List of OrganizationSearchResponse
     */
    List<OrganizationSearchV2Response> organizationSearch(OrganizationSearchV2Request organizationSearchRequest);

}
