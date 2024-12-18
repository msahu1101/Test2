package com.mgm.services.booking.room.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.Message;
import com.mgm.services.booking.room.model.request.CancelRequest;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class CancelReservationIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/reserve/room/cancel";

    @Test
    public void test_cancelReservation_NoHeaders_validateHeaderMissingError() {
        CancelRequest cancelRequest = CancelRequestBuilder("test123");
        validatePostRequestNoHeaderTest(baseServiceUrl, cancelRequest);
    }

    @Test
    public void test_cancelReservation_cancelReservationWithValidParameters_validateCancelReservationResponse() {

        // Make a reservation first
        RoomReservationResponse responseCheckout = makeSuccessTestReservation();

        // Confirm the reservation is booked
        assertEquals("Status should be booked", ReservationState.Booked, responseCheckout.getState());

        String confirmationNumber = responseCheckout.getConfirmationNumber();

        log.info("Confirmation Number: {}", confirmationNumber);

        // Delay required as Checkout Process is async in the back end.
        addDelay(15000);

        // Cancel the room for the given first name, last name and confirmation
        // number

        CancelRequest cancelRequest = CancelRequestBuilder(confirmationNumber);

        ClientResponse clientResponse = realClient.post().uri(builder -> builder.path(baseServiceUrl).build())
                .body(BodyInserters.fromValue(cancelRequest)).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error on cancelling the booking (" + baseServiceUrl
                            + "), Error message : " + error.getMessage(), error);
                }).block();

        int status = clientResponse.statusCode().value();

        // Assert for successful cancellation or invalid failure
        // In lower environment, most times reservation doesn't get posted to
        // opera immediately, so
        // immediate cancel calls always returns 400 bad request
        if (status == 200) {
            RoomReservationResponse cancelResponse = clientResponse.bodyToMono(RoomReservationResponse.class).block();
            assertEquals("Status should be Cancelled", ReservationState.Cancelled, cancelResponse.getState());

        } else {
            Message msg = clientResponse.bodyToMono(Message.class).block();
            if (null != msg) {
                assertNotNull("Message code should not be empty", msg.getCode());
                assertNotNull("Message shouldn't be empty", msg.getMsg());
            }
        }

    }

    private CancelRequest CancelRequestBuilder(String confirmationNumber) {
        // Checkout the room for the given reservation-id and X_AUTH_TOKEN
        File requestFile = new File(getClass().getResource("/cancel-request-post.json").getPath());

        CancelRequest cancelRequest = convert(requestFile, CancelRequest.class);

        cancelRequest.setConfirmationNumber(confirmationNumber);
        cancelRequest.setLastName(defaultTestData.getLastName());
        cancelRequest.setFirstName(defaultTestData.getLastName());
        return cancelRequest;
    }

    @Test
    public void test_cancelReservation_cancelReservationWithWithInvalidConfirmation_validateReservationNotFoundError() {

        CancelRequest cancelRequest = CancelRequestBuilder("1234");

        // send wrong confirmation number
        Mono<Message> resultPostCancel = realClient.post().uri(builder -> builder.path(baseServiceUrl).build())
                .body(BodyInserters.fromValue(cancelRequest)).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error on cancelling the booking (" + baseServiceUrl
                            + "), Error message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, baseServiceUrl))
                .flatMap(clientResponse -> clientResponse.bodyToMono(Message.class));

        Message responseCancel = resultPostCancel.block();

        assertEquals("Message should be reservation not found", ErrorCode.RESERVATION_NOT_FOUND.getDescription(),
                responseCancel.getMsg());

    }

}
