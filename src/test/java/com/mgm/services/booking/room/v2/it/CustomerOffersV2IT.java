package com.mgm.services.booking.room.v2.it;

import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.ApiDetails;

public class CustomerOffersV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        return new ApiDetails(ApiDetails.Method.GET, "/v2/customer/offers", queryParams, null);
    }

    @Test
    public void getCustomerOffers_WithValidInput_validateCustomerOffers() {
        client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParam("propertyId", defaultTestData.getPropertyId()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$.offers").isArray()
                .jsonPath("$.offers.[0].id").exists();
    }

    @Test
    public void getCustomerOffers_WithAllParams_validateCustomerOffers() {
        client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getCustomerOffersQueryParams()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$.offers").isArray()
                .jsonPath("$.offers.[0].id").exists();
    }

    private MultiValueMap<String, String> getCustomerOffersQueryParams() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("customerId", customerId);
        queryParams.add("notRolledToSegments", "true");
        queryParams.add("notSorted", "true");
        return queryParams;
    }

}
