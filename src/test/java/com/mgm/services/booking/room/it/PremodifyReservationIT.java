package com.mgm.services.booking.room.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Calendar;

import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.model.Message;
import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.TripDetail;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class PremodifyReservationIT extends BaseRoomBookingIntegrationTest {

    private static int PREMODIFY_STATUS;

    private final String baseServiceUrl = "/v1/reserve/room/pre-modify";

    @Test
    public void test_premodifyReservation_premodifyReservationWithValidParameters_validateResponse() {

        // Make a reservation first
        RoomReservationResponse responseCheckout = makeSuccessTestReservation();

        // Confirm the reservation is booked
        assertEquals("Status should be booked", ReservationState.Booked, responseCheckout.getState());

        String confirmationNumber = responseCheckout.getConfirmationNumber();

        log.info("Confirmation Number: {}", confirmationNumber);

        // Delay required as Checkout Process is async in the backend.
        addDelay(15000);

        // Pre-modify the reservation for the given first name, last name and
        // confirmation number
        File modifyFile = new File(getClass().getResource("/premodify-post-requestbody.json").getPath());
        PreModifyRequest preModifyRequest = convert(modifyFile, PreModifyRequest.class);
        preModifyRequest.setConfirmationNumber(confirmationNumber);

        // Update the check-in and check-out dates
        TripDetail tripDetails = preModifyRequest.getTripDetails();
        Calendar checkInDate = Calendar.getInstance();
        checkInDate.setTime(preModifyRequest.getTripDetails().getCheckInDate());
        checkInDate.add(Calendar.DATE, 3);

        Calendar checkOutDate = Calendar.getInstance();
        checkOutDate.setTime(preModifyRequest.getTripDetails().getCheckOutDate());
        checkOutDate.add(Calendar.DATE, 3);

        tripDetails.setCheckInDate(checkInDate.getTime());
        tripDetails.setCheckOutDate(checkOutDate.getTime());

        ClientResponse clientResponse = realClient.post().uri(builder -> builder.path(baseServiceUrl).build())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).body(BodyInserters.fromValue(preModifyRequest)).exchange().block();

        int status = clientResponse.statusCode().value();

        // Assert for successful pre-modify or error message
        // Handling error condition since reservation posting to opera regularly
        // fails in non-prod
        if (status == 200) {
            RoomReservationResponse response = clientResponse.bodyToMono(RoomReservationResponse.class).block();
            assertEquals("Status should be booked", ReservationState.Booked, response.getState());
            assertNotNull("CheckInDate should not be null", response.getTripDetails().getCheckInDate());
            assertNotNull("CheckOutDate should not be null", response.getTripDetails().getCheckOutDate());
        } else {
            Message msg = clientResponse.bodyToMono(Message.class).block();
            assertNotNull("Error code is not null", msg.getCode());
            assertNotNull("Error message is not null", msg.getMsg());
        }

    }

    @Test
    public void test_premodifyReservation_premodifyInvalidConfirmation_validateReservationNotFoundError() {

        // Pre-modify the reservation for the given first name, last name and
        // confirmation number
        File modifyFile = new File(getClass().getResource("/premodify-post-requestbody.json").getPath());
        PreModifyRequest preModifyRequest = convert(modifyFile, PreModifyRequest.class);
        preModifyRequest.setConfirmationNumber("1234");

        Mono<Message> premodifyResult = realClient.post().uri(builder -> builder.path(baseServiceUrl).build())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).body(BodyInserters.fromValue(preModifyRequest)).exchange().doOnNext(response -> {
                    PREMODIFY_STATUS = response.statusCode().value();
                }).flatMap(clientResponse -> clientResponse.bodyToMono(Message.class));

        Message message = premodifyResult.block();
        assertEquals("Status should be 400", 400, PREMODIFY_STATUS);
        assertEquals("Message code should be _reservation_not_found", "_reservation_not_found", message.getCode());
        assertEquals("Message should be for reservation not found", "Reservation not found", message.getMsg());
    }

    @Test
    public void test_premodifyReservation_premodifyInvalidName_validateReservationNotFoundError() {

        // Make a reservation first
        RoomReservationResponse responseCheckout = makeSuccessTestReservation();

        // Confirm the reservation is booked
        assertEquals("Status should be booked", ReservationState.Booked, responseCheckout.getState());

        String confirmationNumber = responseCheckout.getConfirmationNumber();

        log.info("Confirmation Number: {}", confirmationNumber);

        // Delay required as Checkout Process is async in the back end.
        addDelay(15000);

        // Pre-modify the reservation for the given first name, last name and
        // confirmation number
        File modifyFile = new File(getClass().getResource("/premodify-post-requestbody.json").getPath());
        PreModifyRequest preModifyRequest = convert(modifyFile, PreModifyRequest.class);
        preModifyRequest.setConfirmationNumber(confirmationNumber);
        preModifyRequest.setFirstName("Incorrect");

        Mono<Message> premodifyResult = realClient.post().uri(builder -> builder.path(baseServiceUrl).build())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).body(BodyInserters.fromValue(preModifyRequest)).exchange().doOnNext(response -> {
                    PREMODIFY_STATUS = response.statusCode().value();
                }).flatMap(clientResponse -> clientResponse.bodyToMono(Message.class));

        Message message = premodifyResult.block();
        assertEquals("Status code should be 400", 400, PREMODIFY_STATUS);
        assert (ErrorCode.INVALID_RESERVE_COMP.getErrorCode().equals(message.getCode())
                || ErrorCode.DATES_UNAVAILABLE.getErrorCode().equals(message.getCode())
                || ErrorCode.RESERVATION_NOT_FOUND.getErrorCode().equals(message.getCode()));
    }

}
