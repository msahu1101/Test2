package com.mgm.services.booking.room.service.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mgm.services.booking.room.model.request.ResortPriceV2Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.PerpetaulProgram;
import com.mgm.services.booking.room.model.response.PricingModes;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;

@ExtendWith(MockitoExtension.class)
public class ResortPriceServiceHelperTest {

	@Mock
	private RoomProgramCacheService programCacheService;

	@Mock
	private RoomProgramService programService;

	@Mock
	AuroraProperties auroraProperties;

	@Mock
	private AuroraPriceResponse auroraPriceResponse1;

	@Mock
	private AuroraPriceResponse auroraPriceResponse2;

	@InjectMocks
	private ResortPriceServiceHelper resortPriceServiceHelper;

	private ResortPriceV2Request validRequest;

	private List<AuroraPriceResponse> auroraPriceResponseList;

	@BeforeEach
	void setUp() {
		validRequest = new ResortPriceV2Request();
		validRequest.setProgramId("validProgramId");
		validRequest.setSource("test");
		validRequest.setCustomerId(567893456);
		validRequest.setPropertyId("876786");
		validRequest.setRedemptionCode("4566");

		auroraPriceResponseList = new ArrayList<>();
		AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
		auroraPriceResponse.setStatus(AvailabilityStatus.AVAILABLE);
		auroraPriceResponse.setRoomTypeId("465");
		auroraPriceResponse.setProgramId("validProgramId");
		auroraPriceResponseList.add(auroraPriceResponse);
	}

	@Test
	void testValidateProgram_ValidProgram_Eligible() {
		// Arrange
		RoomProgram roomProgram = new RoomProgram();
		roomProgram.setPropertyId("validPropertyId");
		roomProgram.setId("897");

		when(programCacheService.getRoomProgram(anyString())).thenReturn(roomProgram);
		RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
		validateResponse.setEligible(true);
		when(programService.validateProgramV2(any())).thenReturn(validateResponse);

		// Act
		assertDoesNotThrow(() -> resortPriceServiceHelper.validateProgram(validRequest));

		// Assert
		assertEquals("897", validRequest.getProgramId());
		assertEquals("validPropertyId", validRequest.getPropertyId());
	}

	@Test
	public void testValidateProgram_ValidProgram_NotEligible() {
		// Arrange
		RoomProgram roomProgram = new RoomProgram();
		roomProgram.setPropertyId("validPropertyId");
		when(programCacheService.getRoomProgram(anyString())).thenReturn(roomProgram);
		RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
		validateResponse.setEligible(false);
		when(programService.validateProgramV2(any())).thenReturn(validateResponse);

		// Act & Assert
		BusinessException exception = assertThrows(BusinessException.class,
				() -> resortPriceServiceHelper.validateProgram(validRequest));
		assertEquals(ErrorCode.OFFER_NOT_ELIGIBLE, exception.getErrorCode());
		assertNull(validRequest.getProgramId());
	}

	@Test
	void testValidateProgram_ProgramNotAvailable() {
		// Arrange
		when(programCacheService.getRoomProgram(anyString())).thenReturn(null);

		// Act & Assert
		BusinessException exception = assertThrows(BusinessException.class,
				() -> resortPriceServiceHelper.validateProgram(validRequest));
		assertEquals(ErrorCode.OFFER_NOT_AVAILABLE, exception.getErrorCode());
	}
	
	@Test
	public void testGroupPricesByRoom() {
		// Preparing data
		List<AuroraPriceResponse> pricingList = new ArrayList<>();
		AuroraPriceResponse auroraPriceResponse1 = new AuroraPriceResponse();
		auroraPriceResponse1.setStatus(AvailabilityStatus.AVAILABLE);
		auroraPriceResponse1.setRoomTypeId("room1");
		auroraPriceResponse1.setProgramId("validProgramId");
		pricingList.add(auroraPriceResponse1);

		AuroraPriceResponse auroraPriceResponse2 = new AuroraPriceResponse();
		auroraPriceResponse2.setStatus(AvailabilityStatus.SOLDOUT);
		auroraPriceResponse2.setRoomTypeId("room2");
		pricingList.add(auroraPriceResponse2);

		AuroraPriceResponse auroraPriceResponse3 = new AuroraPriceResponse();
		auroraPriceResponse3.setStatus(AvailabilityStatus.AVAILABLE);
		auroraPriceResponse3.setRoomTypeId("room1");
		pricingList.add(auroraPriceResponse3);

		// Call the method under test
		Map<String, List<AuroraPriceResponse>> roomMap = resortPriceServiceHelper.groupPricesByRoom(
				pricingList, validRequest);

		// Assertions
		assertNotNull(roomMap);
		assertEquals(1, roomMap.size()); // Assuming only one room type is available
		assertTrue(roomMap.containsKey("room1")); // Room1 should be available
		assertEquals(2, roomMap.get("room1").size()); // Two prices for room1        
	}

