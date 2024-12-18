package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.model.request.RoomAvailabilityRequest;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.RatePlanResponse;
import com.mgm.services.booking.room.service.RoomProgramService;

@RunWith(MockitoJUnitRunner.class)
public class RoomRatePlanServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private RoomPriceDAO pricingDao;

    @Mock
    private RoomProgramService programService;

    @InjectMocks
    RoomAvailabilityServiceImpl availabilityServiceImpl;

    private List<AuroraPriceResponse> getRoomAvailability() {
        File file = new File(getClass().getResource("/room-rate-plans.json").getPath());

        return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, AuroraPriceResponse.class));
    }

    /**
     * Test getRatePlans for default return values.
     */
    @Test
    public void getRoomPlansTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());

        RoomAvailabilityRequest availabilityRequest = new RoomAvailabilityRequest();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setCheckInDate(getFutureLocalDate(5));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(7));

        List<RatePlanResponse> response = availabilityServiceImpl.getRatePlans(availabilityRequest);

        assertEquals("c4cf4722-5d41-4138-a775-a136e4b5219b", response.get(0).getProgramId());
        assertNotNull(response.get(0).getRooms());
        assertEquals(2, response.get(0).getRooms().size());
        response.get(0).getRooms().forEach(room -> {
            assertEquals(3, room.getPrice().getItemized().size());
        });

        assertEquals("5f678471-8cfc-4ab7-8ca6-6286ec5efe6c", response.get(1).getProgramId());
        assertNotNull(response.get(1).getRooms());
        assertEquals(2, response.get(1).getRooms().size());
        response.get(1).getRooms().forEach(room -> {
            assertEquals(3, room.getPrice().getItemized().size());
        });

        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0", response.get(2).getProgramId());
        assertNotNull(response.get(2).getRooms());
        assertEquals(4, response.get(2).getRooms().size());
        response.get(2).getRooms().forEach(room -> {
            assertEquals(3, room.getPrice().getItemized().size());
        });

    }

    /**
     * Test getRatePlans for different program failures.
     */
    @Test
    public void getRoomPlansProgramFailuresTest() {

        when(programService.isProgramApplicable(Mockito.any())).thenReturn(false);

        RoomAvailabilityRequest availabilityRequest = new RoomAvailabilityRequest();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setProgramId("89364848-c326-4319-a083-d5665df90349");

        try {
            availabilityServiceImpl.getRatePlans(availabilityRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }

        when(programService.getProgramByPromoCode("BOGO", "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
                .thenReturn(StringUtils.EMPTY);
        availabilityRequest.setPromoCode("BOGO");
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        try {
            availabilityServiceImpl.getRatePlans(availabilityRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_available>[ Offer is not available or invalid ]", ex.getMessage());
        }

        when(programService.getProgramByPromoCode("BOGO", "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
                .thenReturn("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");

        try {
            availabilityServiceImpl.getRatePlans(availabilityRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }
    }
}
