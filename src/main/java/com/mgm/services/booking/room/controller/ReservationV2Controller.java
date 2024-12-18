package com.mgm.services.booking.room.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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
import com.mgm.services.booking.room.model.request.CreatePartyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.CreateRoomReservationRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.reservation.AgentInfo;
import com.mgm.services.booking.room.model.response.CreatePartyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.service.IATAV2Service;
import com.mgm.services.booking.room.service.ReservationService;
import com.mgm.services.booking.room.validator.CreatePartyRoomReservationRequestValidator;
import com.mgm.services.booking.room.validator.CreateRoomReservationRequestValidator;
import com.mgm.services.booking.room.validator.MyVegasTokenScopes;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;

/**
 * Controller to handle room booking confirmation service v2 for checkout.
 *
 * @author laknaray
 * 
 */
@RestController
@RequestMapping("/v2")
@V2Controller
public class ReservationV2Controller extends ExtendedBaseV2Controller {

    private final Validator reservationRequestvalidator = new CreateRoomReservationRequestValidator();

    private final Validator partyValidator = new CreatePartyRoomReservationRequestValidator();

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private IATAV2Service iataService;
    
    @Autowired
    private TokenValidator tokenValidator;

    /**
     * Service to be called on checkout to confirm the booking. Accepts users
     * profile and billing information to process the reservation. Returns
     * booking error or confirmation number. If an iata code is attached, then
     * validate the same. If the code is not valid, then an exception will be
     * thrown. If the code is valid, the reservation flow will continue
     * 
     * @param source
     *            Source header
     * @param skipMyVegasConfirm
     *            whether to skip myVegas confirmation, mainly used for test
     *            cases to skip consuming a redemption code
     * @param createRoomReservationRequest
     *            Create Room Reservation request
     * @param servletRequest
     *            HttpServletRequest
     * @return Returns create room reservation response
     */
    @PostMapping("/reservation")
    public CreateRoomReservationResponse reservation(@RequestHeader String source, @RequestHeader(
            defaultValue = "false") String skipMyVegasConfirm,
            @RequestBody CreateRoomReservationRequest createRoomReservationRequest, HttpServletRequest servletRequest) {

        tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.CREATE_RESERVATION);
        if (StringUtils.isNotEmpty(createRoomReservationRequest.getRoomReservation().getMyVegasPromoCode())) {
            tokenValidator.validateTokens(servletRequest, MyVegasTokenScopes.values());
        }
        preprocess(source, createRoomReservationRequest.getRoomReservation(), null, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(createRoomReservationRequest.getRoomReservation(), createRoomReservationRequest.getRoomReservation().getPropertyId());
        Errors errors = new BeanPropertyBindingResult(createRoomReservationRequest, "createRoomReservationRequest");
        reservationRequestvalidator.validate(createRoomReservationRequest, errors);
        handleValidationErrors(errors);

        String agentId = getIataAgentId(createRoomReservationRequest.getRoomReservation());

        if (StringUtils.isNotBlank(agentId)) {
            iataService.validateCode(agentId);
        }
        return reservationService.makeRoomReservationV2(createRoomReservationRequest, skipMyVegasConfirm);
    }

    /**
     * Returns the IATA agentId
     */
    private String getIataAgentId(RoomReservationRequest roomReservationRequest) {
        String agentId = null;
        AgentInfo agentInfo = roomReservationRequest.getAgentInfo();
        if (null != agentInfo && StringUtils.equals("IATA", agentInfo.getAgentType())) {
            agentId = agentInfo.getAgentId();
        }
        return agentId;
    }

    /**
     * Creates party room reservation.
     * 
     * @param source
     *            Source header
     * @param skipMyVegasConfirm
     *            whether to skip myVegas confirmation, mainly used for test
     *            cases to skip consuming a redemption code
     * @param createPartyRoomReservationRequest
     *            Create Party Room Reservation request
     * @param servletRequest
     *            HttpServletRequest
     * @return Returns create party room reservation response
     */
    @PostMapping("/reservation/party")
    public CreatePartyRoomReservationResponse partyReservation(@RequestHeader String source, @RequestHeader(
            defaultValue = "false") String skipMyVegasConfirm,
            @RequestBody CreatePartyRoomReservationRequest createPartyRoomReservationRequest,
            HttpServletRequest servletRequest) {

        tokenValidator.validateServiceToken(servletRequest, RBSTokenScopes.CREATE_RESERVATION);
        preprocess(source, createPartyRoomReservationRequest.getRoomReservation(), null, servletRequest, null);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(createPartyRoomReservationRequest.getRoomReservation(), createPartyRoomReservationRequest.getRoomReservation().getPropertyId());
        Errors errors = new BeanPropertyBindingResult(createPartyRoomReservationRequest,
                "createPartyRoomReservationRequest");
        partyValidator.validate(createPartyRoomReservationRequest, errors);
        handleValidationErrors(errors);

        return reservationService.makePartyRoomReservation(createPartyRoomReservationRequest, skipMyVegasConfirm);
    }    
}
