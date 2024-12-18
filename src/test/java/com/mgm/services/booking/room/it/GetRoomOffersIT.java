package com.mgm.services.booking.room.it;

import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingIntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.exception.ErrorCode;

public class GetRoomOffersIT extends BaseRoomBookingIntegrationTest {

    private final String baseServiceUrl = "/v1/offers/room";
    private final String baseServiceSegmentUrl = "/v1/offers/room/segment/<programId>";

    @Test
    public void test_getRoomOffers_getRoomOffersWithNoHeaders_validateHeaderMissingError() {
        validateGetRequestNoHeaderTest(baseServiceUrl);
    }

    @Test
    public void test_getRoomOffers_getRoomOffersWithMlifeNumber_validateRoomOfferResponse() {
        createTokenWithCustomerDetails(defaultTestData.getMlifeNumber(), -1);
        validateGetRequestIdAndTypeExists(baseServiceUrl);

    }

    @Test
    public void test_getRoomOffers_getRoomOffersWithCustomerId_validateRoomOfferResponse() {
        createTokenWithCustomerDetails(null, Long.parseLong(defaultTestData.getCustomerId()));
        validateGetRequestIdAndTypeExists(baseServiceUrl);
    }

    @Test
    public void test_getRoomOffers_getRoomOffersWithValidProgramSegment_validateRoomOfferResponse() {
        validateGetRequestAttributesExists(baseServiceSegmentUrl.replaceAll("<programId>", defaultTestData.getProgramPartOfSegment()),
                "$", "segmentId", "programIds").jsonPath("$.programIds").isNotEmpty();

    }

    @Test
    public void test_getRoomOffers_getRoomOffersWithNoProgramSegment_validateNoProgramSegmentError() {
        validateGetRequestErrorDetails(
                baseServiceSegmentUrl.replaceAll("<programId>", defaultTestData.getPatronProgramId()),
                ErrorCode.NO_SEGMENT_ID_FOUND.getErrorCode(), ErrorCode.NO_SEGMENT_ID_FOUND.getDescription());
    }

    @Test
    public void test_getRoomOffers_getRoomOffersWithInvalidProgramSegment_validateInvalidProgramSegmentError() {
        validateGetRequestErrorDetails(baseServiceSegmentUrl.replaceAll("<programId>", ServiceConstant.WHITESPACE_STRING),
                ErrorCode.INVALID_PROGRAM_ID.getErrorCode(), ErrorCode.INVALID_PROGRAM_ID.getDescription());
        validateGetRequestErrorDetails(baseServiceSegmentUrl.replaceAll("<programId>", "123"),
                ErrorCode.INVALID_PROGRAM_ID.getErrorCode(), ErrorCode.INVALID_PROGRAM_ID.getDescription());
    }

    
    @Test
    public void test_getRoomOffers_getTransientOffers_validateRoomOfferResponse() {
        validateShapeGetRequestIdAndTypeExists(baseServiceUrl);
    }

    @Test
    public void test_getRoomOffers_getTransientOffersWithFilter_validateRoomOfferResponse() {
        validateShapeGetRequestIdAndTypeExists(baseServiceUrl + "?propertyIds=" + defaultTestData.getPropertyId());
    }
}
