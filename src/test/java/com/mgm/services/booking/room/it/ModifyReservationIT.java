package com.mgm.services.booking.room.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Calendar;

import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.Message;
import com.mgm.services.booking.room.model.request.ModifyRequest;
import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.TripDetail;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class ModifyReservationIT extends BaseRoomBookingIntegrationTest {

    @Test
    public void test_modifyReservation_modifyRoomReservation_validateResponse() {

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

        // prepone the trip by 1 day
        TripDetail tripDetails = preModifyRequest.getTripDetails();
        Calendar checkInDate = Calendar.getInstance();
        checkInDate.setTime(preModifyRequest.getTripDetails().getCheckInDate());
        checkInDate.add(Calendar.DATE, -1);
        tripDetails.setCheckInDate(checkInDate.getTime());

        // Make the in-flight modification
        ClientResponse clientResponse = realClient.post()
                .uri(builder -> builder.path("/v1/reserve/room/pre-modify").build()).headers(headers -> {
                    addAllHeaders(headers);
                }).body(BodyInserters.fromValue(preModifyRequest)).exchange().block();

        int status = clientResponse.statusCode().value();

        log.info("Response code for pre-modify: {}, {}", STATE_TOKEN, status);

        // Assert for successful pre-modify or dates not available message
        if (status == 200) {
            RoomReservationResponse preModifyResponse = clientResponse.bodyToMono(RoomReservationResponse.class)
                    .block();
            assertEquals("Status should be booked", ReservationState.Booked, preModifyResponse.getState());
            assertNotNull("CheckInDate should not be null", preModifyResponse.getTripDetails().getCheckInDate());
            assertNotNull("CheckOutDate should not be null", preModifyResponse.getTripDetails().getCheckOutDate());

            // Create modify request with received reservation id
            ModifyRequest modifyRequest = new ModifyRequest();
            modifyRequest.setReservationId(preModifyResponse.getItemId());

            preModifyRequest.setTripDetails(tripDetails);

            RoomReservationResponse modifyResponse = realClient.put()
                    .uri(builder -> builder.path("/v1/reserve/room").build()).headers(headers -> {
                        addAllHeaders(headers);
                    }).body(BodyInserters.fromValue(modifyRequest)).retrieve().bodyToMono(RoomReservationResponse.class)
                    .block();

            log.info("Response for modify reservation: {}, {}", STATE_TOKEN, modifyResponse);

            // Assert to make reservation is still booked
            assertEquals("Status should be booked", ReservationState.Booked, modifyResponse.getState());
            assertEquals("Confirmation number should match", confirmationNumber,
                    modifyResponse.getConfirmationNumber());
            assertNotNull("CheckInDate should not be null", modifyResponse.getTripDetails().getCheckInDate());
            assertNotNull("CheckOutDate should not be null", modifyResponse.getTripDetails().getCheckOutDate());
            assertEquals("CheckInDate should match with format", format.format(tripDetails.getCheckInDate()),
                    format.format(modifyResponse.getTripDetails().getCheckInDate()));

        } else {
            Message msg = clientResponse.bodyToMono(Message.class).block();
            if (null != msg) {
                assertNotNull("Message code should not be empty", msg.getCode());
                assertNotNull("Message shouldn't be empty", msg.getMsg());
            }
        }

    }

    @Test
    public void test_modifyReservation_modifyInvalidRoomReservation_validateReservationNotFoundError() {

        ModifyRequest modifyRequest = new ModifyRequest();
        // incorrect reservation number
        modifyRequest.setReservationId("1234");

        Mono<Message> modifyResponse = realClient.put().uri(builder -> builder.path("/v1/reserve/room").build())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).body(BodyInserters.fromValue(modifyRequest)).exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            "Error occurred on executing url :/v1/reserve/room, Error Message : " + error.getMessage(),
                            error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, "/v1/reserve/room"))
                .flatMap(clientResponse -> clientResponse.bodyToMono(Message.class));

        Message result = modifyResponse.block();

        assertEquals("Message should be for token expired", ErrorCode.ITEM_NOT_FOUND.getDescription(), result.getMsg());
    }

}
