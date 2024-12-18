package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.ErrorResponse;
import com.mgm.services.booking.room.exception.ErrorTypes;
import com.mgm.services.booking.room.exception.ErrorVo;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.PreviewCommitRequest;
import com.mgm.services.booking.room.model.request.TripDetailsRequest;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.ModifyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.common.exception.ErrorCode;

public class CommitReservationV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.PUT, "/v2/reservation/commit", null, preparePreviewCommitRequest());
    }

    @Test
    public void previewCommit_NonLoggedInUserNonLoyaltyReservation_ValidInput_validateSuccess() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));

        PreModifyV2Request previewRequest = preparePreModifyV2Request(createRoomReservationResponse.getRoomReservation());
        previewRequest.setFirstName(createRoomReservationResponse.getRoomReservation().getProfile().getFirstName());
        previewRequest.setLastName(createRoomReservationResponse.getRoomReservation().getProfile().getLastName());
        ModifyRoomReservationResponse previewReservationResponse = realClient.put()
                .uri(builder -> builder.path(V2_PREVIEW_RESERVATION_API).build())
                .body(BodyInserters.fromValue(previewRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, V2_PREVIEW_RESERVATION_API, error.getMessage()),
                            error);
                }).doOnSuccess(response -> validateSuccessResponse(response, V2_PREVIEW_RESERVATION_API))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        PreviewCommitRequest commitRequest = preparePreviewCommitRequest(
                previewReservationResponse.getRoomReservation().getRatesSummary(), previewRequest);
        ModifyRoomReservationResponse commitResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(commitRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        assertEquals("Room Reservation confirmation number should be same in commit.",
                commitRequest.getConfirmationNumber(),
                commitResponse.getRoomReservation().getConfirmationNumber());
        assertTrue("Booking source should be ICE",
                StringUtils.equals(commitResponse.getRoomReservation().getBookingSource(), TestConstant.ICE));
        assertTrue("Booking channel should be ICE",
                StringUtils.equals(commitResponse.getRoomReservation().getBookingChannel(), TestConstant.ICE));

    }

    /**
     * This is to test the commit reservation flow. In PreviewCommitRequest
     * tripDetails will not be sent and roomRequests will be sent as as empty array.
     * 
     * Expecting:
     * 1. No change in checkIn and checkOut dates
     * 2. specialRequets should be removed.
     */
    @Test
    public void previewCommit_NonLoggedInUserNonLoyaltyReservation_withOutTripDetails_removeSpecialRequest_validateSuccess() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-transient-specialrequest.json"));

        PreModifyV2Request previewRequest = preparePreModifyV2Request(createRoomReservationResponse.getRoomReservation());
        previewRequest.setFirstName(createRoomReservationResponse.getRoomReservation().getProfile().getFirstName());
        previewRequest.setLastName(createRoomReservationResponse.getRoomReservation().getProfile().getLastName());
        previewRequest.setRoomRequests(new ArrayList<>());
        previewRequest.setTripDetails(null);
        ModifyRoomReservationResponse previewReservationResponse = realClient.put()
                .uri(builder -> builder.path(V2_PREVIEW_RESERVATION_API).build())
                .body(BodyInserters.fromValue(previewRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, V2_PREVIEW_RESERVATION_API, error.getMessage()),
                            error);
                }).doOnSuccess(response -> validateSuccessResponse(response, V2_PREVIEW_RESERVATION_API))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        PreviewCommitRequest commitRequest = preparePreviewCommitRequest(
                previewReservationResponse.getRoomReservation().getRatesSummary(), previewRequest);
        commitRequest.setRoomRequests(new ArrayList<>());
        commitRequest.setTripDetails(null);
        ModifyRoomReservationResponse commitResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(commitRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        assertEquals("Room Reservation confirmation number should be same in commit.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                commitResponse.getRoomReservation().getConfirmationNumber());
        assertTrue("Special requests should be removed",
                commitResponse.getRoomReservation().getSpecialRequests().isEmpty());
        assertEquals("Should not be any change in CheckIn date",
                createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckInDate(),
                commitResponse.getRoomReservation().getTripDetails().getCheckInDate());
        assertEquals("Should not be any change in CheckOut date",
                createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckOutDate(),
                commitResponse.getRoomReservation().getTripDetails().getCheckOutDate());
    }

    @Test
    public void previewCommit_NonLoggedInUserNonLoyaltyReservation_Price_expectError() {
        
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-transient-specialrequest.json"));
        
        PreModifyV2Request previewRequest = preparePreModifyV2Request(createRoomReservationResponse.getRoomReservation());
        previewRequest.setFirstName(createRoomReservationResponse.getRoomReservation().getProfile().getFirstName());
        previewRequest.setLastName(createRoomReservationResponse.getRoomReservation().getProfile().getLastName());
        
        ModifyRoomReservationResponse previewReservationResponse = realClient.put()
                .uri(builder -> builder.path(V2_PREVIEW_RESERVATION_API).build())
                .body(BodyInserters.fromValue(previewRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, V2_PREVIEW_RESERVATION_API, error.getMessage()),
                            error);
                }).doOnSuccess(response -> validateSuccessResponse(response, V2_PREVIEW_RESERVATION_API))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        PreviewCommitRequest commitRequest = preparePreviewCommitRequest(
                previewReservationResponse.getRoomReservation().getRatesSummary(), previewRequest);
        
        // Setting incorrect value
        commitRequest.setPreviewReservationDeposit(commitRequest.getPreviewReservationDeposit() -1);
        
        ErrorVo errorResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(commitRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block().getError();

        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.MODIFY_VIOLATION_PRICE_CHANGE,
                ErrorTypes.FUNCTIONAL_ERROR);
        // Get the reservation id for the given confirmation number
        assertEquals("Message should be price change", expectedErrorResponse.getError().getCode(),
                errorResponse.getCode());

    }

    private PreviewCommitRequest preparePreviewCommitRequest() {
        return convert(new File(getClass().getResource("/previewCommitRequest-dev-resv.json").getPath()), PreviewCommitRequest.class);
    }

    private PreviewCommitRequest preparePreviewCommitRequest(RatesSummary ratesSummary, PreModifyV2Request previewRequest) {
        PreviewCommitRequest request = preparePreviewCommitRequest();

        request.setConfirmationNumber(previewRequest.getConfirmationNumber());
        request.setFirstName(previewRequest.getFirstName());
        request.setLastName(previewRequest.getLastName());
        if (null != previewRequest.getTripDetails()) {
            request.getTripDetails().setCheckInDate(previewRequest.getTripDetails().getCheckInDate());
            request.getTripDetails().setCheckOutDate(previewRequest.getTripDetails().getCheckOutDate());
        }
        request.setPreviewReservationDeposit(ratesSummary.getDepositDue());
        request.setPreviewReservationTotal(ratesSummary.getReservationTotal());

        return request;
    }

    private PreModifyV2Request preparePreModifyV2Request(RoomReservationV2Response roomReservationV2Response) {
        PreModifyV2Request previewRequest = new PreModifyV2Request();
        previewRequest
                .setConfirmationNumber(roomReservationV2Response.getConfirmationNumber());
        previewRequest.setTripDetails(new TripDetailsRequest());
        previewRequest.getTripDetails().setCheckInDate(
                addDays(roomReservationV2Response.getTripDetails().getCheckInDate(), 1));
        previewRequest.getTripDetails().setCheckOutDate(
                addDays(roomReservationV2Response.getTripDetails().getCheckOutDate(), 1));
        return previewRequest;
    }

    private Date addDays(Date input, int numberOfDays) {
        Calendar c = Calendar.getInstance();
        c.setTime(input);
        c.add(Calendar.DATE, numberOfDays);
        return c.getTime();
    }

}
