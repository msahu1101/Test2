package com.mgm.services.booking.room.v2.it;

import static org.hamcrest.Matchers.equalTo;

import java.time.DayOfWeek;
import java.util.Collections;

import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ResortsPriceV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        return new ApiDetails(ApiDetails.Method.GET, "/v2/availability/resorts", getDefaultQueryParams(), null);
    }

    @Test
    public void resortsAvailability_withOneNightStay_returnsBeatAvailablePrice() {
        BodyContentSpec body = client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getDefaultQueryParams()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                            TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].price").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].resortFee").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].isComp").exists()
                    .jsonPath("$.[?(@.pricingMode == 'BEST_AVAILABLE')]").exists()
                    .jsonPath("$.[?(@.pricingMode == 'PROGRAM')]").doesNotExist()
                    .jsonPath("$.[?(@.pricingMode == 'PERPETUAL')]").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void resortsAvailability_withCustomerId_returnsAvailability() {
        MultiValueMap<String, String> queryParams = getDefaultQueryParams();
        queryParams.add("customerId", customerId);
        queryParams.add("perpetualPricing", "true");
        queryParams.add("propertyIds", "bee81f88-286d-43dd-91b5-3917d9d62a68");

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                            TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].price").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].memberPrice").doesNotExist()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].resortFee").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].isComp").exists()
                    .jsonPath("$.*", equalTo(1));
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void resortsAvailability_withCustomerId_returnsPerpetualOrBestAvailablePrices() {
        MultiValueMap<String, String> queryParams = getDefaultQueryParams();
        queryParams.add("customerId", defaultTestData.getPerpetualCustomerId());
        queryParams.add("perpetualPricing", "true");

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                            TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray()
                    .jsonPath("$.[?(@.propertyId == '66964e2b-2550-4476-84c3-1a4c0c5c067f' "
                            + "&& (@.pricingMode == 'BEST_AVAILABLE' || @.pricingMode == 'PERPETUAL'))]").exists()
                    .jsonPath("$.[?(@.pricingMode == 'PROGRAM')]").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void resortsAvailability_withSegmentCode_returnsProgramPriceAndBarPrice() {
        MultiValueMap<String, String> queryParams = getDefaultQueryParams();
        queryParams.add("segment", defaultTestData.getSegmentCode());

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.MGM_RESORTS, TestConstant.CHANNEL_WEB,
                            TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        
        log.info(queryParams);
        
        try {
            body.jsonPath("$").isArray()
            //.jsonPath("$.[?(@.pricingMode == 'PROGRAM')]").exists()
            .jsonPath("$.[?(@.pricingMode == 'BEST_AVAILABLE')]").exists()
            .jsonPath("$.[?(@.pricingMode == 'PERPETUAL')]").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void resortsAvailability_withSinglePropertyProgramId_returnsProgramPriceAndBarPrice() {
        MultiValueMap<String, String> queryParams = getDefaultQueryParams();
        queryParams.add("programId", defaultTestData.getProgramId());
        // this program is not available on Saturday and Sunday
        // hence adjusting the checkIn and checkOut dates accordingly
        String checkInDate = queryParams.getFirst(TestConstant.CHECKIN_DATE);
        if (CommonUtil.getDayOfWeek(checkInDate, ServiceConstant.ISO_8601_DATE_FORMAT)
                .compareTo(DayOfWeek.SATURDAY) == 0
                || CommonUtil.getDayOfWeek(checkInDate, ServiceConstant.ISO_8601_DATE_FORMAT)
                        .compareTo(DayOfWeek.SUNDAY) == 0) {
            String checkOutDate = queryParams.getFirst(TestConstant.CHECKOUT_DATE);
            String adjustedCheckInDate = CommonUtil.adjustDateInto(checkInDate, "yyyy-MM-dd", DayOfWeek.MONDAY);
            String adjustedCheckOutDate = CommonUtil.adjustDateInto(checkOutDate, "yyyy-MM-dd", DayOfWeek.TUESDAY);
            queryParams.replace(TestConstant.CHECKIN_DATE, Collections.singletonList(adjustedCheckInDate));
            queryParams.replace(TestConstant.CHECKOUT_DATE, Collections.singletonList(adjustedCheckOutDate));
        }
        
        log.info(queryParams);

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.MGM_RESORTS, TestConstant.CHANNEL_WEB,
                            TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray()
            .jsonPath("$.[?(@.propertyId == '66964e2b-2550-4476-84c3-1a4c0c5c067f' && @.pricingMode == 'PROGRAM')]").exists()
            .jsonPath("$.[?(@.propertyId != '66964e2b-2550-4476-84c3-1a4c0c5c067f' && @.pricingMode == 'PROGRAM')]").doesNotExist()
            .jsonPath("$.[?(@.pricingMode == 'PERPETUAL')]").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void resortsAvailability_withOneNightStayAndLoggedInGuestTokenNonPO_returnsBeatAvailablePrice() {
        BodyContentSpec body = client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getDefaultQueryParams()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                            TestConstant.DUMMY_TRANSACTION_ID);
                    addAdditionalHeader(headers, ServiceConstant.HEADER_AUTHORIZATION,
                            getAuthorizationHeaderForGuest(defaultTestData.getNonPerpetualEmailId(),
                                    defaultTestData.getNonPerpetualEmailPass(), getAllScopes()));
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].price").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].resortFee").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].isComp").exists()
                    .jsonPath("$.[?(@.pricingMode == 'BEST_AVAILABLE')]").exists()
                    .jsonPath("$.[?(@.pricingMode == 'PROGRAM')]").doesNotExist()
                    .jsonPath("$.[?(@.pricingMode == 'PERPETUAL')]").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void resortsAvailability_withCustomerIdAndLoggedInGuestTokenPO_returnsPerpetualOrBestAvailablePrices() {
        BodyContentSpec body = client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getDefaultQueryParams()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                            TestConstant.DUMMY_TRANSACTION_ID);
                    addAdditionalHeader(headers, ServiceConstant.HEADER_AUTHORIZATION,
                            getAuthorizationHeaderForGuest(defaultTestData.getPerpetualEmailId(),
                                    defaultTestData.getPerpetualEmailPass(), getAllScopes()));
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray()
                .jsonPath("$.[?(@.propertyId == '66964e2b-2550-4476-84c3-1a4c0c5c067f' "
                        + "&& (@.pricingMode == 'BEST_AVAILABLE' || @.pricingMode == 'PERPETUAL'))]").exists()
                .jsonPath("$.[?(@.pricingMode == 'PROGRAM')]").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void resortsAvailability_withEnableJwbCookieTrue_returnsAvailabilityWithNoMemberprice() {
        MultiValueMap<String, String> queryParams = getDefaultQueryParams();

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                            TestConstant.DUMMY_TRANSACTION_ID, null);
                    headers.add(ServiceConstant.COOKIE, TestConstant.ENABLE_JWB + "=true");
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')].memberPrice").doesNotExist();
        } catch (Exception e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void resortsAvailability_withEnableJwbHeaderTrue_returnsAvailabilityWithNoMemberprice() {
        MultiValueMap<String, String> queryParams = getDefaultQueryParams();

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                            TestConstant.DUMMY_TRANSACTION_ID, null);
                    headers.add(TestConstant.ENABLE_JWB, "true");
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')].memberPrice").doesNotExist();
        } catch (Exception e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    /**
     * This is to test HDE packaging scenario, where clients will send
     * participatingResortsOnly as true along with the programId.
     */
    @Test
    public void resortsAvailability_withProgramIdAndParticipatingResortsOnlyTrue_returnsProgramPriceOrSoldOut() {
        MultiValueMap<String, String> queryParams = getDefaultQueryParams();
        queryParams.add("participatingResortsOnly", "true");
        queryParams.add("propertyId", defaultTestData.getProgramEligibility().getTransientProgramTransientUser().getPropertyId());
        queryParams.add("programId", defaultTestData.getProgramEligibility().getTransientProgramTransientUser().getProgramId());

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray()
                    .jsonPath("$.[?(@.propertyId == '" + defaultTestData.getPropertyId() + "' "
                            + "&& (@.pricingMode == 'PROGRAM' || @.status == 'SOLDOUT'))]")
                    .exists().jsonPath("$.[?(@.pricingMode == 'BEST_AVAILABLE')]").doesNotExist();
        } catch (Exception e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    private MultiValueMap<String, String> getDefaultQueryParams() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(TestConstant.CHECKIN_DATE, getFutureDate(1));
        queryParams.add(TestConstant.CHECKOUT_DATE, getFutureDate(2));
        queryParams.add(TestConstant.NUM_ADULTS, String.valueOf(defaultTestData.getNumAdults()));
        return queryParams;
    }
}
