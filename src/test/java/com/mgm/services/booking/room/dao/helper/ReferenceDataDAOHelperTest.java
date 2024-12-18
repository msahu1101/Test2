package com.mgm.services.booking.room.dao.helper;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.mgm.services.booking.room.model.Room;
import com.mgm.services.booking.room.model.TripDetailsV3;
import com.mgm.services.booking.room.model.request.ModificationChangesRequest;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.util.PropertyConfig.PropertyValue;
import com.mgm.services.booking.room.util.RequestSourceConfig;
import com.mgm.services.booking.room.util.RequestSourceConfig.SourceDetails;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

class ReferenceDataDAOHelperTest {

	@Mock
    private PropertyConfig propertyConfig;
    
    @Mock
    private RequestSourceConfig requestSourceConfig;
	
	@Mock
    private SecretsProperties secretProperties;

    @Mock
    private AcrsProperties acrsProperties;
    
    @InjectMocks
    ReferenceDataDAOHelper referenceDataDAOHelper;
    
	String propertyId = "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
	String programId = "RPCD-v-TFRSA-d-PROP-v-MV021";
    

	Map<String, PropertyValue> propertyValuesMap = null;
	
	@BeforeEach
	void init() {
    	MockitoAnnotations.initMocks(this);
		propertyValuesMap = new HashMap<>();
		PropertyValue propertyValue = new PropertyValue();
    	List<String> ids = new ArrayList<>();
    	ids.add("propertyCode");
    	propertyValue.setAcrsPropertyIds(ids);
    	propertyValue.setGsePropertyIds(ids);
    	propertyValue.setGseMerchantID("gseMerchantID");
    	propertyValue.setPatronSiteId(1111);
    	propertyValuesMap.put(propertyId, propertyValue);
    	when(propertyConfig.getPropertyValuesMap()).thenReturn(propertyValuesMap);
	}
    
    @Test
    void testUpdateAcrsReferencesFromGse_RoomReservation() {    
    	RoomReservation roomReservation = this.createRoomReservation();
    	referenceDataDAOHelper.updateAcrsReferencesFromGse(roomReservation);

    	// Assertions
    	Mockito.verify(propertyConfig, Mockito.atLeastOnce()).getPropertyValuesMap();
    }

    @Test
    void testUpdateAcrsReferencesFromGse_ModificationChangesRequest() {
    	ModificationChangesRequest modificationChangesRequest = new ModificationChangesRequest();
    	modificationChangesRequest.setPropertyId(propertyId);
    	List<RoomPrice> bookings = new ArrayList<>();
    	RoomPrice booking = new RoomPrice();
    	booking.setOverrideProgramId(programId);
    	booking.setProgramId("GRPCD-v-GRP1-d-PROP-v-MV021");
    	bookings.add(booking);
    	modificationChangesRequest.setBookings(bookings);
    	modificationChangesRequest.setRoomTypeId("roomTypeId");
    	modificationChangesRequest.setProgramId("GRPCD-v-GRP1-d-PROP-v-MV021");
    	
    	ModificationChangesRequest response = null;
    	response = referenceDataDAOHelper.updateAcrsReferencesFromGse(modificationChangesRequest);
    	
    	// Assertions
    	assertNotNull(response);
    	assertTrue(response.isGroupCode());
    	assertEquals("GRP1", response.getProgramId());
    	assertEquals("TFRSA", response.getBookings().get(0).getOverrideProgramId());
    }
    
    @Test
    void testUpdateAcrsReferencesToGse_RoomReservation() {
    	RoomReservation roomReservation = this.createRoomReservation();
    	roomReservation.setIsGroupCode(true);
    	referenceDataDAOHelper.updateAcrsReferencesToGse(roomReservation);

    	// Assertions
    	Mockito.verify(propertyConfig, Mockito.atLeastOnce()).getPropertyValuesMap();
    }
    
