package com.mgm.services.booking.room.controller;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.RoomReservationBasicInfoRequest;
import com.mgm.services.booking.room.model.response.GetRoomReservationResponse;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;
import com.mgm.services.booking.room.service.FindReservationService;
import com.mgm.services.booking.room.validator.FindReservationV2RequestValidator;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;
/**
 * Controller to find reservation for v2 version with confirmation number.
 * 
 * @author jayveera
 *
 */
@RestController
@RequestMapping("/v2")
@V2Controller
public class FindReservationV2Controller extends ExtendedBaseV2Controller {

    private final FindReservationV2RequestValidator validator = new FindReservationV2RequestValidator();

    @Autowired
    private FindReservationService findReservationService;
    
    @Autowired
    private TokenValidator tokenValidator;
    
    /**
     * Returns the reservation response with v2 version of attributes for the given confirmation number.
     * 
     * @param source source
     * @param findReservationV2Request request
     * @param result result
     * @param servletRequest HttpServlet request object
     * @return RoomReservationResponseV2 v2 version response.
     */
    @GetMapping("/reservation")
    public GetRoomReservationResponse findRoomReservation(@RequestHeader String source,
            @Valid FindReservationV2Request findReservationV2Request, HttpServletRequest servletRequest,
            BindingResult result) {
        
        tokenValidator.validateTokenAnyScopes(servletRequest, Arrays.asList(RBSTokenScopes.GET_RESERVATION.getValue(),
                RBSTokenScopes.ALT_GET_RESERVATION.getValue(), RBSTokenScopes.GET_RESERVATION_ELEVATED.getValue()));
        preprocess(source, findReservationV2Request, result, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(findReservationV2Request, findReservationV2Request.getPropertyId());
        Errors errors = new BeanPropertyBindingResult(findReservationV2Request, "findReservationV2Request");
        validator.validate(findReservationV2Request, errors);
        handleValidationErrors(errors);

        return findReservationService.findRoomReservationResponse(findReservationV2Request);
    }
    
    /**
     * Returns the list of reservation basic details of party and share with
     * reservations for the given request.
     * 
     * @param source                          Source header
     * @param roomReservationBasicInfoRequest reservation request object
     * @param result                          Binding result
     * @param servletRequest                  HttpServlet request object
     * @param enableJwb                       enableJwb header
     * @return Returns the reservation basic information.
     * 
     */
    @GetMapping("/reservation/party/info")
    public ReservationsBasicInfoResponse getPartyOrShareWithReservations(@RequestHeader String source,
            @Valid RoomReservationBasicInfoRequest roomReservationBasicInfoRequest, BindingResult result,
            HttpServletRequest servletRequest, @RequestHeader(defaultValue = "false") String enableJwb) {
        tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.GET_RESERVATION);
        preprocess(source, roomReservationBasicInfoRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(roomReservationBasicInfoRequest, null);
        return findReservationService.getReservationBasicInfoList(roomReservationBasicInfoRequest);
    }
    
}
