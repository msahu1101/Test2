/**
 * Class to keep integration tests related to reservation under ReservationV2Controller.
 */
package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.ErrorResponse;
import com.mgm.services.booking.room.exception.ErrorTypes;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.CreateRoomReservationRequest;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.common.exception.ErrorCode;

import reactor.core.publisher.Mono;

/**
 * Class to keep integration tests related to reservation under
 * ReservationV2Controller.
 * 
 * @author laknaray
 *
 */
public class ReservationV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.POST, "/v2/reservation", null,
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));
    }

    /**
     * makeRoomReservation with the basic props for transient user.
     * 
     */
    @Test
    public void reservationV2_withTransientUser_returnsRoomReservationWithProfileAddress() {
        // Creating a reservation
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
        assertNotNull(TestConstant.EMPTY_PROFILE_ADDRESS, bookedReservation.getProfile().getAddresses());
        assertTrue("Booking source should be ICE",
                StringUtils.equals(bookedReservation.getBookingSource(), TestConstant.ICE));
        assertTrue("Booking channel should be ICE",
                StringUtils.equals(bookedReservation.getBookingChannel(), TestConstant.ICE));
    }

    /**
     * makeRoomReservation with the basic props for transient user and perform AFS check.
     * 
     */
    @Test
    public void reservationV2_performAFSCheck_withTransientUser_returnsRoomReservation() {
        // Creating a reservation
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-basic-transient.json"), TestConstant.ICE,
                TestConstant.WEB);

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
    }

    /**
     * makeRoomReservation with the basic props.
     * 
     */
    @Test
    public void reservationV2_withBasicDetails_returnsRoomReservation() {
        // Creating a reservation
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-basic.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
    }

    /**
     * makeRoomReservation with the basic props along with the below
     * 
     * <li>with special requests</li>
     */
    @Test
    public void reservationV2_withSpecialRequests_returnsRoomReservationWithSpecialRequests() {
        // Creating a reservation with special requests
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-specialrequest.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
        assertArrayEquals(TestConstant.INCORRECT_SPECIAL_REQUESTS, bookedReservation.getSpecialRequests().toArray(),
                defaultTestData.getSpecialRequests().split(","));
    }

    /**
     * makeRoomReservation with the basic props along with the below
     * 
     * <li>with multiple phone numbers</li>
     * <li>with both email address</li>
     * <li>with multiple addresses</li>
     * <li>with customerRank</li>
     * <li>with customerSegment</li>
     * <li>with customerDominantPlay</li>
     */
    @Test
    public void reservationV2_withUpdateProfile_returnsReservationWithUpdatedProfile() {
        // Creating a reservation with updated profile
        CreateRoomReservationRequest request = createRequestBasic("/createroomreservationrequest-updatedprofile.json");
        CreateRoomReservationResponse response = makeReservationV2(request);

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
        assertEquals(TestConstant.INCORRECT_PHONENUMBERS, bookedReservation.getProfile().getPhoneNumbers().size(), 2);
        assertEquals(TestConstant.INCORRECT_ADDRESSES, bookedReservation.getProfile().getAddresses().size(), 2);
        assertEquals(TestConstant.INCORRECT_CUSTOMERDOMINANTPLAY,
                request.getRoomReservation().getCustomerDominantPlay(), bookedReservation.getCustomerDominantPlay());
    }

    /**
     * makeRoomReservation with the basic props along with the below
     * 
     * <li>with multiple cc</li>
     */
    @Test
    public void reservationV2_withMultipleCC_returnsReservationWithMultipleCC() {
        // Creating a reservation with multiple cc in the billing
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-multiplecc.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
        assertEquals(TestConstant.INCORRECT_BILLINGS, bookedReservation.getBilling().size(), 2);
    }

    /**
     * makeRoomReservation with the basic props along with the below
     * 
     * <li>with more than one night stay</li>
     */
    @Test
    public void reservationV2_withMultipleDays_returnsReservationWithMultiNights() {
        // Creating a reservation with more than one night stay
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestMultiNight("/createroomreservationrequest-multinightstay.json", TestConstant.TWO_NIGHTS));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
        assertEquals(TestConstant.INCORRECT_BOOKINGS, bookedReservation.getBookings().size(), 2);
        assertEquals(TestConstant.INCORRECT_MARKETS, bookedReservation.getMarkets().size(), 2);
    }

    /**
     * makeRoomReservation with the basic props but <i>with out guaranteeCode</i>
     */
    @Test
    public void reservationV2_withoutGuaranteeCode_returnReservationWithGuaranteeCode() {
        // Creating a reservation without guaranteeCode and with rrUpcell
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-noguaranteecode.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
        assertNotNull(TestConstant.EMPTY_GUARANTEECODE, bookedReservation.getGuaranteeCode());
    }

    /**
     * makeRoomReservation with the basic props along with the below
     * 
     * <li>with alerts</li>
     * <li>with traces</li>
     * <li>with routingInstructions</li>
     */
    @Test
    public void reservationV2_withAlertsTracesRoutingInstructions_returnsRoomReservation() {
        // Creating a reservation with alerts, tracers and routingInstructions
        CreateRoomReservationResponse response = makeReservationV2(createRequestAlert(
                "/createroomreservationrequest-withalertstracesrouting.json", TestConstant.ONE_NIGHT));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
        assertFalse(TestConstant.EMPTY_ALERTS_OBJECT, CollectionUtils.isEmpty(bookedReservation.getAlerts()));
        assertFalse(TestConstant.EMPTY_TRACES_OBJECT, CollectionUtils.isEmpty(bookedReservation.getTraces()));
        assertFalse(TestConstant.EMPTY_ROUTING_INSTRUCTIONS_OBJECT,
                CollectionUtils.isEmpty(bookedReservation.getRoutingInstructions()));
        assertEquals(TestConstant.INCORRECT_ALERT_ITEMS, bookedReservation.getAlerts().size(), 2);
        assertEquals(TestConstant.INCORRECT_TRACE_ITEMS, bookedReservation.getTraces().size(), 2);
        assertEquals(TestConstant.INCORRECT_ROUTING_INSTRUCTION_ITEMS,
                bookedReservation.getRoutingInstructions().size(), 1);
    }

    /**
     * makeRoomReservation with the basic props along with the below
     * 
     * <li>with single shareWithCustomers</li>
     */
    @Test
    public void reservationV2_withSingleShareWithCustomers_returnsRoomReservation() {
        // Creating a reservation with single shareWithCustomers
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-singlesharewith.json"));

        // Asserting the booked reservation response object
        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        String confNumber = bookedReservation.getConfirmationNumber();
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, confNumber);
    }

    /**
     * makeRoomReservation with the basic props along with the below
     * 
     * <li>with multiple shareWithCustomers</li>
     */
    @Test
    public void reservationV2_withMultipleShareWithCustomers_returnsRoomReservation() {
        // Creating a reservation with multiple shareWithCustomers
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-multiplesharewith.json"));

        // Asserting the booked reservation response object
        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        String confNumber = bookedReservation.getConfirmationNumber();
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, confNumber);
    }

    /**
     * makeRoomReservation with the basic props along with the below
     * 
     * <li>with invalid agentInfo</li>
     */
    @Test
    public void reservationV2_withInvlidAgentInfo_returnsInvalidIATAErrorCode() {
        File requestFile = new File(getClass().getResource("/createroomreservationrequest-iata.json").getPath());
        CreateRoomReservationRequest createRoomReservationRequest = convert(requestFile,
                CreateRoomReservationRequest.class);
        updateRequestWithTestData(createRoomReservationRequest, TestConstant.ONE_NIGHT);

        Mono<ErrorResponse> result = realClient.post().uri(builder -> builder.path(V2_RESERV_URI).build())
                .body(BodyInserters.fromValue(createRoomReservationRequest))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException("Error on reserving room for endpoint " + V2_RESERV_URI
                            + ", Error message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, V2_RESERV_URI))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class));

        ErrorResponse response = result.block();

        assertEquals("Message should be IATA code is not valid", ErrorCode.INVALID_IATA_CODE.getDescription(),
                response.getError().getMessage());

    }

    /**
     * makeRoomReservation with the basic props along with the below
     * 
     * <li>DCC check</li>
     */
    @Test
    public void reservationV2_withDccTrue_returnsDccRoomReservation() {
        File requestFile = new File(getClass().getResource("/createroomreservationrequest-dcc.json").getPath());
        CreateRoomReservationRequest createRoomReservationRequest = convert(requestFile,
                CreateRoomReservationRequest.class);
        updateRequestWithTestData(createRoomReservationRequest, TestConstant.ONE_NIGHT);
        //This is to set the foreign cc token details again, as the above method erase foreign token details
        String[] tokenDetails = defaultTestData.getForeignCCTokenDetails().split("\\|");
        Optional.ofNullable(createRoomReservationRequest.getRoomReservation().getBilling()).ifPresent(payments -> {
            payments.forEach(billing -> {
                billing.getPayment().setType(tokenDetails[0]);
                billing.getPayment().setCcToken(tokenDetails[1]);
                billing.getPayment().setFxCurrencyCode(tokenDetails[2]);
            });
        });
        createRoomReservationRequest.getRoomReservation().setItineraryId(createItineraryId());
        BodyContentSpec body = client.post().uri(builder -> builder.path(V2_RESERV_URI).build())
                .body(BodyInserters.fromValue(createRoomReservationRequest))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();

        try {
            body.jsonPath("$.roomReservation").exists().jsonPath("$.roomReservation.confirmationNumber").exists()
                    .jsonPath("$.roomReservation.bookings").isArray()
                    .jsonPath("$.roomReservation.payments[0].fxCurrencyCode").isEqualTo(createRoomReservationRequest
                            .getRoomReservation().getBilling().get(0).getPayment().getFxCurrencyCode())
                    .jsonPath("$.roomReservation.payments[0].fxAcceptMessage").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    /**
     * makeRoomReservation without billing.
     * 
     */
    @Test
    public void reservationV2_withoutBilling_returnsRoomReservation() {
        // Creating a reservation
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-withoutbilling.json"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
    }

    /**
     * makeRoomReservation without billing for non ice channel.
     * 
     */
    @Test
    public void reservationV2_withoutBilling_returnsInvalidBillingValidationError() {
        File requestFile = new File(
                getClass().getResource("/createroomreservationrequest-withoutbilling.json").getPath());
        CreateRoomReservationRequest createRoomReservationRequest = convert(requestFile,
                CreateRoomReservationRequest.class);
        updateRequestWithTestData(createRoomReservationRequest, TestConstant.ONE_NIGHT);

        Mono<ErrorResponse> result = realClient.post().uri(builder -> builder.path(V2_RESERV_URI).build())
                .body(BodyInserters.fromValue(createRoomReservationRequest))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException("Error on reserving room for endpoint " + V2_RESERV_URI
                            + ", Error message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, V2_RESERV_URI))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class));

        ErrorResponse response = result.block();
        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.INVALID_BILLING, ErrorTypes.VALIDATION_ERROR);

        assertEquals("Error code should be 632-1-121", expectedErrorResponse.getError().getCode(),
                response.getError().getCode());

    }

    //@Test // AFS removed this support; waiting to get it back
    public void reservationV2_withAFSRejectedEmailId_returnAntiFraudError() {
        // Checkout the room for the given reservation-id and
        File requestFile = new File(
                getClass().getResource("/createroomreservationrequest-basic.json").getPath());
        CreateRoomReservationRequest createRoomReservationRequest = convert(requestFile,
                CreateRoomReservationRequest.class);
        createRoomReservationRequest.getRoomReservation().getProfile().setEmailAddress1(ServiceConstant.INVALID_TX_EMAIL);
        createRoomReservationRequest.getRoomReservation().getProfile().setEmailAddress2(ServiceConstant.INVALID_TX_EMAIL);
        updateCcToken(createRoomReservationRequest.getRoomReservation().getBilling());
        Mono<ErrorResponse> result = realClient.post().uri(builder -> builder.path(V2_RESERV_URI).build())
                .body(BodyInserters.fromValue(createRoomReservationRequest))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.MGM_RESORTS, TestConstant.WEB,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    throw new TestExecutionException("Error on reserving room for endpoint " + V2_RESERV_URI
                            + ", Error message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validate4XXFailureResponse(response, V2_RESERV_URI))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ErrorResponse.class));;

        ErrorResponse response = result.block();
        ErrorResponse expectedErrorResponse = getErrorResponse(ErrorCode.TRANSACTION_NOT_AUTHORIZED, ErrorTypes.FUNCTIONAL_ERROR);

        assertEquals("Error code should be 632-2-159", expectedErrorResponse.getError().getCode(),
                response.getError().getCode());
    }

    /**
     * makeRoomReservation with the basic props with source and channel as
     * mgmresorts and WEB respectively.
     * 
     */
    @Test
    public void reservationV2_withBasicDetailsAndWEB_returnsRoomReservation() {
        // Creating a reservation
        CreateRoomReservationResponse response = makeReservationV2(
                createRequestBasic("/createroomreservationrequest-basic.json"), TestConstant.MGM_RESORTS, TestConstant.WEB);

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
        assertTrue("Booking source should be mgmri",
                StringUtils.equals(response.getRoomReservation().getBookingSource(), "mgmri"));
        assertTrue("Booking channel should be WEB",
                StringUtils.equals(response.getRoomReservation().getBookingChannel(), TestConstant.WEB));
    }

    // @Test - commenting as currently it is failing due to 72 hours rules violation
    public void reservationV2_withMyVegasCodeAndServiceToken_returnsRoomReservation() {
        // Creating a reservation
        CreateRoomReservationRequest request = createRequestBasic("/createroomreservationrequest-myvegas.json");
        request.getRoomReservation().getBookings().get(0).setPricingRuleId(null);
        request.getRoomReservation().getBookings().get(0).setProgramIdIsRateTable(false);
        CreateRoomReservationResponse response = makeReservationV2(request, TestConstant.ICE, TestConstant.ICE, null,
                null, getAllScopes(),
                Collections.singletonMap(TestConstant.HEADER_SKIP_MYVEGAS_CONFIRM, "true"));

        assertNotNull(TestConstant.EMPTY_RESPONSE, response);
        RoomReservationV2Response bookedReservation = response.getRoomReservation();
        assertNotNull(TestConstant.EMPTY_ROOM_RESERVATION, bookedReservation);
        assertFalse(TestConstant.EMPTY_BOOKING_OBJECT, CollectionUtils.isEmpty(bookedReservation.getBookings()));
        assertNotNull(TestConstant.EMPTY_CONFIRMATION_NUMBER, bookedReservation.getConfirmationNumber());
        assertEquals(String.format(TestConstant.INCORRECT_PROGRAM_ID, defaultTestData.getMyVegasProgramId()),
                defaultTestData.getMyVegasProgramId(), bookedReservation.getBookings().get(0).getProgramId());
    }

}
