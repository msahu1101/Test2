package com.mgm.services.booking.room.it;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.BeforeClass;
import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.ProgramEligibility;
import com.mgm.services.booking.room.model.ValidAvailabilityData;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class AddRoomToCartIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = TestConstant.URL_ADD_TO_CART;
    private static ValidAvailabilityData data;
    
    @BeforeClass
    public static void getAvailableData() {
        ProgramEligibility request = new ProgramEligibility();
        request.setPropertyId(defaultTestData.getPropertyId());
        data = getAvailabilityTestData(request, false, false);
    }

    @Test
    public void test_addRoomToCart_addRoomToCartWithNoHeaders_validateHeaderMissingError() {
        
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        validatePostRequestNoHeaderTest(baseServiceUrl, preReserveRequest);
    }

    @Test
    public void test_addRoomToCart_addRoomWithPatronId_validateResponse() {
        // Try adding room with patron id to the cart and it should be
        // successful
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setProgramId(defaultTestData.getPatronProgramId());

        createTokenWithCustomerDetails(defaultTestData.getPatronMlifeNumber(), -1);
        validatePostRequestAttributesExists(baseServiceUrl, preReserveRequest, "$", "itemId", "tripDetails", "rates");

        // Try adding another room with patron id to the cart and it should
        // throw an exception as only one room
        // with patron if can be added to the cart
        validatePostRequestErrorDetails(baseServiceUrl, preReserveRequest,
                ErrorCode.PROGRAM_ALREADY_IN_CART.getErrorCode(), ErrorCode.PROGRAM_ALREADY_IN_CART.getDescription());

    }

    @Test
    public void test_addRoomToCart_addRoomWithEnableJwbHeaderTrue_validateResponse() {
        // Try adding room with enableJwb header to the cart and it should be
        // successful
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setProgramId(defaultTestData.getPatronProgramId());

        createTokenWithCustomerDetails(defaultTestData.getPatronMlifeNumber(), -1);
        validatePostRequestAttributesExists(baseServiceUrl, preReserveRequest, true, false, "$", "itemId",
                "tripDetails", "rates");
    }

    @Test
    public void test_addRoomToCart_addRoomWithEnableJwbCookieTrue_validateResponse() {
        // Try adding room with enableJwb header to the cart and it should be
        // successful
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setProgramId(defaultTestData.getPatronProgramId());

        createTokenWithCustomerDetails(defaultTestData.getPatronMlifeNumber(), -1);
        validatePostRequestAttributesExists(baseServiceUrl, preReserveRequest, false, true, "$", "itemId",
                "tripDetails", "rates");
    }

    @Test
    public void test_addRoomToCart_addRoomWithEnableJwbCookieFalse_validateResponse() {
        // Try adding room with enableJwb header to the cart and it should be
        // successful
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setProgramId(defaultTestData.getPatronProgramId());

        createTokenWithCustomerDetails(defaultTestData.getPatronMlifeNumber(), -1);
        validatePostRequestAttributesExists(baseServiceUrl, preReserveRequest, false, false, "$", "itemId",
                "tripDetails", "rates");
    }

    @Test
    public void test_addRoomToCart_addRoomWithPreReserveNoDates_validateInvalidDateError() {
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setCheckInDate(null);
        preReserveRequest.setCheckOutDate(null);
        validatePostRequestErrorDetails(baseServiceUrl, preReserveRequest, ErrorCode.INVALID_DATES.getErrorCode(),
                ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_addRoomToCart_addRoomWithPreReserveInvalidDates_validateInvalidDateError() {
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        final LocalDate checkOutLDate = LocalDate.parse(getPastDate(), formatter);
        preReserveRequest.setCheckOutDate(checkOutLDate);
        validatePostRequestErrorDetails(baseServiceUrl, preReserveRequest, ErrorCode.INVALID_DATES.getErrorCode(),
                ErrorCode.INVALID_DATES.getDescription());
    }

    @Test
    public void test_addRoomToCart_addRoomWithPreReserveNoProperty_validateInvalidPropertyError() {
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setPropertyId("");
        validatePostRequestErrorDetails(baseServiceUrl, preReserveRequest, ErrorCode.INVALID_PROPERTY.getErrorCode(),
                ErrorCode.INVALID_PROPERTY.getDescription());

    }

    @Test
    public void test_addRoomToCart_addRoomWithEmptyRoomType_validateInvalidRoomTypeError() {
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setRoomTypeId("");
        validatePostRequestErrorDetails(baseServiceUrl, preReserveRequest, ErrorCode.INVALID_ROOMTYPE.getErrorCode(),
                ErrorCode.INVALID_ROOMTYPE.getDescription());
    }

    @Test
    public void test_addRoomToCart_addRoomToCart_validateSuccessResponse() {
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        validatePostRequestAttributesExists(baseServiceUrl, preReserveRequest, "$", "itemId", "tripDetails", "rates");
    }

    @Test
    public void test_addRoomToCart_addRoomToCartWithAvailabilityAndEligibility_validateSuccessResponse() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramMlifeUser();
        eligibility.setAvailabilityData(getAvailabilityTestData(eligibility, false, false));

        RoomCartRequest preReserveRequest = prereserveRequestBuilder(eligibility.getAvailabilityData());
        preReserveRequest.setPropertyId(eligibility.getPropertyId());
        preReserveRequest.setProgramId(eligibility.getProgramId());
        preReserveRequest.setRoomTypeId(eligibility.getAvailabilityData().getRoomTypeId());

        createTokenWithCustomerDetails(eligibility.getMlifeNumber(), -1);

        validatePostRequestAttributesExists(baseServiceUrl, preReserveRequest, "$", "itemId", "tripDetails", "rates");
    }

    @Test
    public void test_addRoomToCart_addRoomToCartWithPromoCode_validateSuccessResponse() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getCasinoProgramMlifeUser();
        eligibility.setAvailabilityData(getAvailabilityTestData(eligibility, false, false));

        RoomCartRequest preReserveRequest = prereserveRequestBuilder(eligibility.getAvailabilityData());
        preReserveRequest.setPropertyId(eligibility.getPropertyId());
        preReserveRequest.setPromoCode(eligibility.getPromoCode());
        preReserveRequest.setRoomTypeId(eligibility.getAvailabilityData().getRoomTypeId());
        createTokenWithCustomerDetails(eligibility.getMlifeNumber(), -1);

        validatePostRequestAttributesExists(baseServiceUrl, preReserveRequest, "$", "itemId", "tripDetails", "rates");
    }

    @Test
    public void test_addRoomToCart_addRoomToCartWithIneligiblePromoCode_validateIneligiblePromocodeError() {
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setPromoCode(defaultTestData.getPromoCode());

        validatePostRequestErrorDetails(baseServiceUrl, preReserveRequest, ErrorCode.OFFER_NOT_ELIGIBLE.getErrorCode(),
                ErrorCode.OFFER_NOT_ELIGIBLE.getDescription());

    }

    @Test
    public void test_addRoomToCart_addRoomToCartWithIneligibleProgram_validateIneligibleProgramError() {
        ProgramEligibility eligibility = defaultTestData.getProgramEligibility().getPatronProgramNonListedMlifeUser();
        createTokenWithCustomerDetails(eligibility.getMlifeNumber(), -1);
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setPropertyId(eligibility.getPropertyId());
        preReserveRequest.setRoomTypeId(defaultTestData.getRoomTypeId());
        preReserveRequest.setProgramId(eligibility.getProgramId());

        log.info(preReserveRequest);

        validatePostRequestErrorDetails(baseServiceUrl, preReserveRequest, ErrorCode.OFFER_NOT_ELIGIBLE.getErrorCode(),
                ErrorCode.OFFER_NOT_ELIGIBLE.getDescription());

    }

    @Test
    public void test_addRoomToCart_addRoomToCartWithInvalidPromoCode_validateOfferNotFoundError() {
        RoomCartRequest preReserveRequest = prereserveRequestBuilder(data);
        preReserveRequest.setPropertyId(defaultTestData.getPropertyId());
        preReserveRequest.setRoomTypeId(defaultTestData.getRoomTypeId());
        preReserveRequest.setPromoCode("ABCS");

        validatePostRequestErrorDetails(baseServiceUrl, preReserveRequest, ErrorCode.OFFER_NOT_AVAILABLE.getErrorCode(),
                ErrorCode.OFFER_NOT_AVAILABLE.getDescription());

    }

}
