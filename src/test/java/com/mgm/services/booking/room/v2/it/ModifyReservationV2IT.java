/**
 * Class to keep integration tests related to reservation under ReservationV2Controller.
 */
package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Test;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.request.CreditCardRequest;
import com.mgm.services.booking.room.model.request.ModifyRoomReservationRequest;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.ModifyRoomReservationResponse;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

/**
 * Class to keep integration tests related to modify reservation under
 * ModifyV2Controller.
 * 
 * @author vararora
 *
 */
@Log4j2
public class ModifyReservationV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.PUT, "/v2/reservation", null,
                createRequestBasic("/createroomreservationrequest-forModify.json"));
    }

    @Test
    public void modifyReservation_withUpdatedDetails_returnModifiedReservation() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-forModify.json"));
        File file = new File(getClass().getResource("/modifyRoomReservationRequest-updatedprofile.json").getPath());
        ModifyRoomReservationRequest modifyReservationRequest = convert(file, ModifyRoomReservationRequest.class);
        if (null != customerId) {
            modifyReservationRequest.getRoomReservation().setCustomerId(NumberUtils.toLong(customerId));
            if (null != modifyReservationRequest.getRoomReservation().getProfile()) {
                modifyReservationRequest.getRoomReservation().getProfile().setId(NumberUtils.toLong(customerId));
            }
        }
        modifyReservationRequest.getRoomReservation().getBookings().get(0)
                .setProgramId(createRoomReservationResponse.getRoomReservation().getBookings().get(0).getProgramId());
        modifyReservationRequest.getRoomReservation().getBookings().get(0).setPricingRuleId(
                createRoomReservationResponse.getRoomReservation().getBookings().get(0).getPricingRuleId());
        modifyReservationRequest.getRoomReservation()
                .setItineraryId(createRoomReservationResponse.getRoomReservation().getItineraryId());
        modifyReservationRequest.getRoomReservation()
                .setRoomTypeId(createRoomReservationResponse.getRoomReservation().getRoomTypeId());
        modifyReservationRequest.getRoomReservation()
                .setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        modifyReservationRequest.getRoomReservation()
                .setState(createRoomReservationResponse.getRoomReservation().getState());
        modifyReservationRequest.getRoomReservation().setId(createRoomReservationResponse.getRoomReservation().getId());
        Date checkInDate = createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckInDate();
        Date checkOutDate = createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckOutDate();
        modifyReservationRequest.getRoomReservation().getTripDetails().setCheckInDate(checkInDate);
        modifyReservationRequest.getRoomReservation().getTripDetails().setCheckOutDate(checkOutDate);
        modifyReservationRequest.getRoomReservation().getBookings().get(0).setDate(checkInDate);
        modifyReservationRequest.getRoomReservation().getTripDetails()
                .setNumAdults(createRoomReservationResponse.getRoomReservation().getTripDetails().getNumAdults() + 1);
        modifyReservationRequest.getRoomReservation().getProfile().setFirstName("IT Test - First Name");
        modifyReservationRequest.getRoomReservation().getProfile().setLastName("IT Test - Last Name");
        modifyReservationRequest.getRoomReservation().getPayments().get(0)
        .setChargeAmount(modifyReservationRequest.getRoomReservation().getDepositDetails().getAmount());
        updateCcToken(modifyReservationRequest.getRoomReservation().getBilling());
        ModifyRoomReservationResponse modifyRoomReservationResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(modifyReservationRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> {
                    validateSuccessResponse(response, getApiDetails().getBaseServiceUrl());
                    validateResponseHeaders(response);
                }).flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        assertEquals("Room Reservation confirmation number should be same.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                modifyRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        assertEquals("Num Adults should be equal",
                modifyRoomReservationResponse.getRoomReservation().getTripDetails().getNumAdults(),
                createRoomReservationResponse.getRoomReservation().getTripDetails().getNumAdults() + 1);
        assertEquals("First Name should be equal",
                modifyRoomReservationResponse.getRoomReservation().getProfile().getFirstName(), "IT Test - First Name");
        assertEquals("Last Name should be equal",
                modifyRoomReservationResponse.getRoomReservation().getProfile().getLastName(), "IT Test - Last Name");
        assertTrue("Booking source should be ICE", StringUtils
                .equals(modifyRoomReservationResponse.getRoomReservation().getBookingSource(), TestConstant.ICE));
        assertTrue("Booking channel should be ICE", StringUtils
                .equals(modifyRoomReservationResponse.getRoomReservation().getBookingChannel(), TestConstant.ICE));
    }

    @Test
    public void modifyReservation_withSpecialRequests_returnModifiedReservation() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-forModify.json"));
        File file = new File(getClass().getResource("/modifyRoomReservationRequest-updatedprofile.json").getPath());
        ModifyRoomReservationRequest modifyReservationRequest = convert(file, ModifyRoomReservationRequest.class);
        if (null != customerId) {
            modifyReservationRequest.getRoomReservation().setCustomerId(NumberUtils.toLong(customerId));
            if (null != modifyReservationRequest.getRoomReservation().getProfile()) {
                modifyReservationRequest.getRoomReservation().getProfile().setId(NumberUtils.toLong(customerId));
            }
        }
        modifyReservationRequest.getRoomReservation().getBookings().get(0)
                .setProgramId(createRoomReservationResponse.getRoomReservation().getBookings().get(0).getProgramId());
        modifyReservationRequest.getRoomReservation().getBookings().get(0).setPricingRuleId(
                createRoomReservationResponse.getRoomReservation().getBookings().get(0).getPricingRuleId());
        modifyReservationRequest.getRoomReservation()
                .setItineraryId(createRoomReservationResponse.getRoomReservation().getItineraryId());
        modifyReservationRequest.getRoomReservation()
                .setRoomTypeId(createRoomReservationResponse.getRoomReservation().getRoomTypeId());
        modifyReservationRequest.getRoomReservation()
                .setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        modifyReservationRequest.getRoomReservation()
                .setState(createRoomReservationResponse.getRoomReservation().getState());
        modifyReservationRequest.getRoomReservation().setId(createRoomReservationResponse.getRoomReservation().getId());
        List<String> specialRequests = new ArrayList<>();
        specialRequests.add("03ca81ad-b895-4e04-8993-aaed5b80c605");
        specialRequests.add("a8f0b450-dcf2-46e5-b06e-4006d4f619d1");
        modifyReservationRequest.getRoomReservation().setSpecialRequests(specialRequests);

        Date checkInDate = createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckInDate();
        Date checkOutDate = createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckOutDate();
        modifyReservationRequest.getRoomReservation().getTripDetails().setCheckInDate(checkInDate);
        modifyReservationRequest.getRoomReservation().getTripDetails().setCheckOutDate(checkOutDate);
        modifyReservationRequest.getRoomReservation().getBookings().get(0).setDate(checkInDate);
        modifyReservationRequest.getRoomReservation().getPayments().get(0)
        .setChargeAmount(modifyReservationRequest.getRoomReservation().getDepositDetails().getAmount());
        updateCcToken(modifyReservationRequest.getRoomReservation().getBilling());
        ModifyRoomReservationResponse modifyRoomReservationResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(modifyReservationRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        assertEquals("Room Reservation confirmation number should be same.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                modifyRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        assertEquals("Room Reservation special request size should be same.", 2,
                modifyRoomReservationResponse.getRoomReservation().getSpecialRequests().size());
    }

    @Test
    public void modifyReservation_withDateChanges_returnModifiedReservation() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-forModify.json"));
        File file = new File(getClass().getResource("/modifyRoomReservationRequest-updatedprofile.json").getPath());
        ModifyRoomReservationRequest modifyReservationRequest = convert(file, ModifyRoomReservationRequest.class);
        if (null != customerId) {
            modifyReservationRequest.getRoomReservation().setCustomerId(NumberUtils.toLong(customerId));
            if (null != modifyReservationRequest.getRoomReservation().getProfile()) {
                modifyReservationRequest.getRoomReservation().getProfile().setId(NumberUtils.toLong(customerId));
            }
        }
        modifyReservationRequest.getRoomReservation().getBookings().get(0)
                .setProgramId(createRoomReservationResponse.getRoomReservation().getBookings().get(0).getProgramId());
        modifyReservationRequest.getRoomReservation().getBookings().get(0).setPricingRuleId(
                createRoomReservationResponse.getRoomReservation().getBookings().get(0).getPricingRuleId());
        modifyReservationRequest.getRoomReservation()
                .setItineraryId(createRoomReservationResponse.getRoomReservation().getItineraryId());
        modifyReservationRequest.getRoomReservation()
                .setRoomTypeId(createRoomReservationResponse.getRoomReservation().getRoomTypeId());
        modifyReservationRequest.getRoomReservation()
                .setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        modifyReservationRequest.getRoomReservation()
                .setState(createRoomReservationResponse.getRoomReservation().getState());
        modifyReservationRequest.getRoomReservation().setId(createRoomReservationResponse.getRoomReservation().getId());

        Date checkInDate = getFutureDateObj(30);
        Date checkOutDate = getFutureDateObj(31);
        modifyReservationRequest.getRoomReservation().getTripDetails().setCheckInDate(checkInDate);
        modifyReservationRequest.getRoomReservation().getTripDetails().setCheckOutDate(checkOutDate);
        modifyReservationRequest.getRoomReservation().getBookings().get(0).setDate(checkInDate);
        modifyReservationRequest.getRoomReservation().getPayments().get(0)
        .setChargeAmount(modifyReservationRequest.getRoomReservation().getDepositDetails().getAmount());
        updateCcToken(modifyReservationRequest.getRoomReservation().getBilling());
        
        log.info("Modify Reservation Request: {}", CommonUtil.convertObjectToJsonString(modifyReservationRequest));
        
        ModifyRoomReservationResponse modifyRoomReservationResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(modifyReservationRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();
        
        log.info("Modify Reservation Response: {}", CommonUtil.convertObjectToJsonString(modifyRoomReservationResponse));

        assertEquals("Room Reservation confirmation number should be same.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                modifyRoomReservationResponse.getRoomReservation().getConfirmationNumber());

    }

    @Test
    public void modifyReservation_withPaymentChange_returnModifiedReservation() {
        CreateRoomReservationResponse createRoomReservationResponse = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-forModify.json"));
        File file = new File(getClass().getResource("/modifyRoomReservationRequest-updatedprofile.json").getPath());
        ModifyRoomReservationRequest modifyReservationRequest = convert(file, ModifyRoomReservationRequest.class);
        if (null != customerId) {
            modifyReservationRequest.getRoomReservation().setCustomerId(NumberUtils.toLong(customerId));
            if (null != modifyReservationRequest.getRoomReservation().getProfile()) {
                modifyReservationRequest.getRoomReservation().getProfile().setId(NumberUtils.toLong(customerId));
            }
        }
        modifyReservationRequest.getRoomReservation().getBookings().get(0)
                .setProgramId(createRoomReservationResponse.getRoomReservation().getBookings().get(0).getProgramId());
        modifyReservationRequest.getRoomReservation().getBookings().get(0).setPricingRuleId(
                createRoomReservationResponse.getRoomReservation().getBookings().get(0).getPricingRuleId());
        modifyReservationRequest.getRoomReservation()
                .setItineraryId(createRoomReservationResponse.getRoomReservation().getItineraryId());
        modifyReservationRequest.getRoomReservation()
                .setRoomTypeId(createRoomReservationResponse.getRoomReservation().getRoomTypeId());
        modifyReservationRequest.getRoomReservation()
                .setConfirmationNumber(createRoomReservationResponse.getRoomReservation().getConfirmationNumber());
        modifyReservationRequest.getRoomReservation()
                .setState(createRoomReservationResponse.getRoomReservation().getState());
        modifyReservationRequest.getRoomReservation().setId(createRoomReservationResponse.getRoomReservation().getId());

        Date checkInDate = createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckInDate();
        Date checkOutDate = createRoomReservationResponse.getRoomReservation().getTripDetails().getCheckOutDate();
        modifyReservationRequest.getRoomReservation().getTripDetails().setCheckInDate(checkInDate);
        modifyReservationRequest.getRoomReservation().getTripDetails().setCheckOutDate(checkOutDate);
        modifyReservationRequest.getRoomReservation().getBookings().get(0).setDate(checkInDate);
        CreditCardRequest ccRequest = modifyPayment(
                modifyReservationRequest.getRoomReservation().getBilling().get(0).getPayment());
        modifyReservationRequest.getRoomReservation().getBilling().get(0).setPayment(ccRequest);
        modifyReservationRequest.getRoomReservation().getPayments().get(0)
        .setChargeAmount(modifyReservationRequest.getRoomReservation().getDepositDetails().getAmount());

        ModifyRoomReservationResponse modifyRoomReservationResponse = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(modifyReservationRequest)).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().doOnError(error -> {
                    throw new TestExecutionException("Error occurred on executing url : "
                            + getApiDetails().getBaseServiceUrl() + ", Error Message : " + error.getMessage(), error);
                }).doOnSuccess(response -> validateSuccessResponse(response, getApiDetails().getBaseServiceUrl()))
                .flatMap(clientResponse -> clientResponse.bodyToMono(ModifyRoomReservationResponse.class)).block();

        assertEquals("Room Reservation confirmation number should be same.",
                createRoomReservationResponse.getRoomReservation().getConfirmationNumber(),
                modifyRoomReservationResponse.getRoomReservation().getConfirmationNumber());

        assertEquals("Room Reservation payment ccToken should be same.", ccRequest.getCcToken(),
                modifyReservationRequest.getRoomReservation().getBilling().get(0).getPayment().getCcToken());
        assertEquals("Room Reservation payment card holder should be same.", ccRequest.getCardHolder(),
                modifyReservationRequest.getRoomReservation().getBilling().get(0).getPayment().getCardHolder());
        assertEquals("Room Reservation payment expiry should be same.", ccRequest.getExpiry(),
                modifyReservationRequest.getRoomReservation().getBilling().get(0).getPayment().getExpiry());
    }

    private CreditCardRequest modifyPayment(CreditCardRequest payment) {
        NumberFormat numberFormat = new DecimalFormat("00");
        Calendar cal2 = Calendar.getInstance();
        String expiryString = numberFormat.format(cal2.get(Calendar.MONTH) + 1) + "/"
                + Integer.toString(cal2.get(Calendar.YEAR) + 1);
        payment.setCcToken("LXM8dmG5n4Q/vcxr2RI3INxrHJkyRbwncs7AhDGc+YC/uXGHoatACqkVWNTUuRKoSBReazxuYfCpsGDeO0ydsB==");
        payment.setExpiry(expiryString);
        payment.setCardHolder(" Test Test");
        return payment;
    }

}
