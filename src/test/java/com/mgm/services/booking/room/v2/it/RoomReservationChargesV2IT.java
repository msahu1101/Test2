package com.mgm.services.booking.room.v2.it;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.web.reactive.function.BodyInserters;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.ValidAvailabilityData;
import com.mgm.services.booking.room.model.request.RoomReservationChargesRequest;
import com.mgm.services.booking.room.model.response.RoomReservationChargesResponse;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Log4j2
public class RoomReservationChargesV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.PUT, "/v2/reservation/charges", null,
                new RoomReservationChargesRequest());
    }

    @Test
    public void calculateRoomReservationCharges_withSpecialRequest_validateResponse() {
        RoomReservationChargesRequest requestWithSpecialRequest = getRoomReservationChargesRequest();
        List<String> specialRequests = new ArrayList<>();
        specialRequests.add("03ca81ad-b895-4e04-8993-aaed5b80c605");
        requestWithSpecialRequest.setSpecialRequests(specialRequests);

        Mono<RoomReservationChargesResponse> resultPost = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(requestWithSpecialRequest))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException("Error on accessing endpoint "
                            + getApiDetails().getBaseServiceUrl() + ", Error message : " + error.getMessage(), error);
                }).flatMap(clientResponse -> {
                    validateSuccessResponse(clientResponse, getApiDetails().getBaseServiceUrl());
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("RoomReservationCharges - with SR Response: {}", entity.getBody());
                        return convert(entity.getBody(), RoomReservationChargesResponse.class);
                    });

                });
        RoomReservationChargesResponse responseWithSpecialRequest = resultPost.block();
        RoomReservationChargesRequest requestWithoutSpecialRequest = getRoomReservationChargesRequest();

        RoomReservationChargesResponse responseWithoutSpecialRequest = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(requestWithoutSpecialRequest))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException("Error on accessing endpoint "
                            + getApiDetails().getBaseServiceUrl() + ", Error message : " + error.getMessage(), error);
                }).flatMap(clientResponse -> {
                    validateSuccessResponse(clientResponse, getApiDetails().getBaseServiceUrl());
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("RoomReservationCharges - without SR Response: {}", entity.getBody());
                        return convert(entity.getBody(), RoomReservationChargesResponse.class);
                    });

                }).block();

        assertTrue("Special Request should return", !responseWithSpecialRequest.getSpecialRequests().isEmpty());
        assertTrue("Special Request should be empty", responseWithoutSpecialRequest.getSpecialRequests().isEmpty());
        /*
         * assertTrue("Special request reservation Amount should be greater",
         * responseWithSpecialRequest.getChargesAndTaxes().getCharges().get(0)
         * .getAmount() >
         * responseWithoutSpecialRequest.getChargesAndTaxes().getCharges().get(0).
         * getAmount());
         */
    }

    @Test
    public void calculateRoomReservationCharges_withRoomPriceOverride_validateResponse() {
        RoomReservationChargesRequest request = getRoomReservationChargesRequest();
        request.getBookings().get(0).setOverridePrice(100);

        Mono<RoomReservationChargesResponse> resultPost = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(request))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException("Error on accessing endpoint "
                            + getApiDetails().getBaseServiceUrl() + ", Error message : " + error.getMessage(), error);
                }).flatMap(clientResponse -> {
                    validateSuccessResponse(clientResponse, getApiDetails().getBaseServiceUrl());
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("RoomReservationCharges Response: {}", entity.getBody());
                        return convert(entity.getBody(), RoomReservationChargesResponse.class);
                    });

                });
        RoomReservationChargesResponse response = resultPost.block();
        assertTrue("Override price in booking should be populated non-zero",
                response.getBookings().get(0).getOverridePrice() > 0);
    }

    @Test
    public void calculateRoomReservationCharges_withDepositOverride_validateResponse() {
        RoomReservationChargesRequest request = getRoomReservationChargesRequest();
        request.getDepositDetails().setOverrideAmount(100);

        Mono<RoomReservationChargesResponse> resultPost = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(request))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException("Error on accessing endpoint "
                            + getApiDetails().getBaseServiceUrl() + ", Error message : " + error.getMessage(), error);
                }).flatMap(clientResponse -> {
                    validateSuccessResponse(clientResponse, getApiDetails().getBaseServiceUrl());
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("RoomReservationCharges Response: {}", entity.getBody());
                        return convert(entity.getBody(), RoomReservationChargesResponse.class);
                    });

                });
        RoomReservationChargesResponse response = resultPost.block();
        assertTrue("Deposit Override price should be populated non-zero",
                response.getDepositDetails().getOverrideAmount() > 0);
    }

    @Test
    public void calculateRoomReservationCharges_multiNight_validateResponse() {
        RoomReservationChargesRequest request = getRoomReservationChargesRequest(
                "roomreservationchargesrequest-multinight.json");

        Mono<RoomReservationChargesResponse> resultPost = realClient.put()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).build())
                .body(BodyInserters.fromValue(request))
                .headers(httpHeaders -> addAllHeaders(httpHeaders, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().doOnError(error -> {
                    log.error("Error trying to make reservation {}", error);
                    throw new TestExecutionException("Error on accessing endpoint "
                            + getApiDetails().getBaseServiceUrl() + ", Error message : " + error.getMessage(), error);
                }).flatMap(clientResponse -> {
                    validateSuccessResponse(clientResponse, getApiDetails().getBaseServiceUrl());
                    return clientResponse.toEntity(String.class).map(entity -> {
                        log.info("RoomReservationCharges Response: {}", entity.getBody());
                        return convert(entity.getBody(), RoomReservationChargesResponse.class);
                    });

                });
        int numNights = request.getBookings().size();
        RoomReservationChargesResponse response = resultPost.block();
        assertTrue("Bookings should be three", response.getBookings().size() == numNights);
        assertTrue("chargesAndTaxes.charges should be three",
                response.getChargesAndTaxes().getCharges().size() == numNights);
        assertTrue("chargesAndTaxes.taxesAndFees should be three",
                response.getChargesAndTaxes().getTaxesAndFees().size() == numNights);
        assertTrue("markets should be three", response.getMarkets().size() == numNights);
        assertTrue("depositDetails.dueDate should be present", response.getDepositDetails().getDueDate() != null);
    }

    private RoomReservationChargesRequest getRoomReservationChargesRequest() {
        ValidAvailabilityData availData = getAvailability(TestConstant.ONE_NIGHT);
        File requestFile = new File(getClass().getResource("/roomreservationchargesrequest-basic.json").getPath());
        RoomReservationChargesRequest request = convert(requestFile, RoomReservationChargesRequest.class);
        if (null != customerId) {
            request.setCustomerId(NumberUtils.toLong(customerId));
        }

        Date checkInDate = getDate(availData.getCheckInDate());
        Date checkOutDate = getDate(availData.getCheckOutDate());

        request.setRoomTypeId(availData.getRoomTypeId());
        request.setProgramId(availData.getProgramId());

        request.getTripDetails().setCheckInDate(checkInDate);
        request.getTripDetails().setCheckOutDate(checkOutDate);

        request.getBookings().get(0).setDate(checkInDate);
        request.getBookings().get(0).setProgramId(availData.getProgramId());
        request.getBookings().get(0).setProgramIdIsRateTable(availData.isProgramIdIsRateTable());
        request.getDepositDetails().setDueDate(checkInDate);
        request.getDepositDetails().setForfeitDate(addDays(availData.getCheckInDate(), TestConstant.TWO_NIGHTS));
        return request;
    }

    /**
     * Return RoomReservationChargesRequest object for three nights.
     * 
     * @param fileName fileName
     * @return RoomReservationChargesRequest object for three nights
     */
    private RoomReservationChargesRequest getRoomReservationChargesRequest(String fileName) {
        ValidAvailabilityData availData = getAvailability(TestConstant.THREE_NIGHTS);
        File requestFile = new File(getClass().getResource("/" + fileName).getPath());
        RoomReservationChargesRequest request = convert(requestFile, RoomReservationChargesRequest.class);
        if (null != customerId) {
            request.setCustomerId(NumberUtils.toLong(customerId));
        }

        Date checkInDate = getDate(availData.getCheckInDate());
        Date checkOutDate = getDate(availData.getCheckOutDate());

        request.setRoomTypeId(availData.getRoomTypeId());
        request.setProgramId(availData.getProgramId());

        request.getTripDetails().setCheckInDate(checkInDate);
        request.getTripDetails().setCheckOutDate(checkOutDate);

        request.getBookings().get(0).setDate(checkInDate);
        request.getBookings().get(0).setProgramId(availData.getProgramId());
        request.getBookings().get(0).setProgramIdIsRateTable(availData.isProgramIdIsRateTable());
        request.getBookings().get(1).setDate(addDays(availData.getCheckInDate(), 1));
        request.getBookings().get(1).setProgramId(availData.getProgramId());
        request.getBookings().get(1).setProgramIdIsRateTable(availData.isProgramIdIsRateTable());
        request.getBookings().get(2).setDate(addDays(availData.getCheckInDate(), 2));
        request.getBookings().get(2).setProgramId(availData.getProgramId());
        request.getBookings().get(2).setProgramIdIsRateTable(availData.isProgramIdIsRateTable());
        return request;
    }
}
