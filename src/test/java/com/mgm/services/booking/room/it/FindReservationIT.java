package com.mgm.services.booking.room.it;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.Message;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class FindReservationIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/reserve/room";

    @Test
    public void test_findReservation_findReservationWithNoHeaders_validateHeaderMissingError() {
        validateGetRequestNoHeaderTest(baseServiceUrl + "?firstName=" + defaultTestData.getFirstName() + "&lastName="
                + defaultTestData.getLastName() + "&confirmationNumber=111");
    }

    @Test
    public void test_findReservation_findReservationWithValidParameters_validateSuccessResponse() {
        processFindReservation();
    }

    @Test
    public void test_findReservation_findReservationWithInvalidConfirmation_validateReservationNotFoundError() {

        Mono<Message> result = realClient.get()
                .uri(builder -> builder.path(baseServiceUrl).queryParam("firstName", defaultTestData.getFirstName())
                        .queryParam("lastName", defaultTestData.getLastName()).queryParam("confirmationNumber", "1234")
                        .build())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : " + baseServiceUrl
                            + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, baseServiceUrl))
                .flatMap(clientResponse -> clientResponse.bodyToMono(Message.class));

        Message response = result.block();

        // Get the reservation id for the given confirmation number
        assertEquals("Message should be reservation not found", ErrorCode.RESERVATION_NOT_FOUND.getDescription(),
                response.getMsg());

    }

    private void processFindReservation() {

        // Make a reservation first
        RoomReservationResponse responseCheckout = makeSuccessTestReservation();

        // Confirm the reservation is booked
        assertEquals("Status should be booked", ReservationState.Booked, responseCheckout.getState());

        String confirmationNumber = responseCheckout.getConfirmationNumber();

        log.info("Confirmation Number: {}", confirmationNumber);

        // Delay required as Checkout Process is async in the backend.
        addDelay(15000);

        // Find the reservation for the given confirmation number, first name
        // and last name
        Mono<RoomReservationResponse> resultPostFind = realClient.get()
                .uri(builder -> builder.path(baseServiceUrl).queryParam("firstName", defaultTestData.getFirstName())
                        .queryParam("lastName", defaultTestData.getLastName())
                        .queryParam("confirmationNumber", confirmationNumber).build())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : " + baseServiceUrl
                            + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, baseServiceUrl))
                .flatMap(clientResponse -> clientResponse.bodyToMono(RoomReservationResponse.class));

        RoomReservationResponse responseFind = resultPostFind.block();

        // Get the reservation id for the given confirmation number
        assertEquals("Confirmation number should be same", confirmationNumber, responseFind.getConfirmationNumber());
        assertThat("State should be cancelled or booked", responseFind.getState(),
                anyOf(is(ReservationState.Cancelled), is(ReservationState.Booked)));
    }

}
