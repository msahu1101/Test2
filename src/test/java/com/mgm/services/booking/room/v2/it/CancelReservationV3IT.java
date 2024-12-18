package com.mgm.services.booking.room.v2.it;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.CancelV3Request;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;

public class CancelReservationV3IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.POST, "/v3/reservation/cancel", null, new CancelV3Request());
    }

    @Test
    public void cancel_transientResvServiceToken_validateSuccessResponse() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));
        CancelV3Request cancelV3Request = new CancelV3Request();
        cancelV3Request.setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        cancelV3Request.setCancellationReason(TestConstant.TEST_CANCEL_RESERVATION);

        CancelRoomReservationV2Response cancelReservationResponse = realClient.post()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(cancelV3Request)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(CancelRoomReservationV2Response.class)).block();

        assertEquals("Room Reservation confirmation number should be same.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                cancelReservationResponse.getRoomReservation().getConfirmationNumber());
        assertThat("State should be cancelled", cancelReservationResponse.getRoomReservation().getState(),
                is(ReservationState.Cancelled));
        assertTrue("Booking source should be ICE", StringUtils
                .equals(cancelReservationResponse.getRoomReservation().getBookingSource(), TestConstant.ICE));
        assertTrue("Booking channel should be ICE", StringUtils
                .equals(cancelReservationResponse.getRoomReservation().getBookingChannel(), TestConstant.ICE));
    }

    @Test
    public void cancel_memberResvGuestToken_validateSuccessResponse() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic.json"));
        CancelV3Request cancelV3Request = new CancelV3Request();
        cancelV3Request.setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        cancelV3Request.setCancellationReason(TestConstant.TEST_CANCEL_RESERVATION);

        CancelRoomReservationV2Response cancelReservationResponse = realClient.post()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(cancelV3Request)).headers(headers -> {
                    addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.CHANNEL_WEB,
                            TestConstant.DUMMY_TRANSACTION_ID, "tlntest3@yopmail.com", "Mlife1234");
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(CancelRoomReservationV2Response.class)).block();

        assertEquals("Room Reservation confirmation number should be same.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                cancelReservationResponse.getRoomReservation().getConfirmationNumber());
        assertThat("State should be cancelled", cancelReservationResponse.getRoomReservation().getState(),
                is(ReservationState.Cancelled));
    }
}
