package com.mgm.services.booking.room.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.request.TokenV2Request;
import com.mgm.services.booking.room.model.response.TokenV2Response;
import com.mgm.services.booking.room.service.CustomerService;
import com.mgm.services.booking.room.service.TokenService;
import com.mgm.services.common.controller.BaseController;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

import lombok.extern.log4j.Log4j2;

/**
 * Controller to generate token based on the credentials passed in the request.
 *will remove Nullable
 */
@RestController
@RequestMapping("/v2")
@Log4j2
public class TokenV2Controller extends BaseController {
	@Nullable
    @Autowired
    private TokenService tokenService;
	@Nullable
    @Autowired
    private CustomerService customerService;
	@Nullable
    @Autowired
    private RoomCartController cartController;

    /**
     * Service to handle POST request of requesting a token. Invokes Okta
     * service to generate a token based on request parameters. If the user has
     * not specified
     * 
     * @param request
     *            servlet request
     * @param headers
     *            all the headers
     * @param osidCookie
     *            osid cookie value
     * @param jwtCookie
     *            access token cookie value
     */
    @PostMapping("/token")
    @ResponseStatus(
            value = HttpStatus.NO_CONTENT)
    public void generateToken(HttpServletRequest request, @RequestHeader Map<String, String> headers, @CookieValue(
            name = "osid",
            required = false) String osidCookie,
            @CookieValue(
                    name = "access_token",
                    required = false) String jwtCookie) {
        log.info("Request Headers :: {}", headers);

        processInput(headers, osidCookie, jwtCookie);

    }

    /**
     * Service to handle PUT request of requesting a token. Invokes Okta service
     * to generate a token based on request parameters. The API will generate a
     * new x-state-token for guest or logged in users based on passed
     * credentials
     * 
     * @param request
     *            http request
     * @param headers
     *            all the headers
     * @param osidCookie
     *            value for osid cookie
     * @param state
     *            value for x-state-token cookie
     * @param jwtCookie
     *            access token cookie value
     * @return Returns session in header
     */
    @PutMapping("/token")
    @ResponseStatus(
            value = HttpStatus.NO_CONTENT)
    public void updateToken(HttpServletRequest request, @RequestHeader Map<String, String> headers, @CookieValue(
            name = "osid",
            required = false) String osidCookie,
            @CookieValue(
                    name = "x-state-token",
                    required = false) String state,
            @CookieValue(
                    name = "access_token",
                    required = false) String jwtCookie) {
        log.info("Request Headers :: {}", headers);
        log.info("State Cookie :: {}", state);

        // X-STATE-TOKEN is mandatory
        if (StringUtils.isBlank(headers.get(ServiceConstant.X_STATE_TOKEN)) && StringUtils.isBlank(state)) {
            throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_STATE_TOKEN.getErrorCode()));
        }

        processInput(headers, osidCookie, jwtCookie);

        request.changeSessionId();

    }

    private void processInput(@RequestHeader Map<String, String> headers, String osidCookie, String jwtCookie) {

        // If osid is not in header, check in cookies. Fallback for DMP/Gen3
        String osid = headers.get(ServiceConstant.HEADER_OKTA_SESSION_ID);
        if (StringUtils.isEmpty(osid) && StringUtils.isNotEmpty(osidCookie)) {
            osid = osidCookie.replaceAll("\"", "").split("\\|")[0];
        } else if (StringUtils.isNotEmpty(osid)) {
            osid = osid.replaceAll("\"", "").split("\\|")[0];
        }

        // if access token is not in header, check in cookies. Fallback for Gen3
        // webview
        String accessToken = headers.get(ServiceConstant.HEADER_OKTA_ACCESS_TOKEN);
        if (StringUtils.isEmpty(osid) && StringUtils.isNotEmpty(jwtCookie)) {
            accessToken = jwtCookie;
        }

        TokenV2Request tokenRequest = new TokenV2Request();
        tokenRequest.setAccessToken(accessToken);
        tokenRequest.setOktaSessionId(osid);

        log.info("OSID::{}, ACCESS_TOKEN:: {}", osid, tokenRequest.getAccessToken());

        if (StringUtils.isNotBlank(tokenRequest.getOktaSessionId())
                || StringUtils.isNotBlank(tokenRequest.getAccessToken())) {
            TokenV2Response response = tokenService.generateV2Token(tokenRequest);
            setSessionInfo(response);
        } else {
            setTransientInfo();
        }
    }

    private void setSessionInfo(TokenV2Response response) {

        if (response.getProfile() != null && StringUtils.isNotBlank(response.getProfile().getMlifeNumber())) {
            sSession.setCustomer(customerService.getCustomer(response.getProfile().getMlifeNumber()));
        }

        sSession.setTransientFlag(response.isTransientFlag());
        log.info("Updated session info");

        // re-prices room items in cart
        cartController.repriceRoomsOnLogin();

    }

    private void setTransientInfo() {

        // clearing cart items if the user was logged-in before
        if (null != sSession.getCustomer() && sSession.getCustomer().getMlifeNumber() > 0) {
            log.info("User was logged-in before with mlife number: {}. Session will be cleared",
                    sSession.getCustomer().getMlifeNumber());
            sSession.setCartItems(new ArrayList<>());
        }

        sSession.setCustomer(null);
        sSession.setTransientFlag(true);
        log.info("Updated transient session info");

    }

}
