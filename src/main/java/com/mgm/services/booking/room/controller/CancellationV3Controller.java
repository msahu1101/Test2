package com.mgm.services.booking.room.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.mgm.services.booking.room.model.request.ReleaseV3Request;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import org.apache.commons.httpclient.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.CancelV3Request;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.service.CancelService;
import com.mgm.services.booking.room.util.TokenValidationUtil;
import com.mgm.services.booking.room.validator.CancelV3RequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;

import java.util.Arrays;

/**
 * Controller to handle room booking cancellation V3 related end points.
 * 
 * @author laknaray
 *
 */
@RestController
@RequestMapping("/v3")
@V2Controller
public class CancellationV3Controller extends ExtendedBaseV2Controller {

    private final Validator validator = new CancelV3RequestValidator();

    @Autowired
    private CancelService cancelService;
    
    @Autowired
    private TokenValidator tokenValidator;

    /**
     * Cancels existing room reservation.
     * 
     * @param source         Source header
     * @param cancelRequest  Cancel request object
     * @param result         binding validation result with errors.
     * @param servletRequest http request
     * @return Returns room reservation response with cancelled status
     */
    @PostMapping("/reservation/cancel")
    public CancelRoomReservationV2Response cancel(@RequestHeader String source,
            @RequestBody @Valid CancelV3Request cancelRequest, BindingResult result, HttpServletRequest servletRequest) {
        tokenValidator.validateTokenAnyScopes(servletRequest, Arrays.asList(RBSTokenScopes.CANCEL_RESERVATION.getValue(),
               RBSTokenScopes.UPDATE_RESERVATION_ELEVATED.getValue()));
        preprocess(source, cancelRequest, result, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(cancelRequest, cancelRequest.getPropertyId());
        // Validate and report errors
        Errors errors = new BeanPropertyBindingResult(cancelRequest, "CancelV3Request");
        validator.validate(cancelRequest, errors);
        handleValidationErrors(errors);

        String token = TokenValidationUtil.extractJwtToken(servletRequest);
        return cancelService.cancelReservation(cancelRequest, token);
    }

    /**
     * Ignores existing room reservation.
     *
     * @param source              Source header
     * @param releaseRequest      Release request object
     * @param result              binding validation result with errors.
     * @param servletRequest      http request
     * @param httpServletResponse
     * @param enableJwb           jwb flag
     * @return Returns HttpServletResponse
     */
    @DeleteMapping("/reservation/release")
    public void ignore(@RequestHeader String source,  @Valid ReleaseV3Request releaseRequest,
                       BindingResult result, HttpServletRequest servletRequest, HttpServletResponse httpServletResponse,
                       @RequestHeader(defaultValue = "false") String enableJwb) {
        tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.CANCEL_RESERVATION);
        preprocess(source, releaseRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(releaseRequest, releaseRequest.getPropertyId());
        if (null != result) {
            handleValidationErrors(result);
        }

        if(cancelService.ignoreReservationV3(releaseRequest)) {
            httpServletResponse.setStatus(HttpStatus.SC_NO_CONTENT);
        }
    }

    @PostMapping("/reservation/cancelF1")
    public CancelRoomReservationV2Response cancelF1(@RequestHeader String source,
                                                  @RequestBody @Valid CancelV3Request cancelRequest, BindingResult result, HttpServletRequest servletRequest) {
        preprocess(source, cancelRequest, result, servletRequest, null);
        preProcessPerpetualPricing(cancelRequest, cancelRequest.getPropertyId());
        // Validate and report errors
        Errors errors = new BeanPropertyBindingResult(cancelRequest, "CancelV3Request");
        validator.validate(cancelRequest, errors);
        handleValidationErrors(errors);

        String token = TokenValidationUtil.extractJwtToken(servletRequest);
        return cancelService.cancelReservationF1(cancelRequest, token);
    }
}
