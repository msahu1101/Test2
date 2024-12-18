package com.mgm.services.booking.room.controller;

import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.service.IATAV2Service;
import com.mgm.services.common.controller.BaseController;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Controller to generate token based on the credentials passed in the request.
 *
 */
@RestController
@RequestMapping("/v1")
public class IATAController extends BaseController {

    @Autowired
    private IATAV2Service iataService;

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

}
