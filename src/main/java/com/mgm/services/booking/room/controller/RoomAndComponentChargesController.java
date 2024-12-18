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
import com.mgm.services.booking.room.model.request.CalculateRoomChargesRequest;
import com.mgm.services.booking.room.model.response.CalculateRoomChargesResponse;
import com.mgm.services.booking.room.service.RoomAndComponentChargesService;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.RoomAndComponentChargesRequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;

/**
 * Controller class to handle all requests related to room and component charges
 * 
 * @author uttam
 *
 */

@RestController
@RequestMapping("/v2")
@V2Controller
public class RoomAndComponentChargesController extends ExtendedBaseV2Controller {

    RoomAndComponentChargesRequestValidator roomAndComponentChargesRequestValidator = new RoomAndComponentChargesRequestValidator();
    @Autowired
    private RoomAndComponentChargesService roomAndComponentChargesService;

    @Autowired
    private TokenValidator tokenValidator;

    /**
     * Method to intercept requests related to calculating room reservation
     * charges including all available component prices.
     * @param authorization
     * @param source
     * @param channel
     * @param calculateRoomChargesRequest
     * @param servletRequest
     * @param enableJwb
     * @return
     */
    @PutMapping("/reservation/hold")
    public CalculateRoomChargesResponse calculateRoomAndComponentCharges(@RequestHeader String authorization,
            @RequestHeader String source, @RequestHeader String channel,
            @RequestBody CalculateRoomChargesRequest calculateRoomChargesRequest, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {
        tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.GET_RESERVATION_CHARGES);
        preprocess(source, calculateRoomChargesRequest, null, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(calculateRoomChargesRequest, calculateRoomChargesRequest.getPropertyId());

        Errors errors = new BeanPropertyBindingResult(calculateRoomChargesRequest, "calculateRoomChargesRequest");
        roomAndComponentChargesRequestValidator.validate(calculateRoomChargesRequest, errors);

        handleValidationErrors(errors);

        return roomAndComponentChargesService.calculateRoomAndComponentCharges(calculateRoomChargesRequest);
    }
}
