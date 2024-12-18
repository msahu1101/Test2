package com.mgm.services.booking.room.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.content.Room;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

@RunWith(MockitoJUnitRunner.class)
public class RoomContentDAOImplTest extends BaseRoomBookingTest {
	@Mock
	private static URLProperties urlProperties;
	@Mock
	private static DomainProperties domainProperties;
	@Mock
	private static ApplicationProperties applicationProperties;
	@Mock
	private static RestTemplate client;
	
	@InjectMocks
	@Spy
    private RoomContentDAOImpl roomContentDAOImpl;
	static Logger logger = LoggerFactory.getLogger(RoomContentDAOImplTest.class);
	
	@BeforeEach
	public void init() {
		roomContentDAOImpl = new RoomContentDAOImpl(urlProperties, domainProperties, applicationProperties);
	}
    
	
    @Test
    public void getRoomContentTestWithroomId() {
    	String roomId = "ROOMCD-v-SESK-d-PROP-v-MV275";
    	when(roomContentDAOImpl.getClient()).thenReturn(client);
    	when(domainProperties.getContentapi()).thenReturn("");
        when(urlProperties.getRoomContentApi()).thenReturn("");
		ResponseEntity<Room[]> entity =ResponseEntity.ok(new Room[] {new Room(), new Room()});
        when(client.getForEntity("", Room[].class, roomId)).thenReturn(entity);
        
        Room response = roomContentDAOImpl.getRoomContent(roomId);
        Assert.assertNotNull(response);
        verify(roomContentDAOImpl.getClient()).getForEntity("",Room[].class, roomId);
    }
    
    @Test
    public void getRoomContentTestWithoperaCode(){
    	String operaCode = "";
    	String operaPropertyCode = "";
    	boolean isPrimary = true;
    	when(roomContentDAOImpl.getClient()).thenReturn(client);
    	when(domainProperties.getContentapi()).thenReturn("");
    	when(urlProperties.getPropertyRoomContentApi()).thenReturn("");
		ResponseEntity<Room[]> entity =ResponseEntity.ok(new Room[] {new Room(), new Room()});
		Map<String, Object> uriParams = new HashMap<>();
		uriParams.put("operaCode", operaCode);
		uriParams.put("operaPropertyCode", operaPropertyCode);
		uriParams.put("primary", isPrimary);
		when(client.getForEntity("",Room[].class,uriParams)).thenReturn(entity);
		
		Room response = roomContentDAOImpl.getRoomContent(operaCode, operaPropertyCode, isPrimary);
		Assert.assertNotNull(response);
        verify(roomContentDAOImpl.getClient()).getForEntity("",Room[].class, uriParams);

    }
}
