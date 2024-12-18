package com.mgm.services.booking.room.it;

import static org.hamcrest.Matchers.greaterThan;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.DateUtil;

public class RoomRatePlansIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/room/rate-plans";

    @Test
    public void test_roomRatePlans_getRoomRatePlansWithNoHeaders_validateHeaderMissingError() {
        validateGetRequestNoHeaderTest(baseServiceUrl + "?checkInDate=" + getCheckInDate() + "&checkOutDate="
                + getCheckOutDate() + "&propertyId=" + defaultTestData.getPropertyId() + "&numGuests="
                + defaultTestData.getNumAdults());
    }

    @Test
    public void test_roomRatePlans_getRoomRatePlansWithNoDates_validateInvalidDatesError() {
        validateMissingParametersErrorDetails(
                baseServiceUrl + "?propertyId=" + defaultTestData.getPropertyId() + "&numGuests="
                        + defaultTestData.getNumAdults(),
                ErrorCode.INVALID_DATES.getErrorCode(), ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_roomRatePlans_getRoomRatePlansWithInvalidDates_validateInvalidDatesError() {
        validateGetRequestErrorDetails(
                baseServiceUrl + "?checkInDate=" + getPastDate() + "&checkOutDate=" + getCheckOutDate() + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults(),
                ErrorCode.INVALID_DATES.getErrorCode(), ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_roomRatePlans_getRoomRatePlansWithNoProperty_validateInvalidPropertyError() {
        validateMissingParametersErrorDetails(
                baseServiceUrl + "?checkInDate=" + getCheckInDate() + "&checkOutDate=" + getCheckOutDate()
                        + "&numGuests=" + defaultTestData.getNumAdults(),
                ErrorCode.INVALID_PROPERTY.getErrorCode(), ErrorCode.INVALID_PROPERTY.getDescription());
    }

    @Test
    public void test_roomRatePlans_getRoomRatePlans_validateRoomRatePlansResponse() {

        // call the service method.
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?checkInDate=" + getCheckInDate() + "&checkOutDate=" + getCheckOutDate() + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers, true);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.[0].startingPrice").exists().jsonPath("$.[0].startingPrice.resortFee").isNumber()
                    .jsonPath("$.[0].startingPrice.baseAveragePrice").isNumber()
                    .jsonPath("$.[0].startingPrice.discountedAveragePrice").isNumber()
                    .jsonPath("$.[0].startingPrice.baseSubtotal").isNumber()
                    .jsonPath("$.[0].startingPrice.discountedSubtotal").isNumber()
                    .jsonPath("$.[0].rooms.*", greaterThan(1));
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_roomRatePlans_getRoomRatePlansWithDateOutOfRange_validateErrorResponse() {

        LocalDate startDateTime = LocalDate.now().minusDays(60);
        LocalDate endDateTime = LocalDate.now().plusDays(60);
        String startDate = format.format(DateUtil.toDate(startDateTime));
        String endDate = format.format(DateUtil.toDate(endDateTime));

        // call the service method.
        client.get()
                .uri(baseServiceUrl + "?checkInDate=" + startDate + "&checkOutDate=" + endDate + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().is4xxClientError();

    }
}
