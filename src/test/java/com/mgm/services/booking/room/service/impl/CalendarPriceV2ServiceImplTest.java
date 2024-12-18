package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.request.CalendarPriceV2Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.CalendarPriceV2Response;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;

@RunWith(MockitoJUnitRunner.class)
public class CalendarPriceV2ServiceImplTest extends BaseRoomBookingTest{
	@Mock
	private RoomPriceDAO pricingDao;

	@Mock
	private RoomProgramService programService;

	@Mock
	private RoomProgramCacheService programCacheService;

	@InjectMocks
	private CalendarPriceV2ServiceImpl calendarPriceV2ServiceImpl;

	private List<AuroraPriceResponse> getIterableCalendarPricesV2(String fileName) {
		File file = new File(getClass().getResource(fileName).getPath());

		return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, AuroraPriceResponse.class));

	}

	@Test
	public void getCalendarPrices_ExcludeNonOffer_Test() {

		CalendarPriceV2Request calendarRequest = new CalendarPriceV2Request ();
		calendarRequest.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");
		calendarRequest.setExcludeNonOffer(true);

		RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
		validateResponse.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");
		validateResponse.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		validateResponse.setEligible(true);

		when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);
		when(pricingDao.getIterableCalendarPricesV2(Mockito.any()))
		.thenReturn(getIterableCalendarPricesV2("/calendar-prices.json"));
	
		
		List<CalendarPriceV2Response> response = calendarPriceV2ServiceImpl.getCalendarPrices(calendarRequest);
		
		assertNotNull(response);
        assertFalse(response.get(0).isComp());
        assertEquals("OFFER",response.get(0).getStatus().toString());
        assertEquals(8999.99100000001,response.get(0).getPrice(),0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de",response.get(0).getProgramId());
        assertFalse(response.get(0).isPOApplicable());
      
        assertFalse(response.get(1).isComp());
        assertEquals("SOLDOUT",response.get(1).getStatus().toString());
        assertEquals(7999.99100000001,response.get(1).getPrice(),0.01);
        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0",response.get(1).getProgramId());
        assertFalse(response.get(1).isPOApplicable());
        
	}

	@Test
	public void getCalendarPrices_Test() {
		CalendarPriceV2Request calendarRequest = new CalendarPriceV2Request ();
		calendarRequest.setExcludeNonOffer(false);
		calendarRequest.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");

		RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
		validateResponse.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");
		validateResponse.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		validateResponse.setEligible(true);

		when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);
        when(pricingDao.getCalendarPricesV2(Mockito.any())).thenReturn(getIterableCalendarPricesV2("/calendar-prices.json"));
		
		List<CalendarPriceV2Response> response = calendarPriceV2ServiceImpl.getCalendarPrices(calendarRequest);

		assertNotNull(response);
        assertFalse(response.get(0).isComp());
        assertEquals("OFFER",response.get(0).getStatus().toString());
        assertEquals(8999.99100000001,response.get(0).getPrice(),0.01);
        assertEquals("1795e3c6-8e06-4baf-b1df-c09b20fbe1de",response.get(0).getProgramId());
        assertFalse(response.get(0).isPOApplicable());
        
        assertFalse(response.get(1).isComp());
        assertEquals("AVAILABLE",response.get(1).getStatus().toString());
        assertEquals(7999.99100000001,response.get(1).getPrice(),0.01);
        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0",response.get(1).getProgramId());
        assertFalse(response.get(1).isPOApplicable());

	}

	@Test
	public void overlayOfferPricesTest() {
		CalendarPriceV2Request calendarRequest = new CalendarPriceV2Request();
		calendarRequest.setProgramId("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");
		calendarRequest.setExcludeNonOffer(false);

		List<AuroraPriceResponse> programList = new ArrayList<>();
		List<AuroraPriceResponse> defaultList = new ArrayList<>();
		List<CalendarPriceV2Response> calendarResponseList = new ArrayList<>();
		
		AuroraPriceResponse response1 =new AuroraPriceResponse();
		response1.setStatus(AvailabilityStatus.AVAILABLE);
		programList.add(response1);

		AuroraPriceResponse response2 =new AuroraPriceResponse();
		java.util.Date checkInDate = new GregorianCalendar(2023, Calendar.NOVEMBER, 15).getTime();
		response2.setDate(checkInDate);
		response2.setStatus(AvailabilityStatus.AVAILABLE);
		programList.add(response2);
		defaultList.add(response2);

		calendarPriceV2ServiceImpl.overlayOfferPrices(calendarRequest, programList, defaultList, calendarResponseList);

		assertEquals("AVAILABLE", response1.getStatus().toString());
		assertFalse(response1.isPOApplicable());
		assertFalse(response1.isComp());
		assertFalse(response1.isProgramIdIsRateTable());
        assertNull(response1.getMemberProgramId());
        
        assertEquals("AVAILABLE", response2.getStatus().toString());
		assertFalse(response2.isPOApplicable());
		assertFalse(response2.isComp());
		assertFalse(response2.isProgramIdIsRateTable());
        assertNull(response2.getMemberProgramId());

	}
}
