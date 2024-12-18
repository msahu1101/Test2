package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.model.request.CalendarPriceV3Request;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.CalendarPriceV3Response;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.util.ServiceConversionHelper;

    @RunWith(MockitoJUnitRunner.class)

    public class CalendarPriceV3ServiceImplTest extends BaseRoomBookingTest {

	@Mock
	private RoomPriceDAO pricingDao;

	@Mock
	private RoomProgramService programService;

	@Mock
	private RoomProgramCacheService programCacheService;
	
	@Mock
    private RoomProgramDAO programDao;
	
	@Mock
    private ServiceConversionHelper serviceConversionHelper;
	
	@InjectMocks
	private CalendarPriceV3ServiceImpl calendarPriceV3ServiceImpl;
	
	private List<AuroraPriceV3Response> getLOSBasedCalendarPrices(String fileName) {
		File file = new File(getClass().getResource(fileName).getPath());

		return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, AuroraPriceV3Response.class));

	}
	
	@Test
	public void getLOSBasedCalendarPrices_perpetualPricing_Test() {
		
		CalendarPriceV3Request calendarRequest = new CalendarPriceV3Request();
		calendarRequest.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");
		calendarRequest.setPerpetualPricing(true);
		
		RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
		validateResponse.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");
		validateResponse.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		validateResponse.setEligible(true);
		
		when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);
		when(programDao.isProgramPO(Mockito.any())).thenReturn(true);		
		when(pricingDao.getLOSBasedCalendarPrices(Mockito.any())).thenReturn(getLOSBasedCalendarPrices("/calendar-prices-v3.json"));
        
		List<CalendarPriceV3Response> response = calendarPriceV3ServiceImpl.getLOSBasedCalendarPrices(calendarRequest);
		
		assertNotNull(response);
        assertFalse(response.get(0).isPOApplicable());
        assertEquals(318.00, response.get(0).getTotalNightlyTripBasePrice(),0.01);
        assertEquals("PROGRAM", response.get(0).getPricingMode().toString());
        assertEquals("AVAILABLE",response.get(0).getStatus().toString());

        assertFalse(response.get(1).isPOApplicable());
        assertEquals(258.00, response.get(1).getTotalNightlyTripBasePrice(),0.01);
        assertEquals("PROGRAM", response.get(1).getPricingMode().toString());
        assertEquals("AVAILABLE",response.get(1).getStatus().toString());

	}
	
	@Test
	public void getLOSBasedCalendarPrices_Test() {
		CalendarPriceV3Request calendarRequest = new CalendarPriceV3Request();
		calendarRequest.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");
		calendarRequest.setPerpetualPricing(false);
		
		RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
		validateResponse.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");
		validateResponse.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		validateResponse.setEligible(true);
		
		when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);
		when(pricingDao.getLOSBasedCalendarPrices(Mockito.any())).thenReturn(getLOSBasedCalendarPrices("/calendar-prices-v3.json"));

		
		List<CalendarPriceV3Response> response = calendarPriceV3ServiceImpl.getLOSBasedCalendarPrices(calendarRequest);
		assertNotNull(response);
        assertFalse(response.get(0).isPOApplicable());
        assertEquals(318.00, response.get(0).getTotalNightlyTripBasePrice(),0.01);
        assertEquals("PROGRAM", response.get(0).getPricingMode().toString());
        assertEquals("AVAILABLE",response.get(0).getStatus().toString());

        assertFalse(response.get(1).isPOApplicable());
        assertEquals(258.00, response.get(1).getTotalNightlyTripBasePrice(),0.01);
        assertEquals("PROGRAM", response.get(1).getPricingMode().toString());
        assertEquals("AVAILABLE",response.get(1).getStatus().toString());

	}
	
}
