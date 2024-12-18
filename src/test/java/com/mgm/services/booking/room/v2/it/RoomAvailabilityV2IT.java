package com.mgm.services.booking.room.v2.it;

import static org.hamcrest.Matchers.greaterThan;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RoomAvailabilityV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("checkInDate", getFutureDate(5));
        queryParams.add("checkOutDate", getFutureDate(6));
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));
        return new ApiDetails(ApiDetails.Method.GET, "/v2/availability/trip", queryParams, null);
    }

    @Test
    public void availabilityV2_givenValidInputs_returnsAvailability() {

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("checkInDate", getFutureDate(5));
        queryParams.add("checkOutDate", getFutureDate(6));
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));

        // call the service method.
        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.*", greaterThan(1));
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void availabilityV2_givenValidInputs_returnsRatePlans() {

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("checkInDate", getFutureDate(1));
        queryParams.add("checkOutDate", getFutureDate(3));
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));
        queryParams.add("enableMrd", "true");

        // call the service method.
        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans[0].startingPrice").exists().jsonPath("$.ratePlans[0].startingPrice.resortFee")
                    .isNumber().jsonPath("$.ratePlans[0].startingPrice.baseAveragePrice").isNumber()
                    .jsonPath("$.ratePlans[0].startingPrice.discountedAveragePrice").isNumber()
                    .jsonPath("$.ratePlans[0].startingPrice.baseSubtotal").isNumber()
                    .jsonPath("$.ratePlans[0].startingPrice.discountedSubtotal").isNumber()
                    .jsonPath("$.ratePlans[0].rooms.*", greaterThan(1));
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    /**
     * Validating the existence of additional fields added as part of booksvc-36307
     * and booksvc-36311.
     */
    @Test
    public void availabilityV2_givenValidInputs_returnsRatePlansWithAdditionalFields() {

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("checkInDate", getFutureDate(5));
        queryParams.add("checkOutDate", getFutureDate(7));
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));
        queryParams.add("enableMrd", "true");

        // call the service method.
        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.ratePlans..rooms..price.discountsTotal").exists()
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
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void availabilityV2_invalidDates_returnsValidationError() {

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("checkInDate", getFutureDate(-60));
        queryParams.add("checkOutDate", getFutureDate(60));
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));

        // call the service method.
        client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().is4xxClientError();
    }

    @Test
    public void availabilityV2_withDateOutOfRange_returnsValidationError() {

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("checkInDate", getFutureDate(-60));
        queryParams.add("checkOutDate", getFutureDate(60));
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));

        // call the service method.
        client.get().uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().is4xxClientError();

    }
}
