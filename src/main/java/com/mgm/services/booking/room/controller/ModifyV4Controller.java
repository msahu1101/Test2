package com.mgm.services.booking.room.controller;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.ModifyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.PaymentRoomReservationRequest;
import com.mgm.services.booking.room.model.request.PreviewCommitRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.response.ModifyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RefundRoomReservationResponse;
import com.mgm.services.booking.room.service.ModifyReservationService;
import com.mgm.services.booking.room.util.TokenValidationUtil;
import com.mgm.services.booking.room.validator.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Controller to update/modify room bookings.
 *
 * @author vararora
 *
 */
@RestController
@RequestMapping("/v4")
@V2Controller
public class ModifyV4Controller extends ExtendedBaseV2Controller {

    private final Validator modifyRoomValidator = new ModifyRoomReservationRequestValidator();

    private final Validator commitValidator = new PreviewCommitRequestValidator();
    @Autowired
    private ModifyReservationService modifyReservationService;
    
    @Autowired
    private TokenValidator tokenValidator;
    private final Validator refundValidator = new RefundRequestValidator();

    /**
     *
     * @param source
     * @param modifyRoomReservationRequest
     * @param servletRequest
     * @return
     */
    @PutMapping("/reservation/pending")
    public ModifyRoomReservationResponse reservationModifyPending(@RequestHeader String source,
                                                           @RequestBody ModifyRoomReservationRequest modifyRoomReservationRequest, HttpServletRequest servletRequest) {

        tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.UPDATE_RESERVATION);
        preprocess(source, modifyRoomReservationRequest.getRoomReservation(), null, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(modifyRoomReservationRequest.getRoomReservation(), modifyRoomReservationRequest.getRoomReservation().getPropertyId());
        Errors errors = new BeanPropertyBindingResult(modifyRoomReservationRequest, "modifyRoomReservationRequest");
        modifyRoomValidator.validate(modifyRoomReservationRequest, errors);
        handleValidationErrors(errors);
        return modifyReservationService.reservationModifyPendingV4(modifyRoomReservationRequest);
    }
    @PutMapping("/reservation/commit")
    public ModifyRoomReservationResponse commitRefund(@RequestHeader String source,
                                                      @RequestBody PaymentRoomReservationRequest refundRequest, HttpServletRequest servletRequest,
                                                      HttpServletResponse servletResponse) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.UPDATE_RESERVATION);
        preprocess(source, refundRequest, null, servletRequest, null);
        Errors errors = new BeanPropertyBindingResult(refundRequest, "PaymentRoomReservationRequest");
        refundValidator.validate(refundRequest, errors);
        handleValidationErrors(errors);
        return modifyReservationService.commitPaymentReservation(refundRequest);
    }
    @PutMapping("/reservation/preview/pending")
    public ModifyRoomReservationResponse reservationModifyPreviewPending(@RequestHeader String source,
                                                                  @RequestBody PreviewCommitRequest commitRequest, HttpServletRequest servletRequest,
                                                                  HttpServletResponse servletResponse) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.UPDATE_RESERVATION);
        preprocess(source, commitRequest, null, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(commitRequest, commitRequest.getPropertyId());
        Errors errors = new BeanPropertyBindingResult(commitRequest, "PreviewCommitRequest");
        commitValidator.validate(commitRequest, errors);
        handleValidationErrors(errors);

        String token = TokenValidationUtil.extractJwtToken(servletRequest);
        ModifyRoomReservationResponse response = modifyReservationService.reservationModifyPendingV5(commitRequest, token);

        // This is to send updated response with 400 status for price change
        // scenario
        if (null != response.getError()) {
            servletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        }
        return response;
    }

}
