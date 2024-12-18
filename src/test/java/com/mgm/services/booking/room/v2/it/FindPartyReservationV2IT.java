package com.mgm.services.booking.room.v2.it;

import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;

public class FindPartyReservationV2IT extends BaseRoomBookingV2IntegrationTest {

    private static final String PARTY = "PARTY";
    private static final String SHAREWITH = "SHAREWITH";

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        return new ApiDetails(ApiDetails.Method.GET, "/v2/reservation/party/info", queryParams, null);
    }

    @Test
    public void getPartyReservation_SuccessRetrival_validateResponse() {

        client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParam("confirmationNumber", defaultTestData.getPartyReservation())
                        .queryParam(TestConstant.CACHE_ONLY_KEY, TestConstant.TRUE_STRING).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$.reservationAdditionalInfo").isArray()
                .jsonPath("$.reservationAdditionalInfo.[0].status").exists()
                .jsonPath("$.reservationAdditionalInfo.[0].reservationTypes[0]").exists().equals(PARTY);
    }

    @Test
    public void getShareWithReservation_SuccessRetrival_validateResponse() {
        CreateRoomReservationResponse response = makeReservationV2AndValidate(
                createRequestBasic("/createroomreservationrequest-multiplesharewith.json"));
        client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParam("confirmationNumber", response.getRoomReservation().getConfirmationNumber())
                        .queryParam(TestConstant.CACHE_ONLY_KEY, TestConstant.TRUE_STRING).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$.reservationAdditionalInfo").isArray()
                .jsonPath("$.reservationAdditionalInfo.[0].status").exists()
                .jsonPath("$.reservationAdditionalInfo.[0].reservationTypes[0]").exists().equals(SHAREWITH);
    }

}
