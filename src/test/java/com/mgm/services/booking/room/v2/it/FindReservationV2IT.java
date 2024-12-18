package com.mgm.services.booking.room.v2.it;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.ErrorResponse;
import com.mgm.services.booking.room.exception.ErrorTypes;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.ReservationAssociateRequest;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.GetRoomReservationResponse;
import com.mgm.services.booking.room.model.response.UpdateProfileInfoResponse;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Log4j2
public class FindReservationV2IT extends BaseRoomBookingV2IntegrationTest {
    
    private String confNumber;

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(TestConstant.CONFIRMATION_NUMBER, "A1B2C3");
        return new ApiDetails(ApiDetails.Method.GET, "/v2/reservation", queryParams, null);
    }

    @Test
    public void findReservation_WithValidParameters_validateSuccessResponse() {
        CreateRoomReservationResponse createReservationResponse = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));

        GetRoomReservationResponse findReservationResponse = realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParam(TestConstant.CONFIRMATION_NUMBER,
                                createReservationResponse.getRoomReservation().getConfirmationNumber())
                        .build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(GetRoomReservationResponse.class)).block();
        assertEquals("Room Reservation confirmation number should be same.",
                createReservationResponse.getRoomReservation().getConfirmationNumber(),
                findReservationResponse.getRoomReservation().getConfirmationNumber());
        assertThat("State should be cancelled or booked", findReservationResponse.getRoomReservation().getState(),
                anyOf(is(ReservationState.Cancelled), is(ReservationState.Booked)));
        assertTrue("Booking source should be ICE", StringUtils
                .equals(createReservationResponse.getRoomReservation().getBookingSource(), TestConstant.ICE));
        assertTrue("Booking channel should be ICE", StringUtils
                .equals(createReservationResponse.getRoomReservation().getBookingChannel(), TestConstant.ICE));

    }

    //@Test
    public void findReservation_WithInvalidConfirmationNumber_validateReservationNotFoundError() {

        Mono<ErrorResponse> result = realClient.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParam(TestConstant.CONFIRMATION_NUMBER, "123ABC").build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class));

        ErrorResponse response = result.block();
        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.RESERVATION_NOT_FOUND,
                ErrorTypes.FUNCTIONAL_ERROR);
        // Get the reservation id for the given confirmation number
        assertEquals("Message should be reservation not found", expectedErrorResponse.getError().getCode(),
                response.getError().getCode());

    }
    
    @Test
    public void findReservation_WithEmptyConfirmationNumber_validate400Error() {

        Mono<ErrorResponse> result = realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class));

        ErrorResponse response = result.block();
        // Get the reservation id for the given confirmation number
        assertEquals("632-1-143", response.getError().getCode());
        assertEquals("No confirmation number",
                response.getError().getMessage());

    }

    @Test
    public void findReservation_WithLimitedScopeAndIncorrectFirstAndLastNames_validateReservationNotFoundError() {
        CreateRoomReservationResponse createReservationResponse = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(TestConstant.CONFIRMATION_NUMBER, createReservationResponse.getRoomReservation().getConfirmationNumber());
        queryParams.add(TestConstant.FIRST_NAME, "someFName21");
        queryParams.add(TestConstant.LAST_NAME, "someLName12");

        log.info("Reservation confirmationNumber:: {}, firstName:: {} and lastName:: {}",
                createReservationResponse.getRoomReservation().getConfirmationNumber(),
                createReservationResponse.getRoomReservation().getProfile().getFirstName(),
                createReservationResponse.getRoomReservation().getProfile().getLastName());

        log.info("Requested confirmationNumber:: {}, firstName:: {} and lastName:: {}",
                createReservationResponse.getRoomReservation().getConfirmationNumber(),
                queryParams.getFirst(TestConstant.FIRST_NAME), queryParams.getFirst(TestConstant.LAST_NAME));

        Mono<ErrorResponse> result = realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.MGM_RESORTS, TestConstant.WEB, TestConstant.DUMMY_TRANSACTION_ID);
                           // RBSTokenScopes.GET_RESERVATION.getValue());
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class));

        ErrorResponse response = result.block();
        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.RESERVATION_NOT_FOUND,
                ErrorTypes.FUNCTIONAL_ERROR);
        // Get the reservation id for the given confirmation number
        assertEquals("Message should be reservation not found", expectedErrorResponse.getError().getCode(),
                response.getError().getCode());

    }

    @Test
    public void findReservation_WithElevatedAccessAndWithoutFirstAndLastNames_validateSuccessResponse() {
        CreateRoomReservationResponse createReservationResponse = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(TestConstant.CONFIRMATION_NUMBER, createReservationResponse.getRoomReservation().getConfirmationNumber());

        log.info("Reservation confirmationNumber:: {}, firstName:: {} and lastName:: {}",
                createReservationResponse.getRoomReservation().getConfirmationNumber(),
                createReservationResponse.getRoomReservation().getProfile().getFirstName(),
                createReservationResponse.getRoomReservation().getProfile().getLastName());

        log.info("Requested confirmationNumber:: {}, firstName:: {} and lastName:: {}",
                createReservationResponse.getRoomReservation().getConfirmationNumber(),
                queryParams.getFirst(TestConstant.FIRST_NAME), queryParams.getFirst(TestConstant.LAST_NAME));

        GetRoomReservationResponse findReservationResponse = realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParam(TestConstant.CONFIRMATION_NUMBER,
                                createReservationResponse.getRoomReservation().getConfirmationNumber())
                        .build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(GetRoomReservationResponse.class)).block();
        assertEquals("Room Reservation confirmation number should be same.",
                createReservationResponse.getRoomReservation().getConfirmationNumber(),
                findReservationResponse.getRoomReservation().getConfirmationNumber());
        assertThat("State should be cancelled or booked", findReservationResponse.getRoomReservation().getState(),
                anyOf(is(ReservationState.Cancelled), is(ReservationState.Booked)));

    }
    
    private String createTransientReservation() {

        if (StringUtils.isEmpty(confNumber)) {
            CreateRoomReservationResponse createReservationResponse = makeReservationV2AndValidate(
                    createRequestBasic("/createroomreservationrequest-basic-transient.json"));

            this.confNumber = createReservationResponse.getRoomReservation().getConfirmationNumber();
        }

        return confNumber;

    }

    @Test
    public void findReservation_withMlifeNumInGuestToken_validateSuccessResponse() {
        
        String confNumber = createTransientReservation();
        File file = new File(getClass().getResource("/associationRequest.json").getPath());
        ReservationAssociateRequest associationRequest = convert(file, ReservationAssociateRequest.class);
        associationRequest.setConfirmationNumber(confNumber);

        realClient.put().uri(builder -> builder.path(V2_ASSOCIATE_RESERVATION_API).build())
                .body(BodyInserters.fromValue(associationRequest))
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getNonPerpetualEmailId(),
                        defaultTestData.getNonPerpetualEmailPass()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, V2_ASSOCIATE_RESERVATION_API, error));
                }).doOnSuccess(response -> validateSuccessResponse(response, V2_ASSOCIATE_RESERVATION_API))
                .flatMap(clientResponse -> clientResponse.bodyToMono(UpdateProfileInfoResponse.class)).block();

        GetRoomReservationResponse findReservationResponse = realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParam(TestConstant.CONFIRMATION_NUMBER,
                                confNumber)
                        .build())
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getNonPerpetualEmailId(),
                        defaultTestData.getNonPerpetualEmailPass()))//, RBSTokenScopes.GET_RESERVATION.getValue()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(GetRoomReservationResponse.class)).block();
        assertNotNull(TestConstant.EMPTY_RESPONSE, findReservationResponse.getRoomReservation());
    }
    
    @Test
    public void findReservation_withMgmIdInGuestToken_validateSuccessResponse() {
        
        String confNumber = defaultTestData.getResvWithMgmId();

        GetRoomReservationResponse findReservationResponse = realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParam(TestConstant.CONFIRMATION_NUMBER,
                                confNumber)
                        .build())
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getPerpetualEmailId(),
                        defaultTestData.getPerpetualEmailPass()))//, RBSTokenScopes.GET_RESERVATION.getValue()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(GetRoomReservationResponse.class)).block();
        assertNotNull(TestConstant.EMPTY_RESPONSE, findReservationResponse.getRoomReservation());
    }
    
    @Test
    public void findReservation_withMlifeNumInGuestTokenNotMatchingReservation_validateSuccessResponse() {

        String confNumber = createTransientReservation();

        Mono<ErrorResponse> result = realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParam(TestConstant.CONFIRMATION_NUMBER, confNumber).build())
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getPerpetualEmailId(),
                        defaultTestData.getPerpetualEmailPass()))//, RBSTokenScopes.GET_RESERVATION.getValue()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class));
        
        
        ErrorResponse response = result.block();
        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.RESERVATION_NOT_FOUND,
                ErrorTypes.FUNCTIONAL_ERROR);
        // Get the reservation id for the given confirmation number
        assertEquals("Message should be reservation not found", expectedErrorResponse.getError().getCode(),
                response.getError().getCode());
    }
    
    @Test
    public void findReservation_withSecondaryOperaConfNumber_expectSecondaryReservation () {
        
        String confNumber = defaultTestData.getShareWithReservation().getSecondaryOperaConfNumber();
        
        GetRoomReservationResponse findReservationResponse = realClient.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParam(TestConstant.CONFIRMATION_NUMBER,
                                confNumber)
                        .build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(GetRoomReservationResponse.class)).block();
        assertEquals("Room Reservation opera confirmation number should be same.",
                confNumber,
                findReservationResponse.getRoomReservation().getOperaConfirmationNumber());
        assertEquals("Room Reservation primary opera confirmation number should be same.",
                defaultTestData.getShareWithReservation().getPrimaryOperaConfNumber(),
                findReservationResponse.getRoomReservation().getPrimarySharerConfirmationNumber());
    }
}
