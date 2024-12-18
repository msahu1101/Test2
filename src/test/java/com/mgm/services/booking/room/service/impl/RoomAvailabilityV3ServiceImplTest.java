package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.dao.ProductInventoryDAO;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.inventory.InventoryDetails;
import com.mgm.services.booking.room.model.inventory.InventoryGetRes;
import com.mgm.services.booking.room.model.inventory.InventoryObj;
import com.mgm.services.booking.room.model.inventory.Level;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV3Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.model.response.PricingModes;
import com.mgm.services.booking.room.model.response.RatePlanV2Response;
import com.mgm.services.booking.room.model.response.RoomAvailabilityCombinedResponse;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.model.response.ShoppingFlow;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.helper.RoomAvailabilityServiceHelper;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import com.mgm.services.common.exception.BusinessException;

@RunWith(MockitoJUnitRunner.class)
public class RoomAvailabilityV3ServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private RoomPriceDAO pricingDao;
    
    @Mock
    private RoomProgramDAO programDao;

    @Mock
    private RoomProgramService programService;

    @Mock
    private RoomProgramCacheService programCacheService;

    @Mock
    private ServiceConversionHelper serviceConversionHelper;

    @InjectMocks
    RoomAvailabilityV3ServiceImpl availabilityServiceImpl;

    @Mock
    ApplicationProperties applicationProperties;
    
    @Mock
    ProductInventoryDAO productInventoryDAO;
    
    @Mock
    ComponentDAO componentDAO;

    @Before
    public void setup() {
    	MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(availabilityServiceImpl, "availabilityServiceHelper",
                new RoomAvailabilityServiceHelper());
    }

    private AuroraPricesResponse getAuroraPrices(String filePath) {
        File file = new File(getClass().getResource(filePath).getPath());

        return convert(file, AuroraPricesResponse.class);
    }

    @Test
    public void getRoomAvailability_givenPerpetualUserWithNoSpecifiedProgramAndNoPOPricing_expectSingleRatePlanResponse() {

        // given perpetual pricing request
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setPerpetualPricing(true);
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        // mock dao pricing response
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-prices-v2.json"));

        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);

        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        
        assertEquals(1, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.PERPETUAL, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.BEST_AVAILABLE, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        assertEquals(26, returned.getRatePlans().get(0).getRooms().size());

        returned.getRatePlans().get(0).getRooms().forEach(response -> {
            if (response.getRoomTypeId().equals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd")) {
                assertEquals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertEquals(60089.94, response.getPrice().getBaseSubtotal(), 0.1);
                assertEquals(10014.99, response.getPrice().getBaseAveragePrice(), 0.1);
                assertEquals(44069.96, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(7344.99, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }

            if (response.getRoomTypeId().equals("b2135b7f-7172-4d53-b39d-217de6f5c970")) {
                assertEquals("b2135b7f-7172-4d53-b39d-217de6f5c970", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertEquals(600089.94, response.getPrice().getBaseSubtotal(), 0.1);
                assertEquals(100014.99, response.getPrice().getBaseAveragePrice(), 0.1);
                assertEquals(120029.99, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(20005, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }
        });
    }

    @Test
    public void getRoomAvailability_givenRegularUserWithNoSpecifiedProgram_expectMultipleRatePlansResponse() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setIncludeSoldOutRooms(true);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        // mock dao pricing response
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-rate-plans-v2.json"));

        // invoke service
        RoomAvailabilityCombinedResponse combinedResponse = availabilityServiceImpl.getRoomAvailability(request);

        // expect rate plans and not availability
        assertNotNull(combinedResponse.getRatePlans());
        assertEquals(5, combinedResponse.getRatePlans().size());
        assertEquals(ShoppingFlow.RATE_PLANS, combinedResponse.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.BEST_AVAILABLE, combinedResponse.getMetadata().getPricingMode());

        // Perform some field level assertions
        List<RatePlanV2Response> response = combinedResponse.getRatePlans();
        validateEachProgram(response, "55204b8f-6d58-4dab-894c-e79dda626c85", 0);
        validateEachProgram(response, "81c0ce15-7c42-4ee0-a031-5b02d7c5d15f", 1);
        validateEachProgram(response, "d8ec7e74-1d53-4ca6-84f1-3ecd6619c021", 2);
        validateEachProgram(response, "1dc21cfc-9f40-45d1-bdff-96c5107b5261", 3);
        validateEachProgram(response, "ecd2c44d-d537-4087-9212-0fca6296e6c0", 4);
    }

    private void validateEachProgram(List<RatePlanV2Response> response, String programId, int index) {
        assertEquals(programId, response.get(index).getProgramId());
        assertNotNull(response.get(0).getRooms());
        assertEquals(61, response.get(0).getRooms().size());
        response.get(index).getRooms().forEach(room -> {
            if (!room.isUnavailable()) {
                assertEquals(6, room.getPrice().getItemized().size());
            } else {
                assertTrue(room.isUnavailable());
            }
        });
    }

    @Test
    public void getRoomAvailability_givenPerpetualUserWithSpecifiedProgram_expectSingleRatePlanResponse() {

        // given perpetual pricing request with program specified
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setPerpetualPricing(true);
        request.setIncludeDefaultRatePlans(true);
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any()))
                .thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(true);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

        //F1-adjustments
        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");
        //program.setTags(new String[]{"F1Package", "F12023PGS"});

        //when(appProps.getF1PackageTag()).thenReturn("F1Package");
        //when(appProps.getValidF1ProductCodes()).thenReturn(new ArrayList<String>(Arrays.asList(new String[]{"F12023BGS", "F12023HGS", "F12023PGS"})));
        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);

        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);
        
//        when(programDao.isProgramPO(Mockito.anyString())).thenReturn(false);

        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(1, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.PERPETUAL, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        assertEquals(26, returned.getRatePlans().get(0).getRooms().size());

        returned.getRatePlans().get(0).getRooms().forEach(response -> {
            if (response.getRoomTypeId().equals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd")) {
                assertEquals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertEquals(60089.94, response.getPrice().getBaseSubtotal(), 0.1);
                assertEquals(10014.99, response.getPrice().getBaseAveragePrice(), 0.1);
                assertEquals(42089.96, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(7014.99, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }

            if (response.getRoomTypeId().equals("b2135b7f-7172-4d53-b39d-217de6f5c970")) {
                assertEquals("b2135b7f-7172-4d53-b39d-217de6f5c970", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertEquals(600089.94, response.getPrice().getBaseSubtotal(), 0.1);
                assertEquals(100014.99, response.getPrice().getBaseAveragePrice(), 0.1);
                assertEquals(240059.98, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(40010, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }
        });
    }

    @Test
    public void getRoomAvailability_givenRegularUserWithSpecifiedProgramDontIncludeDefaultRatePlans_expectSingleRatePlanResponse() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setIncludeSoldOutRooms(true);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        request.setIncludeDefaultRatePlans(false);

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any()))
                .thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(true);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

        //F1-adjustments
        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");
        //program.setTags(new String[]{"F1Package", "F12023PGS"});

        //when(appProps.getF1PackageTag()).thenReturn("F1Package");
        //when(appProps.getValidF1ProductCodes()).thenReturn(new ArrayList<String>(Arrays.asList(new String[]{"F12023BGS", "F12023HGS", "F12023PGS"})));
        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);
        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);

        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(1, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.RATE_PLANS, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        assertEquals(61, returned.getRatePlans().get(0).getRooms().size());

        returned.getRatePlans().get(0).getRooms().forEach(response -> {
            if (response.getRoomTypeId().equals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd")) {
                assertEquals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertEquals(60089.94, response.getPrice().getBaseSubtotal(), 0.1);
                assertEquals(10014.99, response.getPrice().getBaseAveragePrice(), 0.1);
                assertEquals(42089.96, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(7014.99, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }

            if (response.getRoomTypeId().equals("b2135b7f-7172-4d53-b39d-217de6f5c970")) {
                assertEquals("b2135b7f-7172-4d53-b39d-217de6f5c970", response.getRoomTypeId());
                assertEquals(37, response.getResortFee(), 0.1);
                assertEquals(600089.94, response.getPrice().getBaseSubtotal(), 0.1);
                assertEquals(100014.99, response.getPrice().getBaseAveragePrice(), 0.1);
                assertEquals(240059.98, response.getPrice().getDiscountedSubtotal(), 0.1);
                assertEquals(40010, response.getPrice().getDiscountedAveragePrice(), 0.1);
            }
        });
    }

    @Test
    public void getRoomAvailability_givenRegularUserWithSpecifiedProgramAndIncludeDefaultRatePlans_expectMultipleRatePlansResponse() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setIncludeSoldOutRooms(true);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        request.setIncludeDefaultRatePlans(true);

        // mock dao pricing response and program validation calls
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(true);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {
                AuroraPriceRequest request = (AuroraPriceRequest) invocation.getArgument(0);
                if (StringUtils.isNotEmpty(request.getProgramId())) {
                    return getAuroraPrices("/room-prices-v2-with-programId.json");
                } else {
                    return getAuroraPrices("/room-rate-plans-v2.json");
                }
            }

        });

        //F1-adjustments
        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");
        //program.setTags(new String[]{"F1Package", "F12023PGS"});

        //when(appProps.getF1PackageTag()).thenReturn("F1Package");
        //when(appProps.getValidF1ProductCodes()).thenReturn(new ArrayList<String>(Arrays.asList(new String[]{"F12023BGS", "F12023HGS", "F12023PGS"})));
        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);

        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);

        // expect rate plans and not availability
        assertNotNull(returned.getRatePlans());
        assertEquals(6, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.RATE_PLANS, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        List<RatePlanV2Response> response = returned.getRatePlans();

        validateEachProgram(response, "7b36149a-0ab8-442b-8651-cfa5f800349e", 0);
        validateEachProgram(response, "55204b8f-6d58-4dab-894c-e79dda626c85", 1);
        validateEachProgram(response, "81c0ce15-7c42-4ee0-a031-5b02d7c5d15f", 2);
        validateEachProgram(response, "d8ec7e74-1d53-4ca6-84f1-3ecd6619c021", 3);
        validateEachProgram(response, "1dc21cfc-9f40-45d1-bdff-96c5107b5261", 4);
        validateEachProgram(response, "ecd2c44d-d537-4087-9212-0fca6296e6c0", 5);

        // repeat the test with program already part of rate plans
        request.setProgramId("d8ec7e74-1d53-4ca6-84f1-3ecd6619c021");
        validateResponse.setEligible(true);
        validateResponse.setProgramId("d8ec7e74-1d53-4ca6-84f1-3ecd6619c021");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

        // invoke service
        returned = availabilityServiceImpl.getRoomAvailability(request);

        // expect rate plans and not availability
        assertNotNull(returned.getRatePlans());
        assertEquals(5, returned.getRatePlans().size());

        // Perform some field level assertions
        response = returned.getRatePlans();

        validateEachProgram(response, "d8ec7e74-1d53-4ca6-84f1-3ecd6619c021", 0);
        validateEachProgram(response, "55204b8f-6d58-4dab-894c-e79dda626c85", 1);
        validateEachProgram(response, "81c0ce15-7c42-4ee0-a031-5b02d7c5d15f", 2);
        validateEachProgram(response, "1dc21cfc-9f40-45d1-bdff-96c5107b5261", 3);
        validateEachProgram(response, "ecd2c44d-d537-4087-9212-0fca6296e6c0", 4);

    }

    @Test(
            expected = BusinessException.class)
    public void getRoomAvailability_givenIneligibleProgram_expectErrorResponse() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setIncludeSoldOutRooms(true);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        request.setIncludeDefaultRatePlans(true);

        // mock dao pricing response and program validation calls
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(false);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

        availabilityServiceImpl.getRoomAvailability(request);
    }

    @Test(
            expected = BusinessException.class)
    public void getRoomAvailability_givenIneligiblePromoCode_expectErrorResponse() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setIncludeSoldOutRooms(true);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setPromoCode("DISC");
        request.setIncludeDefaultRatePlans(true);

        // mock dao pricing response and program validation calls
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(false);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);
        availabilityServiceImpl.getRoomAvailability(request);
    }
    
    @Test
    public void getRoomAvailability_givenPerpetualUserWithSpecifiedProgramWithPartialAvailability_expectNoRoomsInResponse() {

        // given perpetual pricing request with program specified
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setPerpetualPricing(true);
        request.setIncludeDefaultRatePlans(true);
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any()))
                .thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {
                
                AuroraPriceRequest request = (AuroraPriceRequest)invocation.getArgument(0);
                if (request.isProgramRate()){
                    return getAuroraPrices("/room-prices-v2-partial-availability.json");
                }
                else {
                    return getAuroraPrices("/room-prices-v2-mixed-programs.json");
                }
                
            }
            
        });
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(true);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);
        
        when(programDao.isProgramPO(Mockito.anyString())).thenReturn(false);

        //F1-adjustments
        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");
        //program.setTags(new String[]{"F1Package", "F12023PGS"});

        //when(appProps.getF1PackageTag()).thenReturn("F1Package");
        //when(appProps.getValidF1ProductCodes()).thenReturn(new ArrayList<String>(Arrays.asList(new String[]{"F12023BGS", "F12023HGS", "F12023PGS"})));
        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);

        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);
        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(1,returned.getRatePlans().size());
        assertEquals(ShoppingFlow.PERPETUAL, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());
    }
    
    @Test
    public void getRoomAvailability_givenRegularUserWithMixedProgramPricingAndNoIncludeDefaultRatePlans_expectNoAvailability() {

        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setIncludeDefaultRatePlans(false);
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any()))
                .thenReturn(getAuroraPrices("/room-prices-v2-with-programId.json"));
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {
                
                AuroraPriceRequest request = (AuroraPriceRequest)invocation.getArgument(0);
                if (request.isProgramRate()){
                    return getAuroraPrices("/room-prices-v2-partial-availability.json");
                }
                else {
                    return getAuroraPrices("/room-prices-v2-mixed-programs.json");
                }
                
            }
            
        });
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(true);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);
        //F1-adjustments
        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");
        //program.setTags(new String[]{"F1Package", "F12023PGS"});

        //when(appProps.getF1PackageTag()).thenReturn("F1Package");
        //when(appProps.getValidF1ProductCodes()).thenReturn(new ArrayList<String>(Arrays.asList(new String[]{"F12023BGS", "F12023HGS", "F12023PGS"})));
        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);
        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);

        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(1, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.RATE_PLANS, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());       
    }
    
    @Test
    public void getRoomAvailability_givenRegularUserWithSingleSpecifiedProgram_expectResponseWithProgramAndSellStrategyPrograms() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setIncludeSoldOutRooms(true);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setProgramIds(Collections.singletonList("7b36149a-0ab8-442b-8651-cfa5f800349e"));
        request.setIncludeDefaultRatePlans(true);

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {
            	AuroraPricesResponse auroraPricesResponse = new AuroraPricesResponse();
              auroraPricesResponse.setMrdPricing(true);
              List<AuroraPriceResponse> auroraPriceResponseList = new ArrayList<>();
             
                AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
                auroraPriceResponse.setComp(false);
                auroraPriceResponse.setCloseToArrival(false);
                auroraPriceResponse.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
                auroraPriceResponse.setRoomTypeId("ROOM_TYPE_1");
                auroraPriceResponse.setBasePrice(200.00);
                auroraPriceResponse.setBaseAmtAftTax(230.00);
                auroraPriceResponseList.add(auroraPriceResponse);

              auroraPricesResponse.setAuroraPrices(auroraPriceResponseList);
              return auroraPricesResponse;
          }
      });
        
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(true);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        validateResponse.setProgramId("55204b8f-6d58-4dab-894c-e79dda626c85");
        validateResponse.setProgramId("81c0ce15-7c42-4ee0-a031-5b02d7c5d15f");
        validateResponse.setProgramId("d8ec7e74-1d53-4ca6-84f1-3ecd6619c021");
        validateResponse.setProgramId("1dc21cfc-9f40-45d1-bdff-96c5107b5261");
        validateResponse.setProgramId("ecd2c44d-d537-4087-9212-0fca6296e6c0");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

        final Map<String, String> programPromoMap = new HashMap<>();
        request.getProgramIds().stream().forEach(x-> {
            programPromoMap.put(x, null);
        });

        when(programDao.getProgramPromoAssociation(Mockito.any())).thenReturn(programPromoMap);

        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");

        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);
        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);
        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(1, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.RATE_PLANS, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        List<RatePlanV2Response> response = returned.getRatePlans();
        assertEquals("7b36149a-0ab8-442b-8651-cfa5f800349e", response.get(0).getProgramId());
    }
    
    @Test
    public void getRoomAvailability_givenRegularUserWithMultipleSpecifiedProgram_expectResponseWithMultipleProgramAndSellStrategyPrograms() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setIncludeSoldOutRooms(true);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        String[] programs = {"08a1ee63-c99f-48b5-80b8-c3284339e7db","7b36149a-0ab8-442b-8651-cfa5f800349e"};
        request.setProgramIds(Arrays.asList(programs));
        request.setIncludeDefaultRatePlans(true); 
        AuroraPricesResponse res = getAuroraPrices("/room-prices-v2-7b36149a-0ab8-442b-8651-cfa5f800349e.json");
        
        // remove soldoout entry
        List<AuroraPriceResponse> collect = res.getAuroraPrices()
                .stream()
                .filter(a -> a.getStatus().equals(AvailabilityStatus.SOLDOUT))
                .collect(Collectors.toList());

        res.getAuroraPrices().removeAll(collect);

        List<AuroraPriceResponse> collect1 = res.getAuroraPrices().stream()
                .map(a -> {
                    a.setStatus(AvailabilityStatus.AVAILABLE);
                    return a;
                }).collect(Collectors.toList());
        // make rest of them available
        
        collect1.add(res.getAuroraPrices().get(0));

        AuroraPricesResponse response2 = new AuroraPricesResponse();
        response2.setAuroraPrices(collect1);

        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(response2);
        
        final Map<String, String> programPromoMap = new HashMap<>();
        request.getProgramIds().stream().forEach(x-> {
            programPromoMap.put(x, null);
        });

        when(programDao.getProgramPromoAssociation(Mockito.any())).thenReturn(programPromoMap);

        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");

        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);
        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);
        
        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(2, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.RATE_PLANS, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        List<RatePlanV2Response> response = returned.getRatePlans();
        
        assertEquals("08a1ee63-c99f-48b5-80b8-c3284339e7db", response.get(0).getProgramId());
        assertEquals("7b36149a-0ab8-442b-8651-cfa5f800349e", response.get(1).getProgramId());
    }
   
    @Test
    public void getRoomAvailability_givenPerpetualUserWithSingleSpecifiedProgram_expectResponseWithProgramAndPoPrograms() {

        // given perpetual pricing request with program specified
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setPerpetualPricing(true);
        request.setIncludeDefaultRatePlans(true);
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        String[] programs = {"7b36149a-0ab8-442b-8651-cfa5f800349e"};
        request.setProgramIds(Arrays.asList(programs));
        request.setIncludeDefaultRatePlans(true);

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {
                
                AuroraPriceRequest request = (AuroraPriceRequest)invocation.getArgument(0);
                if (StringUtils.isNotBlank(request.getProgramId())){
                    return getAuroraPrices("/room-prices-v2-partial-availability.json");
                }
                else {
                    return getAuroraPrices("/room-rate-plans-v2-single.json");
                }
                
            }
            
        });
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(true);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

        final Map<String, String> programPromoMap = new HashMap<>();
        request.getProgramIds().stream().forEach(x-> {
            programPromoMap.put(x, null);
        });

        when(programDao.getProgramPromoAssociation(Mockito.any())).thenReturn(programPromoMap);

        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");

        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);
        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);
        
        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(2, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.PERPETUAL, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        List<RatePlanV2Response> response = returned.getRatePlans();
        assertEquals("7b36149a-0ab8-442b-8651-cfa5f800349e", response.get(0).getProgramId());
    }
    
    @Test
    public void getRoomAvailability_givenPerpetualUserWithMultipleSpecifiedProgram_expectResponseWithMultipleProgramsAndPoPrograms() {

        // given perpetual pricing request with program specified
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setPerpetualPricing(true);
        request.setIncludeDefaultRatePlans(true);
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        String[] programs = {"08a1ee63-c99f-48b5-80b8-c3284339e7db","7b36149a-0ab8-442b-8651-cfa5f800349e"};
        request.setProgramIds(Arrays.asList(programs));
        request.setIncludeDefaultRatePlans(true);

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {
                
                AuroraPriceRequest request = (AuroraPriceRequest)invocation.getArgument(0);
                if (StringUtils.isNotBlank(request.getProgramId()) && request.getProgramId().equalsIgnoreCase("7b36149a-0ab8-442b-8651-cfa5f800349e")){
                    return getAuroraPrices("/room-prices-v2-7b36149a-0ab8-442b-8651-cfa5f800349e.json");
                }
                else if (StringUtils.isNotBlank(request.getProgramId()) && request.getProgramId().equalsIgnoreCase("08a1ee63-c99f-48b5-80b8-c3284339e7db")){
                    return getAuroraPrices("/room-prices-v2-08a1ee63-c99f-48b5-80b8-c3284339e7db.json");
                }
                else {
                    return getAuroraPrices("/room-rate-plans-v2-single.json");
                }
                
            }
            
        });

        final Map<String, String> programPromoMap = new HashMap<>();
        request.getProgramIds().stream().forEach(x-> {
            programPromoMap.put(x, null);
        });

        when(programDao.getProgramPromoAssociation(Mockito.any())).thenReturn(programPromoMap);

        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");

        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);
        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);
        
        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(3, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.PERPETUAL, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        List<RatePlanV2Response> response = returned.getRatePlans();
        assertEquals("08a1ee63-c99f-48b5-80b8-c3284339e7db", response.get(0).getProgramId());
    }
    
    @Test
    public void getRoomAvailability_givenPerpetualUserWithPoAsFirstSpecifiedProgram_expectResponseWithPoProgramAsFirstInTheRatePlans() {

        // given perpetual pricing request with program specified
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setPerpetualPricing(true);
        request.setIncludeDefaultRatePlans(true);
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        List<String> programIds = new ArrayList<String>();
        programIds.add("55204b8f-6d58-4dab-894c-e79dda626c85");
        programIds.add("7b36149a-0ab8-442b-8651-cfa5f800349e");
        
        request.setProgramIds(programIds);
        request.setIncludeDefaultRatePlans(true);

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {
                
                AuroraPriceRequest request = (AuroraPriceRequest)invocation.getArgument(0);
                if (StringUtils.isNotBlank(request.getProgramId()) && request.getProgramId().equalsIgnoreCase("7b36149a-0ab8-442b-8651-cfa5f800349e")){
                    return getAuroraPrices("/room-prices-v2-7b36149a-0ab8-442b-8651-cfa5f800349e.json");
                }
                else {
                    return getAuroraPrices("/room-rate-plans-v2-single.json");
                }
                
            }
            
        });
        
        when(programDao.isProgramPO("55204b8f-6d58-4dab-894c-e79dda626c85")).thenReturn(true);

        final Map<String, String> programPromoMap = new HashMap<>();
        request.getProgramIds().stream().forEach(x-> {
            programPromoMap.put(x, null);
        });

        when(programDao.getProgramPromoAssociation(Mockito.any())).thenReturn(programPromoMap);

        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");

        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);
        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);
        
        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(2, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.PERPETUAL, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        List<RatePlanV2Response> response = returned.getRatePlans();
        // Applied program should be first
        assertEquals("55204b8f-6d58-4dab-894c-e79dda626c85", response.get(0).getProgramId());
        
    }
    
    @Test
    public void getRoomAvailability_givenPerpetualUserWithPoAsSecondSpecifiedProgram_expectResponseWithPoProgramAsSecondInTheRatePlans() {

        // given perpetual pricing request with program specified
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setPerpetualPricing(true);
        request.setIncludeDefaultRatePlans(true);
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        List<String> programIds = new ArrayList<String>();
        programIds.add("7b36149a-0ab8-442b-8651-cfa5f800349e");
        programIds.add("55204b8f-6d58-4dab-894c-e79dda626c85");
        
        request.setProgramIds(programIds);
        request.setIncludeDefaultRatePlans(true);

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {
                
                AuroraPriceRequest request = (AuroraPriceRequest)invocation.getArgument(0);
                if (StringUtils.isNotBlank(request.getProgramId()) && request.getProgramId().equalsIgnoreCase("7b36149a-0ab8-442b-8651-cfa5f800349e")){
                    return getAuroraPrices("/room-prices-v2-7b36149a-0ab8-442b-8651-cfa5f800349e.json");
                }
                else {
                    return getAuroraPrices("/room-rate-plans-v2-single.json");
                }
                
            }
            
        });
        
        when(programDao.isProgramPO("55204b8f-6d58-4dab-894c-e79dda626c85")).thenReturn(true);

        final Map<String, String> programPromoMap = new HashMap<>();
        request.getProgramIds().stream().forEach(x-> {
            programPromoMap.put(x, null);
        });

        when(programDao.getProgramPromoAssociation(Mockito.any())).thenReturn(programPromoMap);

        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");

        when(programCacheService.getRoomProgram(anyString())).thenReturn(program);

        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);
        
        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(2, returned.getRatePlans().size());
        assertEquals(ShoppingFlow.PERPETUAL, returned.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.PROGRAM, returned.getMetadata().getPricingMode());

        // Perform some field level assertions
        List<RatePlanV2Response> response = returned.getRatePlans();
        // Applied program should be first
        assertEquals("7b36149a-0ab8-442b-8651-cfa5f800349e", response.get(0).getProgramId());
        
    }
    
    @Test
    public void getRoomAvailability_givenRegularUserWithNoSpecifiedProgramAndOneRatePlanAvailable_expectSingleRatePlansResponse() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        // mock dao pricing response
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenReturn(getAuroraPrices("/room-rate-plans-v2-single.json"));

        // invoke service
        RoomAvailabilityCombinedResponse combinedResponse = availabilityServiceImpl.getRoomAvailability(request);

        // expect rate plans and not availability
        assertNotNull(combinedResponse.getRatePlans());
        assertEquals(1, combinedResponse.getRatePlans().size());
        assertEquals(ShoppingFlow.RATE_PLANS, combinedResponse.getMetadata().getShoppingFlow());
        assertEquals(PricingModes.BEST_AVAILABLE, combinedResponse.getMetadata().getPricingMode());

        // Perform some field level assertions
        List<RatePlanV2Response> response = combinedResponse.getRatePlans();

        assertEquals("55204b8f-6d58-4dab-894c-e79dda626c85", response.get(0).getProgramId());
        assertNotNull(response.get(0).getRooms());
        assertEquals(23, response.get(0).getRooms().size());
        response.get(0).getRooms().forEach(room -> {
            if (!room.isUnavailable()) {
                assertEquals(6, room.getPrice().getItemized().size());
            } else {
                assertTrue(room.isUnavailable());
            }
        });
    }

    @Test
    public void getRoomAvailability_givenRegularUserWithSingleSpecifiedProgram_expectResponseWithDefaultRateProgramAndSellStrategyPrograms() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setIncludeSoldOutRooms(true);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setProgramIds(Collections.singletonList("7b36149a-0ab8-442b-8651-cfa5f800349e"));
        request.setIncludeDefaultRatePlans(false);
        request.setMlifeNumber("abc123");
        request.setSource("WEB");

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {

                AuroraPriceRequest request = (AuroraPriceRequest)invocation.getArgument(0);
                if (StringUtils.isNotBlank(request.getProgramId())){
                    return getAuroraPrices("/room-prices-v2-sample-availability.json");
                }
                else {
                    return getAuroraPrices("/room-rate-plans-v2.json");
                }

            }

        });
        List<String> validF1ProductCodes=new ArrayList<>();
        validF1ProductCodes.add(ServiceConstant.F1_COMPONENT_CASINO_START_TAG.concat("12023BG"));
        validF1ProductCodes.add("F12023BGS");
        validF1ProductCodes.add("F12023HGS");
        validF1ProductCodes.add("F12023PGS");

        when(applicationProperties.getDefaultTimezone()).thenReturn("America/New_York");
        when(applicationProperties.getF1PackageTag()).thenReturn("F1PACKAGE");
        when(applicationProperties.getValidF1ProductCodes()).thenReturn(validF1ProductCodes);
        String[] tags= {"F1PACKAGE","F12023BGS","F12023HGS","F12023PGS",ServiceConstant.F1_COMPONENT_CASINO_START_TAG.concat("12023BG")};

        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");
        program.setTags(tags);
        when(programCacheService.getRoomProgram(Mockito.any())).thenReturn(program);

        InventoryGetRes inventoryGetRes=new InventoryGetRes();
        List<InventoryDetails> inventories=new ArrayList<>();
        InventoryDetails inventoryDetails=new InventoryDetails();
        InventoryObj IinventoryObj=new InventoryObj();
        List<Level> level1=new ArrayList<>();
        Level level=new Level();
        level.setTotalAvailableUnits("20");
        level1.add(level);
        Level levela=new Level();
        levela.setTotalAvailableUnits("10");
        level1.add(levela);
        IinventoryObj.setLevel1(level1);
        inventoryDetails.setInventory(IinventoryObj);
        inventories.add(inventoryDetails);
        inventoryGetRes.setInventories(inventories);
        when(productInventoryDAO.getInventory(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(inventoryGetRes);

        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(true);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        validateResponse.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

        final Map<String, String> programPromoMap = new HashMap<>();
        request.getProgramIds().stream().forEach(x-> {
            programPromoMap.put(x, null);
        });

        when(programDao.getProgramPromoAssociation(Mockito.any())).thenReturn(programPromoMap);

        RoomComponent roomComponent=new RoomComponent();
        roomComponent.setPrice(100F);
        when(componentDAO.getRoomComponentByCode(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), 
        		Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(roomComponent);
        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);

        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(1, returned.getRatePlans().size());

        // Perform some field level assertions
        List<RatePlanV2Response> response = returned.getRatePlans();
        String roomTypeId=response.get(0).getRooms().stream()
        		.filter(p-> "48b9d77c-4d88-446e-97d7-ebd4f51a8b72".equals(p.getRoomTypeId())).findAny().get().getRoomTypeId();

        assertEquals("7b36149a-0ab8-442b-8651-cfa5f800349e", response.get(0).getProgramId());
        assertEquals("48b9d77c-4d88-446e-97d7-ebd4f51a8b72", roomTypeId);
    }

    @Test
    public void getRoomAvailability_givenRegularUserWithSingleSpecifiedProgram_expectResponseWithNoDefaultRateProgramAndSellStrategyPrograms() {

        // given request for regular non-po shopper
        RoomAvailabilityV3Request request = new RoomAvailabilityV3Request();
        request.setCheckInDate(getFutureLocalDate(12));
        request.setCheckOutDate(getFutureLocalDate(18));
        request.setNumAdults(2);
        request.setIncludeSoldOutRooms(true);
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        request.setIncludeDefaultRatePlans(true);
        request.setMlifeNumber("abc123");
        request.setSource("WEB");

        // mock dao pricing response and program validation calls
        when(pricingDao.getRoomPricesV2(Mockito.any())).thenAnswer(new Answer<AuroraPricesResponse>() {

            @Override
            public AuroraPricesResponse answer(InvocationOnMock invocation) throws Throwable {

                AuroraPriceRequest request = (AuroraPriceRequest)invocation.getArgument(0);
                if (StringUtils.isNotBlank(request.getProgramId())){
                    return getAuroraPrices("/room-prices-v2-sample-availability.json");
                }
                else {
                    return getAuroraPrices("/room-rate-plans-v2-single-sample.json");
                }

            }

        });
        List<String> validF1ProductCodes=new ArrayList<>();
        validF1ProductCodes.add(ServiceConstant.F1_COMPONENT_CASINO_START_TAG.concat("12023BG"));
        validF1ProductCodes.add("F12023BGS");
        validF1ProductCodes.add("F12023HGS");
        validF1ProductCodes.add("F12023PGS");

        when(applicationProperties.getDefaultTimezone()).thenReturn("America/New_York");
        when(applicationProperties.getF1PackageTag()).thenReturn("F1PACKAGE");
        //when(applicationProperties.getMaxF1TicketCount()).thenReturn(2);
        when(applicationProperties.getValidF1ProductCodes()).thenReturn(validF1ProductCodes);
        //when(applicationProperties.getF1N24AdditionalTicketByGrandStand(Mockito.anyString())).thenReturn("grandstandName");
        String[] tags= {"F1PACKAGE","F12023BGS","F12023HGS","F12023PGS",ServiceConstant.F1_COMPONENT_CASINO_START_TAG.concat("12023BG"),
        		ServiceConstant.F1_COMPONENT_TRANSIENT_START_TAG.concat("12023BG")};

        final RoomProgram program = new RoomProgram();
        program.setCategory("Test");
        program.setId("id");
        program.setName("name");
        program.setTags(tags);
        when(programCacheService.getRoomProgram(Mockito.any())).thenReturn(program);

        InventoryGetRes inventoryGetRes=new InventoryGetRes();
        List<InventoryDetails> inventories=new ArrayList<>();
        InventoryDetails inventoryDetails=new InventoryDetails();
        InventoryObj IinventoryObj=new InventoryObj();
        List<Level> level1=new ArrayList<>();
        Level level=new Level();
        level.setTotalAvailableUnits("20");
        level1.add(level);
        Level levela=new Level();
        levela.setTotalAvailableUnits("10");
        level1.add(levela);
        IinventoryObj.setLevel1(level1);
        inventoryDetails.setInventory(IinventoryObj);
        inventories.add(inventoryDetails);
        inventoryGetRes.setInventories(inventories);
        when(productInventoryDAO.getInventory(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(inventoryGetRes);

        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        validateResponse.setEligible(true);
        validateResponse.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
        validateResponse.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        when(programService.validateProgramV2(Mockito.any())).thenReturn(validateResponse);

        final Map<String, String> programPromoMap = new HashMap<>();
        request.getProgramIds().stream().forEach(x-> {
            programPromoMap.put(x, null);
        });

        RoomComponent roomComponent=new RoomComponent();
        roomComponent.setPrice(100F);
        when(componentDAO.getRoomComponentByCode(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), 
        		Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(roomComponent);
        // invoke service
        RoomAvailabilityCombinedResponse returned = availabilityServiceImpl.getRoomAvailability(request);

        // expect availability and not rate plans
        assertNotNull(returned.getRatePlans());
        assertEquals(2, returned.getRatePlans().size());

        // Perform some field level assertions
        List<RatePlanV2Response> response = returned.getRatePlans();

        assertEquals("7b36149a-0ab8-442b-8651-cfa5f800349e", response.get(0).getProgramId());
        assertEquals("55204b8f-6d58-4dab-894c-e79dda626c85", response.get(1).getProgramId());
        // Check for presence before asserting   
        response.get(0).getRooms().stream()      
        .filter(p -> "48b9d77c-4d88-446e-97d7-ebd4f51a8b72".equals(p.getRoomTypeId())) 
        .findAny()      
        .ifPresent(room -> assertEquals("48b9d77c-4d88-446e-97d7-ebd4f51a8b72", room.getRoomTypeId())); 
        response.get(1).getRooms().stream() 
        .filter(p -> "b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd".equals(p.getRoomTypeId()))
        .findAny() 
        .ifPresent(room -> assertEquals("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd", room.getRoomTypeId()));
     }

}
