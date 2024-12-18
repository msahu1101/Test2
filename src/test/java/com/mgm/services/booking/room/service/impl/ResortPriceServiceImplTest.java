package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.ResortPriceRequest;
import com.mgm.services.booking.room.model.request.RoomAvailabilityRequest;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.PerpetaulProgram;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.service.RoomAvailabilityService;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.helper.ResortPriceServiceHelper;

/**
 * Unit test class for all services in ResortPriceServiceImpl service
 * implementation.
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class ResortPriceServiceImplTest extends BaseRoomBookingTest {

    @Mock
    AuroraProperties auroraProperties;

    @Mock
    RoomPriceDAO pricingDao;

    @Mock
    RoomProgramService programService;

    @Mock
    RoomProgramCacheService programCacheService;

    @Mock
    RoomAvailabilityService availabilityService;

    @InjectMocks
    ResortPriceServiceImpl resortPriceService;

    @InjectMocks
    ResortPriceServiceHelper resortPriceServiceHelper;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(resortPriceServiceHelper, "auroraProperties", auroraProperties);
        ReflectionTestUtils.setField(resortPriceServiceHelper, "programCacheService", programCacheService);
        ReflectionTestUtils.setField(resortPriceServiceHelper, "programService", programService);
        ReflectionTestUtils.setField(resortPriceService, "helper", resortPriceServiceHelper);
    }

    private List<AuroraPriceResponse> getRoomAvailability() {
        File file = new File(getClass().getResource("/resort-prices.json").getPath());

        return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, AuroraPriceResponse.class));

    }

    private <T> List<T> getContents(String fileName, Class<T> className) {
        File file = new File(getClass().getResource(fileName).getPath());

        return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, className));

    }

    private List<String> getPropertyIds() {
        String propertyIds = "66964e2b-2550-4476-84c3-1a4c0c5c067f,dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad,8bf670c2-3e89-412b-9372-6c87a215e442,44e610ab-c209-4232-8bb4-51f7b9b13a75,2159252c-60d3-47db-bbae-b1db6bb15072,e0f70eb3-7e27-4c33-8bcd-f30bf3b1103a,4a65a92a-962b-433e-841c-37e18dc5d68d,e2704b04-d515-45b0-8afd-4fa1424ff0a8,607c07e7-3e31-4e4c-a4e1-f55dca66fea2,1f3ed672-3f8f-44d8-9215-81da3c845d83,f8d6a944-7816-412f-a39a-9a63aad26833,160cdf9b-ccdc-40ce-b0ac-1a58b69dcf4f,a0be1590-65c2-4e4d-b208-94ea5cac658f,bee81f88-286d-43dd-91b5-3917d9d62a68";
        return Arrays.asList(propertyIds.split(","));
    }

    /**
     * Test getResortPrices for default return values.
     */
    @Test
    public void getResortPricesTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());

        ResortPriceRequest pricingRequest = new ResortPriceRequest();
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        assertEquals(14, resortPriceService.getResortPrices(pricingRequest).size());

    }

    /**
     * Test getResortPrices for default return values with property ids
     * filtered.
     */
    @Test
    public void getResortPricesWithFilteringTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());

        ResortPriceRequest pricingRequest = new ResortPriceRequest();
        List<String> propertyIds = new ArrayList<String>();
        propertyIds.add("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
        propertyIds.add("f8d6a944-7816-412f-a39a-9a63aad26833");
        pricingRequest.setPropertyIds(propertyIds);
        pricingRequest.setCustomerId(-1);
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        List<ResortPriceResponse> response = resortPriceService.getResortPrices(pricingRequest);

        assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.get(0).getPropertyId());
        assertEquals(22.58, response.get(0).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());
        assertTrue(response.get(0).isComp());
        assertEquals(99999.99, response.get(0).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(0.0, response.get(0).getPrice().getDiscountedAveragePrice(), 0.1);

        assertEquals("f8d6a944-7816-412f-a39a-9a63aad26833", response.get(1).getPropertyId());
        assertEquals(31.91, response.get(1).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(1).getStatus().toString());
        assertFalse(response.get(1).isComp());
        assertEquals(99999.99, response.get(1).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(9249.37, response.get(1).getPrice().getDiscountedAveragePrice(), 0.1);

    }

    /**
     * Test getResortPrices for logged-in use case with property ids filtered.
     */
    @Test
    public void getResortPricesLoggedInTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());

        ResortPriceRequest pricingRequest = new ResortPriceRequest();
        List<String> propertyIds = new ArrayList<String>();
        propertyIds.add("f8d6a944-7816-412f-a39a-9a63aad26833");
        pricingRequest.setPropertyIds(propertyIds);
        pricingRequest.setCustomerId(123456);
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        List<ResortPriceResponse> response = resortPriceService.getResortPrices(pricingRequest);

        assertEquals("f8d6a944-7816-412f-a39a-9a63aad26833", response.get(0).getPropertyId());
        assertEquals(31.91, response.get(0).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());
        assertFalse(response.get(0).isComp());
        assertEquals(99999.99, response.get(0).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(9249.37, response.get(0).getPrice().getDiscountedAveragePrice(), 0.1);

    }

    /**
     * Test getResortPrices for logged-in use case with property ids filtered.
     */
    @Test
    public void createAvailabilityRequestsTest() {

        String propertyIds = "bee81f88-286d-43dd-91b5-3917d9d62a68, a0be1590-65c2-4e4d-b208-94ea5cac658f, 160cdf9b-ccdc-40ce-b0ac-1a58b69dcf4f";
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());

        when(programService.getDefaultPerpetualPrograms(Mockito.any()))
                .thenReturn(getContents("/default-perpetual-offers.json", PerpetaulProgram.class));

        ResortPriceRequest pricingRequest = new ResortPriceRequest();
        List<RoomAvailabilityRequest> request = resortPriceService.createAvailabilityRequests(pricingRequest);

        assertEquals("No of pricing requests should be 14", 14, request.size());
        assertEquals("No of pricing requests with program should be 2", 2, request.stream()
                .filter(r -> StringUtils.isNotEmpty(r.getProgramId())).collect(Collectors.toList()).size());

        pricingRequest.setPropertyIds(Arrays.asList(propertyIds.split(",")));
        request = resortPriceService.createAvailabilityRequests(pricingRequest);

        assertEquals("No of pricing requests should be 3", 3, request.size());
    }

    /**
     * Test getResortPrices with program use case and properties filtered.
     */
    @Test
    public void getResortPricesProgramTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());
        when(programCacheService.getProgramsBySegmentId("def64735-d34f-4daa-afd3-bcab36a318f0"))
                .thenReturn(new ArrayList<>());
        when(programService.isProgramApplicable(Mockito.any())).thenReturn(true);
        RoomProgram program = new RoomProgram();
        program.setId("def64735-d34f-4daa-afd3-bcab36a318f0");
        program.setPropertyId("f8d6a944-7816-412f-a39a-9a63aad26833");
        when(programCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);

        ResortPriceRequest pricingRequest = new ResortPriceRequest();
        List<String> propertyIds = new ArrayList<String>();
        propertyIds.add("f8d6a944-7816-412f-a39a-9a63aad26833");
        pricingRequest.setPropertyIds(propertyIds);
        pricingRequest.setCustomerId(123456);
        pricingRequest.setProgramId("def64735-d34f-4daa-afd3-bcab36a318f0");
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        List<ResortPriceResponse> response = resortPriceService.getResortPrices(pricingRequest);

        assertEquals("f8d6a944-7816-412f-a39a-9a63aad26833", response.get(0).getPropertyId());
        assertEquals(31.91, response.get(0).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());
        assertFalse(response.get(0).isComp());
        assertEquals(99999.99, response.get(0).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(9249.37, response.get(0).getPrice().getDiscountedAveragePrice(), 0.1);

    }

    /**
     * Test getResortPrices with program id part of segment use case.
     */
    @Test
    public void getResortPricesProgramInSegmentTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());
        when(programCacheService.getProgramsBySegmentId("def64735-d34f-4daa-afd3-bcab36a318f0"))
                .thenReturn(new ArrayList<>());
        when(programService.isProgramApplicable(Mockito.any())).thenReturn(true);
        RoomProgram program = new RoomProgram();
        program.setId("def64735-d34f-4daa-afd3-bcab36a318f0");
        program.setPropertyId("f8d6a944-7816-412f-a39a-9a63aad26833");
        program.setSegmentId("7f730ca2-3327-11e9-b210-d663bd873d93");
        when(programCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);

        ResortPriceRequest pricingRequest = new ResortPriceRequest();
        List<String> propertyIds = new ArrayList<String>();
        propertyIds.add("f8d6a944-7816-412f-a39a-9a63aad26833");
        pricingRequest.setPropertyIds(propertyIds);
        pricingRequest.setCustomerId(123456);
        pricingRequest.setProgramId("def64735-d34f-4daa-afd3-bcab36a318f0");
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        List<ResortPriceResponse> response = resortPriceService.getResortPrices(pricingRequest);

        assertEquals("f8d6a944-7816-412f-a39a-9a63aad26833", response.get(0).getPropertyId());
        assertEquals(31.91, response.get(0).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());
        assertFalse(response.get(0).isComp());
        assertEquals(99999.99, response.get(0).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(9249.37, response.get(0).getPrice().getDiscountedAveragePrice(), 0.1);

    }

    /**
     * Test getResortPrices by passing segment id as program id.
     */
    @Test
    public void getResortPricesSegmentTest() {

        when(pricingDao.getRoomPrices(Mockito.any())).thenReturn(getRoomAvailability());
        List<RoomProgram> programList = new ArrayList<>();
        RoomProgram program = new RoomProgram();
        program.setId("def64735-d34f-4daa-afd3-bcab36a318f0");
        program.setPropertyId("f8d6a944-7816-412f-a39a-9a63aad26833");
        programList.add(program);
        when(programCacheService.getProgramsBySegmentId("7f730ca2-3327-11e9-b210-d663bd873d93"))
                .thenReturn(programList);

        ResortPriceRequest pricingRequest = new ResortPriceRequest();
        List<String> propertyIds = new ArrayList<String>();
        propertyIds.add("f8d6a944-7816-412f-a39a-9a63aad26833");
        pricingRequest.setPropertyIds(propertyIds);
        pricingRequest.setCustomerId(123456);
        pricingRequest.setProgramId("7f730ca2-3327-11e9-b210-d663bd873d93");
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        List<ResortPriceResponse> response = resortPriceService.getResortPrices(pricingRequest);

        assertEquals("f8d6a944-7816-412f-a39a-9a63aad26833", response.get(0).getPropertyId());
        assertEquals(31.91, response.get(0).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());
        assertFalse(response.get(0).isComp());
        assertEquals(99999.99, response.get(0).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(9249.37, response.get(0).getPrice().getDiscountedAveragePrice(), 0.1);

    }

    /**
     * Test getResortPrices for program failure cases.
     */
    @Test
    public void getResortPricesSegmentFailuresTest() {

//        when(programCacheService.getProgramsBySegmentId(Mockito.anyString()))
//                .thenReturn(new ArrayList<>());
        when(programCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(null);

        ResortPriceRequest pricingRequest = new ResortPriceRequest();
        pricingRequest.setCustomerId(123456);
        pricingRequest.setProgramId("def64735-d34f-4daa-afd3-bcab36a318f0");

        try {
            resortPriceService.getResortPrices(pricingRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_available>[ Offer is not available or invalid ]", ex.getMessage());
        }

        when(programService.isProgramApplicable(Mockito.any())).thenReturn(false);
        RoomProgram program = new RoomProgram();
        program.setId("def64735-d34f-4daa-afd3-bcab36a318f0");
        program.setPropertyId("f8d6a944-7816-412f-a39a-9a63aad26833");
        when(programCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);

        try {
            resortPriceService.getResortPrices(pricingRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }
    }
}
