package com.mgm.services.booking.room.it;

import static org.hamcrest.Matchers.greaterThan;

import java.time.LocalDate;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.DateUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RoomAvailabilityIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/room/availability";

    @Test
    public void test_roomAvailability_getRoomAvailabilityWithNoHeaders_validateHeaderMissingError() {
        validateGetRequestNoHeaderTest(baseServiceUrl + "?checkInDate=" + getCheckInDate() + "&checkOutDate="
                + getCheckOutDate() + "&propertyId=" + defaultTestData.getPropertyId() + "&numGuests="
                + defaultTestData.getNumAdults());
    }

    @Test
    public void test_roomAvailability_getRoomAvailabilityWithNoDates_validateInvalidDatesError() {
        validateMissingParametersErrorDetails(
                baseServiceUrl + "?propertyId=" + defaultTestData.getPropertyId() + "&numGuests="
                        + defaultTestData.getNumAdults(),
                ErrorCode.INVALID_DATES.getErrorCode(), ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_roomAvailability_getRoomAvailabilityWithInvalidDates_validateInvalidDatesError() {
        validateGetRequestErrorDetails(
                baseServiceUrl + "?checkInDate=" + getPastDate() + "&checkOutDate=" + getCheckOutDate() + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults(),
                ErrorCode.INVALID_DATES.getErrorCode(), ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_roomAvailability_getRoomAvailabilityWithNoProperty_validateInvalidPropertyError() {
        validateMissingParametersErrorDetails(
                baseServiceUrl + "?checkInDate=" + getCheckInDate() + "&checkOutDate=" + getCheckOutDate()
                        + "&numGuests=" + defaultTestData.getNumAdults(),
                ErrorCode.INVALID_PROPERTY.getErrorCode(), ErrorCode.INVALID_PROPERTY.getDescription());
    }

    @Test
    public void test_roomAvailability_getRoomAvailability_validateRoomAvailabilityResponse() {

        LocalDate startDateTime = LocalDate.now().plusDays(45);
        LocalDate endDateTime = LocalDate.now().plusDays(60);
        String startDate = format.format(DateUtil.toDate(startDateTime));
        String endDate = format.format(DateUtil.toDate(endDateTime));

        // call the service method.
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?checkInDate=" + startDate + "&checkOutDate=" + endDate + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers, true);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.*", greaterThan(1));
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_roomAvailability_getRoomAvailabilityWithDateOutOfRange_validateErrorResponse() {

        LocalDate startDateTime = LocalDate.now().minusDays(60);
        LocalDate endDateTime = LocalDate.now().plusDays(60);
        String startDate = format.format(DateUtil.toDate(startDateTime));
        String endDate = format.format(DateUtil.toDate(endDateTime));
        createTokenWithCustomerDetails(defaultTestData.getMlifeNumber(), -1);

        // call the service method.
        client.get().uri(baseServiceUrl + "?checkInDate=" + startDate + "&checkOutDate=" + endDate + "&propertyId="
                + defaultTestData.getPropertyId()).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().is4xxClientError();

    }

}
