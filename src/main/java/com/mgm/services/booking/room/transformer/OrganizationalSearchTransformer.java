package com.mgm.services.booking.room.transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.model.crs.guestprofiles.OrganizationalAgencyID;
import com.mgm.services.booking.room.model.crs.guestprofiles.OrganizationalSearchResponse;
import com.mgm.services.booking.room.model.crs.guestprofiles.OrganizationalSummary;
import com.mgm.services.booking.room.model.response.IATAResponse;
import com.mgm.services.booking.room.model.response.OrganizationSearchV2Response;

import lombok.experimental.UtilityClass;

/**
 * Utility class providing functions for reservation object transformations
 * required for API outputs.
 *
 */
@UtilityClass
public class OrganizationalSearchTransformer {

    /**
     * Transforms ACRS OrganizationalSearchResponse object into RBS
     * OrganizationalSearchResponse
     * 
     * @param crsResponse
     *            ACRS Organizational Search Response object
     * @return List of Organization Search Response
     */
    public static List<OrganizationSearchV2Response> transform(OrganizationalSearchResponse crsResponse) {
        return crsResponse.getData().stream().map(organization -> acrsOrganizationalSummarytoResponse(organization))
                .collect(Collectors.toList());
    }

    /**
     * Conver from ACRS OrganizationSummary to Organization Response
     * 
     * @param organizationalSummary
     *            OrganizationalSummary Object
     * @return OrganizationSearchResponse
     */
    private static OrganizationSearchV2Response acrsOrganizationalSummarytoResponse(
            OrganizationalSummary organizationalSummary) {
        OrganizationSearchV2Response organizationSearchResponse = new OrganizationSearchV2Response();
        organizationSearchResponse
                .setFullName(organizationalSummary.getShortName());
        organizationSearchResponse.setShortName(
                organizationalSummary.getShortName() != null ? organizationalSummary.getShortName() : null);
         if (organizationalSummary.getAgencyID() != null && organizationalSummary.getAgencyID().get(0).getCode() != null) {
                organizationSearchResponse.setIataCode(
                        Collections.singletonList(organizationalSummary.getAgencyID().get(0).getCode()));
            }
        else if(organizationalSummary.getIataCodes()!= null) {
             organizationSearchResponse.setIataCode(organizationalSummary.getIataCodes());
         }
        else {
            organizationSearchResponse.setIataCode(null);
        }
        return organizationSearchResponse;
    }
    
    public static List<OrganizationSearchV2Response> transform(IATAResponse iataResponse){
    	final List<OrganizationSearchV2Response> organizationSearchResponseList = new ArrayList<OrganizationSearchV2Response>();
    	final OrganizationSearchV2Response organizationSearchV2Response = new OrganizationSearchV2Response();
    	
    	organizationSearchV2Response.setFullName(iataResponse.getTravelAgentName());
    	organizationSearchV2Response.setShortName(iataResponse.getTravelAgentName());
    	final List<String> iataCode = new ArrayList<String>();
    	iataCode.add(iataResponse.getTravelAgentId());
    	organizationSearchV2Response.setIataCode(iataCode);
    	
    	organizationSearchResponseList.add(organizationSearchV2Response);
    	return organizationSearchResponseList;
    }
}
