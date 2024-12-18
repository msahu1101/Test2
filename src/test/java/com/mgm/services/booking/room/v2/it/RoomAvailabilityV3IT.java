package com.mgm.services.booking.room.v2.it;

import static org.hamcrest.Matchers.greaterThan;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RoomAvailabilityV3IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add(TestConstant.CHECKIN_DATE, getFutureDate(1));
        queryParams.add(TestConstant.CHECKOUT_DATE, getFutureDate(2));
        queryParams.add(TestConstant.PROPERTY_ID, defaultTestData.getPropertyId());
        queryParams.add(TestConstant.NUM_ADULTS, String.valueOf(defaultTestData.getNumAdults()));
        return new ApiDetails(ApiDetails.Method.GET, "/v3/availability/trip", queryParams, null);
    }

    @Test
    public void getRoomAvailability_withPerpetualPricingFalseAndNoProgramId_returnsMultipleRatePlans() {
        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParams(getApiDetails().getDefaultQueryParams()).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans[0]").exists().jsonPath("$.ratePlans[1]").exists();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void getRoomAvailability_withLoggedInGuestTokenNonPO_returnsMultipleRatePlans() {
        BodyContentSpec body = client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getApiDetails().getDefaultQueryParams()).build()).headers(headers -> {
                    addAllHeaders(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                            TestConstant.DUMMY_TRANSACTION_ID);
                    addAdditionalHeader(headers, ServiceConstant.HEADER_AUTHORIZATION,
                            getAuthorizationHeaderForGuest(defaultTestData.getNonPerpetualEmailId(),
                                    defaultTestData.getNonPerpetualEmailPass(), getAllScopes()));
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans[0]").exists().jsonPath("$.metadata.shoppingFlow").isEqualTo("RATE_PLANS");
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void getRoomAvailability_withPerpetualPricingFalseAndProgramIdPresent_returnsSingleRatePlan() {
        MultiValueMap<String, String> queryParams = getApiDetails().getDefaultQueryParams();
        queryParams.add(TestConstant.PROGRAM_ID, defaultTestData.getProgramId());

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans[0]").exists().jsonPath("$.ratePlans[1]").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void getRoomAvailability_withIncludeDefaultRatePlansFalseAndProgramIdPresent_returnsSingleRatePlan() {
        MultiValueMap<String, String> queryParams = getApiDetails().getDefaultQueryParams();
        queryParams.add("includeDefaultRatePlans", "false");
        queryParams.add(TestConstant.PROGRAM_ID, defaultTestData.getProgramId());

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans[0]").exists().jsonPath("$.ratePlans[1]").doesNotExist();
        } catch (Exception e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void getRoomAvailability_withPerpetualPricingTrueAndNoProgramId_returnsSingleRatePlan() {
        MultiValueMap<String, String> queryParams = getApiDetails().getDefaultQueryParams();
        queryParams.add(TestConstant.PERPETUAL_PRICING, TestConstant.TRUE_STRING);

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans[0]").exists().jsonPath("$.ratePlans[1]").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void getRoomAvailability_withValidInputs_returnsRatePlans() {
        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParams(getApiDetails().getDefaultQueryParams()).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans[0].startingPrice").exists().jsonPath("$.ratePlans[0].startingPrice.resortFee")
                    .isNumber().jsonPath("$.ratePlans[0].startingPrice.baseAveragePrice").isNumber()
                    .jsonPath("$.ratePlans[0].startingPrice.discountedAveragePrice").isNumber()
                    .jsonPath("$.ratePlans[0].startingPrice.baseSubtotal").isNumber()
                    .jsonPath("$.ratePlans[0].startingPrice.discountedSubtotal").isNumber()
                    .jsonPath("$.ratePlans[0].rooms.*", greaterThan(1));
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    /**
     * Validating the existence of additional fields added as part of booksvc-36307
     * and booksvc-36311.
     */
    @Test
    public void getRoomAvailability_withValidInputs_returnsRatePlansWithAdditionalFields() {
        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParams(getApiDetails().getDefaultQueryParams()).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            validateForAdditionalFields(body);
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void getRoomAvailability_withEnableJwbCookieTrue_returnsMultipleRatePlansWithNoMemberprice() {
        BodyContentSpec body = client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getApiDetails().getDefaultQueryParams()).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                    headers.add(ServiceConstant.COOKIE, TestConstant.ENABLE_JWB + "=true");
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans..rooms..memberPrice").doesNotExist();
        } catch (Exception e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }


    /**
     * Validating the existence of additional fields added as part of booksvc-36307
     * and booksvc-36311.
     */
    @Test
    public void getRoomAvailability_withEnableJwbCookieTrue_returnsMultipleRatePlansWithAdditionalFields() {
        BodyContentSpec body = client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                .queryParams(getApiDetails().getDefaultQueryParams()).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                    headers.add(ServiceConstant.COOKIE, TestConstant.ENABLE_JWB + "=true");
                }).exchange().expectStatus().isOk().expectBody();
        try {
            validateForAdditionalFields(body);
        } catch (Exception e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    @Test
    public void getRoomAvailability_withEnableJwbHeaderTrue_returnsMultipleRatePlansWithNoMemberprice() {
        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParams(getApiDetails().getDefaultQueryParams()).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                    headers.add(TestConstant.ENABLE_JWB, "true");
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans..rooms..memberPrice").doesNotExist();
        } catch (Exception e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }


    /**
     * Validating the existence of additional fields added as part of booksvc-36307
     * and booksvc-36311.
     */
    @Test
    public void getRoomAvailability_withEnableJwbHeaderTrue_returnsMultipleRatePlansWithAdditionalFields() {
        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl())
                        .queryParams(getApiDetails().getDefaultQueryParams()).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                    headers.add(TestConstant.ENABLE_JWB, "true");
                }).exchange().expectStatus().isOk().expectBody();
        try {
            validateForAdditionalFields(body);
        } catch (Exception e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    private void validateForAdditionalFields(BodyContentSpec body) {
        body.jsonPath("$.ratePlans[0].startingPrice.discountedSubtotal").isNumber()
                .jsonPath("$.ratePlans..rooms..price.discountsTotal").exists()
                .jsonPath("$.ratePlans[0].rooms[0].price.discountsTotal").isNumber()
                .jsonPath("$.ratePlans..rooms..price.resortFeeTotal").exists()
                .jsonPath("$.ratePlans[0].rooms[0].price.resortFeeTotal").isNumber()
                .jsonPath("$.ratePlans..rooms..price.tripSubtotal").exists()
                .jsonPath("$.ratePlans[0].rooms[0].price.tripSubtotal").isNumber()
                .jsonPath("$.ratePlans..rooms..price.isDiscounted").exists()
                .jsonPath("$.ratePlans[0].rooms[0].price.isDiscounted").isBoolean()
                .jsonPath("$.ratePlans..rooms..price.itemized..discount").exists()
                .jsonPath("$.ratePlans[0].rooms[0].price.itemized[0].discount").isNumber()
                .jsonPath("$.ratePlans..rooms..price.itemized..isDiscounted").exists()
                .jsonPath("$.ratePlans[0].rooms[0].price.itemized[0].isDiscounted").isBoolean();
    }
}
