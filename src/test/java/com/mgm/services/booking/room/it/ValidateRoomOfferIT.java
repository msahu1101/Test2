package com.mgm.services.booking.room.it;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ProgramEligibility;
import com.mgm.services.common.exception.ErrorCode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ValidateRoomOfferIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/room/offer/validate";

    @Test
    public void test_validateRoomOffer_validateRoomOfferWithNoHeaders_validateHeaderMissingError() {
        validateGetRequestNoHeaderTest(baseServiceUrl + "?promoCode=ABC&propertyId=" + defaultTestData.getPropertyId());
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferWithNoParameters_validateNoParametersError() {
        validateGetRequestErrorDetails(baseServiceUrl, ErrorCode.NO_PROGRAM.getErrorCode(),
                ErrorCode.NO_PROGRAM.getDescription());
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferWithNoProperty_validateNoPropertyError() {
        validateGetRequestErrorDetails(baseServiceUrl + "?promoCode=BOGO", ErrorCode.NO_PROPERTY.getErrorCode(),
                ErrorCode.NO_PROPERTY.getDescription());
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsGuestWithInvalidProgramId_validateOfferNotEligible() {
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?programId=ABC").headers(headers -> {
            addAllHeaders(headers);
        }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("false").jsonPath("$.eligible").isEqualTo("false");
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsGuestWithInvalidPromoCode_validateOfferNotEligible() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?promoCode=ABC&propertyId=" + defaultTestData.getTransientPropertyId())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("false").jsonPath("$.eligible").isEqualTo("false");
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsGuestWithIneligibleProgramId_validateOfferNotEligible() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramTransientUser();
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?programId=" + eligibility.getProgramId())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("false")
                    .jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsGuestWithTransientProgramId_validateOfferEligible() {
        BodyContentSpec body = client.get()
                .uri(baseServiceUrl + "?programId=" + defaultTestData.getProgramEligibility().getTransientProgramTransientUser().getProgramId()).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("true").jsonPath("$.programId")
                    .isNotEmpty().jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }

    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsGuestWithValidDateandValidProgramId_validateOfferEligible() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getTransientProgramTransientUser();
        eligibility.setAvailabilityData(getAvailabilityTestData(eligibility, false, false));
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?programId=" + eligibility.getProgramId()
                + "&propertyId=" + eligibility.getPropertyId()).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("true").jsonPath("$.programId")
                    .isNotEmpty().jsonPath("$.segment").isEqualTo("true").jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsGuestWithTransientPromoCode_validateOfferEligible() {
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?promoCode=" + defaultTestData.getTransientPromoCode()
                + "&propertyId=" + defaultTestData.getTransientPropertyId()).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("true").jsonPath("$.programId")
                    .isNotEmpty().jsonPath("$.segment").isEqualTo("true").jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsGuestWithIneligibleCasinoProgram_validateOfferNotEligible() {
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?programId=" + defaultTestData.getCasinoProgramId())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("false")
                    .jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsMlifeCustomer_validateOfferEligible() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramMlifeUser();
        createTokenWithCustomerDetails(eligibility.getMlifeNumber(), -1);
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?programId=" + eligibility.getProgramId()
                + "&propertyId=" + eligibility.getPropertyId()).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("true").jsonPath("$.programId")
                    .isNotEmpty().jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsMlifeCustomerNotEligibleForOffer_validateOfferNotEligible() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoGoldProgramSapphireMlifeUser();
        createTokenWithCustomerDetails(eligibility.getMlifeNumber(), -1);
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?programId=" + eligibility.getProgramId()
                + "&propertyId=" + eligibility.getPropertyId()).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("false").jsonPath("$.programId")
                    .isNotEmpty().jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsMlifeCustomerWithTransientProgram_validateOfferEligible() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getTransientProgramMlifeUser();
        createTokenWithCustomerDetails(eligibility.getMlifeNumber(), -1);
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?programId=" + eligibility.getProgramId()
                + "&propertyId=" + eligibility.getPropertyId()).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("true").jsonPath("$.programId")
                    .isNotEmpty().jsonPath("$.segment").isEqualTo("true").jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferWithCasinoPromoCode_validateOfferEligible() {
        createTokenWithCustomerDetails(defaultTestData.getMlifeNumber(), -1);
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?promoCode=" + defaultTestData.getCasinoPromoCode()
                + "&propertyId=" + defaultTestData.getCasinoPropertyId()).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("true").jsonPath("$.programId")
                    .isNotEmpty().jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    public void test_validateRoomOffer_validateRoomOfferAsCasinoProgramCustomer_validateOfferEligible() {
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?programId=" + defaultTestData.getCasinoProgramId())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("true").jsonPath("$.programId")
                    .isNotEmpty().jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_validateRoomOffer_validateRoomOfferAsCasinoPromoCodeCustomer_validateOfferEligible() {
        createTokenWithCustomerDetails(null, Long.parseLong(defaultTestData.getCustomerId()));
        BodyContentSpec body = client.get().uri(baseServiceUrl + "?promoCode=" + defaultTestData.getCasinoPromoCode()
                + "&propertyId=" + defaultTestData.getCasinoPropertyId()).headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.valid").isEqualTo("true").jsonPath("$.eligible").isEqualTo("true").jsonPath("$.programId")
                    .isNotEmpty().jsonPath("$.propertyId").isNotEmpty();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }
}
