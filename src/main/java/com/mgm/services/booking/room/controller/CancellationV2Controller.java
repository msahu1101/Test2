package com.mgm.services.booking.room.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.httpclient.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.ReleaseV2Request;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.service.CancelService;
import com.mgm.services.booking.room.validator.CancelV2RequestValidator;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;

/**
 * Controller to handle room booking cancellation V2 related end points.
 * 
 * @author jayveera
 *
 */
@RestController
@RequestMapping("/v2/")
@V2Controller
public class CancellationV2Controller extends ExtendedBaseV2Controller {

    private final Validator validator = new CancelV2RequestValidator();

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
     * @param enableJwb      jwb flag
     * @return Returns room reservation response with cancelled status
     */
    @PostMapping("/reservation/cancel")
    public CancelRoomReservationV2Response cancel(@RequestHeader String source,
            @RequestBody @Valid CancelV2Request cancelRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(defaultValue = "false") String enableJwb) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.CANCEL_RESERVATION);
        preprocess(source, cancelRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(cancelRequest,cancelRequest.getPropertyId());

        // Validate and report errors
        Errors errors = new BeanPropertyBindingResult(cancelRequest, "CancelV2Request");
        validator.validate(cancelRequest, errors);
        handleValidationErrors(errors);

        return cancelService.cancelReservation(cancelRequest, false);
    }

	/**
	 * Ignores existing room reservation.
	 *
	 * @param source              Source header
	 * @param cancelRequest       Cancel request object
	 * @param result              binding validation result with errors.
	 * @param servletRequest      http request
	 * @param httpServletResponse
	 * @param enableJwb           jwb flag
	 * @return Returns HttpServletResponse
	 */
	@DeleteMapping("/reservation/release")
	public void ignore(@RequestHeader String source,  @Valid ReleaseV2Request releaseRequest,
			BindingResult result, HttpServletRequest servletRequest, HttpServletResponse httpServletResponse,
			@RequestHeader(defaultValue = "false") String enableJwb) {
		tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.CANCEL_RESERVATION);
		 preprocess(source, releaseRequest, result, servletRequest, enableJwb);
		 //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
	     preProcessPerpetualPricing(releaseRequest, releaseRequest.getPropertyId());
		if (null != result) {
			handleValidationErrors(result);
		}
        
		if(cancelService.ignoreReservation(releaseRequest)) {
			httpServletResponse.setStatus(HttpStatus.SC_NO_CONTENT);
		}
	}
    
}
