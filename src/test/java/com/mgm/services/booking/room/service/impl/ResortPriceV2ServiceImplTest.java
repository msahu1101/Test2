package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.mgm.services.booking.room.model.AvailabilityStatus;
import org.junit.Assert;
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
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.ResortPriceV2Request;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV2Request;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.service.RoomAvailabilityV2Service;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.helper.ResortPriceServiceHelper;

/**
 * Unit test class for all services in ResortPriceServiceImpl service
 * implementation.
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class ResortPriceV2ServiceImplTest extends BaseRoomBookingTest {

    @Mock
    AuroraProperties auroraProperties;

    @Mock
    RoomPriceDAO pricingDao;
    
    @Mock
    RoomProgramDAO programDao;

    @Mock
    RoomProgramService programService;

    @Mock
    RoomProgramCacheService programCacheService;

    @Mock
    RoomAvailabilityV2Service availabilityV2Service;

    @InjectMocks
    ResortPriceV2ServiceImpl resortPriceV2Service;

    @InjectMocks
    ResortPriceServiceHelper resortPriceServiceHelper;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(resortPriceServiceHelper, "auroraProperties", auroraProperties);
        ReflectionTestUtils.setField(resortPriceServiceHelper, "programCacheService", programCacheService);
        ReflectionTestUtils.setField(resortPriceServiceHelper, "programService", programService);
        ReflectionTestUtils.setField(resortPriceV2Service, "helper", resortPriceServiceHelper);
    }

    private AuroraPricesResponse getRoomAvailability() {
        File file = new File(getClass().getResource("/resort-prices-v2.json").getPath());

        return convert(file, AuroraPricesResponse.class);

    }
    private AuroraPricesResponse getRoomAvailabilityWithGroupBlock() {
        File file = new File(getClass().getResource("/resort-prices-v2-acrs-groupblocks.json").getPath());

        return convert(file, AuroraPricesResponse.class);

    }



    private <T> T getContent(String fileName, Class<T> className) {
        File file = new File(getClass().getResource(fileName).getPath());

        return convert(file, className);

    }

    private List<String> getPropertyIds() {
        String propertyIds = "66964e2b-2550-4476-84c3-1a4c0c5c067f,dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad,"
                + "8bf670c2-3e89-412b-9372-6c87a215e442,44e610ab-c209-4232-8bb4-51f7b9b13a75,"
                + "2159252c-60d3-47db-bbae-b1db6bb15072,13b178b0-8beb-43d5-af25-1738b7267e63,"
                + "6c5cff3f-f01a-4f9b-87ab-8395ae8108db,e0f70eb3-7e27-4c33-8bcd-f30bf3b1103a,"
                + "4a65a92a-962b-433e-841c-37e18dc5d68d,e2704b04-d515-45b0-8afd-4fa1424ff0a8,"
                + "607c07e7-3e31-4e4c-a4e1-f55dca66fea2,"
                + "1f3ed672-3f8f-44d8-9215-81da3c845d83,f8d6a944-7816-412f-a39a-9a63aad26833,"
                + "0990fdce-7fc8-41b1-b8b6-9a25dce3db55,"
                + "160cdf9b-ccdc-40ce-b0ac-1a58b69dcf4f,a0be1590-65c2-4e4d-b208-94ea5cac658f,"
                + "bee81f88-286d-43dd-91b5-3917d9d62a68,773000cc-468a-4d86-a38f-7ae78ecfa6aa,"
                + "40b61feb-750a-45df-ae68-e23e6272b16b";
        return Arrays.asList(propertyIds.split(","));
    }

    /**
     * Test getResortPrices for default return values.
     */
    @Test
    public void test_getResortPrices_withBasicParams_returnsAllResortsAvailability() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getRoomAvailability());
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());

        ResortPriceV2Request pricingRequest = new ResortPriceV2Request();
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        assertEquals(getPropertyIds().size(), resortPriceV2Service.getResortPrices(pricingRequest).size());
    }

    /**
     * Test getResortPrices by passing customerId.
     */
    @Test
    public void test_getResortPrices_withCusomterId_returnsAllResortsAvailability() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getRoomAvailability());
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());

        ResortPriceV2Request pricingRequest = new ResortPriceV2Request();
        pricingRequest.setCustomerId(123456);
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        List<ResortPriceResponse> response = resortPriceV2Service.getResortPrices(pricingRequest);

        assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.get(1).getPropertyId());
        assertEquals(37.0, response.get(1).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(1).getStatus().toString());
        assertFalse(response.get(1).isComp());
        assertEquals(9999.0, response.get(1).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(8499.15, response.get(1).getPrice().getDiscountedAveragePrice(), 0.1);
    }

    /**
     * Test createAvailabilityRequests with property ids filtered.
     */
    @Test
    public void test_createAvailabilityRequestsTest_withPerpetualOffersPropertyIds_returnsFilteredAvailability() {

        String propertyIds = "bee81f88-286d-43dd-91b5-3917d9d62a68, a0be1590-65c2-4e4d-b208-94ea5cac658f, 160cdf9b-ccdc-40ce-b0ac-1a58b69dcf4f";
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());

        ResortPriceV2Request pricingRequest = new ResortPriceV2Request();
        List<RoomAvailabilityV2Request> request = resortPriceV2Service.createAvailabilityRequests(pricingRequest);

        assertEquals("No of pricing requests should be " + getPropertyIds().size(), getPropertyIds().size(), request.size());

        pricingRequest.setPropertyIds(Arrays.asList(propertyIds.split(",")));
        request = resortPriceV2Service.createAvailabilityRequests(pricingRequest);

        assertEquals("No of pricing requests should be " + propertyIds.split(",").length, propertyIds.split(",").length, request.size());
    }

    /**
     * Test getResortPrices with program use case.
     */
    @Test
    public void test_getResortPrices_withValidateProgramV2True_returnsAvailabilty() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getRoomAvailability());
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());
        String source = "mgmri";
        when(programDao.findProgramsIfSegment("ad90965f-c9c5-4dee-b45f-263982d7c8cd", source))
                .thenReturn(new ArrayList<>());
        when(programService.validateProgramV2(Mockito.any()))
                .thenReturn(getContent("/room-program-validate-response.json", RoomProgramValidateResponse.class));
        RoomProgram program = new RoomProgram();
        program.setId("ad90965f-c9c5-4dee-b45f-263982d7c8cd");
        program.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
        when(programCacheService.getRoomProgram("ad90965f-c9c5-4dee-b45f-263982d7c8cd")).thenReturn(program);

        ResortPriceV2Request pricingRequest = new ResortPriceV2Request();
        pricingRequest.setSource(source);
        pricingRequest.setCustomerId(123456);
        pricingRequest.setProgramId("ad90965f-c9c5-4dee-b45f-263982d7c8cd");
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));
        List<ResortPriceResponse> response = resortPriceV2Service.getResortPrices(pricingRequest);

        assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.get(1).getPropertyId());
        assertEquals(37.0, response.get(1).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(1).getStatus().toString());
        assertFalse(response.get(1).isComp());
        assertEquals(9999.0, response.get(1).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(8499.15, response.get(1).getPrice().getDiscountedAveragePrice(), 0.1);
    }

    /**
     * Test getResortPrices with program id part of segment use case.
     */
    @Test
    public void test_getResortPrices_withProgramIdSegmentId_returnsAvailability() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getRoomAvailability());
        when(auroraProperties.getPropertyIds()).thenReturn(getPropertyIds());
        String source = "mgmri";
        when(programDao.findProgramsIfSegment("ad90965f-c9c5-4dee-b45f-263982d7c8cd", source))
                .thenReturn(new ArrayList<>());
        when(programService.validateProgramV2(Mockito.any()))
        .thenReturn(getContent("/room-program-validate-response.json", RoomProgramValidateResponse.class));
        RoomProgram program = new RoomProgram();
        program.setId("ad90965f-c9c5-4dee-b45f-263982d7c8cd");
        program.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
        program.setSegmentId("9b7adb7c-8b92-4726-a773-77b6bc6fcf6a");
        when(programCacheService.getRoomProgram("ad90965f-c9c5-4dee-b45f-263982d7c8cd")).thenReturn(program);
        
        ResortPriceV2Request pricingRequest = new ResortPriceV2Request();
        pricingRequest.setSource(source);
        pricingRequest.setCustomerId(123456);
        pricingRequest.setProgramId("ad90965f-c9c5-4dee-b45f-263982d7c8cd");
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        List<ResortPriceResponse> response = resortPriceV2Service.getResortPrices(pricingRequest);

        assertEquals("773000cc-468a-4d86-a38f-7ae78ecfa6aa", response.get(0).getPropertyId());
        assertEquals("SOLDOUT", response.get(0).getStatus().toString());
        assertEquals(0, response.get(0).getResortFee(), 0.1);
        assertFalse(response.get(0).isComp());

        assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.get(1).getPropertyId());
        assertEquals(37.0, response.get(1).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(1).getStatus().toString());
        assertFalse(response.get(1).isComp());
        assertEquals(9999.0, response.get(1).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(8499.15, response.get(1).getPrice().getDiscountedAveragePrice(), 0.1);

    }

    /**
     * Test getResortPrices by passing segment id as program id and cache available.
     */
    @Test
    public void test_getResortPrices_withProgramCache_returnsAvailability() {

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getRoomAvailability());
        List<RoomProgramBasic> programList = new ArrayList<>();
        RoomProgramBasic programBasic = new RoomProgramBasic("ad90965f-c9c5-4dee-b45f-263982d7c8cd", "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", null, false, false, null, null,  null, null, null, null);
        programList.add(programBasic);
        String source = "mgmri";
        when(programDao.findProgramsIfSegment("9b7adb7c-8b92-4726-a773-77b6bc6fcf6a", source))
                .thenReturn(programList);

        ResortPriceV2Request pricingRequest = new ResortPriceV2Request();
        pricingRequest.setSource(source);
        pricingRequest.setCustomerId(123456);
        pricingRequest.setProgramId("9b7adb7c-8b92-4726-a773-77b6bc6fcf6a");
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(3));

        List<ResortPriceResponse> response = resortPriceV2Service.getResortPrices(pricingRequest);

        assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.get(0).getPropertyId());
        assertEquals(37.0, response.get(0).getResortFee(), 0.1);
        assertEquals("AVAILABLE", response.get(0).getStatus().toString());
        assertFalse(response.get(0).isComp());
        assertEquals(9999.0, response.get(0).getPrice().getBaseAveragePrice(), 0.1);
        assertEquals(8499.15, response.get(0).getPrice().getDiscountedAveragePrice(), 0.1);

    }

    /**
     * Test getResortPrices for program eligibility check failure cases.
     */
    @Test
    public void test_getResortPrices_withValidateProgramV2False_returnsOfferExceptions() {

        when(programCacheService.getRoomProgram("ad90965f-c9c5-4dee-b45f-263982d7c8cd")).thenReturn(null);

        ResortPriceV2Request pricingRequest = new ResortPriceV2Request();
        pricingRequest.setCustomerId(123456);
        pricingRequest.setProgramId("ad90965f-c9c5-4dee-b45f-263982d7c8cd");

        try {
            resortPriceV2Service.getResortPrices(pricingRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_available>[ Offer is not available or invalid ]", ex.getMessage());
        }

        when(programService.validateProgramV2(Mockito.any()))
        .thenReturn(getContent("/room-program-validate-response-false.json", RoomProgramValidateResponse.class));
        RoomProgram program = new RoomProgram();
        program.setId("ad90965f-c9c5-4dee-b45f-263982d7c8cd");
        program.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
        when(programCacheService.getRoomProgram("ad90965f-c9c5-4dee-b45f-263982d7c8cd")).thenReturn(program);

        try {
            resortPriceV2Service.getResortPrices(pricingRequest);
        } catch (Exception ex) {
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }
    }

    /**
     * Test getResortPrices for program eligibility check failure cases.
     */
    @Test
    public void test_getResortPrices_withGroupBlock() {
        ResortPriceV2Request pricingRequest = new ResortPriceV2Request();
        pricingRequest.setGroupCode("ZAERO614P1HDE");
        LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        pricingRequest.setCheckInDate(checkInDate);
        pricingRequest.setCheckOutDate(checkInDate.plusDays(1));
        List<RoomProgramBasic> programList = new ArrayList<>() ;
        RoomProgramBasic program = new RoomProgramBasic();
        program.setPropertyId("e0f70eb3-7e27-4c33-8bcd-f30bf3b1103a");
        program.setProgramId("GRPCD-v-ZAERO614P1HDE-d-PROP-v-MV275");
        program.setRatePlanCode("ZAERO614P1HDE");
        programList.add(program);
        when(programDao.findProgramsByGroupCode(pricingRequest.getGroupCode(),pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate(), pricingRequest.getSource())).thenReturn(programList);
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getRoomAvailabilityWithGroupBlock());
        List<ResortPriceResponse> response = resortPriceV2Service.getResortPrices(pricingRequest);
        Assert.assertNotNull(response);
        assertEquals(1,response.size());
        assertEquals(AvailabilityStatus.AVAILABLE, response.get(0).getStatus());
        assertEquals(response.get(0).getProgramId(), program.getProgramId());

    }
}