    @Test
    void testUpdateAcrsReferencesToGse_AuroraPriceResponse() {
    	List<AuroraPriceResponse> responseList = new ArrayList<>();
    	AuroraPriceResponse response = new AuroraPriceResponse();
    	response.setPropertyId(propertyId);
    	response.setRoomTypeId("roomTypeId");
    	response.setProgramId(programId);
    	responseList.add(response);
    	referenceDataDAOHelper.updateAcrsReferencesToGse(responseList, true);

    	// Assertions
    	Mockito.verify(propertyConfig, Mockito.atLeastOnce()).getPropertyValuesMap();
    }
    
    @Test
    void testUpdateAcrsReferencesToGse_AuroraPriceResponse_GroupCodeFalseException() {
    	List<AuroraPriceResponse> responseList = new ArrayList<>();
    	AuroraPriceResponse response = new AuroraPriceResponse();
    	response.setPropertyId("pId");
    	response.setRoomTypeId("roomTypeId");
    	response.setProgramId(programId);
    	responseList.add(response);

    	// Assertions
    	SystemException ex = assertThrows(SystemException.class, 
    			() -> referenceDataDAOHelper.updateAcrsReferencesToGse(responseList, false));
    	assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
    }

    @Test
    void testRetrieveAcrsPropertyIDException() {
    	SystemException ex = assertThrows(SystemException.class, 
    			() -> referenceDataDAOHelper.retrieveAcrsPropertyID("pid"));

    	// Assertions
    	assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
    }
    
    @Test
    void testUpdateAcrsReferencesToGseV3_AuroraPriceV3Response() {
    	List<AuroraPriceV3Response> responseList = new ArrayList<>();
    	AuroraPriceV3Response response = new AuroraPriceV3Response();
    	response.setRoomTypeId("roomTypeId");
    	List<TripDetailsV3> tripDetails = new ArrayList<>();
    	TripDetailsV3 tripDetailV3 = new TripDetailsV3();
    	tripDetailV3.setProgramId(programId);
    	tripDetails.add(tripDetailV3);
    	response.setTripDetails(tripDetails);
    	responseList.add(response);
    	referenceDataDAOHelper.updateAcrsReferencesToGseV3(responseList, propertyId);
    	
    	// Assertions
    	Mockito.verify(propertyConfig, Mockito.atLeastOnce()).getPropertyValuesMap();
    }

    @Test
    void testRetrieveMerchantID() {
    	String merchantID = null;
    	merchantID = referenceDataDAOHelper.retrieveMerchantID(propertyId);

    	// Assertions
    	assertEquals("gseMerchantID", merchantID);
    }
    
    @Test
    void testRetrieveMerchantIDNull() {
    	BusinessException ex = assertThrows(BusinessException.class, 
    			() -> referenceDataDAOHelper.retrieveMerchantID("pid"));
    	
    	// Assertions
    	assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
    	assertTrue(ex.getMessage().contains("No Merchant id found for property id: pid"));
    }
    
    @Test
    void testGetPatronSiteId() {
    	int patronSiteId = 0;
    	patronSiteId = referenceDataDAOHelper.getPatronSiteId(propertyId);

    	// Assertions
    	assertEquals(1111, patronSiteId);
    }
    
    @Test
    void testGetPatronSiteIdNull() {
    	BusinessException ex = assertThrows(BusinessException.class, 
    			() -> referenceDataDAOHelper.getPatronSiteId("pid"));

    	// Assertions
    	assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
    	assertTrue(ex.getMessage().contains("No PatronSite id found for property id: pid"));
    }
    
    @Test
    void testUpdatGroupeAcrsReferencesToGse_GroupSearchV2Response() {
    	List<GroupSearchV2Response> responseList = new ArrayList<>();
    	GroupSearchV2Response response = new GroupSearchV2Response();
    	response.setPropertyId(propertyId);
    	List<Room> rooms = new ArrayList<>();
    	Room room = new Room("roomType");
    	rooms.add(room);
    	response.setRooms(rooms);
    	responseList.add(response);
    	referenceDataDAOHelper.updateGroupAcrsReferencesToGse(responseList);
    	
    	// Assertions
    	Mockito.verify(propertyConfig, Mockito.atLeastOnce()).getPropertyValuesMap();
    }
    
