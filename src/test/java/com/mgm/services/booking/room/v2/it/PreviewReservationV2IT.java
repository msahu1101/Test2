package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.TripDetailsRequest;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.ModifyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.common.exception.ErrorCode;

public class PreviewReservationV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.PUT, "/v2/reservation/preview", null, new PreModifyV2Request());
    }

    @Test
    public void preModifyReservation_NonLoggedInUserNonLoyaltyReservation_TripDates_validateSuccess() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));
        PreModifyV2Request previewRequest = preparePreModifyV2Request(createRoomReservationResponse.getRoomReservation());
        previewRequest.setFirstName(createRoomReservationResponse.getRoomReservation().getProfile().getFirstName());
        previewRequest.setLastName(createRoomReservationResponse.getRoomReservation().getProfile().getLastName());
        ModifyRoomReservationResponse previewReservationResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(previewRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        assertEquals("Room Reservation confirmation number should be same in preview.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                previewReservationResponse.getRoomReservation().getConfirmationNumber());
        assertTrue("Special request should not be there as expected",
                previewReservationResponse.getRoomReservation().getSpecialRequests().size() == 0);
        assertTrue("Booking source should be ICE", StringUtils
                .equals(previewReservationResponse.getRoomReservation().getBookingSource(), TestConstant.ICE));
        assertTrue("Booking channel should be ICE", StringUtils
                .equals(previewReservationResponse.getRoomReservation().getBookingChannel(), TestConstant.ICE));
    }

    /**
     * This is to test the preview reservation flow. In PreModifyV2Request
     * tripDetails will not be sent and roomRequests will be sent as as empty array.
     * 
     * Expecting:
     * 1. No change in checkIn and checkOut dates
     * 2. specialRequets should be removed.
     */
    @Test
    public void preModifyReservation_NonLoggedInUserNonLoyaltyReservation_withOutTripDetails_removeSpecialRequest_validateSuccess() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-transient-specialrequest.json"));

        PreModifyV2Request previewRequest = preparePreModifyV2Request(createRoomReservationResponse.getRoomReservation());
        previewRequest.setFirstName(createRoomReservationResponse.getRoomReservation().getProfile().getFirstName());
        previewRequest.setLastName(createRoomReservationResponse.getRoomReservation().getProfile().getLastName());
        previewRequest.setRoomRequests(new ArrayList<>());
        previewRequest.setTripDetails(null);

        ModifyRoomReservationResponse previewReservationResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(previewRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        assertEquals("Room Reservation confirmation number should be same in preview.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                previewReservationResponse.getRoomReservation().getConfirmationNumber());
        assertTrue("Special requests should be removed",
                previewReservationResponse.getRoomReservation().getSpecialRequests().isEmpty());
        assertEquals("Should not be any change in CheckIn date",
                createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckInDate(),
                previewReservationResponse.getRoomReservation().getTripDetails().getCheckInDate());
        assertEquals("Should not be any change in CheckOut date",
                createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckOutDate(),
                previewReservationResponse.getRoomReservation().getTripDetails().getCheckOutDate());
    }

    @Test
    public void preModifyReservation_LoggedInUserLoyaltyReservation_TripDateAndSpecialRequest_validateSuccess() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-specialrequest.json"));
        PreModifyV2Request previewRequest = preparePreModifyV2Request(createRoomReservationResponse.getRoomReservation());
        previewRequest.setRoomRequests(new ArrayList<>());
        previewRequest.getRoomRequests()
                .addAll(createRoomReservationResponse.getRoomReservation().getSpecialRequests());
        ModifyRoomReservationResponse previewReservationResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(previewRequest))
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, "testres1@mailinator.com", "Mlife1234"))
                        //RBSTokenScopes. UPDATE_RESERVATION.getValue()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        assertEquals("Room Reservation confirmation number should be same in preview.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                previewReservationResponse.getRoomReservation().getConfirmationNumber());
        assertEquals("Special request should be there as expected",
                createRoomReservationResponse.getRoomReservation().getSpecialRequests(),
                previewReservationResponse.getRoomReservation().getSpecialRequests());
    }

    @Test
    public void preModifyReservation_NonLoggedInUserNonLoyaltyReservationWithNamesMismatch_TripDates_validateErrorResponse() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));
        PreModifyV2Request previewRequest = preparePreModifyV2Request(createRoomReservationResponse.getRoomReservation());
        previewRequest.setFirstName(createRoomReservationResponse.getRoomReservation().getProfile().getFirstName());
        // Concatenating firstName and lastName to create a random string, to make the test fail
        previewRequest.setLastName(createRoomReservationResponse.getRoomReservation().getProfile().getFirstName()
                + createRoomReservationResponse.getRoomReservation().getProfile().getLastName());
        ErrorResponse result = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(previewRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class)).block();

        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.RESERVATION_NOT_FOUND,
                ErrorTypes.FUNCTIONAL_ERROR);

        assertEquals("Error code should be 632-2-140", expectedErrorResponse.getError().getCode(),
                result.getError().getCode());
    }

    @Test
    public void preModifyReservation_LoggedInUserLoyaltyReservationMlifeMismatch_TripDateAndSpecialRequest_validateErrorResponse() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-specialrequest.json"));
        PreModifyV2Request previewRequest = preparePreModifyV2Request(createRoomReservationResponse.getRoomReservation());
        previewRequest.setRoomRequests(new ArrayList<>());
        previewRequest.getRoomRequests()
                .addAll(createRoomReservationResponse.getRoomReservation().getSpecialRequests());
        ErrorResponse result = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(previewRequest)).headers(headers -> {
                    addAllHeadersWithGuestToken(headers, TestConstant.ICE, TestConstant.ICE,
                            TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getNonPerpetualEmailId(),
                            defaultTestData.getNonPerpetualEmailPass());
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException(String.format(TestConstant.ERROR_MESSAGE,
                            getApiDetails().getBaseServiceUrl(), error.getMessage()), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class)).block();

        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.RESERVATION_NOT_MODIFIABLE,
                ErrorTypes.FUNCTIONAL_ERROR);

        assertEquals("Error code should be 632-2-246", expectedErrorResponse.getError().getCode(),
                result.getError().getCode());
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