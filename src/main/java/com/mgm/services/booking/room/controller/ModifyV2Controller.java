package com.mgm.services.booking.room.controller;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.response.RefundRoomReservationResponse;
import com.mgm.services.booking.room.validator.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.response.ModifyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.UpdateProfileInfoResponse;
import com.mgm.services.booking.room.service.ModifyReservationService;
import com.mgm.services.booking.room.util.TokenValidationUtil;

/**
 * Controller to update/modify room bookings.
 *
 * @author vararora
 *
 */
@RestController
@RequestMapping("/v2")
@V2Controller
public class ModifyV2Controller extends ExtendedBaseV2Controller {

    private final Validator updateProfileRequestvalidator = new UpdateProfileInfoRequestValidator();

    private final Validator modifyRoomValidator = new ModifyRoomReservationRequestValidator();
    
    private final Validator preModifyRequestValidator = new PreModifyRequestV2Validator();
    
    private final Validator commitValidator = new PreviewCommitRequestValidator();

    private final Validator associationRequestValidator = new ReservationAssociateRequestValidator();

    private final Validator refundValidator = new RefundRequestValidator();

    @Autowired
    private ModifyReservationService modifyReservationService;
    
    @Autowired
    private TokenValidator tokenValidator;

    /**
     * Updates profile info in room reservation, and returns updated room
     * reservation object.
     *
     * @param source
     *            Source header
     * @param updateProfileRequest
     *            update profile info request
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwb
     *            enableJwb header
     * @return Returns updated v2 room reservation object.
     */
    @PutMapping("/reservation/profile")
    public UpdateProfileInfoResponse updateProfileInfo(@RequestHeader String source,
            @RequestBody @Valid UpdateProfileInfoRequest updateProfileRequest, BindingResult result,
            HttpServletRequest servletRequest, @RequestHeader(
                    defaultValue = "false") String enableJwb) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.UPDATE_RESERVATION);
        preprocess(source, updateProfileRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(updateProfileRequest, updateProfileRequest.getPropertyId());
        Errors errors = new BeanPropertyBindingResult(updateProfileRequest, "UpdateProfileInfoRequest");
        updateProfileRequestvalidator.validate(updateProfileRequest, errors);
        handleValidationErrors(errors);

        String token = TokenValidationUtil.extractJwtToken(servletRequest);
        return modifyReservationService.updateProfileInfo(updateProfileRequest, token);
    }

    /**
     * Modify room reservation
     *
     * @param source
     *            Source header
     * @param modifyRoomReservationRequest
     *            Modify Party Room Reservation request
     * @param servletRequest
     *            HttpServletRequest
     * @return modify party room reservation response
     */
    @PutMapping("/reservation")
    public ModifyRoomReservationResponse modifyReservation(@RequestHeader String source,
            @RequestBody ModifyRoomReservationRequest modifyRoomReservationRequest, HttpServletRequest servletRequest) {

        tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.UPDATE_RESERVATION);
        preprocess(source, modifyRoomReservationRequest.getRoomReservation(), null, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(modifyRoomReservationRequest.getRoomReservation(), modifyRoomReservationRequest.getRoomReservation().getPropertyId());
        Errors errors = new BeanPropertyBindingResult(modifyRoomReservationRequest, "modifyRoomReservationRequest");
        modifyRoomValidator.validate(modifyRoomReservationRequest, errors);
        handleValidationErrors(errors);

        return modifyReservationService.modifyRoomReservationV2(modifyRoomReservationRequest);
    }
    
    /**
     * Preview the reservation for modify flow.
     * 
     * @param source source header.
     * @param preModifyRequest request.
     * @param servletRequest servlet request.
     * @return response response object.
     */
    @PutMapping("/reservation/preview")
    public ModifyRoomReservationResponse preModifyRoom(@RequestHeader String source,
            @RequestBody PreModifyV2Request preModifyRequest, HttpServletRequest servletRequest) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.UPDATE_RESERVATION);
        preprocess(source, preModifyRequest, null, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(preModifyRequest, preModifyRequest.getPropertyId());
        Errors errors = new BeanPropertyBindingResult(preModifyRequest, "PreModifyV2Request");
        preModifyRequestValidator.validate(preModifyRequest, errors);
        handleValidationErrors(errors);

        String token = TokenValidationUtil.extractJwtToken(servletRequest);
        return modifyReservationService.preModifyReservation(preModifyRequest, token);
    }
    
    /**
     * Commit the room reservation changes done via preview flow.
     * 
     * @param source source header.
     * @param commitRequest request.
     * @param servletRequest servlet request.
     * @return response response object.
     */
    @PutMapping("/reservation/commit")
    public ModifyRoomReservationResponse previewCommit(@RequestHeader String source,
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
        ModifyRoomReservationResponse response = modifyReservationService.commitReservation(commitRequest, token);

        // This is to send updated response with 400 status for price change
        // scenario
        if (null != response.getError()) {
            servletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        }
        return response;
    }

    /**
     * Associate a transient room reservation to a customer.
     * 
     * @param source source header
     * @param associationRequest request
     * @param result binding result
     * @param servletRequest servlet request
     * @return update profile info response object
     */
    @PutMapping("/reservation/associate")
    public UpdateProfileInfoResponse associateReservation(@RequestHeader String source,
            @RequestBody ReservationAssociateRequest associationRequest, BindingResult result,
            HttpServletRequest servletRequest) {
        
        tokenValidator.validateTokenAnyScopes(servletRequest, Arrays.asList(
               RBSTokenScopes.UPDATE_RESERVATION.getValue(), RBSTokenScopes.ALT_UPDATE_RESERVATION.getValue()));
        preprocess(source, associationRequest, null, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(associationRequest,null);
        Errors errors = new BeanPropertyBindingResult(associationRequest, "ReservationAssociateRequest");
        associationRequestValidator.validate(associationRequest, errors);
        handleValidationErrors(errors);

        String token = TokenValidationUtil.extractJwtToken(servletRequest);
        return modifyReservationService.associateReservation(associationRequest, token);
    }
    /**
     * Commit the room reservation changes done via preview flow.
     *
     * @param source source header.
     * @param commitRequest request.
     * @param servletRequest servlet request.
     * @return response response object.
     */
    @PutMapping("/reservation/commit/refund")
    public ModifyRoomReservationResponse commitRefund(@RequestHeader String source,
                                                      @RequestBody PaymentRoomReservationRequest refundRequest, HttpServletRequest servletRequest,
                                                      HttpServletResponse servletResponse) {
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.UPDATE_RESERVATION);
        preprocess(source, refundRequest, null, servletRequest, null);
        Errors errors = new BeanPropertyBindingResult(refundRequest, "CommitRefund");
        refundValidator.validate(refundRequest, errors);
        handleValidationErrors(errors);
        return modifyReservationService.commitPaymentReservation(refundRequest);
    }
}