    @Test
    void testGetAcrsVendor() {
		Map<String, SourceDetails> requestSourcesMap = new HashMap<>();
		SourceDetails sourceDetails = new SourceDetails();
		sourceDetails.setAcrsVendor("acrsVendor");
		requestSourcesMap.put("source", sourceDetails);
    	when(requestSourceConfig.getRequestSourcesMap()).thenReturn(requestSourcesMap);

    	String vendor = null;
    	vendor = referenceDataDAOHelper.getAcrsVendor("source");

    	// Assertions
    	assertEquals("acrsVendor", vendor);
    }
    
    @Test
    void testGetAcrsVendorInvalidSource() {
    	BusinessException ex = assertThrows(BusinessException.class, 
    			() -> referenceDataDAOHelper.getAcrsVendor("source"));

    	// Assertions
    	assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
    	assertTrue(ex.getMessage().contains("Invalid Source Header"));
    }
    
    @Test
    void testGetRequestSource() {
		Map<String, SourceDetails> requestSourcesMap = new HashMap<>();
		SourceDetails sourceDetails = new SourceDetails();
		sourceDetails.setAcrsChannel("acrsChannelTest");
		requestSourcesMap.put("source", sourceDetails);
    	when(requestSourceConfig.getRequestSourcesMap()).thenReturn(requestSourcesMap);

    	SourceDetails sourceDetailsResponse = null;
    	sourceDetailsResponse = referenceDataDAOHelper.getRequestSource("source");

    	// Assertions
    	assertNotNull(sourceDetailsResponse);
    	assertEquals("acrsChannelTest", sourceDetailsResponse.getAcrsChannel());
    }
    
    @Test
    void testGetRequestSourceNull() {
    	BusinessException ex = assertThrows(BusinessException.class, 
    			() -> referenceDataDAOHelper.getRequestSource("source"));

    	// Assertions
    	assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
    	assertTrue(ex.getMessage().contains("Invalid Source Header"));
    }
    
    @Test
    void testGetChannelName() {
		Map<String, SourceDetails> requestSourcesMap = new HashMap<>();
		SourceDetails sourceDetails = new SourceDetails();
		sourceDetails.setAcrsSubChannel("acrsSubChannel");
		sourceDetails.setChannelName("channelName");
		requestSourcesMap.put("source", sourceDetails);
    	when(requestSourceConfig.getRequestSourcesMap()).thenReturn(requestSourcesMap);

    	String channel = null;
    	channel = referenceDataDAOHelper.getChannelName("acrsSubChannel");

    	// Assertions
    	assertEquals("channelName", channel);
    }
    
    @Test
    void testIsAcrsEnabled() {
    	when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("secretValue");
    	when(acrsProperties.getAcrsPropertyListSecretKey()).thenReturn("acrsSecretKey");
    	boolean isAcrsEnabled = referenceDataDAOHelper.isAcrsEnabled();

    	// Assertions
    	assertTrue(isAcrsEnabled);
    }
    
    @Test
    void testIsPropertyManagedByAcrs() {
    	when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("secretValue");
    	when(acrsProperties.getAcrsPropertyListSecretKey()).thenReturn("acrsSecretKey");
    	boolean isPropertyManagedByAcrs = referenceDataDAOHelper.isPropertyManagedByAcrs(propertyId);

    	// Assertions
    	assertFalse(isPropertyManagedByAcrs);
    }
    
    private RoomReservation createRoomReservation() {
    	String propertyId = "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
    	String programId = "RPCD-v-TFRSA-d-PROP-v-MV021";
    	
    	RoomReservation roomReservation = new RoomReservation();
    	roomReservation.setPropertyId(propertyId);
    	roomReservation.setRoomTypeId("roomTypeId");
    	roomReservation.setProgramId(programId);
    	List<RoomPrice> bookings = new ArrayList<>();
    	RoomPrice booking = new RoomPrice();
    	booking.setOverrideProgramId(programId);
    	booking.setProgramId("programIdNotUuid");
    	bookings.add(booking);
    	roomReservation.setBookings(bookings);
    	
    	return roomReservation;
    }
}
