package com.mgm.services.booking.room.it;

import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.exception.TestExecutionException;

public class GetPerpetualOffersIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/offers/room/default-perpetual";

    @Test
    public void test_perpetualOffers_getPerpetualOffersWithNoHeaders_validateHeaderMissingError() {
        validateGetRequestNoHeaderTest(baseServiceUrl);
    }

    @Test
    public void test_perpetualOffers_getPerpetualOffersWithMlifeNumber_validatePerpetualOfferResponse() {
        createTokenWithCustomerDetails(defaultTestData.getPerpetualMlifeNumber(), -1);
        BodyContentSpec body = client.get().uri(baseServiceUrl).headers(headers -> {
            addAllHeaders(headers);
        }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[0].id").exists().jsonPath("$.[0].propertyId").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_perpetualOffers_getPerpetualOffersWithCustomerId_validatePerpetualOfferResponse() {
        createTokenWithCustomerDetails(null, Long.parseLong(defaultTestData.getPerpetualCustomerId()));
        BodyContentSpec body = client.get().uri(baseServiceUrl).headers(headers -> {
            addAllHeaders(headers);
        }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[0].id").exists().jsonPath("$.[0].propertyId").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }
}
