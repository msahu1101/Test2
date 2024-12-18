package com.mgm.services.booking.room.it;

import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.common.exception.ErrorCode;

public class CalendarPriceIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/room/calendar/price";

    @Test
    public void test_calendarPrice_getCalendarPriceWithNoHeaders_validateHeaderMissingError() {
        validateGetRequestNoHeaderTest(baseServiceUrl+ "?startDate=" + getCheckInDate() + "&endDate=" + getCheckOutDate()
        + "&propertyId=" + defaultTestData.getPropertyId() + "&numGuests="
        + defaultTestData.getNumAdults());
    }

    @Test
    public void test_calendarPrice_getCalendarPriceWithNoDates_validateInvalidDatesError() {
        validateMissingParametersErrorDetails(
                baseServiceUrl + "?propertyId=" + defaultTestData.getPropertyId() + "&numGuests="
                        + defaultTestData.getNumAdults(),
                ErrorCode.INVALID_DATES.getErrorCode(), ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_calendarPrice_getCalendarPriceWithNoProperty_validateInvalidPropertyError() {
        validateMissingParametersErrorDetails(
                baseServiceUrl + "?startDate=" + getCheckInDate() + "&endDate=" + getCheckOutDate() + "&numGuests="
                        + defaultTestData.getNumAdults(),
                ErrorCode.INVALID_PROPERTY.getErrorCode(), ErrorCode.INVALID_PROPERTY.getDescription());
    }

    @Test
    public void test_calendarPrice_getCalendarPriceWithInvalidDates_validateInvalidDatesError() {
        validateGetRequestErrorDetails(
                baseServiceUrl + "?startDate=" + getPastDate() + "&endDate=" + getCheckOutDate() + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults(),
                ErrorCode.INVALID_DATES.getErrorCode(), ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_calendarPrice_getCalendarPriceWithProgramIdAndIncludeNonOffer_validateCalenderPriceResponse() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?startDate=" + getFutureDate(1) + "&endDate=" + getFutureDate(10)
                        + "&propertyId=" + defaultTestData.getPropertyId() + "&programId="
                        + defaultTestData.getPartialProgramId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].date").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists().jsonPath("$.[?(@.status == 'OFFER')]")
                    .isArray().jsonPath("$.[?(@.status == 'OFFER')].date").exists()
                    .jsonPath("$.[?(@.status == 'OFFER')].status").exists().jsonPath("$.[?(@.status == 'OFFER')].price")
                    .exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void getCalendarPriceWithProgramIdAndExcludeNonOfferTest() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?startDate=" + getFutureDate(1) + "&endDate=" + getFutureDate(10)
                        + "&propertyId=" + defaultTestData.getPropertyId() + "&programId="
                        + defaultTestData.getPartialProgramId() + "&excludeNonOffer=true&numGuests="
                        + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'OFFER')]").isArray()
                    .jsonPath("$.[?(@.status == 'OFFER')].date").exists().jsonPath("$.[?(@.status == 'OFFER')].status")
                    .exists().jsonPath("$.[?(@.status == 'SOLDOUT')]").isArray()
                    .jsonPath("$.[?(@.status == 'SOLDOUT')].date").exists()
                    .jsonPath("$.[?(@.status == 'SOLDOUT')].status").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].price").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void getCalendarPriceWithNoProgramId() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?startDate=" + getFutureDate(1) + "&endDate=" + getFutureDate(10)
                        + "&propertyId=" + defaultTestData.getPropertyId() + "&numGuests="
                        + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].date").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists()
                    .jsonPath("$.[?(@.status == 'OFFER')].price").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void getCalendarPriceTransientTest() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?startDate=" + getCheckInDate() + "&endDate=" + getCheckOutDate()
                        + "&propertyId=" + defaultTestData.getPropertyId() + "&numGuests="
                        + defaultTestData.getNumAdults())
                .headers(httpHeaders -> addAllHeaders(httpHeaders, true)).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void getCalendarPriceWithNoProgramIdAndjwbCookieTrue() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?startDate=" + getFutureDate(3) + "&endDate=" + getFutureDate(9) + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers);
                    headers.add(ServiceConstant.COOKIE, TestConstant.ENABLE_JWB + "=true");
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[0].memberPrice").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void getCalendarPriceWithNoProgramIdAndjwbCookieFalse() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?startDate=" + getFutureDate(3) + "&endDate=" + getFutureDate(9) + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers);
                    headers.add(ServiceConstant.COOKIE, TestConstant.ENABLE_JWB + "=false");
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void getCalendarPriceWithNoProgramIdAndjwbHeaderTrue() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?startDate=" + getFutureDate(3) + "&endDate=" + getFutureDate(9) + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers, false, true);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[0].memberPrice").doesNotExist();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void getCalendarPriceWithNoProgramIdAndjwbHeaderFalse() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?startDate=" + getFutureDate(3) + "&endDate=" + getFutureDate(9) + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers, false, false);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

}
