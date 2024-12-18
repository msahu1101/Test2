package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.GroupSearchDAO;
import com.mgm.services.booking.room.dao.ProgramContentDAO;
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.AuroraError;
import com.mgm.services.booking.room.model.content.CuratedOfferResponse;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.request.CustomerOffersV3Request;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.RoomProgramDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.response.CustomerOfferV3Response;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.model.response.PerpetaulProgram;
import com.mgm.services.booking.room.model.response.RoomOfferDetails;
import com.mgm.services.booking.room.model.response.RoomProgram;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.model.response.RoomSegmentResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.cache.PropertyContentCacheService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.SystemException;
import com.mgmresorts.aurora.service.EAuroraException;

/**
 * Unit test class for service methods in RoomProgramService.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RoomProgramServiceImplTest extends BaseRoomBookingTest {

    @Mock
    RoomProgramDAO roomProgramDao;

    @Mock
    RoomProgramCacheService roomProgramCacheService;

    @Mock
    private ProgramContentDAO programContentDAO;
    
    @Mock
    private PropertyContentCacheService propertyCacheService;
    
    @Mock
    SecretsProperties secretProps;
    
    @Mock
    ApplicationProperties appProps;
    
    @Mock
    AuroraProperties auroraProperties;
    
    @Mock
    GroupSearchDAO groupSearchDAO;

    @Mock
    AcrsProperties acrsProperties;
   
    @Mock
    ReferenceDataDAOHelper referenceDataDAOHelper;
    
    @Mock
    ServiceConversionHelper serviceConversionHelper;
    
    @Mock
    AuroraError auroraError;
   
    @InjectMocks
    RoomProgramServiceImpl roomProgramServiceImpl;

    private List<RoomProgram> getRoomPrograms() {
        File file = new File(getClass().getResource("/room-offers.json").getPath());

        return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, RoomProgram.class));
    }

    private void setMockProgramCache() {
        com.mgm.services.booking.room.model.phoenix.RoomProgram program = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        program.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        program.setTravelPeriodStart(getFutureDate(10));
        program.setTravelPeriodEnd(getFutureDate(12));
        program.setBookBy(getFutureDate(5));
        program.setActiveFlag(true);
        when(roomProgramCacheService.getRoomProgram("def64735-d34f-4daa-afd3-bcab36a318f0")).thenReturn(program);
        com.mgm.services.booking.room.model.phoenix.RoomProgram program1 = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        program1.setPropertyId("a689885f-cba2-48e8-b8e0-1dff096b8835");
        program1.setTravelPeriodStart(getFutureDate(12));
        program1.setTravelPeriodEnd(getFutureDate(15));
        program1.setBookBy(getFutureDate(7));
        program1.setActiveFlag(true);
        when(roomProgramCacheService.getRoomProgram("b10a48ff-19cc-45c0-95a6-db4a436789e5")).thenReturn(program1);
    }

    /**
     * Test getRoomOffers when there's no property ids.
     */
    @Test
    public void getRoomOffersNoPropertiesTest() {
        // Set data for the roomProgramDao mock
        when(roomProgramDao.getRoomOffers(Mockito.any())).thenReturn(getRoomPrograms());
        setMockProgramCache();

        RoomProgramRequest offersRequest = new RoomProgramRequest();

        List<RoomProgram> response = roomProgramServiceImpl.getRoomOffers(offersRequest);

        assertEquals(4, response.size());

        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0", response.get(0).getId());
        assertEquals("PROGRAM", response.get(0).getType().toString());
        assertTrue(DateUtils.isSameDay(getFutureDate(10), response.get(0).getStartDate()));
        assertTrue(DateUtils.isSameDay(getFutureDate(12), response.get(0).getEndDate()));
        assertTrue(DateUtils.isSameDay(getFutureDate(5), response.get(0).getBookByDate()));

        assertEquals("075f9f68-1d1b-41a6-9bb9-23f7f872d904", response.get(1).getId());
        assertEquals("SEGMENT", response.get(1).getType().toString());
        assertNull(response.get(1).getBookByDate());
        assertNull(response.get(1).getStartDate());
        assertNull(response.get(1).getEndDate());

    }

    /**
     * Test getRoomOffers when there's property ids to filter.
     */
    @Test
    public void getRoomOffersWithPropertiesTest() {
        // Set data for the roomProgramDao mock
        when(roomProgramDao.getRoomOffers(Mockito.any())).thenReturn(getRoomPrograms());
        setMockProgramCache();

        RoomProgramRequest offersRequest = new RoomProgramRequest();
        List<String> propertyIds = new ArrayList<String>();
        propertyIds.add("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        propertyIds.add("a689885f-cba2-48e8-b8e0-1dff096b8835");
        offersRequest.setPropertyIds(propertyIds);

        List<RoomProgram> response = roomProgramServiceImpl.getRoomOffers(offersRequest);
        assertEquals(2, response.size());

        assertEquals("def64735-d34f-4daa-afd3-bcab36a318f0", response.get(0).getId());
        assertEquals("PROGRAM", response.get(0).getType().toString());
        assertTrue(DateUtils.isSameDay(getFutureDate(10), response.get(0).getStartDate()));
        assertTrue(DateUtils.isSameDay(getFutureDate(12), response.get(0).getEndDate()));
        assertTrue(DateUtils.isSameDay(getFutureDate(5), response.get(0).getBookByDate()));

        assertEquals("b10a48ff-19cc-45c0-95a6-db4a436789e5", response.get(1).getId());
        assertEquals("PROGRAM", response.get(1).getType().toString());
        assertTrue(DateUtils.isSameDay(getFutureDate(12), response.get(1).getStartDate()));
        assertTrue(DateUtils.isSameDay(getFutureDate(15), response.get(1).getEndDate()));
        assertTrue(DateUtils.isSameDay(getFutureDate(7), response.get(1).getBookByDate()));

    }

    @Test
    public void test_getProgram_GSE_Cache() {

        RoomProgramV2Request request = new RoomProgramV2Request();
        List<String> programIds = new ArrayList<>();
        programIds.add("74fd80f9-5831-4038-8949-9abacf39dc24");
        request.setProgramIds(programIds);
        request.setStartDate("2030-01-01");
        request.setEndDate("2031-01-01");

        com.mgm.services.booking.room.model.phoenix.RoomProgram cachedProgram = getProgram();
        cachedProgram.setId(programIds.get(0));

        when(roomProgramCacheService.getRoomProgram("74fd80f9-5831-4038-8949-9abacf39dc24")).thenReturn(cachedProgram);

        List<RoomOfferDetails> program = roomProgramServiceImpl.getProgram(request);
        assertNotNull(program);
        assertEquals(program.get(0).getId(), programIds.get(0));

    }

    @Test
    public void test_getProgram_Group_search() {

        final RoomProgramV2Request request = new RoomProgramV2Request();
        final List<String> programIds = new ArrayList<>();
        programIds.add(ACRSConversionUtil.createGroupCodeGuid("GR1", "MV021"));
        request.setProgramIds(programIds);
        request.setStartDate("2030-01-01");
        request.setEndDate("2031-01-01");
        request.setSource("ice");

        final List<GroupSearchV2Response> groupResponses = new ArrayList<>();
        final GroupSearchV2Response groupResponse = new GroupSearchV2Response();
        groupResponse.setId(programIds.get(0));
        groupResponses.add(groupResponse);

        when(roomProgramCacheService.getRoomProgram(anyString())).thenReturn(null);
        when(groupSearchDAO.searchGroup(any(GroupSearchV2Request.class))).thenReturn(groupResponses);

        List<RoomOfferDetails> program = roomProgramServiceImpl.getProgram(request);
        assertNotNull(program);
        assertEquals(program.get(0).getId(), programIds.get(0));
    }

    @Test
    public void test_getProgram_Rateplan_search() {

        final RoomProgramV2Request request = new RoomProgramV2Request();
        final List<String> programIds = new ArrayList<>();
        programIds.add(ACRSConversionUtil.createRatePlanCodeGuid("TBBAR", "MV021"));
        request.setProgramIds(programIds);
        request.setStartDate("2030-01-01");
        request.setEndDate("2031-01-01");

        RoomOfferDetails program = new RoomOfferDetails();
        program.setId(programIds.get(0));

        when(roomProgramCacheService.getRoomProgram(anyString())).thenReturn(null);
        when(roomProgramDao.getRatePlanById(any())).thenReturn(Arrays.asList(program));

        List<RoomOfferDetails> responsePrograms = roomProgramServiceImpl.getProgram(request);
        assertNotNull(responsePrograms);
        assertEquals(responsePrograms.get(0).getId(), programIds.get(0));
    }

    private com.mgm.services.booking.room.model.phoenix.RoomProgram getProgram() {
        com.mgm.services.booking.room.model.phoenix.RoomProgram patronProgram = new com.mgm.services.booking.room.model.phoenix.RoomProgram();
        patronProgram.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        patronProgram.setTravelPeriodStart(getFutureDate(10));
        patronProgram.setTravelPeriodEnd(getFutureDate(12));
        patronProgram.setBookBy(getFutureDate(5));
        patronProgram.setActiveFlag(true);
        return patronProgram;
    }
    
    @Test
    public void test_getCustomerOffers_whenUserIsNotLoggedIn_expectOnlyCuratedNonMemberPrograms() {
        
        when(programContentDAO.getCuratedHotelOffers(anyString())).thenReturn(convert("/curated-offers-list.json", CuratedOfferResponse.class));
        
        CustomerOffersV3Request request = new CustomerOffersV3Request();
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        
        CustomerOfferV3Response response = roomProgramServiceImpl.getCustomerOffers(request);
        
        assertEquals(3, response.getOffers().size());
    }
    
    @Test
    public void test_getCustomerOffers_whenUserIsNotLoggedInAndRegionIsSpecified_expectOnlyCuratedNonMemberRegionPrograms() {
        
        when(programContentDAO.getCuratedHotelOffers(anyString())).thenReturn(convert("/curated-offers-list.json", CuratedOfferResponse.class));
        
        CustomerOffersV3Request request = new CustomerOffersV3Request();
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setRegion("lv");
        
        CustomerOfferV3Response response = roomProgramServiceImpl.getCustomerOffers(request);
        
        assertEquals(1, response.getOffers().size());
        
        request.setRegion("nonlv");
        
        response = roomProgramServiceImpl.getCustomerOffers(request);
        
        assertEquals(2, response.getOffers().size());
    }
    
    private void setupRoomProgramsBasedOnAttributes() {
    	when(appProps.getEnableNewSegmentsKey()).thenReturn("rbs-enableNewSegments");
    	when(appProps.getRbsEnv()).thenReturn("q");
        when(secretProps.getSecretValue(Mockito.anyString())).thenReturn("false");
        Property property1 = new Property("66964e2b-2550-4476-84c3-1a4c0c5c067f", "MGM Grand", null, null, "LV", "03");
        Property property2 = new Property("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", "NYNY", null, null, "LV", "05");
        Property property3 = new Property("4a65a92a-962b-433e-841c-37e18dc5d68d", "Bellagio", null, null, "LV", "03");
        Property property4 = new Property("0990fdce-7fc8-41b1-b8b6-9a25dce3db55", "MGM Grand", null, null, "LV", "06");
        Property property5 = new Property("773000cc-468a-4d86-a38f-7ae78ecfa6aa", "Borgata", null, null, "LV", "09");
        when(propertyCacheService.getPropertyByRegion(Mockito.any())).thenReturn(Arrays.asList(property1, property2, property3, property4, property5));
        when(roomProgramDao.getRoomPrograms(Mockito.any())).thenAnswer(new Answer<RoomProgramsResponseDTO>() {
            @Override
            public RoomProgramsResponseDTO answer(InvocationOnMock invocation) {
                RoomProgramsRequestDTO request = (RoomProgramsRequestDTO) invocation.getArgument(0);
                RoomProgramsResponseDTO response = convert("/customer-room-programs-all.json",
                        RoomProgramsResponseDTO.class);
                if (!request.isPerpetualPricing()) {
                    response.setPoPrograms(new ArrayList<RoomProgramDTO>());
                } 
                if (!request.getChannel().equalsIgnoreCase("ice")) {
                    response.setIceChannelPrograms(new ArrayList<RoomProgramDTO>());
                }
                return response;
            }
        });
    }
    
    @Test
    public void test_getCustomerOffers_whenUserIsMlifeNonPerpetual_expectOnlyCuratedNonPoPrograms() {
        
        when(programContentDAO.getCuratedHotelOffers(anyString())).thenReturn(convert("/curated-offers-list.json", CuratedOfferResponse.class));
        setupRoomProgramsBasedOnAttributes();
        
        CustomerOffersV3Request request = new CustomerOffersV3Request();
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setChannel("web");
        request.setCustomerId(123456l);
        request.setMlifeNumber("8238382");
        request.setIncludeNonBookableOnline(false);
        
        CustomerOfferV3Response response = roomProgramServiceImpl.getCustomerOffers(request);
        
        // 4 from curated list + 2 from patron programs
        assertEquals(6, response.getOffers().size());
    }
    
    @Test
    public void test_getCustomerOffers_whenUserIsMlifeNonPerpetualFromIce_expectOnlyCuratedNonPoAndIcePrograms() {
        
        when(programContentDAO.getCuratedHotelOffers(anyString())).thenReturn(convert("/curated-offers-list.json", CuratedOfferResponse.class));
        setupRoomProgramsBasedOnAttributes();
        
        CustomerOffersV3Request request = new CustomerOffersV3Request();
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setChannel("ice");
        request.setCustomerId(123456l);
        request.setMlifeNumber("8238382");
        request.setIncludeNonBookableOnline(false);
        
        CustomerOfferV3Response response = roomProgramServiceImpl.getCustomerOffers(request);
        
        // 4 from curated list + 2 from patron programs + 2 ice only program
        assertEquals(8, response.getOffers().size());
    }
    
    @Test
    public void test_getCustomerOffers_whenUserIsMlifePerpetual_expectPoPatronPrograms() {

        when(programContentDAO.getCuratedHotelOffers(anyString())).thenReturn(convert("/curated-offers-list.json", CuratedOfferResponse.class));
        setupRoomProgramsBasedOnAttributes();
        
        CustomerOffersV3Request request = new CustomerOffersV3Request();
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setChannel("web");
        request.setCustomerId(123456l);
        request.setMlifeNumber("8238382");
        request.setPerpetualPricing(true);
        request.setIncludeNonBookableOnline(false);
        
        CustomerOfferV3Response response = roomProgramServiceImpl.getCustomerOffers(request);
        
        // 4 curated + 2 from patron programs + 2 po programs
        assertEquals(8, response.getOffers().size());
    }
    
    @Test
    public void test_getCustomerOffers_whenUserIsMlifePerpetualRequestingOnlyPoRates_expectOnlyPoPrograms() {
        
        setupRoomProgramsBasedOnAttributes();
        
        CustomerOffersV3Request request = new CustomerOffersV3Request();
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setChannel("web");
        request.setCustomerId(123456l);
        request.setMlifeNumber("8238382");
        request.setPerpetualPricing(true);
        request.setOnlyPoPrograms(true);
        
        CustomerOfferV3Response response = roomProgramServiceImpl.getCustomerOffers(request);
        
        // 2 po programs
        assertEquals(2, response.getOffers().size());
    }
    @Test
    public void test_validateProgramV2() {
    	MockHttpServletRequest request1 = new MockHttpServletRequest();
    	request1.addHeader(ServiceConstant.X_MGM_CHANNEL, "test");
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request1));

    	RoomProgramValidateRequest request = new RoomProgramValidateRequest();

    	request.setPropertyId("f8d6a944-7816-412f-a39a-9a63aad26833");
    	request.setProgramId("03a71184-1723-487f-991e-a2b79ad15ad9");
    	RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();

    	validateResponse.setMyvegas(true); 
    	validateResponse.setProgramId("03a71184-1723-487f-991e-a2b79ad15ad9");
    	validateResponse.setRatePlanTags(Collections.singletonList("test"));
    	when(roomProgramDao.validateProgramV2(request)).thenReturn(validateResponse);
    	when(appProps.getEnableNewSegmentsKey()).thenReturn("rbs-enableNewSegments");
    	when(appProps.getRbsEnv()).thenReturn("q");
    	when(secretProps.getSecretValue(anyString())).thenReturn("false");

    	RoomProgramValidateResponse response = roomProgramServiceImpl.validateProgramV2(request);
    	assertNotNull(response);
    }
    
    @Test
    public void test_getRoomSegment() {
    	RoomSegmentResponse response = new RoomSegmentResponse();
    	String segment = "f622861f-334c-4e52-8715-714e96a3665f";
    	String source = "ice";
    	String programId = "dcbf639c-b6fa-4055-8921-fc4948c81145";
    	Mockito.when(roomProgramDao.getRoomSegment(segment,programId, source)).thenReturn(response);
    	RoomSegmentResponse actualResponse = roomProgramServiceImpl.getRoomSegment(segment,programId, source);
    	assertNotNull(actualResponse);
    	assertNotNull(response);
    }
    
    @Test
    public void test_getRoomSegment_segmentandsource() {
    	RoomSegmentResponse segmentResponse = new RoomSegmentResponse();
    	String segment = "f622861f-334c-4e52-8715-714e96a3665f";
    	String source = "ice";
        Mockito.when(roomProgramDao.getRoomSegment(segment, source, false)).thenReturn(segmentResponse);
    	RoomSegmentResponse actualResponse = roomProgramServiceImpl.getRoomSegment(segment,source);
    	assertNotNull(actualResponse);
    }

    @Test
    public void test_getDefaultPerpetualProgramsV2() {

    	PerpetualProgramRequest request = new PerpetualProgramRequest();

    	List<String> propertyIds = new ArrayList<String>();
    	propertyIds.add("66964e2b-2550-4476-84c3-1a4c0c5c067f");
    	propertyIds.add("a689885f-cba2-48e8-b8e0-1dff096b8835");
    	request.setChannel("web");
    	request.setPropertyIds(propertyIds);
    	List<PerpetaulProgram> expectedPrograms = new ArrayList<>();
    	when(roomProgramDao.getDefaultPerpetualPrograms(request)).thenReturn(expectedPrograms);
    	List<PerpetaulProgram> result = roomProgramServiceImpl.getDefaultPerpetualProgramsV2(request);
    	assertEquals(expectedPrograms, result);
    }

    @Test
    public void test_getDefaultPerpetualProgramsV2WithFunctionalError() {
              PerpetualProgramRequest request = new PerpetualProgramRequest();
              EAuroraException ex = new EAuroraException(new Throwable(), com.mgmresorts.aurora.common.ErrorCode.InvalidChannelId);

    when(roomProgramDao.getDefaultPerpetualPrograms(any())).thenThrow(ex);
        assertThrows(BusinessException.class, () -> {             
            roomProgramServiceImpl.getDefaultPerpetualProgramsV2(request);
        });
    }
    @Test
    public void test_getDefaultPerpetualProgramsV2WithSYSTEM_ERROR() {
              PerpetualProgramRequest request = new PerpetualProgramRequest();
              EAuroraException ex = new EAuroraException(new Throwable(), com.mgmresorts.aurora.common.ErrorCode.ServerUnavailable);

    when(roomProgramDao.getDefaultPerpetualPrograms(any())).thenThrow(ex);
              assertThrows(SystemException.class, () -> {         
            roomProgramServiceImpl.getDefaultPerpetualProgramsV2(request);
        });
       
    }
   
}


