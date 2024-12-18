package com.mgm.services.booking.room.v2.it;

import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;

/**
 * This class contains all the integration test cases for availability calendar
 * service v2
 *
 * @author laknaray
 *
 */
public class CalendarPriceV2IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("startDate", getPastDate());
        queryParams.add("endDate", getCheckOutDate());
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));
        return new ApiDetails(ApiDetails.Method.GET, "/v2/availability/calendar", queryParams, null);
    }

    /**
     * Call to availability calendar service with numChildren and expecting
     * availability
     */
    @Test
    public void calendarV2Availability_givenNumChildren_returnsAvailability() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("startDate", getFutureDate(3));
        queryParams.add("endDate", getFutureDate(15));
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));
        queryParams.add("numChildren=", String.valueOf(defaultTestData.getNumChildren()));

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(queryParams).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].date").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    /**
     * Call to availability calendar service with programId and expecting
     * availability
     */
    @Test
    public void calendarV2Availability_givenProgramId_returnsOfferAndAvailableDates() {
        BodyContentSpec body = client.get()
                .uri(getApiDetails().getBaseServiceUrl() + "?startDate=" + getFutureDate(3) + "&endDate="
                        + getFutureDate(15) + "&propertyId=" + defaultTestData.getPropertyId() + "&programId="
                        + defaultTestData.getPartialProgramId() + "&numAdults="
                        + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
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

    /**
     * Call to availability calendar service with programId and expecting
     * availability without offer dates
     */
    @Test
    public void calendarV2Availability_givenProgramIdAndExcludeNonOffer_returnsOfferAndSoldoutDates() {
        BodyContentSpec body = client.get()
                .uri(getApiDetails().getBaseServiceUrl() + "?startDate=" + getFutureDate(3) + "&endDate="
                        + getFutureDate(15) + "&propertyId=" + defaultTestData.getPropertyId() + "&programId="
                        + defaultTestData.getPartialProgramId() + "&excludeNonOffer=true"
                        + "&numAdults=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
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

    /**
     * Call to availability calendar service without programId and expecting
     * availability without offer dates
     */
    @Test
    public void calendarV2Availability_givenNoProgramId_returnsAvailabilityWithoutOfferDates() {
        BodyContentSpec body = client.get()
                .uri(getApiDetails().getBaseServiceUrl() + "?startDate=" + getFutureDate(3) + "&endDate="
                        + getFutureDate(15) + "&propertyId=" + defaultTestData.getPropertyId() + "&numAdults="
                        + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
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

    /**
     * Call to availability calendar service with numRooms and expecting
     * availability
     */
    @Test
    public void calendarV2Availability_givenNumRooms_returnsAvailability() {
        BodyContentSpec body = client.get()
                .uri(getApiDetails().getBaseServiceUrl() + "?startDate=" + getFutureDate(3) + "&endDate="
                        + getFutureDate(15) + "&propertyId=" + defaultTestData.getPropertyId() + "&numAdults="
                        + defaultTestData.getNumAdults() + "&numRooms=" + defaultTestData.getNumRooms())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].date").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    /**
     * Call to availability calendar service with ignoreChannelMargins as true and
     * expecting availability
     */
    @Test
    public void calendarV2Availability_givenIgnoreChannelMargins_returnsAvailability() {
        BodyContentSpec body = client.get()
                .uri(getApiDetails().getBaseServiceUrl() + "?startDate=" + getFutureDate(3) + "&endDate="
                        + getFutureDate(15) + "&propertyId=" + defaultTestData.getPropertyId() + "&numAdults="
                        + defaultTestData.getNumAdults() + "&ignoreChannelMargins=true")
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].date").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    /**
     * Call to availability calendar service with operaConfirmationNumber and
     * expecting availability
     */
    @Test
    public void calendarV2Availability_givenOperaConfirmationNumber_returnsAvailability() {
        BodyContentSpec body = client.get()
                .uri(getApiDetails().getBaseServiceUrl() + "?startDate=" + getFutureDate(3) + "&endDate="
                        + getFutureDate(15) + "&propertyId=" + defaultTestData.getPropertyId() + "&numAdults="
                        + defaultTestData.getNumAdults() + "&operaConfirmationNumber="
                        + defaultTestData.getOperaConfirmationNumber())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].date").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    /**
     * Call to availability calendar service with customerDominantPlay as 'Slot' and
     * expecting availability
     */
    @Test
    public void calendarV2Availability_givenCustomerDominantPlay_returnsAvailability() {
        BodyContentSpec body = client.get()
                .uri(getApiDetails().getBaseServiceUrl() + "?startDate=" + getFutureDate(3) + "&endDate="
                        + getFutureDate(15) + "&propertyId=" + defaultTestData.getPropertyId() + "&numAdults="
                        + defaultTestData.getNumAdults() + "&customerDominantPlay="
                        + defaultTestData.getCustomerDominantPlay())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].date").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    /**
     * Call to availability calendar service with customerRank as 1 and expecting
     * availability
     */
    @Test
    public void calendarV2Availability_givenCustomerRank_returnsAvailability() {
        BodyContentSpec body = client.get()
                .uri(getApiDetails().getBaseServiceUrl() + "?startDate=" + getFutureDate(3) + "&endDate="
                        + getFutureDate(15) + "&propertyId=" + defaultTestData.getPropertyId() + "&numAdults="
                        + defaultTestData.getNumAdults() + "&customerRank=1")
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE, TestConstant.DUMMY_TRANSACTION_ID, null);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].date").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }
}
