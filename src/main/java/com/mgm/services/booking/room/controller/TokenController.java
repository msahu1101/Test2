package com.mgm.services.booking.room.controller;

import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.model.request.TokenRequest;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.service.CustomerService;
import com.mgm.services.booking.room.service.TokenService;
import com.mgm.services.booking.room.validator.TokenRequestValidator;
import com.mgm.services.common.controller.BaseController;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

import lombok.extern.log4j.Log4j2;

/**
 * Controller to generate token based on the credentials passed in the request.
 *
 */
@RestController
@Log4j2
public class TokenController extends BaseController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private CustomerService customerService;

    private final Validator validator = new TokenRequestValidator();

    /**
     * Service to handle POST request of requesting a token. Invokes Okta
     * service to generate a token based on request parameters.
     * 
     * @param tokenRequest
     *            token request
     * @return Returns session in header
     */
    @PostMapping(
            value = { "/v1/token", "/v1/authorize" })
    @ResponseStatus(
            value = HttpStatus.NO_CONTENT)
    public void generateToken(@RequestBody TokenRequest tokenRequest) {

        Errors errors = new BeanPropertyBindingResult(tokenRequest, "tokenRequest");
        validator.validate(tokenRequest, errors);
        handleValidationErrors(errors);

        try {
            TokenResponse response = tokenService.generateToken(tokenRequest);
            populateCustomerDetails(response, tokenRequest);

        } catch (BusinessException ex) {
            log.error("User supplied credentials are not valid. {}", ex);
            throw new ValidationException(
                    Collections.singletonList(ErrorCode.INVALID_CLIENT_CREDENTIALS.getErrorCode()));
        }

    }

    private void populateCustomerDetails(TokenResponse response, TokenRequest tokenRequest) {
        log.debug("User supplied credentials are valid, session will be created");
        log.debug("Value of TokenId {}", response.getAccessToken());
        if (tokenRequest.getCustomerId() > 0) {
            sSession.setCustomer(customerService.getCustomerById(tokenRequest.getCustomerId()));
        } else if (StringUtils.isNotBlank(tokenRequest.getMlifeNumber())
                && Integer.parseInt(tokenRequest.getMlifeNumber()) > 0) {
            sSession.setCustomer(customerService.getCustomer(tokenRequest.getMlifeNumber()));
        } else {
            sSession.setTransientFlag(true);
        }

        // Recording okta token just for validation purposes in other APIs
        sSession.setOktaToken(response.getAccessToken());

    }

}
