package com.mgm.services.booking.room.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.RoomReservationChargesRequest;
import com.mgm.services.booking.room.model.response.RoomReservationChargesResponse;
import com.mgm.services.booking.room.service.RoomReservationChargesService;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.RoomReservationChargesRequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;

/**
 * Controller class to handle all requests related to room reservation charges
 * 
 * @author swakulka
 *
 */

@RestController
@RequestMapping("/v2")
@V2Controller
public class RoomReservationChargesController extends ExtendedBaseV2Controller {

    RoomReservationChargesRequestValidator roomReservationChargesRequestValidator = new RoomReservationChargesRequestValidator();

    @Autowired
    private RoomReservationChargesService roomRervationChargesService;
    
    @Autowired
    private TokenValidator tokenValidator;

    /**
     * Method to intercept requests related to calculating room reservation charges.
     * 
     * @param authorization                 - Authorization token
     * @param source                        - Source
     * @param channel                       - Channel calling the endpoint
     * @param roomReservationChargesRequest - RoomReservationChargesRequest object
     * @param result                        - Binding result containing validation
     *                                      errors
     * @param servletRequest                - HttpServletRequest object
     * @return RoomReservationChargesResponse object
     */
    @PutMapping("/reservation/charges")
    public RoomReservationChargesResponse calculateRoomReservationCharges(@RequestHeader String authorization,
            @RequestHeader String source, @RequestHeader String channel,
            @RequestBody RoomReservationChargesRequest roomReservationChargesRequest, HttpServletRequest servletRequest) {
        tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.GET_RESERVATION_CHARGES);
        preprocess(source, roomReservationChargesRequest, null, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(roomReservationChargesRequest, roomReservationChargesRequest.getPropertyId());
        Errors errors = new BeanPropertyBindingResult(roomReservationChargesRequest, "roomReservationChargesRequest");
        roomReservationChargesRequestValidator.validate(roomReservationChargesRequest, errors);
        
        handleValidationErrors(errors);
        
        return roomRervationChargesService.calculateRoomReservationCharges(roomReservationChargesRequest);
    }
    
}
