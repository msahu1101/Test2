package com.mgm.services.booking.room.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.model.request.CancelRequest;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CancelValidateResponse;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.CancelService;
import com.mgm.services.booking.room.service.ComponentService;
import com.mgm.services.booking.room.service.FindReservationService;
import com.mgm.services.booking.room.service.ReservationEmailService;
import com.mgm.services.booking.room.transformer.CancelValidateResponseTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.validator.CancelRequestValidator;
import com.mgm.services.common.controller.BaseController;
import com.mgm.services.common.model.ReservationCancellationDetails;

import lombok.extern.log4j.Log4j2;

/**
 * Controller to handle room booking cancellation related end points.
 *
 */
@RestController
@RequestMapping("/v1/reserve/room")
@Log4j2
public class CancellationController extends BaseController {

    private final Validator validator = new CancelRequestValidator();

    @Autowired
    private ApplicationProperties appProperties;

    @Autowired
    private FindReservationService findReservationService;

    @Autowired
    private CancelService cancelService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private ReservationEmailService emailService;

    /**
     * Validate if the room reservation is eligible for cancellation. If not
     * eligible, returns error code for reason. If eligible, return forfeit
     * amount and refund amount.
     * 
     * @param source
     *            Source header
     * @param validateRequest
     *            Request object
     * @param result
     *            Binding Result
     * @return Returns validation status and forfeit information as appropriate
     */
    @GetMapping("/cancel/validate")
    public CancelValidateResponse validateCancellation(@RequestHeader String source,
            @Valid CancelRequest validateRequest, BindingResult result) {

        preprocess(source, validateRequest, result);

        // Find reservation based on confirmation number and guest name
        final FindReservationRequest reservationRequest = new FindReservationRequest();
        reservationRequest.setSource(validateRequest.getSource());
        reservationRequest.setConfirmationNumber(validateRequest.getConfirmationNumber());
        reservationRequest.setFirstName(validateRequest.getFirstName());
        reservationRequest.setLastName(validateRequest.getLastName());

        RoomReservation reservation = findReservationService.findRoomReservation(reservationRequest);
        //RoomReservation reservation = cancelService.validateCancelReservation(validateRequest);

        putCancellationDetailsinSession(reservation);
        return CancelValidateResponseTransformer.getCancelValidateResponse(reservation, appProperties);

    }

    private void putCancellationDetailsinSession(RoomReservation reservation) {
        ReservationCancellationDetails details = new ReservationCancellationDetails();
        details.setConfirmationNumber(reservation.getConfirmationNumber());
        details.setFirstName(reservation.getProfile().getFirstName());
        details.setLastName(reservation.getProfile().getLastName());
        details.setItineraryId(reservation.getItineraryId());
        details.setReservationId(reservation.getId());
        details.setCustomerId(reservation.getProfile().getId());

        sSession.getReservationCancellationDetails().put(reservation.getConfirmationNumber(), details);

    }

    private void populateCancellationDetailsFromSession(CancelRequest cancelRequest) {
        ReservationCancellationDetails details = sSession.getReservationCancellationDetails()
                .get(cancelRequest.getConfirmationNumber());
        if (details != null) {
            cancelRequest.setReservationId(details.getReservationId());
            cancelRequest.setItineraryId(details.getItineraryId());
            cancelRequest.setCustomerId(details.getCustomerId());
        }
    }

    private void removeCancellationDetailsFromSession(String confirmationNumber) {
        sSession.getReservationCancellationDetails().remove(confirmationNumber);
    }

    /**
     * Cancels existing room reservation.
     * 
     * @param source
     *            Source header
     * @param cancelRequest
     *            Cancel request object
     * @return Returns room reservation response with cancelled status
     */
    @PostMapping("/cancel")
    public RoomReservationResponse cancel(@RequestHeader String source, @RequestBody CancelRequest cancelRequest) {

        preprocess(source, cancelRequest, null);

        // Validate and report errors
        final Errors errors = new BeanPropertyBindingResult(cancelRequest, "cancelRequest");
        validator.validate(cancelRequest, errors);
        handleValidationErrors(errors);

        populateCancellationDetailsFromSession(cancelRequest);
        final String confirmationNumber = cancelRequest.getConfirmationNumber();

        RoomReservation reservation = cancelService.cancelReservation(cancelRequest);

        final RoomReservationResponse response = RoomReservationTransformer.transform(reservation, appProperties);

        log.info("Financial Impact:{}", ReservationUtil.getCancelFinancialImpact(reservation, response, appProperties));
        // Send cancellation email
        emailService.sendCancellationEmail(reservation, response, reservation.getPropertyId());

        // Returns the room components attached to the reservation
        final List<RoomRequest> roomRequests = new ArrayList<>();
        final List<String> componentIds = reservation.getSpecialRequests();
        componentIds
                .forEach(id -> roomRequests.add(componentService.getRoomComponent(reservation.getRoomTypeId(), id)));

        response.setRoomRequests(roomRequests);
        removeCancellationDetailsFromSession(confirmationNumber);

        return response;
    }

}
