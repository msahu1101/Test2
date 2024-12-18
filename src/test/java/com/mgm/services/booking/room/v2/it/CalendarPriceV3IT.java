package com.mgm.services.booking.room.v2.it;

import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mgm.services.booking.room.BaseRoomBookingV2IntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ApiDetails;

/**
 * This class contains all the integration test cases for availability calendar
 * service v3 (returns availability based on length of stay).
 *
 * @author laknaray
 *
 */
public class CalendarPriceV3IT extends BaseRoomBookingV2IntegrationTest {

    @Override
    public ApiDetails getApiDetails() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("startDate", getFutureDate(3));
        queryParams.add("endDate", getFutureDate(15));
        queryParams.add("propertyId", defaultTestData.getPropertyId());
        queryParams.add("numAdults", String.valueOf(defaultTestData.getNumAdults()));
        queryParams.add("totalNights", String.valueOf(defaultTestData.getTotalNights()));
        return new ApiDetails(ApiDetails.Method.GET, "/v3/availability/calendar", queryParams, null);
    }

    /**
     * Call to calendar v3 service (returns availability based on length of stay)
     * with numChildren and numRooms and expecting availability.
     */
    @Test
    public void calendarV3Availability_givenNumChildrenNumRooms_returnsAvailability() {
        MultiValueMap<String, String> params = getApiDetails().getDefaultQueryParams();
        params.add("numChildren", String.valueOf(defaultTestData.getNumChildren()));
        params.add("numRooms", String.valueOf(defaultTestData.getNumRooms()));

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(params).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            doBasicValidation(body);
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    /**
     * Call to calendar v3 service (returns availability based on length of stay)
     * with partial programId, expecting offer dates as AVAILABLE dates without any
     * dates as OFFER or SOLDOUT dates.
     */
    @Test
    public void calendarV3Availability_givenProgramId_returnsOfferAndNonOfferDates() {
        MultiValueMap<String, String> params = getApiDetails().getDefaultQueryParams();
        params.add("programId", defaultTestData.getPartialProgramId());

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(params).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            doBasicValidation(body);
            body.jsonPath("$.[?(@.pricingMode == 'PROGRAM')]").exists()
                .jsonPath("$.[?(@.status == 'AVAILABLE')]").exists();
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    /**
     * Call to calendar v3 service (returns availability based on length of stay)
     * with ignoreChannelMargins as true and expecting availability.
     */
    @Test
    public void calendarV3Availability_givenIgnoreChannelMargins_returnsAvailability() {
        MultiValueMap<String, String> params = getApiDetails().getDefaultQueryParams();
        params.add("ignoreChannelMargins", "true");

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(params).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            doBasicValidation(body);
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    /**
     * Call to calendar v3 service (returns availability based on length of stay)
     * with operaConfirmationNumber and expecting availability.
     */
    @Test
    public void calendarV3Availability_givenOperaConfirmationNumber_returnsAvailability() {
        MultiValueMap<String, String> params = getApiDetails().getDefaultQueryParams();
        params.add("operaConfirmationNumber", defaultTestData.getOperaConfirmationNumber());

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(params).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            doBasicValidation(body);
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    /**
     * Call to calendar v3 service (returns availability based on length of stay)
     * with customerDominantPlay as 'Slot' and expecting availability.
     */
    @Test
    public void calendarV3Availability_givenCustomerDominantPlay_returnsAvailability() {
        MultiValueMap<String, String> params = getApiDetails().getDefaultQueryParams();
        params.add("customerDominantPlay", defaultTestData.getCustomerDominantPlay());

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(params).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            doBasicValidation(body);
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    /**
     * Call to calendar v3 service (returns availability based on length of stay)
     * with customerRank as 1 and expecting availability.
     */
    @Test
    public void calendarV3Availability_givenCustomerRank_returnsAvailability() {
        MultiValueMap<String, String> params = getApiDetails().getDefaultQueryParams();
        params.add("customerRank", "1");

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(params).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            doBasicValidation(body);
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    /**
     * Call to calendar v3 service (returns availability based on length of stay)
     * with customerId and perpetualPricing true and expecting availability.
     */
    @Test
    public void calendarV3Availability_givenCustomerIdAndPerpetualPricingTrue_returnsAvailability() {
        MultiValueMap<String, String> params = getApiDetails().getDefaultQueryParams();
        params.add(TestConstant.CUSTOMER_ID, defaultTestData.getPerpetualCustomerId());
        params.add(TestConstant.PERPETUAL_PRICING, TestConstant.TRUE_STRING);

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(params).build())
                .headers(headers -> addAllHeaders(headers, TestConstant.ICE, TestConstant.ICE,
                        TestConstant.DUMMY_TRANSACTION_ID, null))
                .exchange().expectStatus().isOk().expectBody();
        try {
            doBasicValidation(body);
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }
    }

    /**
     * Call to calendar v3 service (returns availability based on length of stay)
     * with a logged in guest token who is eligible for perpetualPricing and
     * expecting availability.
     */
    @Test
    public void calendarV3Availability_givenLoggedInGuestTokenPO_returnsAvailability() {
        MultiValueMap<String, String> params = getApiDetails().getDefaultQueryParams();

        BodyContentSpec body = client.get()
                .uri(builder -> builder.path(getApiDetails().getBaseServiceUrl()).queryParams(params).build())
                .headers(headers -> {
                    addAllHeaders(headers, TestConstant.MGM_RESORTS, TestConstant.WEB,
                            TestConstant.DUMMY_TRANSACTION_ID);
                    addAdditionalHeader(headers, ServiceConstant.HEADER_AUTHORIZATION,
                            getAuthorizationHeaderForGuest(defaultTestData.getPerpetualEmailId(),
                                    defaultTestData.getPerpetualEmailPass(), getAllScopes()));
                }).exchange().expectStatus().isOk().expectBody();
        try {
            doBasicValidation(body);
        } catch (Throwable e) {
            throw new TestExecutionException(String.format(TestConstant.JSON_VALIDATION_ERROR_MESSAGE, e.getMessage(),
                    new String(body.returnResult().getResponseBodyContent())), e);
        }

    }

    private void doBasicValidation(BodyContentSpec body) {
        body.jsonPath("$").isArray()
                .jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                .jsonPath("$.[?(@.status == 'AVAILABLE')].date").exists()
                .jsonPath("$.[?(@.status == 'AVAILABLE')].roomTypeId").exists()
                .jsonPath("$.[?(@.status == 'AVAILABLE')].tripDetails").isArray()
                .jsonPath("$.[?(@.status == 'AVAILABLE' && @.tripDetails.length() != 2)]").isEmpty()
                .jsonPath("$.[?(@.status == 'AVAILABLE')].isPOApplicable").exists()
                .jsonPath("$.[?(@.status == 'AVAILABLE')].totalCompNights").exists()
                .jsonPath("$.[?(@.status == 'AVAILABLE')].totalNightlyTripPrice").exists()
                .jsonPath("$.[?(@.status == 'AVAILABLE')].averageNightlyTripPrice").exists();
    }
}
