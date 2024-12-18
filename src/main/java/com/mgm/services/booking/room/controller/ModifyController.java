package com.mgm.services.booking.room.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.RoomCartItem;
import com.mgm.services.booking.room.model.request.ModifyRequest;
import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.Payment;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.ComponentService;
import com.mgm.services.booking.room.service.ModifyReservationService;
import com.mgm.services.booking.room.service.ReservationEmailService;
import com.mgm.services.booking.room.transformer.RoomComponentRequestTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.validator.PreModifyRequestValidator;
import com.mgm.services.common.controller.BaseController;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Controller to handle modify operations on room booking.
 *
 */
@RestController
@RequestMapping("/v1/reserve")
public class ModifyController extends BaseController {

    private final Validator validator = new PreModifyRequestValidator();

    @Autowired
    private ModifyReservationService modifyService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private ReservationEmailService emailService;
    
    @Autowired
    private ApplicationProperties appProperties;

    /**
     * Update attributes on the room reservation to find updated room charges
     * and taxes. Used to show users updated charges/totals before confirming on
     * the modification. Currently modification is restricted to trip dates and
     * room requests.
     * 
     * @param source
     *            Source header
     * @param preModifyRequest
     *            Pre-modify request
     * @return Returns modified room reservation response with updated
     *         prices/totals.
     */
    @PostMapping("/room/pre-modify")
    public RoomReservationResponse preModifyRoom(@RequestHeader String source,
            @RequestBody PreModifyRequest preModifyRequest) {

        preprocess(source, preModifyRequest, null);
        Errors errors = new BeanPropertyBindingResult(preModifyRequest, "preModifyRequest");
        validator.validate(preModifyRequest, errors);
        handleValidationErrors(errors);

        RoomReservation reservation = modifyService.preModifyReservation(preModifyRequest);

        RoomReservationResponse response = RoomReservationTransformer.transform(reservation, appProperties);

        // Construct the room component request
        RoomComponentRequest componentRequest = RoomComponentRequestTransformer.getRoomComponentRequest(source,
                response);

        // Get the available room requests along with existing ones on the
        // reservation
        List<RoomRequest> roomRequests = getRoomRequests(reservation, componentRequest);

        response.setRoomRequests(roomRequests);

        // Save the reservation object as modify item into session
        RoomCartItem cartItem = addToCart(reservation, response);

        sSession.setModifyItem(cartItem);

        return response;

    }

    private RoomCartItem addToCart(RoomReservation reservation, RoomReservationResponse response) {
        RoomCartItem cartItem = new RoomCartItem();
        cartItem.setReservation(reservation);
        cartItem.setReservationId(response.getItemId());
        return cartItem;
    }

    private List<RoomRequest> getRoomRequests(RoomReservation reservation, RoomComponentRequest componentRequest) {
        
        // Special handling for borgata
        ReservationUtil.removeSpecialRequests(reservation, appProperties);
        
        List<String> componentIds = reservation.getSpecialRequests();
        List<RoomRequest> roomRequests = new ArrayList<>();
        componentService.getAvailableRoomComponents(componentRequest).forEach(roomRequest -> {
            if (componentIds.contains(roomRequest.getId())) {
                roomRequest.setSelected(true);
            }
            roomRequests.add(roomRequest);
        });
        return roomRequests;
    }

    /**
     * Confirms changes to existing room reservation which was previewed via
     * POST /v1/reserve/room/pre-modify e.g., updates to check-in/check-out
     * dates.
     * 
     * @param source
     *            Source header
     * @param modifyRequest
     *            Modify request object
     * @return Returns modified room reservation response
     */
    @PutMapping("/room")
    public RoomReservationResponse modifyRoom(@RequestHeader String source,
            @RequestBody ModifyRequest modifyRequest) {

        preprocess(source, modifyRequest, null);

        RoomCartItem item = null;
        if (sSession.getModifyItem() instanceof RoomCartItem) {
            item = (RoomCartItem) sSession.getModifyItem();
        }

        // Performs initial validation of cart info availability
        if (StringUtils.isEmpty(modifyRequest.getReservationId())) {
            throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_RESERVE.getErrorCode()));
        } else if (null == item || !item.getReservationId().equals(modifyRequest.getReservationId())) {
            throw new ValidationException(Collections.singletonList(ErrorCode.ITEM_NOT_FOUND.getErrorCode()));
        }

        RoomReservation roomReservation = item.getReservation();
        roomReservation.setCreditCardCharges(getPayments(roomReservation));

        RoomReservation reservation = modifyService.modifyReservation(source, roomReservation);
        RoomReservationResponse response = RoomReservationTransformer.transform(reservation, appProperties);

        // Send confirmation email
        emailService.sendConfirmationEmail(reservation, response, reservation.getPropertyId());

        // Remove the modified item from session
        sSession.setModifyItem(null);

        List<RoomRequest> roomRequests = new ArrayList<>();
        List<String> componentIds = reservation.getSpecialRequests();
        componentIds
                .forEach(id -> roomRequests.add(componentService.getRoomComponent(reservation.getRoomTypeId(), id)));

        response.setRoomRequests(roomRequests);

        return response;
    }

    /**
     * Construct credit card charges based on information from room reservation
     * object.
     * 
     * @param roomReservation
     *            Room Reservation Object
     * @return List of credit card charges
     */
    private List<CreditCardCharge> getPayments(RoomReservation roomReservation) {

        double amountPaid = ServiceConstant.ZERO_DOUBLE_VALUE;
        for (Payment payment : roomReservation.getPayments()) {
            amountPaid += payment.getChargeAmount();
        }
        double additionalCharge = roomReservation.getDepositCalc().getAmount() - amountPaid;
        if (additionalCharge < 0) {
            additionalCharge = 0;
        }

        CreditCardCharge existingCard = roomReservation.getCreditCardCharges().get(0);

        CreditCardCharge cardCharge = new CreditCardCharge();
        cardCharge.setAmount(additionalCharge);
        cardCharge.setCvv(existingCard.getCvv());
        cardCharge.setExpiry(existingCard.getExpiry());
        cardCharge.setHolder(existingCard.getHolder());
        cardCharge.setMaskedNumber(existingCard.getMaskedNumber());
        cardCharge.setNumber(existingCard.getNumber());
        cardCharge.setType(existingCard.getType());

        List<CreditCardCharge> cardChargesList = new ArrayList<>();
        cardChargesList.add(cardCharge);

        return cardChargesList;
    }
}
