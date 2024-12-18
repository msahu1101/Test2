package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.invocation.MockitoMethod;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.RoomTripPriceV2;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV2Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityCombinedResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityV2Response;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.helper.RoomAvailabilityServiceHelper;

@RunWith(MockitoJUnitRunner.class)
public class RoomAvailabilityV2ServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private RoomPriceDAO pricingDao;

    @Mock
    private RoomProgramService programService;

    @Mock
    private RoomProgramCacheService programCacheService;

    @InjectMocks
    RoomAvailabilityV2ServiceImpl availabilityV2ServiceImpl;

    @Mock
    RoomAvailabilityServiceHelper availabilityServiceHelper;

    @Before
    public void setup() {
        when(availabilityServiceHelper.getChannelHeader()).thenReturn("ice");
        ReflectionTestUtils.setField(availabilityV2ServiceImpl, "availabilityServiceHelper",
                availabilityServiceHelper);
    }

    private AuroraPricesResponse getAuroraPrices(String filePath) {
        File file = new File(getClass().getResource(filePath).getPath());

        return convert(file, AuroraPricesResponse.class);
    }

    /**
     * Test getRoomPrices with <i>includeSoldOutRooms</i> true and expecting sold
     * out rooms also in the response.
     */
    @Test
    public void getRoomPricesV2_givenIncludeSoldOutRooms_returnsSoldOutRooms() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-prices-v2.json"));

        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setCheckInDate(getFutureLocalDate(12));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(18));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        availabilityRequest.setIncludeSoldOutRooms(true);

        Set<RoomAvailabilityV2Response> returned = availabilityV2ServiceImpl.getRoomPrices(availabilityRequest);
        
        assertEquals(61, returned.size());

        returned.forEach(response -> {
            if (response.getRoomTypeId().equals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd")) {
                assertFalse(response.isUnavailable());
            }

            if (response.getRoomTypeId().equals("7aba5c42-745b-4f2e-96cf-9e256dff306a")) {
                assertTrue(response.isUnavailable());
            }
        });

    }

    /**
     * Test getRoomPrices with customerId in the request and expect only prices.
     */
    @Test
    public void getRoomPricesV2_givenCustomerId_returnsPrices() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-prices-v2.json"));

        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(923600551937l);
        availabilityRequest.setCheckInDate(getFutureLocalDate(12));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(18));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        when(availabilityServiceHelper.getRoomTripPriceV2(Mockito.anyLong(), Mockito.anyDouble(), Mockito.anyDouble(),
                Mockito.any(), Mockito.anyDouble())).thenReturn(new RoomTripPriceV2());

        Set<RoomAvailabilityV2Response> returned = availabilityV2ServiceImpl.getRoomPrices(availabilityRequest);

        assertEquals(26, returned.size());

        returned.forEach(response -> {
            if (response.getRoomTypeId().equals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd")) {
                assertEquals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertNotNull(response.getPrice());
            }

            if (response.getRoomTypeId().equals("b2135b7f-7172-4d53-b39d-217de6f5c970")) {
                assertEquals("b2135b7f-7172-4d53-b39d-217de6f5c970", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertNotNull(response.getPrice());
            }
        });

    }
    

    /**
     * Test getRoomPrices with program id and expect price.
     */
    @Test
    public void getRoomPricesV2_givenProgramId_returnsPrices() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        availabilityRequest.setCheckInDate(getFutureLocalDate(12));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(18));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        when(availabilityServiceHelper.getRoomTripPriceV2(Mockito.anyLong(), Mockito.anyDouble(), Mockito.anyDouble(),
                Mockito.any(), Mockito.anyDouble())).thenReturn(new RoomTripPriceV2());

        Set<RoomAvailabilityV2Response> returned = availabilityV2ServiceImpl.getRoomPrices(availabilityRequest);

        assertEquals(26, returned.size());

        returned.forEach(response -> {
            if (response.getRoomTypeId().equals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd")) {
                assertEquals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertNotNull(response.getPrice());
            }

            if (response.getRoomTypeId().equals("b2135b7f-7172-4d53-b39d-217de6f5c970")) {
                assertEquals("b2135b7f-7172-4d53-b39d-217de6f5c970", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertNotNull(response.getPrice());
            }
        });

    }
    
    /**
     * Test getRoomPrices with program id and expect price.
     */
    @Test
    public void getRoomAvailabilityV2_givenProgramId_returnsPrices() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        availabilityRequest.setCheckInDate(getFutureLocalDate(12));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(18));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        when(availabilityServiceHelper.getRoomTripPriceV2(Mockito.anyLong(), Mockito.anyDouble(), Mockito.anyDouble(),
                Mockito.any(), Mockito.anyDouble())).thenReturn(new RoomTripPriceV2());

        RoomAvailabilityCombinedResponse returned = availabilityV2ServiceImpl.getRoomAvailability(availabilityRequest);

        assertTrue(!returned.getAvailability().isEmpty());

        returned.getAvailability().forEach(response -> {
            if (response.getRoomTypeId().equals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd")) {
                assertEquals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertNotNull(response.getPrice());
            }

            if (response.getRoomTypeId().equals("b2135b7f-7172-4d53-b39d-217de6f5c970")) {
                assertEquals("b2135b7f-7172-4d53-b39d-217de6f5c970", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertNotNull(response.getPrice());
            }
        });

    }

	/**
     * Test getRoomAvailability with program id and expect price.
     */
    @Test
    public void getRoomAvailabilityV2_mrd_givenProgramId_returnsPrices() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        availabilityRequest.setCheckInDate(getFutureLocalDate(12));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(18));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setEnableMrd(true);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        when(availabilityServiceHelper.getRoomTripPriceV2(Mockito.anyLong(), Mockito.anyDouble(), Mockito.anyDouble(),
                Mockito.any(), Mockito.anyDouble())).thenReturn(new RoomTripPriceV2());

        RoomAvailabilityCombinedResponse returned = availabilityV2ServiceImpl.getRoomAvailability(availabilityRequest);

        assertTrue(!returned.getAvailability().isEmpty());

        returned.getAvailability().forEach(response -> {
            if (response.getRoomTypeId().equals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd")) {
                assertEquals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertNotNull(response.getPrice());
            }

            if (response.getRoomTypeId().equals("b2135b7f-7172-4d53-b39d-217de6f5c970")) {
                assertEquals("b2135b7f-7172-4d53-b39d-217de6f5c970", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertNotNull(response.getPrice());
            }
        });

    }

	/**
     * Test getRoomAvailability with program id and expect price.
     */
    @Test
    public void getRoomAvailabilityV2_mrd1_givenProgramId_returnsPrices() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        availabilityRequest.setCheckInDate(getFutureLocalDate(12));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(18));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setEnableMrd(true);
        availabilityRequest.setIncludeSoldOutRooms(true);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        
        AuroraPricesResponse auroraPricesResponse=new AuroraPricesResponse();
        auroraPricesResponse.setMrdPricing(true);
        List<AuroraPriceResponse> auroraPricesLisr=new ArrayList<>();
        AuroraPriceResponse auroraPriceResponse=new AuroraPriceResponse();
        auroraPriceResponse.setProgramId("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd");
        auroraPriceResponse.setPromo("OFFERS");
        auroraPriceResponse.setStatus(AvailabilityStatus.AVAILABLE);
        auroraPriceResponse.setDiscountedMemberPrice(12.0);
        auroraPriceResponse.setBaseMemberPrice(120);
        auroraPricesLisr.add(auroraPriceResponse);
        auroraPriceResponse=new AuroraPriceResponse();
        auroraPriceResponse.setProgramId("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74ce");
        auroraPriceResponse.setPromo("SPECIAL");
        auroraPriceResponse.setStatus(AvailabilityStatus.SOLDOUT);
        auroraPricesLisr.add(auroraPriceResponse);
        auroraPricesResponse.setAuroraPrices(auroraPricesLisr);
        when(availabilityServiceHelper.isMemberPriceApplicable(Mockito.anyLong(),Mockito.anyString())).thenReturn(true);
        Map<String, List<AuroraPriceResponse>> ratePlanMap= new LinkedHashMap<>();
        List<AuroraPriceResponse> priceList = new LinkedList<>();
        ratePlanMap.put("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", priceList);
        Mockito.doCallRealMethod().when(availabilityServiceHelper).populatePlanMap(Mockito.any(), Mockito.any()); 

        when(availabilityServiceHelper.getRoomTripPriceV2(Mockito.anyLong(), Mockito.anyDouble(), Mockito.anyDouble(),
                Mockito.any(), Mockito.anyDouble())).thenReturn(new RoomTripPriceV2());
        
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(auroraPricesResponse);

        RoomAvailabilityCombinedResponse returned = availabilityV2ServiceImpl.getRoomAvailability(availabilityRequest);

        assertTrue(!returned.getRatePlans().isEmpty());

        returned.getRatePlans().forEach(response -> {
        	if("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74ce".equalsIgnoreCase(response.getProgramId()))
        	{
        		response.getRooms().forEach(res-> {
        			assertTrue(res.isUnavailable());
        		});
        	}else if("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd".equalsIgnoreCase(response.getProgramId()))
        	{
        		response.getRooms().forEach(res-> {
        			assertTrue(!res.isUnavailable());
        		});
        	}
        });

    }

	/**
     * Test getRoomAvailability with program id and expect price.
     */
    @Test
    public void getRoomAvailabilityV2_mrd_alt_givenProgramId_returnsPrices() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(123456);
        availabilityRequest.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        availabilityRequest.setCheckInDate(getFutureLocalDate(12));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(18));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setEnableMrd(true);
        availabilityRequest.setIncludeSoldOutRooms(true);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        
        AuroraPricesResponse auroraPricesResponse=new AuroraPricesResponse();
        auroraPricesResponse.setMrdPricing(true);
        List<AuroraPriceResponse> auroraPricesLisr=new ArrayList<>();
        AuroraPriceResponse auroraPriceResponse=new AuroraPriceResponse();
        auroraPriceResponse.setProgramId("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd");
        auroraPriceResponse.setPromo("OFFERS");
        auroraPriceResponse.setStatus(AvailabilityStatus.AVAILABLE);
        auroraPriceResponse.setPOApplicable(true);
        auroraPriceResponse.setBaseMemberPrice(120);
        auroraPricesLisr.add(auroraPriceResponse);
        auroraPriceResponse=new AuroraPriceResponse();
        auroraPriceResponse.setProgramId("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74ce");
        auroraPriceResponse.setPromo("SPECIAL");
        auroraPriceResponse.setStatus(AvailabilityStatus.SOLDOUT);
        auroraPricesLisr.add(auroraPriceResponse);
        auroraPricesResponse.setAuroraPrices(auroraPricesLisr);
        
        Map<String, List<AuroraPriceResponse>> ratePlanMap= new LinkedHashMap<>();
        List<AuroraPriceResponse> priceList = new LinkedList<>();
        ratePlanMap.put("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", priceList);
        Mockito.doCallRealMethod().when(availabilityServiceHelper).populatePlanMap(Mockito.any(), Mockito.any()); 
        when(availabilityServiceHelper.isMemberPriceApplicable(Mockito.anyLong(),Mockito.anyString())).thenReturn(true);

        when(availabilityServiceHelper.getRoomTripPriceV2(Mockito.anyLong(), Mockito.anyDouble(), Mockito.anyDouble(),
                Mockito.any(), Mockito.anyDouble())).thenReturn(new RoomTripPriceV2());
        
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(auroraPricesResponse);

        RoomAvailabilityCombinedResponse returned = availabilityV2ServiceImpl.getRoomAvailability(availabilityRequest);

        assertTrue(!returned.getRatePlans().isEmpty());

        returned.getRatePlans().forEach(response -> {
        	if("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74ce".equalsIgnoreCase(response.getProgramId()))
        	{
        		response.getRooms().forEach(res-> {
        			assertTrue(res.isUnavailable());
        		});
        	}else if("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd".equalsIgnoreCase(response.getProgramId()))
        	{
        		response.getRooms().forEach(res-> {
        			assertTrue(!res.isUnavailable());
        		});
        	}
        });

    }

	/**
     * Test getRoomAvailability with program id and expect price.
     */
    @Test
    public void getLowestRoomPrice_givenProgramId_returnsPrices() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        availabilityRequest.setCheckInDate(getFutureLocalDate(12));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(18));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setEnableMrd(true);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        
        Optional<RoomAvailabilityV2Response> returned = availabilityV2ServiceImpl.getLowestRoomPrice(availabilityRequest);
        assertEquals(true,returned.isPresent());
    }

	/**
     * Test getRoomAvailability with program id and expect price.
     */
    @Test
    public void getLowestRoomPrice_Exception_givenProgramId_returnsPrices() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenThrow(new NullPointerException());
        
        RoomAvailabilityV2Request availabilityRequest = new RoomAvailabilityV2Request();
        availabilityRequest.setCustomerId(-1);
        availabilityRequest.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        availabilityRequest.setCheckInDate(getFutureLocalDate(12));
        availabilityRequest.setCheckOutDate(getFutureLocalDate(18));
        availabilityRequest.setNumAdults(2);
        availabilityRequest.setNumChildren(1);
        availabilityRequest.setEnableMrd(true);
        availabilityRequest.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        
        Optional<RoomAvailabilityV2Response> returned = availabilityV2ServiceImpl.getLowestRoomPrice(availabilityRequest);
        assertEquals(false,returned.isPresent());
    }

}
