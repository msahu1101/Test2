package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.TokenRequest;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.model.response.TokenV2Response;

/**
 * DAO interface to expose services to generate token based on the client
 * credentials.
 *
 */
public interface TokenDAO {

    /**
     * DAO method to validate Okta client credentials by calling Okta's token
     * endpoint and return access_token if it;s valid
     * 
     * @param tokenRequest
     *            Token request
     * @return Returns token response if client credentials are valid
     */
    TokenResponse generateToken(TokenRequest tokenRequest);

    /**
     * DAO method to validate Okta session id by calling Okta's session endpoint
     * 
     * @param sessionId
     *            the session id to validate
     * @return response from Okta
     */
    TokenV2Response validateOktaSession(String sessionId);

    /**
     * DAO method to fetch user details from Okta based on the user's email id
     * 
     * @param emailId
     *            the email id to fetch the details for
     * @return response from Okta
     */
    TokenV2Response fetchUserDetails(String emailId);

    /**
     * DAO method to validate Okta access token by calling Okta's access token
     * endpoint
     * 
     * @param accessToken
     *            the accessToken to validate
     * @return response from Okta
     */
    TokenV2Response validateOktaAccessToken(String accessToken);

}
