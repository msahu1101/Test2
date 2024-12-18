package com.mgm.services.booking.room.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.CalendarPriceV2Request;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV2Request;

/**
 * Class to test AuroraPriceRequestTransformer logic.
 * 
 * @author laknaray
 *
 */
public class AuroraPriceRequestTransformerTest extends BaseRoomBookingTest {

    @Test
    public void test_getAuroraRequest_withCalendarPriceV2Request_withItineraryIds_returnRequestWithItineraryIds() {
        CalendarPriceV2Request calendarRequest = new CalendarPriceV2Request();
        calendarRequest.setStartDate(getFutureLocalDate(30));
        calendarRequest.setEndDate(getFutureLocalDate(45));
        calendarRequest.setProgramId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        calendarRequest.setNumAdults(2);
        calendarRequest.setNumAdults(1);
        String[] itineraryIds = {"12345","12346"};
        calendarRequest.setItineraryIds(Arrays.asList(itineraryIds));
        AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraRequest(calendarRequest);
        assertEquals(2, request.getAuroraItineraryIds().size());
    }

    @Test
    public void test_getAuroraPriceV2Request_withRoomAvailabilityV2Request_withProgramRateTrueAndItineraryIds_returnRequestWithItineraryIds() {
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCheckInDate(getFutureLocalDate(30));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(45));
        availabilityRequest.setProgramId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumAdults(1);
        availabilityRequest.setNumRooms(1);
        String[] itineraryIds = {"12345","12346"};
        availabilityRequest.setItineraryIds(Arrays.asList(itineraryIds));
        AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraPriceV2Request(availabilityRequest, true);
        assertEquals(2, request.getAuroraItineraryIds().size());
    }

    @Test
    public void test_getAuroraPriceV2Request_withRoomAvailabilityV2Request_withItineraryIds_returnRequestWithItineraryIds() {
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCheckInDate(getFutureLocalDate(30));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(45));
        availabilityRequest.setProgramId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumAdults(1);
        availabilityRequest.setNumRooms(1);
        String[] itineraryIds = {"12345","12346"};
        availabilityRequest.setItineraryIds(Arrays.asList(itineraryIds));
        AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraPriceV2Request(availabilityRequest);
        assertEquals(2, request.getAuroraItineraryIds().size());
    }
    
}
