package com.mgm.services.booking.room.v2.it;

import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.ApiDetails;

public class RoomProgramV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        return new ApiDetails(ApiDetails.Method.GET, "/v2/programs/applicable", queryParams, null);
    }

    @Test
    public void getApplicablePrograms_WithValidInput_validateProgramIds() {
        client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getApiDetails().getDefaultQueryParams()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectBody().jsonPath("$.programIds").isArray().jsonPath("$.programIds.[0]").exists();
    }

    @Test
    public void getApplicablePrograms_WithAllParams_validateProgramIds() {
        client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getApplicableProgramsQueryParams()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$.programIds").isArray()
                .jsonPath("$.programIds.[0]").exists();

    }

    private MultiValueMap<String, String> getApplicableProgramsQueryParams() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("customerId", customerId);
        queryParams.add("roomTypeId", defaultTestData.getRoomTypeId());
        queryParams.add("bookDate", getCheckInDate());
        queryParams.add("travelDate", getCheckInDate());
        queryParams.add("filterBookable", "true");
        queryParams.add("filterViewable", "true");
        queryParams.add("checkInDate", getCheckInDate());
        queryParams.add("checkOutDate", getCheckOutDate());
        queryParams.add("numAdults", "2");
        queryParams.add("numChildren", "1");
        return queryParams;
    }
}
