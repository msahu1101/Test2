package com.mgm.services.booking.room.controller;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.OrganizationSearchV2Request;
import com.mgm.services.booking.room.model.response.OrganizationSearchV2Response;
import com.mgm.services.booking.room.service.IATAV2Service;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;


/**
 * Controller to generate token based on the credentials passed in the request.
 *
 */

@RestController
@RequestMapping("/v2")
@V2Controller
public class IATAV2Controller extends ExtendedBaseV2Controller {

    @Autowired
    private IATAV2Service iataService;
    
    @Autowired
	private TokenValidator tokenValidator;


    /**
     * Service to validate IATA code.
     * 
     * @param iataCode
     *            the iata code to validate
     * @return 204 if code is valid or 404 if it is not
     */
    @GetMapping("/iata/{iataCode}")
    @ResponseStatus(
            value = HttpStatus.NO_CONTENT)
    public void validateCode(@PathVariable String iataCode) {
        if (StringUtils.isBlank(iataCode)) {
            throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_IATA_CODE.getErrorCode()));
        }

        iataService.validateCode(iataCode);
    }
    
    
    /**
     * Lookup service to find organization by iata code and organization Name.
     * 
     * @param source
     *            Source header
     * @param organizationSearchRequest
     *            organization search Request
     * @param servletRequest
     *            HttpServlet request object
     * @return List of OrganizationSearchResponse
     */
    @GetMapping("/iata/organizations")
    public List<OrganizationSearchV2Response> organizationSearch(@RequestHeader String source,
            @Valid OrganizationSearchV2Request organizationSearchRequest, HttpServletRequest servletRequest) {
    	
    	tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_PROGRAMS);
    	preprocess(source, organizationSearchRequest, null);
    	//CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(organizationSearchRequest, null);
        
        if (StringUtils.isBlank(organizationSearchRequest.getIataCode()) && StringUtils.isBlank(organizationSearchRequest.getOrgName())) {
            throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_ORGANIZATION_SEARCH_REQUEST.getErrorCode()));
        }

        return iataService.organizationSearch(organizationSearchRequest);
    }  
}
