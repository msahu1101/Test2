package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.TokenRequest;
import com.mgm.services.booking.room.model.request.TokenV2Request;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.model.response.TokenV2Response;

/**
 * Service interface that exposes service for validating client credentials with
 * Okta
 * 
 */
public interface TokenService {

    /**
     * Service method to validate Okta client credentials and return
     * access_token if it;s valid
     * 
     * @param tokenRequest
     *            Token request
     * @return Returns token response if client credentials are valid
     */
    TokenResponse generateToken(TokenRequest tokenRequest);
    
    /**
     * Service method to validate session based on osid or accessToken
     * 
     * @param tokenRequest
     *            Token request
     * @return Returns token response if client credentials are valid
     */
    TokenV2Response generateV2Token(TokenV2Request tokenRequest);

}
