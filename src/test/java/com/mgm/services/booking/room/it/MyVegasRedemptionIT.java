package com.mgm.services.booking.room.it;

import static org.hamcrest.Matchers.greaterThan;

import java.time.LocalDate;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.exception.TestExecutionException;
import com.mgm.services.booking.room.model.ProgramEligibility;
import com.mgm.services.booking.room.model.ValidAvailabilityData;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.DateUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MyVegasRedemptionIT extends BaseRoomBookingIntegrationTest {

    private final static String baseValidateServiceUrl = "/v1/myvegas/<CODE>/validate";
    private final String baseConfirmServiceUrl = "/v1/myvegas/<CODE>/confirm";
    private final String invalidRedemptionCode = "ABCDE";
    private final String addToCartServiceUrl = "/v1/cart/room";
    private final String calendarServiceUrl = "/v1/room/calendar/price";
    private final String resortsServiceUrl = "/v1/resorts/room-price";
    private final String roomsServiceUrl = "/v1/room/availability";
    private final String ratesServiceUrl = "/v1/room/rate-plans";

    public void setupCoreRedemptionCode() {
        client.get().uri(baseValidateServiceUrl.replaceAll("<CODE>", defaultTestData.getMyVegasRedemptionCode()))
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange();
    }

    public void setupCoreRotatingRedemptionCode() {
        client.get().uri(baseValidateServiceUrl.replaceAll("<CODE>", getRotatingMyVegasCode())).headers(headers -> {
            addAllHeaders(headers);
        }).exchange();
    }

    
    @Test
    public void test_myVegasRedemption_validateRedemptionCodeWithoutCustomerId_validateResponse() {
        validateGetRequestAttributesExists(baseValidateServiceUrl.replaceAll("<CODE>", getRotatingMyVegasCode()), "$",
                "status", "programId", "rewardType", "propertyId");
    }

    
    @Test
    public void test_myVegasRedemption_validateRedemptionCodeWithMlifeNo_validateResponse() {
        createTokenWithCustomerDetails(defaultTestData.getMyVegasMlifeNumber(), -1);
        validateGetRequestAttributesExists(baseValidateServiceUrl.replaceAll("<CODE>", getRotatingMyVegasCode()), "$",
                "status", "programId", "rewardType", "propertyId");
    }

    
    @Test
    public void test_myVegasRedemption_validateRedemptionCodeWithCustomerId_validateResponse() {
        createTokenWithCustomerDetails(null, Long.parseLong(defaultTestData.getMyVegasCustomerId()));
        validateGetRequestAttributesExists((baseValidateServiceUrl).replaceAll("<CODE>", getRotatingMyVegasCode()), "$",
                "status", "programId", "rewardType", "propertyId");
    }

    @Test
    public void test_myVegasRedemption_validateRedemptionCodeWithWrongMlifeNo_validateOfferNotEligibleError() {
        createTokenWithCustomerDetails(defaultTestData.getPerpetualMlifeNumber(), -1);
        validateGetRequestErrorDetails((baseValidateServiceUrl).replaceAll("<CODE>", getRotatingMyVegasCode()),
                ErrorCode.OFFER_NOT_ELIGIBLE.getErrorCode(), ErrorCode.OFFER_NOT_ELIGIBLE.getDescription());
    }

    @Test
    public void test_myVegasRedemption_validateRedemptionCodeWithWrongCustomerId_validateOfferNotEligibleError() {
        createTokenWithCustomerDetails(null, Long.parseLong(defaultTestData.getPerpetualCustomerId()));
        validateGetRequestErrorDetails((baseValidateServiceUrl).replaceAll("<CODE>", getRotatingMyVegasCode()),
                ErrorCode.OFFER_NOT_ELIGIBLE.getErrorCode(), ErrorCode.OFFER_NOT_ELIGIBLE.getDescription());
    }

    
    @Test
    public void test_myVegasRedemption_validateRedemptionCodeEmptyCode_validateNoRedumptionCodeError() {
        validateGetRequestErrorDetails(baseValidateServiceUrl.replaceAll("<CODE>", ServiceConstant.WHITESPACE_STRING),
                ErrorCode.NO_REDEMPTION_CODE.getErrorCode(), ErrorCode.NO_REDEMPTION_CODE.getDescription());
    }

    @Test
    public void test_myVegasRedemption_validateInvalidRedemptionCode_validateUnknownRedumptionCodeError() {
        validateGetRequestErrorDetails(baseValidateServiceUrl.replaceAll("<CODE>", invalidRedemptionCode),
                ErrorCode.MYVEGAS_UNKNOWN_REDEMPTION_CODE.getErrorCode(),
                ErrorCode.MYVEGAS_UNKNOWN_REDEMPTION_CODE.getDescription());
    }

    @Test
    public void test_myVegasRedemption_validateEmptyRedemptionCode_validateEmptyRedumptionCodeError() {
        BodyContentSpec body = client.post().uri(baseConfirmServiceUrl.replaceAll("<CODE>", ServiceConstant.WHITESPACE_STRING))
                .headers(httpHeaders -> addAllHeaders(httpHeaders))
                .header(ServiceConstant.HEADER_SKIP_MYVEGAS_CONFIRMATION, "false").exchange().expectStatus()
                .isBadRequest().expectBody();
        try {
            body.jsonPath("$.code").isEqualTo(ErrorCode.NO_REDEMPTION_CODE.getErrorCode()).jsonPath("$.msg")
                    .isEqualTo(ErrorCode.NO_REDEMPTION_CODE.getDescription());
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_myVegasRedemption_ineligibleMyvegasCalendarPrice_validateIneligibleRedumptionCodeError() {
        createTokenWithCustomerDetails(defaultTestData.getPerpetualMlifeNumber(), -1);
        validateGetRequestErrorDetails(
                calendarServiceUrl + "?startDate=" + getCheckInDate() + "&endDate=" + getCheckOutDate() + "&propertyId="
                        + defaultTestData.getPropertyId() + "&programId=" + defaultTestData.getMyVegasProgramId()
                        + "&numGuests=" + defaultTestData.getNumAdults(),
                ErrorCode.OFFER_NOT_ELIGIBLE.getErrorCode(), ErrorCode.OFFER_NOT_ELIGIBLE.getDescription());

    }

    @Test
    public void test_myVegasRedemption_ineligibleMyvegasAddRoom_validateIneligibleRedumptionCodeError() {
        ProgramEligibility request = new ProgramEligibility();
        request.setPropertyId(defaultTestData.getPropertyId());
        ValidAvailabilityData data =  getAvailabilityTestData(request, false, false);
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);

        preReserveRequest.setProgramId(defaultTestData.getMyVegasProgramId());
        createTokenWithCustomerDetails(defaultTestData.getPerpetualMlifeNumber(), -1);

        validatePostRequestErrorDetails(addToCartServiceUrl, preReserveRequest,
                ErrorCode.OFFER_NOT_ELIGIBLE.getErrorCode(), ErrorCode.OFFER_NOT_ELIGIBLE.getDescription());

    }

    @Test
    public void test_myVegasRedemption_ineligibleMyvegasResortsPrice_validateIneligibleOffereError() {
        createTokenWithCustomerDetails(defaultTestData.getPerpetualMlifeNumber(), -1);
        validateMissingParametersErrorDetails(
                resortsServiceUrl + "?checkInDate=" + getCheckInDate() + "&checkOutDate=" + getCheckOutDate()
                        + "&programId=" + defaultTestData.getMyVegasProgramId(),
                ErrorCode.OFFER_NOT_ELIGIBLE.getErrorCode(), ErrorCode.OFFER_NOT_ELIGIBLE.getDescription());
    }

    @Test
    public void test_myVegasRedemption_ineligibleMyvegasAvailability_validateIneligibleOffereError() {

        LocalDate startDateTime = LocalDate.now().plusDays(45);
        LocalDate endDateTime = LocalDate.now().plusDays(60);
        String startDate = format.format(DateUtil.toDate(startDateTime));
        String endDate = format.format(DateUtil.toDate(endDateTime));
        createTokenWithCustomerDetails(defaultTestData.getPerpetualMlifeNumber(), -1);

        validateGetRequestErrorDetails(
                roomsServiceUrl + "?checkInDate=" + startDate + "&checkOutDate=" + endDate + "&propertyId="
                        + defaultTestData.getPropertyId() + "&programId=" + defaultTestData.getMyVegasProgramId()
                        + "&numGuests=" + defaultTestData.getNumAdults(),
                ErrorCode.OFFER_NOT_ELIGIBLE.getErrorCode(), ErrorCode.OFFER_NOT_ELIGIBLE.getDescription());
    }

    @Test
    public void test_myVegasRedemption_ineligibleMyvegasRatePlans_validateIneligibleOffereError() {

        LocalDate startDateTime = LocalDate.now().plusDays(45);
        LocalDate endDateTime = LocalDate.now().plusDays(60);
        String startDate = format.format(DateUtil.toDate(startDateTime));
        String endDate = format.format(DateUtil.toDate(endDateTime));
        createTokenWithCustomerDetails(defaultTestData.getPerpetualMlifeNumber(), -1);

        validateGetRequestErrorDetails(
                ratesServiceUrl + "?checkInDate=" + startDate + "&checkOutDate=" + endDate + "&propertyId="
                        + defaultTestData.getPropertyId() + "&programId=" + defaultTestData.getMyVegasProgramId()
                        + "&numGuests=" + defaultTestData.getNumAdults(),
                ErrorCode.OFFER_NOT_ELIGIBLE.getErrorCode(), ErrorCode.OFFER_NOT_ELIGIBLE.getDescription());
    }

    @Test
    public void test_myVegasRedemption_validMyvegasAddRoom_validateResponse() {
        ProgramEligibility request = new ProgramEligibility();
        request.setPropertyId(defaultTestData.getPropertyId());
        ValidAvailabilityData data =  getAvailabilityTestData(request, false, false);
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);

        // myvegas program
        preReserveRequest.setProgramId(defaultTestData.getMyVegasProgramId());
        createTokenWithCustomerDetails(defaultTestData.getMyVegasMlifeNumber(), -1);
        setupCoreRedemptionCode();

        validatePostRequestAttributesExists(addToCartServiceUrl, preReserveRequest, "$", "itemId", "tripDetails",
                "rates");
    }

    @Test
    public void test_myVegasRedemption_validMyvegasCalendarPrice_validateResponse() {
        createTokenWithCustomerDetails(defaultTestData.getMyVegasMlifeNumber(), -1);
        setupCoreRotatingRedemptionCode();

        BodyContentSpec body = client.get()
                .uri(calendarServiceUrl + "?startDate=" + getCheckInDate() + "&endDate=" + getCheckOutDate()
                        + "&propertyId=" + defaultTestData.getPropertyId() + "&programId="
                        + defaultTestData.getMyVegasProgramId() + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(httpHeaders -> addAllHeaders(httpHeaders)).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_myVegasRedemption_validMyvegasResortsPrice_validateResponse() {
        createTokenWithCustomerDetails(defaultTestData.getMyVegasMlifeNumber(), -1);
        setupCoreRotatingRedemptionCode();

        BodyContentSpec body = client.get().uri(resortsServiceUrl + "?numGuests=2&checkInDate=" + getCheckInDate()
                + "&checkOutDate=" + getCheckOutDate() + "&programId=" + defaultTestData.getMyVegasProgramId())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$").isArray().jsonPath("$.[?(@.status == 'AVAILABLE')]").isArray()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].propertyId").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].status").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].price").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].resortFee").exists()
                    .jsonPath("$.[?(@.status == 'AVAILABLE')].isComp").exists();
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    
    @Test
    public void test_myVegasRedemption_validMyvegasAvailability_validateResponse() {
        LocalDate startDateTime = LocalDate.now().plusDays(45);
        LocalDate endDateTime = LocalDate.now().plusDays(60);
        String startDate = format.format(DateUtil.toDate(startDateTime));
        String endDate = format.format(DateUtil.toDate(endDateTime));
        createTokenWithCustomerDetails(defaultTestData.getMyVegasMlifeNumber(), -1);
        setupCoreRotatingRedemptionCode();
        // call the service method.
        BodyContentSpec body = client.get()
                .uri(roomsServiceUrl + "?checkInDate=" + startDate + "&checkOutDate=" + endDate + "&propertyId="
                        + defaultTestData.getPropertyId() + "&programId=" + defaultTestData.getMyVegasProgramId()
                        + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.*", greaterThan(1));
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    @Test
    public void test_myVegasRedemption_validMyvegasRatePlans_validateResponse() {
        LocalDate startDateTime = LocalDate.now().plusDays(45);
        LocalDate endDateTime = LocalDate.now().plusDays(60);
        String startDate = format.format(DateUtil.toDate(startDateTime));
        String endDate = format.format(DateUtil.toDate(endDateTime));

        createTokenWithCustomerDetails(defaultTestData.getMyVegasMlifeNumber(), -1);
        setupCoreRotatingRedemptionCode();
        // call the service method.
        BodyContentSpec body = client.get()
                .uri(ratesServiceUrl + "?checkInDate=" + startDate + "&checkOutDate=" + endDate + "&propertyId="
                        + defaultTestData.getPropertyId() + "&programId=" + defaultTestData.getMyVegasProgramId()
                        + "&numGuests=" + defaultTestData.getNumAdults())
                .headers(headers -> {
                    addAllHeaders(headers);
                }).exchange().expectStatus().isOk().expectBody();
        try {
            body.jsonPath("$.[0].rooms.*", greaterThan(1));
        } catch (Throwable e) {
            throw new TestExecutionException("Json validation error occurred. Error message: " + e.getMessage()
                    + ", json response: " + new String(body.returnResult().getResponseBodyContent()), e);
        }
    }

    // @Test
    public void confirmRedemptionCodeTest() {
        client.post().uri(baseConfirmServiceUrl.replaceAll("<CODE>", defaultTestData.getMyVegasRedemptionCode()))
                .headers(httpHeaders -> addAllHeaders(httpHeaders)).exchange().expectStatus().isNoContent();
    }
}
