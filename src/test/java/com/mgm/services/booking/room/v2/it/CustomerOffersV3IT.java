package com.mgm.services.booking.room.v2.it;

import org.junit.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.ApiDetails;

public class CustomerOffersV3IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        return new ApiDetails(ApiDetails.Method.GET, "/v3/customer/offers", queryParams, null);
    }

    @Test
    public void getCustomerOffersV3_whenUserIsTransient_expectValidResponse() {

        client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParam("propertyId", defaultTestData.getPropertyId()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.MGMRI, TestConstant.WEB, TestConstant.DUMMY_TRANSACTION_ID,
                            null);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$.offers").isArray();
    }

    @Test
    public void getCustomerOffersV3_whenUserIsMlife_expectValidResponse() {

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("customerId", customerId);
        queryParams.add("mlifeNumber", defaultTestData.getMlifeNumber());

        invokeAndValidate(queryParams);
    }

    @Test
    public void getCustomerOffersV3_whenUserIsMlifePo_expectValidResponse() {

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("customerId", defaultTestData.getPerpetualCustomerId());
        queryParams.add("mlifeNumber", defaultTestData.getPerpetualMlifeNumber());
        queryParams.add("perpetualPricing", "true");

        invokeAndValidate(queryParams);
    }

    @Test
    public void getCustomerOffersV3_whenUserIsMlifePoRequestingOnlyPoRates_expectValidResponse() {

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("customerId", defaultTestData.getPerpetualCustomerId());
        queryParams.add("mlifeNumber", defaultTestData.getPerpetualMlifeNumber());
        queryParams.add("perpetualPricing", "true");
        queryParams.add("onlyPoPrograms", "true");

        invokeAndValidate(queryParams);
    }

    private void invokeAndValidate(MultiValueMap<String, String> queryParams) {
        client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.MGMRI, TestConstant.WEB, TestConstant.DUMMY_TRANSACTION_ID,
                            null);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$.offers").isArray();
    }

}
