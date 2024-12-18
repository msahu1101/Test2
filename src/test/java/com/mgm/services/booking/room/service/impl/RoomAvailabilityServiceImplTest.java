package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.model.PriceItemized;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.RoomAvailabilityRequest;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityResponse;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;

@RunWith(MockitoJUnitRunner.class)
public class RoomAvailabilityServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private RoomPriceDAO pricingDao;

    @Mock
    private RoomProgramService programService;

    @Mock
    private RoomProgramCacheService programCacheService;

    @InjectMocks
    RoomAvailabilityServiceImpl availabilityServiceImpl;

    private List<AuroraPriceResponse> getRoomAvailability() {
        File file = new File(getClass().getResource("/room-prices.json").getPath());

        return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, AuroraPriceResponse.class));
    }

    /**
     * Test getRoomPrices for default return values with and without member
     * prices.
     */
    @Test
    public void getRoomAvailabilityTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());

        RoomAvailabilityRequest availabilityRequest = new RoomAvailabilityRequest();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setCheckInDate(getFutureLocalDate(5));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(8));

        Set<RoomAvailabilityResponse> returned = availabilityServiceImpl.getRoomPrices(availabilityRequest);

        assertEquals(15, returned.size());

        returned.forEach(response -> {
            if (response.getRoomTypeId().equals("8b6f42b4-e8a4-4921-80cc-35fe87be8acd")) {
                assertEquals(31.93, response.getResortFee(), 0.1);
                assertNotNull(response.getMemberPrice());
                assertEquals(29999.97, response.getPrice().getBaseSubtotal(), 0);
                assertEquals(9999.99, response.getPrice().getBaseAveragePrice(), 0);
                assertEquals(16999.98, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(5666.66, response.getPrice().getDiscountedAveragePrice(), 0.1);
                assertEquals(29999.97, response.getMemberPrice().getBaseSubtotal(), 0);
                assertEquals(9999.99, response.getMemberPrice().getBaseAveragePrice(), 0);
                assertEquals(14999.98, response.getMemberPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(4999.99, response.getMemberPrice().getDiscountedAveragePrice(), 0.1);
            }

            if (response.getRoomTypeId().equals("23f5bef8-63ea-4ba9-a290-13b5a3056595")) {
                assertEquals("23f5bef8-63ea-4ba9-a290-13b5a3056595", response.getRoomTypeId());
                assertEquals(35, response.getResortFee(), 0.1);
                assertNull(response.getMemberPrice());
                assertEquals(30044.97, response.getPrice().getBaseSubtotal(), 0);
                assertEquals(10014.99, response.getPrice().getBaseAveragePrice(), 0);
                assertEquals(17025.48, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(5675.16, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }
        });

    }

    /**
     * Test getRoomPrices for itemized price values along with totals/averages.
     */
    @Test
    public void getRoomAvailabilityItemizedTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());

        RoomAvailabilityRequest availabilityRequest = new RoomAvailabilityRequest();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setCheckInDate(getFutureLocalDate(5));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(8));

        Set<RoomAvailabilityResponse> returned = availabilityServiceImpl.getRoomPrices(availabilityRequest);

        assertEquals(15, returned.size());

        returned.forEach(response -> {
            if (response.getRoomTypeId().equals("8b6f42b4-e8a4-4921-80cc-35fe87be8acd")) {
                assertEquals("8b6f42b4-e8a4-4921-80cc-35fe87be8acd", response.getRoomTypeId());
                assertEquals(31.93, response.getResortFee(), 0.1);
                assertNotNull(response.getMemberPrice());
                assertEquals(29999.97, response.getPrice().getBaseSubtotal(), 0);
                assertEquals(9999.99, response.getPrice().getBaseAveragePrice(), 0);
                assertEquals(16999.98, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(5666.66, response.getPrice().getDiscountedAveragePrice(), 0.1);
                assertEquals(29999.97, response.getMemberPrice().getBaseSubtotal(), 0);
                assertEquals(9999.99, response.getMemberPrice().getBaseAveragePrice(), 0);
                assertEquals(14999.98, response.getMemberPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(4999.99, response.getMemberPrice().getDiscountedAveragePrice(), 0.1);

                PriceItemized price = response.getPrice().getItemized().get(0);
                assertEquals(9999.99, price.getBasePrice(), 0);
                assertEquals(5499.9945, price.getDiscountedPrice(), 0);
                assertEquals("89364848-c326-4319-a083-d5665df90349", price.getProgramId());

                price = response.getPrice().getItemized().get(1);
                assertEquals(9999.99, price.getBasePrice(), 0);
                assertEquals(5499.9945, price.getDiscountedPrice(), 0);
                assertEquals("89364848-c326-4319-a083-d5665df90349", price.getProgramId());

                price = response.getPrice().getItemized().get(2);
                assertEquals(9999.99, price.getBasePrice(), 0);
                assertEquals(5999.99, price.getDiscountedPrice(), 0.1);
                assertEquals("5ec54ac7-3750-4a81-86a2-c127b04ae8ed", price.getProgramId());

                price = response.getMemberPrice().getItemized().get(0);
                assertEquals(9999.99, price.getBasePrice(), 0);
                assertEquals(4999.995, price.getDiscountedPrice(), 0);
                assertEquals("2e44c1cf-097c-4b0b-a86f-7993d239b055", price.getProgramId());

                price = response.getMemberPrice().getItemized().get(1);
                assertEquals(9999.99, price.getBasePrice(), 0);
                assertEquals(4999.995, price.getDiscountedPrice(), 0);
                assertEquals("2e44c1cf-097c-4b0b-a86f-7993d239b055", price.getProgramId());

                price = response.getMemberPrice().getItemized().get(2);
                assertEquals(9999.99, price.getBasePrice(), 0);
                assertEquals(4999.995, price.getDiscountedPrice(), 0.1);
                assertEquals("2e44c1cf-097c-4b0b-a86f-7993d239b055", price.getProgramId());
            }

        });

    }

    /**
     * Test getRoomPrices for logged-in user and expect no member price.
     */
    @Test
    public void getRoomAvailabilityLoggedInTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());

        RoomAvailabilityRequest availabilityRequest = new RoomAvailabilityRequest();
        availabilityRequest.setCustomerId(123456);
        availabilityRequest.setCheckInDate(getFutureLocalDate(5));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(8));

        Set<RoomAvailabilityResponse> returned = availabilityServiceImpl.getRoomPrices(availabilityRequest);

        assertEquals(15, returned.size());

        returned.forEach(response -> {
            if (response.getRoomTypeId().equals("8b6f42b4-e8a4-4921-80cc-35fe87be8acd")) {
                assertEquals("8b6f42b4-e8a4-4921-80cc-35fe87be8acd", response.getRoomTypeId());
                assertEquals(31.93, response.getResortFee(), 0.1);
                assertNull(response.getMemberPrice());
                assertEquals(29999.97, response.getPrice().getBaseSubtotal(), 0);
                assertEquals(9999.99, response.getPrice().getBaseAveragePrice(), 0);
                assertEquals(16999.98, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(5666.66, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }

            if (response.getRoomTypeId().equals("23f5bef8-63ea-4ba9-a290-13b5a3056595")) {
                assertEquals(35, response.getResortFee(), 0.1);
                assertNull(response.getMemberPrice());
                assertEquals(30044.97, response.getPrice().getBaseSubtotal(), 0);
                assertEquals(10014.99, response.getPrice().getBaseAveragePrice(), 0);
                assertEquals(17025.48, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(5675.16, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }
        });

    }

    /**
     * Test getRoomPrices with program id and expect no member price.
     */
    @Test
    public void getRoomAvailabilityProgramTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());
        when(programService.isProgramApplicable(Mockito.any())).thenReturn(true);

        RoomProgram program = new RoomProgram();
        program.setId("89364848-c326-4319-a083-d5665df90349");
        program.setPropertyId("f8d6a944-7816-412f-a39a-9a63aad26833");
        when(programCacheService.getRoomProgram("89364848-c326-4319-a083-d5665df90349")).thenReturn(program);

        RoomAvailabilityRequest availabilityRequest = new RoomAvailabilityRequest();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setProgramId("89364848-c326-4319-a083-d5665df90349");
        availabilityRequest.setCheckInDate(getFutureLocalDate(5));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(8));

        Set<RoomAvailabilityResponse> returned = availabilityServiceImpl.getRoomPrices(availabilityRequest);

        assertEquals(15, returned.size());

        returned.forEach(response -> {
            if (response.getRoomTypeId().equals("8b6f42b4-e8a4-4921-80cc-35fe87be8acd")) {
                assertEquals(31.93, response.getResortFee(), 0.1);
                assertNull(response.getMemberPrice());
                assertEquals(29999.97, response.getPrice().getBaseSubtotal(), 0);
                assertEquals(9999.99, response.getPrice().getBaseAveragePrice(), 0);
                assertEquals(16999.98, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(5666.66, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }

            if (response.getRoomTypeId().equals("23f5bef8-63ea-4ba9-a290-13b5a3056595")) {
                assertEquals("23f5bef8-63ea-4ba9-a290-13b5a3056595", response.getRoomTypeId());
                assertEquals(35, response.getResortFee(), 0.1);
                assertNull(response.getMemberPrice());
                assertEquals(30044.97, response.getPrice().getBaseSubtotal(), 0);
                assertEquals(10014.99, response.getPrice().getBaseAveragePrice(), 0);
                assertEquals(17025.48, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(5675.16, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }
        });

    }

    /**
     * Test getRoomPrices for different program failures.
     */
    @Test
    public void getRoomAvailabilityProgramFailuresTest() {

        when(programService.isProgramApplicable(Mockito.any())).thenReturn(false);

        RoomAvailabilityRequest availabilityRequest = new RoomAvailabilityRequest();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setProgramId("89364848-c326-4319-a083-d5665df90349");

        try {
            availabilityServiceImpl.getRoomPrices(availabilityRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }

        when(programService.getProgramByPromoCode("BOGO", "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
                .thenReturn(StringUtils.EMPTY);
        availabilityRequest.setPromoCode("BOGO");
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        try {
            availabilityServiceImpl.getRoomPrices(availabilityRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_available>[ Offer is not available or invalid ]", ex.getMessage());
        }

        when(programService.getProgramByPromoCode("BOGO", "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
                .thenReturn("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");

        try {
            availabilityServiceImpl.getRoomPrices(availabilityRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }
    }

}