	@Test
	public void testAverageOutPricesByRoom() {
		Map<String, List<AuroraPriceResponse>> roomMap = new HashMap<>();
		List<AuroraPriceResponse> pricingList = new ArrayList<>();
		
		AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
		auroraPriceResponse.setBasePrice(100.0);
		auroraPriceResponse.setDiscountedPrice(80.0);
		auroraPriceResponse.setResortFee(10.0);
		auroraPriceResponse.setPropertyId("property123");
		auroraPriceResponse.setRoomTypeId("roomType123");
		auroraPriceResponse.setComp(false);
		auroraPriceResponse.setProgramId("test123");
		
		pricingList.add(auroraPriceResponse);
	
		roomMap.put("1", pricingList);

		// Test the method
		Map<String, ResortPriceResponse> roomPricingMap = resortPriceServiceHelper.averageOutPricesByRoom(roomMap, 567893456L, "test123",1L);

		// Assertions
		assertNotNull(roomPricingMap);
		assertEquals(1, roomPricingMap.size());
		assertTrue(roomPricingMap.containsKey("roomType123"));

		ResortPriceResponse result = roomPricingMap.get("roomType123");
		assertNotNull(result);
		assertEquals("property123", result.getPropertyId());
		assertEquals(AvailabilityStatus.AVAILABLE, result.getStatus());
		assertFalse(result.isComp());
		assertEquals(10.0, result.getResortFee());
		assertEquals(100.0, result.getPrice().getBaseAveragePrice());
		assertEquals(80.0, result.getPrice().getDiscountedAveragePrice());
	}
	 
	@Test
	public void testFindPerpetualProgramForProperty() {
		// Preparing data
		List<PerpetaulProgram> perpetualPrograms = new ArrayList<>();
		// Add some sample PerpetaulProgram objects to the list
		PerpetaulProgram perpetaulProgram = new PerpetaulProgram();
		perpetaulProgram.setPropertyId("propertyId1");
		perpetaulProgram.setId("test123");
		perpetualPrograms.add(perpetaulProgram);

		// Testing the method
		String result = resortPriceServiceHelper.findPerpetualProgramForProperty(perpetualPrograms, "propertyId1");

		// Assertions
		assertEquals("test123", result);
	}
	
	@Test
    @DisplayName("Test averageOutPricesByRoomV2 method")
    public void testAverageOutPricesByRoomV2() {
        // Prepare data
        Map<String, List<AuroraPriceResponse>> roomMap = new HashMap<>();
        List<AuroraPriceResponse> pricingList = new ArrayList<>();
        AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
        auroraPriceResponse.setProgramId("test123");
        auroraPriceResponse.setPropertyId("property123");
        auroraPriceResponse.setBasePrice(100.0);
        auroraPriceResponse.setComp(false);
        auroraPriceResponse.setDiscountedPrice(80.0);
        auroraPriceResponse.setResortFee(10.0);
        auroraPriceResponse.setAmtAftTax(90.0);
        auroraPriceResponse.setBaseAmtAftTax(110.0);
        auroraPriceResponse.setRoomTypeId("1");
        pricingList.add(auroraPriceResponse);

        // Associate the pricingList with a specific key in roomMap
        roomMap.put("1", pricingList);

        // Testing the method
        Map<String, ResortPriceResponse> roomPricingMap = resortPriceServiceHelper.averageOutPricesByRoomV2(roomMap, 5L, "test123", 1L, PricingModes.BEST_AVAILABLE);

        // Assertions
        assertNotNull(roomPricingMap);
        assertEquals(1, roomPricingMap.size());

        ResortPriceResponse result = roomPricingMap.get("1");
        assertNotNull(result);
        assertEquals("test123", result.getProgramId());
        assertEquals(PricingModes.PROGRAM, result.getPricingMode());
        assertEquals("property123", result.getPropertyId());
        assertEquals(AvailabilityStatus.AVAILABLE, result.getStatus());
        assertFalse(result.isComp());
        assertEquals(10.0, result.getResortFee());
        assertEquals(100.0, result.getPrice().getBaseAveragePrice());
        assertEquals(80.0, result.getPrice().getDiscountedAveragePrice());
        assertEquals(90.0, result.getAmtAftTax());
        assertEquals(110.0, result.getBaseAmtAftTax());
        assertEquals("1", result.getRoomTypeId());
    }

	@Test
	@DisplayName("Test isProgramIncluded method")
	public void testIsProgramIncluded() {
		// Preparing data
		List<AuroraPriceResponse> prices = new ArrayList<>();
		AuroraPriceResponse auroraPriceResponse = new AuroraPriceResponse();
		auroraPriceResponse.setProgramId("programId1");
		prices.add(auroraPriceResponse);

		// Testing the method
		boolean result = resortPriceServiceHelper.isProgramIncluded(prices, "programId1");

		// Assertions
		assertTrue(result);
	}

