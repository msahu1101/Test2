package com.mgm.services.booking.room.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.TokenDAO;
import com.mgm.services.booking.room.model.request.TokenRequest;
import com.mgm.services.booking.room.model.request.TokenV2Request;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.model.response.TokenV2Response;
import com.mgm.services.booking.room.service.TokenService;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class to provide service for validating Okta client
 * credentials and return token response
 */
@Component
@Log4j2
public class TokenServiceImpl implements TokenService {

    @Autowired
    private TokenDAO tokenDAO;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.TokenService#generateToken(com.mgm.
     * services.booking.room.model.request.TokenRequest)
     */
    @Override
    public TokenResponse generateToken(TokenRequest tokenRequest) {

        return tokenDAO.generateToken(tokenRequest);

    }

    @Override
    public TokenV2Response generateV2Token(TokenV2Request tokenRequest) {

        if (StringUtils.isNotBlank(tokenRequest.getOktaSessionId())) {
            return processOktaSessionId(tokenRequest);
        } else {
            return processOktaAccessToken(tokenRequest);
        }
    }

    private TokenV2Response processOktaSessionId(TokenV2Request tokenRequest) {

        TokenV2Response response = tokenDAO.validateOktaSession(tokenRequest.getOktaSessionId());

        if (null != response && StringUtils.isNotBlank(response.getLogin())) {

            return tokenDAO.fetchUserDetails(response.getLogin());
        }

        return null;

    }

    private TokenV2Response processOktaAccessToken(TokenV2Request tokenRequest) {

        TokenV2Response response = tokenDAO.validateOktaAccessToken(tokenRequest.getAccessToken());

        log.info("Access token resolved. {}", response);
        return tokenDAO.fetchUserDetails(response.getEmailId());

    }

}
