package com.mgm.services.booking.room.controller;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.CancelV3Request;
import com.mgm.services.booking.room.model.request.CancelV4Request;
import com.mgm.services.booking.room.model.request.ReleaseV3Request;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.service.CancelService;
import com.mgm.services.booking.room.util.TokenValidationUtil;
import com.mgm.services.booking.room.validator.CancelV3RequestValidator;
import com.mgm.services.booking.room.validator.CancelV4RequestValidator;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;
import org.apache.commons.httpclient.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;

/**
 * Controller to handle room booking cancellation V3 related end points.
 * 
 * @author laknaray
 *
 */
@RestController
@RequestMapping("/v4")
@V2Controller
public class CancellationV4Controller extends ExtendedBaseV2Controller {

    private final Validator validator = new CancelV4RequestValidator();

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
    @PostMapping("/reservation/cancel/preview")
    public CancelRoomReservationV2Response cancelPreviewReservation(@RequestHeader String source,
                                                                    @RequestBody @Valid CancelV4Request cancelRequest, BindingResult result, HttpServletRequest servletRequest) {
        tokenValidator.validateTokenAnyScopes(servletRequest, Arrays.asList(RBSTokenScopes.CANCEL_RESERVATION.getValue(),
               RBSTokenScopes.UPDATE_RESERVATION_ELEVATED.getValue()));
        preprocess(source, cancelRequest, result, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(cancelRequest, cancelRequest.getPropertyId());
        // Validate and report errors
        Errors errors = new BeanPropertyBindingResult(cancelRequest, "CancelV4Request");
        validator.validate(cancelRequest, errors);
        handleValidationErrors(errors);
        String token = TokenValidationUtil.extractJwtToken(servletRequest);
        return cancelService.cancelPreviewReservation(cancelRequest, token);
    }
    @PostMapping("/reservation/cancel/commit")
    public CancelRoomReservationV2Response cancelCommit(@RequestHeader String source,
                                                         @RequestBody @Valid CancelV4Request cancelRequest, BindingResult result, HttpServletRequest servletRequest) {
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
        return cancelService.cancelCommitReservation(cancelRequest, token);
    }




}