	@Test
	public void testAddSoldOutResortsForProgram() {
		// Creating test data
		Map<String, ResortPriceResponse> resortPricingMap = new HashMap<>();
		List<RoomProgramBasic> programList = new ArrayList<>();
		programList.add(new RoomProgramBasic("program1", "property1", null, false, false, null,null, null, null, null, null));
		programList.add(new RoomProgramBasic("program2", "property2", null, false, false, null, null, null, null, null, null));

		// Call the method to be tested
		resortPriceServiceHelper.addSoldOutResortsForProgram(resortPricingMap, programList);

		assertEquals(2, resortPricingMap.size());
	}

	@Test
	public void testPopulatePlanMap() {
		// Mock input data
		AuroraPriceResponse price = new AuroraPriceResponse();
		price.setProgramId("programId");
		price.setPropertyId("propertyId");

		Map<String, List<AuroraPriceResponse>> ratePlanMap = new HashMap<>();
		List<String> processedPropertyList = new ArrayList<>();

		// Call the method
		resortPriceServiceHelper.populatePlanMap(price, ratePlanMap, processedPropertyList);

		// Verify that ratePlanMap contains the expected values
		assertEquals(1, ratePlanMap.size());
		assertEquals(1, ratePlanMap.get("programId").size());
		assertEquals(price, ratePlanMap.get("programId").get(0));

		// Verify that processedPropertyList contains the expected value
		assertEquals(1, processedPropertyList.size());
		assertEquals("propertyId", processedPropertyList.get(0));
	}

	@Test
	void testRemoveSoldOutResorts() {
		// Creating a sample resortPricingMap
		Map<String, ResortPriceResponse> resortPricingMap = new HashMap<>();
		ResortPriceResponse resort1 = new ResortPriceResponse();
		resort1.setRoomTypeId("resort1");
		resort1.setStatus(AvailabilityStatus.AVAILABLE);
		ResortPriceResponse resort2 = new ResortPriceResponse();
		resort2.setRoomTypeId("resort2");
		resort2.setStatus(AvailabilityStatus.SOLDOUT);
		ResortPriceResponse resort3 = new ResortPriceResponse();
		resort3.setRoomTypeId("resort1");
		resort3.setStatus(AvailabilityStatus.OFFER);
		resortPricingMap.put("resort1", resort1);
		resortPricingMap.put("resort2", resort2);
		resortPricingMap.put("resort3", resort3);

		// Calling the method
		resortPriceServiceHelper.removeSoldOutResorts(resortPricingMap);

		// Verifying that the sold-out resort is removed
		assertEquals(2, resortPricingMap.size());
		assertEquals(AvailabilityStatus.AVAILABLE, resortPricingMap.get("resort1").getStatus());
		assertEquals(AvailabilityStatus.OFFER, resortPricingMap.get("resort3").getStatus());
		assertEquals(null, resortPricingMap.get("resort2")); // Sold-out resort should be removed
	}

	@Test
	public void testGetPropertyIdsForFallBackPricing() {
		// Creating a sample resortPricingMap
		Map<String, ResortPriceResponse> resortPricingMap = new HashMap<>();
		resortPricingMap.put("property1", new ResortPriceResponse());
		resortPricingMap.put("property2", new ResortPriceResponse());

		// Mock auroraProperties.getPropertyIds() method
		List<String> fullPropertyIdList = new ArrayList<>();
		fullPropertyIdList.add("property1");
		fullPropertyIdList.add("property2");
		fullPropertyIdList.add("property3"); // This property is not in the resortPricingMap
		when(auroraProperties.getPropertyIds()).thenReturn(fullPropertyIdList);

		// Call the method
		List<String> result = resortPriceServiceHelper.getPropertyIdsForFallBackPricing(resortPricingMap);

		// Verify that the correct property IDs are returned
		assertEquals(1, result.size());
		assertEquals("property3", result.get(0));
	}

	@Test
	public void testAverageOutPricesByRoomV21() {
		// Mock data
		String roomTypeId1 = "roomType1";
		String roomTypeId2 = "roomType2";

		List<AuroraPriceResponse> pricingList1 = Arrays.asList(auroraPriceResponse1, auroraPriceResponse2);
		List<AuroraPriceResponse> pricingList2 = Collections.singletonList(auroraPriceResponse1);
		Map<String, List<AuroraPriceResponse>> roomMap = new HashMap<>();
		roomMap.put(roomTypeId1, pricingList1);
		roomMap.put(roomTypeId2, pricingList2);
		long customerId = 123;
		String programId = "program1";
		long tripDuration = 2;
		PricingModes pricingMode = PricingModes.BEST_AVAILABLE;

		when(auroraPriceResponse1.getProgramId()).thenReturn(programId);
		when(auroraPriceResponse2.getProgramId()).thenReturn(programId);

		// Call the method under test
		Map<String, ResortPriceResponse> result = resortPriceServiceHelper.averageOutPricesByRoomV2(roomMap, customerId,
				programId, tripDuration, pricingMode);

		// Assertions
		assertEquals(1, result.size()); // Asserting the size of the resulting map

	}
}
