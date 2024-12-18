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
import com.mgm.services.booking.room.exception.ErrorResponse;
import com.mgm.services.booking.room.exception.ErrorTypes;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.common.exception.ErrorCode;

import reactor.core.publisher.Mono;

public class CancelReservationV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.POST, "/v2/reservation/cancel", null, new CancelV2Request());
    }

    @Test
    public void cancelReservation_cancellationSuccess_validate() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));
        CancelV2Request cancelRequest = new CancelV2Request();
        cancelRequest.setItineraryId(createRoomReservationResponse.getRoomReservation().getItineraryId());
        cancelRequest.setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        cancelRequest.setCancellationReason(TestConstant.TEST_CANCEL_RESERVATION);
        cancelRequest.setCustomerId(createRoomReservationResponse.getRoomReservation().getProfile().getId());

        CancelRoomReservationV2Response cancelReservationResponse = realClient.post()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(cancelRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
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
    public void cancelReservation_alreadyCancelledStatus_validateErrorMessage() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));
        CancelV2Request cancelRequest = new CancelV2Request();
        cancelRequest.setItineraryId(createRoomReservationResponse.getRoomReservation().getItineraryId());
        cancelRequest.setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        cancelRequest.setCancellationReason(TestConstant.TEST_CANCEL_RESERVATION);
        cancelRequest.setCustomerId(createRoomReservationResponse.getRoomReservation().getProfile().getId());

        CancelRoomReservationV2Response cancelReservationResponse = realClient.post()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(cancelRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(CancelRoomReservationV2Response.class)).block();

        assertEquals("Room Reservation confirmation number should be same.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                cancelReservationResponse.getRoomReservation().getConfirmationNumber());
        assertThat("State should be cancelled", cancelReservationResponse.getRoomReservation().getState(),
                is(ReservationState.Cancelled));

        Mono<ErrorResponse> result = realClient.post()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(cancelRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class));

        ErrorResponse response = result.block();
        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.RESERVATION_ALREADY_CANCELLED,
                ErrorTypes.FUNCTIONAL_ERROR);
        assertEquals("Message should be reservation already cancelled", expectedErrorResponse.getError().getCode(),
                response.getError().getCode());

    }

    @Test
    public void cancelReservation_withInvalidCombination_validateErrorMessage() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));
        CancelV2Request cancelRequest = new CancelV2Request();
        //setting invalid itinerary-id
        cancelRequest.setItineraryId("11111111111");
        cancelRequest.setReservationId(createRoomReservationResponse.getRoomReservation().getId());
        cancelRequest.setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        cancelRequest.setCustomerId(createRoomReservationResponse.getRoomReservation().getProfile().getId());
        cancelRequest.setCancellationReason(TestConstant.TEST_CANCEL_RESERVATION);

        Mono<ErrorResponse> result = realClient.post()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(cancelRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class));

        ErrorResponse response = result.block();
        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.INVALID_COMBINATION_RESERVATION_INPUT_PARAMS,
                ErrorTypes.FUNCTIONAL_ERROR);
        assertEquals("Message should be as invalid combination of input params",
                expectedErrorResponse.getError().getCode(), response.getError().getCode());

    }
}
