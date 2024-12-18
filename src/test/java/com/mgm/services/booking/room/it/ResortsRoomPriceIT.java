package com.mgm.services.booking.room.it;

import static org.hamcrest.Matchers.equalTo;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.common.exception.ErrorCode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResortsRoomPriceIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/resorts/room-price";

    @Test
    public void test_resortsRoomPrice_getResortsRoomPriceWithNoHeaders_validateHeaderMissingError() {
        validateGetRequestNoHeaderTest(
                baseServiceUrl + "?numGuests=2&checkInDate=" + getPastDate() + "&checkOutDate=" + getCheckOutDate());
    }

    @Test
    public void test_resortsRoomPrice_getResortsRoomPriceWithNoDates_validateInvalidDatesError() {
        validateMissingParametersErrorDetails(baseServiceUrl, ErrorCode.INVALID_DATES.getErrorCode(),
                ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_resortsRoomPrice_getResortsRoomPriceWithInvalidDates_validateInvalidDatesError() {
        validateGetRequestErrorDetails(
                baseServiceUrl + "?numGuests=2&checkInDate=" + getPastDate() + "&checkOutDate=" + getCheckOutDate(),
                ErrorCode.INVALID_DATES.getErrorCode(), ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_resortsRoomPrice_getResortsRoomPrice_validateResortsRoomPriceResponse() {

        BodyContentSpec body = client.get().uri(
                baseServiceUrl + "?numGuests=2&checkInDate=" + getCheckInDate() + "&checkOutDate=" + getCheckOutDate())
                .headers(headers -> {
                    addAllHeaders(headers, true);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].price").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].resortFee").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].isComp").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    @Test
    public void test_resortsRoomPrice_getResortsRoomPriceWithTransientFiltering_validateResortsRoomPriceResponse() {

        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?numGuests=2&checkInDate=" + getCheckInDate() + "&checkOutDate="
                        + getCheckOutDate()
                        + "&propertyIds=66964e2b-2550-4476-84c3-1a4c0c5c067f,44e610ab-c209-4232-8bb4-51f7b9b13a75")
                .headers(headers -> {
                    addAllHeaders(headers, true);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[0].status").exists().jsonPath("$.[0].propertyId").exists()
                    .jsonPath("$.[0].resortFee").isNumber().jsonPath("$.[0].isComp").isBoolean()
                    .jsonPath("$.*", equalTo(2));
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    @Test
    public void test_resortsRoomPrice_getResortsRoomPriceWithMlifeNumber_validateResortsRoomPriceResponse() {

        createTokenWithCustomerDetails(defaultTestData.getMlifeNumber(), -1);

        BodyContentSpec body = client.get().uri(
                baseServiceUrl + "?numGuests=2&checkInDate=" + getCheckInDate() + "&checkOutDate=" + getCheckOutDate())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].price").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].resortFee").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].isComp").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_resortsRoomPrice_getResortsRoomPriceWithCustomerId_validateResortsRoomPriceResponse() {
        createTokenWithCustomerDetails(null, Long.parseLong(defaultTestData.getCustomerId()));
        BodyContentSpec body = client.get().uri(
                baseServiceUrl + "?numGuests=2&checkInDate=" + getCheckInDate() + "&checkOutDate=" + getCheckOutDate())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].price").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].resortFee").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].isComp").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }
}
