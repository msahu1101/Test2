package com.mgm.services.booking.room.it;

import java.time.LocalDate;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.ProductionSanityTests;
import com.mgm.services.common.util.DateUtil;

public class SanityIT extends BaseRoomBookingIntegrationTest {

    @Test
    @Category(ProductionSanityTests.class)
    public void test_sanityTestCases_getCalendarPriceTransientUser_validateCalendarPriceResponse() {
        client.get()
                .uri("/v1/room/calendar/price" + "?startDate=" + getCheckInDate() + "&endDate=" + getCheckOutDate()
                        + "&propertyId=" + defaultTestData.getPropertyId() + "&numGuests="
                        + defaultTestData.getNumAdults())
                .headers(httpHeaders -> addAllHeaders(httpHeaders, true)).exchange().expectStatus().isOk().expectBody()
                .jsonPath("$").isArray();
    }

    @Test
    @Category(ProductionSanityTests.class)
    public void test_sanityTestCases_getRoomOffersWithTransientUser_validateRoomOffersResponse() {

        client.get().uri("/v1/offers/room").headers(headers -> {
            addAllHeaders(headers, true);
        }).exchange().expectStatus().isOk().expectBody().jsonPath("$").isArray();
    }

    @Test
    @Category(ProductionSanityTests.class)
    public void test_sanityTestCases_getRoomOffersWithTransientUserAndFilters_validateRoomOffersResponse() {

        client.get().uri("/v1/offers/room" + "?propertyIds=" + defaultTestData.getPropertyId()).headers(headers -> {
            addAllHeaders(headers, true);
        }).exchange().expectStatus().isOk().expectBody().jsonPath("$").isArray();
    }

    @Test
    @Category(ProductionSanityTests.class)
    public void test_sanityTestCases_getResortsRoomPriceWithTransientUserAndFilters_validateResortsRoomPriceResponse() {

        client.get()
                .uri("/v1/resorts/room-price" + "?numGuests=2&checkInDate=" + getCheckInDate() + "&checkOutDate="
                        + getCheckOutDate()
                        + "&propertyIds=66964e2b-2550-4476-84c3-1a4c0c5c067f,44e610ab-c209-4232-8bb4-51f7b9b13a75")
                .headers(headers -> {
                    addAllHeaders(headers, true);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$").isArray();

    }

    @Test
    @Category(ProductionSanityTests.class)
    public void test_sanityTestCases_getResortsRoomPriceWithTransientUser_validateResortsRoomPriceResponse() {

        client.get().uri(
                "/v1/resorts/room-price" + "?numGuests=2&checkInDate=" + getCheckInDate() + "&checkOutDate=" + getCheckOutDate())
                .headers(headers -> {
                    addAllHeaders(headers, true);
                }).exchange().expectStatus().isOk().expectBody().jsonPath("$").isArray();

    }
    
    @Test
    @Category(ProductionSanityTests.class)
    public void test_sanityTestCases_getAvailability_validateAvailabilityResponse() {

        LocalDate startDateTime = LocalDate.now().plusDays(45);
        LocalDate endDateTime = LocalDate.now().plusDays(60);
        String startDate = format.format(DateUtil.toDate(startDateTime));
        String endDate = format.format(DateUtil.toDate(endDateTime));

        // call the service method.
        client.get()
                .uri("/v1/room/availability" + "?checkInDate=" + startDate + "&checkOutDate=" + endDate + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers, true);
                }).exchange().expectStatus().isOk().expectBody();
    }

    @Test
    @Category(ProductionSanityTests.class)
    public void test_sanityTestCases_getRatePlans_validateRatePlansResponse() {

        LocalDate startDateTime = LocalDate.now().plusDays(5);
        LocalDate endDateTime = LocalDate.now().plusDays(7);
        String startDate = format.format(DateUtil.toDate(startDateTime));
        String endDate = format.format(DateUtil.toDate(endDateTime));

        // call the service method.
        client.get()
                .uri("/v1/room/rate-plans" + "?checkInDate=" + startDate + "&checkOutDate=" + endDate + "&propertyId="
                        + defaultTestData.getPropertyId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers, true);
                }).exchange().expectStatus().isOk().expectBody();
    }
}
