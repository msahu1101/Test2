/**
 * Class to keep integration tests related to associateReservation in ModifyV2Controller.
 */
package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.ErrorResponse;
import com.mgm.services.booking.room.exception.ErrorTypes;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.ReservationAssociateRequest;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.UpdateProfileInfoResponse;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Class to keep integration tests related to associateReservation in
 * ModifyV2Controller.
 * 
 * @author laknaray
 *
 */
public class ReservationAssociationIT extends BaseRoomBookingV2IntegrationTest {

    private String confNumber;

    @Override
    public ApiDetails getApiDetails() {
        File file = new File(getClass().getResource("/associationRequest.json").getPath());
        ReservationAssociateRequest associationRequest = convert(file, ReservationAssociateRequest.class);
        return new ApiDetails(ApiDetails.Method.PUT, "/v2/reservation/associate", null, associationRequest);
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
    public void associateReservation_withEmptyConfirmationNumber_validate400Error() {
        String confNumber = StringUtils.EMPTY;

        File file = new File(getClass().getResource("/associationRequest.json").getPath());
        ReservationAssociateRequest associationRequest = convert(file, ReservationAssociateRequest.class);
        associationRequest.setConfirmationNumber(confNumber);

        ErrorResponse errorResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(associationRequest))
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getPerpetualEmailId(),
                        defaultTestData.getPerpetualEmailPass()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, getApiDetails().getBaseServiceUrl(), error));
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class)).block();

        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.ASSOCIATION_VIOLATION_NO_CONFIRMATION_NUMBER,
                ErrorTypes.VALIDATION_ERROR);
        assertEquals(expectedErrorResponse.getError().getCode(), errorResponse.getError().getCode());
    }
    
    //@Test
    public void associateReservation_withInvalidConfirmationNumber_validateReservationNotFoundError() {
        String confNumber = "TEST1234";

        File file = new File(getClass().getResource("/associationRequest.json").getPath());
        ReservationAssociateRequest associationRequest = convert(file, ReservationAssociateRequest.class);
        associationRequest.setConfirmationNumber(confNumber);

        ErrorResponse errorResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(associationRequest))
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGMRI, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getPerpetualEmailId(),
                        defaultTestData.getPerpetualEmailPass()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, getApiDetails().getBaseServiceUrl(), error));
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class)).block();

        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.RESERVATION_NOT_FOUND,
                ErrorTypes.FUNCTIONAL_ERROR);
        assertEquals(expectedErrorResponse.getError().getCode(), errorResponse.getError().getCode());
    }
    
    @Test
    public void associateReservation_withTokenWithInvalidName_validateNameMatchError() {
        String confNumber = createTransientReservation();

        File file = new File(getClass().getResource("/associationRequest.json").getPath());
        ReservationAssociateRequest associationRequest = convert(file, ReservationAssociateRequest.class);
        associationRequest.setConfirmationNumber(confNumber);

        ErrorResponse errorResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(associationRequest))
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getPerpetualEmailId(),
                        defaultTestData.getPerpetualEmailPass()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, getApiDetails().getBaseServiceUrl(), error));
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class)).block();

        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.ASSOCIATION_VIOLATION_NAME_MISMATCH,
                ErrorTypes.FUNCTIONAL_ERROR);
        assertEquals(expectedErrorResponse.getError().getCode(), errorResponse.getError().getCode());
    }

    @Test
    public void associateReservation_withValidInputs_validateSuccessResponse() {
        String confNumber = createTransientReservation();

        File file = new File(getClass().getResource("/associationRequest.json").getPath());
        ReservationAssociateRequest associationRequest = convert(file, ReservationAssociateRequest.class);
        associationRequest.setConfirmationNumber(confNumber);

        UpdateProfileInfoResponse updateProfileResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(associationRequest))
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getNonPerpetualEmailId(),
                        defaultTestData.getNonPerpetualEmailPass()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, getApiDetails().getBaseServiceUrl(), error));
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(UpdateProfileInfoResponse.class)).block();

        assertNotNull(TestConstant.EMPTY_RESPONSE, updateProfileResponse);
        assertTrue(updateProfileResponse.getRoomReservation().getProfile().getMlifeNo() > 0);
    }
    
    @Test
    public void associateReservation_withReservationAlreadyAssociatedToDifferentMlife_validateAlreadyAssociatedError() {
        CreateRoomReservationResponse createReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));
        String confNumber = createReservationResponse.getRoomReservation().getConfirmationNumber();

        File file = new File(getClass().getResource("/associationRequest.json").getPath());
        ReservationAssociateRequest associationRequest = convert(file, ReservationAssociateRequest.class);
        associationRequest.setConfirmationNumber(confNumber);
        
        // Associating to a valid user
        realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(associationRequest))
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getNonPerpetualEmailId(),
                        defaultTestData.getNonPerpetualEmailPass()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, getApiDetails().getBaseServiceUrl(), error));
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(UpdateProfileInfoResponse.class)).block();

        // Trying to associate it to different user, should return error
        ErrorResponse errorResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(associationRequest))
                .headers(headers -> addAllHeadersWithGuestToken(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, defaultTestData.getPerpetualEmailId(),
                        defaultTestData.getPerpetualEmailPass()))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException(
                            String.format(TestConstant.ERROR_MESSAGE, getApiDetails().getBaseServiceUrl(), error));
                }).doOnSuccess(response -> validate4XXFailureResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class)).block();

        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.ASSOCIATION_VIOLATION_MLIFE_MISMATCH,
                ErrorTypes.FUNCTIONAL_ERROR);
        assertEquals(expectedErrorResponse.getError().getCode(), errorResponse.getError().getCode());
    }

}
