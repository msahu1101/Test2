package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import com.mgm.services.booking.room.model.request.CalendarPriceRequest;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.CalendarPriceResponse;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;

/**
 * Unit test class for service methods in CalendarPriceService.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CalendarPriceServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private RoomPriceDAO pricingDao;

    @Mock
    private RoomProgramService programService;

    @Mock
    private RoomProgramCacheService programCacheService;

    @InjectMocks
    CalendarPriceServiceImpl calendarPriceServiceImpl;

    private List<AuroraPriceResponse> getIterableCalendarPrices(String fileName) {
        File file = new File(getClass().getResource(fileName).getPath());

        return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, AuroraPriceResponse.class));

    }

    private List<AuroraPriceResponse> getCalendarPrices(String fileName) {
        File file = new File(getClass().getResource(fileName).getPath());

        return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, AuroraPriceResponse.class));

    }

    /**
     * Test getCalendarPrices for default return values.
     */
    @Test
    public void getCalendarPricesTest() {

        when(pricingDao.getIterableCalendarPrices(Mockito.any()))
                .thenReturn(getIterableCalendarPrices("/calendar-prices.json"));

        CalendarPriceRequest calendarRequest = new CalendarPriceRequest();
        List<CalendarPriceResponse> response = calendarPriceServiceImpl.getCalendarPrices(calendarRequest);

        assertFalse(response.get(0).isComp());
        assertEquals("Price should match", 8999.99, response.get(0).getPrice(), 0.01);
        assertEquals("Program Id should match", "1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(0).getProgramId());
        assertNull(response.get(0).getMemberPrice());
        assertNull("There should be no member price", response.get(0).getMemberProgramId());
        assertEquals("Status should be available", "AVAILABLE", response.get(0).getStatus().toString());

        assertFalse(response.get(1).isComp());
        assertEquals(7999.99, response.get(1).getPrice(), 0.01);
        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0", response.get(1).getProgramId());
        assertNull(response.get(1).getMemberPrice());
        assertNull(response.get(1).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(1).getStatus().toString());

        assertFalse(response.get(2).isComp());
        assertEquals(6999.99, response.get(2).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(2).getProgramId());
        assertNull(response.get(2).getMemberPrice());
        assertNull(response.get(2).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(2).getStatus().toString());

    }

    /**
     * Test getCalendarPrices for different status options.
     */
    @Test
    public void getCalendarPricesStatusTest() {

        when(pricingDao.getIterableCalendarPrices(Mockito.any()))
                .thenReturn(getIterableCalendarPrices("/calendar-prices-mixed-status.json"));

        CalendarPriceRequest calendarRequest = new CalendarPriceRequest();

        List<CalendarPriceResponse> response = calendarPriceServiceImpl.getCalendarPrices(calendarRequest);

        assertTrue(response.get(0).isComp());
        assertEquals(8999.99, response.get(0).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(0).getProgramId());
        assertNull(response.get(0).getMemberPrice());
        assertNull(response.get(0).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());

        assertTrue(response.get(1).isComp());
        assertEquals(7999.99, response.get(1).getPrice(), 0.01);
        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0", response.get(1).getProgramId());
        assertNull(response.get(1).getMemberPrice());
        assertNull(response.get(1).getMemberProgramId());
        assertEquals("NOARRIVAL", response.get(1).getStatus().toString());

        assertFalse(response.get(2).isComp());
        assertEquals(6999.99, response.get(2).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(2).getProgramId());
        assertNull(response.get(2).getMemberPrice());
        assertNull(response.get(2).getMemberProgramId());
        assertEquals("SOLDOUT", response.get(2).getStatus().toString());
    }

    /**
     * Test getCalendarPrices when there's no discount prices.
     */
    @Test
    public void getCalendarPricesNoDiscountTest() {

        when(pricingDao.getIterableCalendarPrices(Mockito.any()))
                .thenReturn(getIterableCalendarPrices("/calendar-prices-nodiscount.json"));

        CalendarPriceRequest calendarRequest = new CalendarPriceRequest();
        List<CalendarPriceResponse> response = calendarPriceServiceImpl.getCalendarPrices(calendarRequest);

        assertFalse(response.get(0).isComp());
        assertEquals(10089.99, response.get(0).getPrice(), 0.01);
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());

        assertFalse(response.get(1).isComp());
        assertEquals(9089.99, response.get(1).getPrice(), 0.01);
        assertEquals("AVAILABLE", response.get(1).getStatus().toString());

        assertFalse(response.get(2).isComp());
        assertEquals(8089.99, response.get(2).getPrice(), 0.01);
        assertEquals("AVAILABLE", response.get(2).getStatus().toString());
    }

    /**
     * Test getCalendarPrices for member price attributes when customer is not
     * logged-in and no program id.
     */
    @Test
    public void getCalendarPricesMemberTransientTest() {

        when(pricingDao.getIterableCalendarPrices(Mockito.any()))
                .thenReturn(getIterableCalendarPrices("/calendar-prices-member.json"));

        CalendarPriceRequest calendarRequest = new CalendarPriceRequest();
        calendarRequest.setCustomerId(-1);

        List<CalendarPriceResponse> response = calendarPriceServiceImpl.getCalendarPrices(calendarRequest);

        assertFalse(response.get(0).isComp());
        assertEquals(8999.99, response.get(0).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(0).getProgramId());
        assertEquals(7890.45, response.get(0).getMemberPrice(), 0);
        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0", response.get(0).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());

        assertFalse(response.get(1).isComp());
        assertEquals(7999.99, response.get(1).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(1).getProgramId());
        assertEquals(7890.45, response.get(1).getMemberPrice(), 0);
        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0", response.get(1).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(1).getStatus().toString());

        assertFalse(response.get(2).isComp());
        assertEquals(6999.99, response.get(2).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(2).getProgramId());
        assertNull(response.get(2).getMemberPrice());
        assertNull(response.get(2).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(2).getStatus().toString());
    }

    /**
     * Test getCalendarPrices for member attributes when there's customer id.
     */
    @Test
    public void getCalendarPricesMemberLoggedTest() {

        when(pricingDao.getIterableCalendarPrices(Mockito.any()))
                .thenReturn(getIterableCalendarPrices("/calendar-prices-member.json"));

        CalendarPriceRequest calendarRequest = new CalendarPriceRequest();
        calendarRequest.setCustomerId(123456);

        List<CalendarPriceResponse> response = calendarPriceServiceImpl.getCalendarPrices(calendarRequest);

        assertFalse(response.get(0).isComp());
        assertEquals(8999.99, response.get(0).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(0).getProgramId());
        assertNull(response.get(0).getMemberPrice());
        assertNull(response.get(0).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());

        assertFalse(response.get(1).isComp());
        assertEquals(7999.99, response.get(1).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(1).getProgramId());
        assertNull(response.get(1).getMemberPrice());
        assertNull(response.get(1).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(1).getStatus().toString());

        assertFalse(response.get(2).isComp());
        assertEquals(6999.99, response.get(2).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(2).getProgramId());
        assertNull(response.get(2).getMemberPrice());
        assertNull(response.get(2).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(2).getStatus().toString());
    }

    /**
     * Test getCalendarPrices when there's customer id and program id involved.
     */
    @Test
    public void getCalendarPricesMemberTransientProgramTest() {

        when(pricingDao.getCalendarPrices(Mockito.any())).thenReturn(getCalendarPrices("/calendar-prices.json"));
        when(programService.isProgramApplicable(Mockito.any())).thenReturn(true);

        CalendarPriceRequest calendarRequest = new CalendarPriceRequest();
        calendarRequest.setCustomerId(123456);
        calendarRequest.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");

        List<CalendarPriceResponse> response = calendarPriceServiceImpl.getCalendarPrices(calendarRequest);

        assertFalse(response.get(0).isComp());
        assertEquals(8999.99, response.get(0).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(0).getProgramId());
        assertNull(response.get(0).getMemberPrice());
        assertNull(response.get(0).getMemberProgramId());
        assertEquals("OFFER", response.get(0).getStatus().toString());

        assertFalse(response.get(1).isComp());
        assertEquals(7999.99, response.get(1).getPrice(), 0.01);
        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0", response.get(1).getProgramId());
        assertNull(response.get(1).getMemberPrice());
        assertNull(response.get(1).getMemberProgramId());
        assertEquals("AVAILABLE", response.get(1).getStatus().toString());

        assertFalse(response.get(2).isComp());
        assertEquals(6999.99, response.get(2).getPrice(), 0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de", response.get(2).getProgramId());
        assertNull(response.get(2).getMemberPrice());
        assertNull(response.get(2).getMemberProgramId());
        assertEquals("OFFER", response.get(2).getStatus().toString());
    }

    /**
     * Test getCustomerPrices for different program failures.
     */
    @Test
    public void getCalendarPricesProgramFailuresTest() {

        when(programService.isProgramApplicable(Mockito.any())).thenReturn(false);

        CalendarPriceRequest calendarRequest = new CalendarPriceRequest();
        calendarRequest.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");

        try {
            calendarPriceServiceImpl.getCalendarPrices(calendarRequest);
        } catch (Exception ex) {
            System.out.println(ex);
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }

        when(programService.getProgramByPromoCode("BOGO", "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
                .thenReturn(StringUtils.EMPTY);
        calendarRequest.setPromoCode("BOGO");
        calendarRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        try {
            calendarPriceServiceImpl.getCalendarPrices(calendarRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_available>[ Offer is not available or invalid ]", ex.getMessage());
        }

        when(programService.getProgramByPromoCode("BOGO", "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
                .thenReturn("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");

        try {
            calendarPriceServiceImpl.getCalendarPrices(calendarRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }
    }

}
