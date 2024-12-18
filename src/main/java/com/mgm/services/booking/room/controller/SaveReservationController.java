package com.mgm.services.booking.room.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.SaveReservationRequest;
import com.mgm.services.booking.room.model.response.SaveReservationResponse;
import com.mgm.services.booking.room.service.ReservationService;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.SaveReservationRequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;

/**
 * Controller to handle save room booking service v2 for checkout.
 *
 * @author jayveera
 * 
 */
@RestController
@RequestMapping("/v2")
@V2Controller
public class SaveReservationController extends ExtendedBaseV2Controller {

    private final Validator reservationRequestvalidator = new SaveReservationRequestValidator();
    
    @Autowired
    private ReservationService reservationService;
    
    @Autowired
    private TokenValidator tokenValidator;
    
    /**
     * Save the reservation as given in the request.
     * 
     * @param source source
     * @param saveReservationRequest request
     * @param servletRequest 
     * @return
     */
    @PostMapping("/reservation/save")
    public SaveReservationResponse saveReservation(@RequestHeader String source,
            @RequestBody SaveReservationRequest saveReservationRequest, HttpServletRequest servletRequest) {

        tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.UPDATE_RESERVATION);
        preprocess(source, saveReservationRequest.getRoomReservation(), null, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(saveReservationRequest.getRoomReservation(), saveReservationRequest.getRoomReservation().getPropertyId());
        Errors errors = new BeanPropertyBindingResult(saveReservationRequest, "saveReservationRequest");
        reservationRequestvalidator.validate(saveReservationRequest, errors);
        handleValidationErrors(errors);

        return reservationService.saveRoomReservation(saveReservationRequest);
    }

}
