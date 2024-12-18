package com.mgm.services.booking.room.dao.impl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

@RunWith(MockitoJUnitRunner.class)
public class PropertyContentDAOImplTest extends BaseRoomBookingTest{
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
	private PropertyContentDAOImpl propertyContentDAOImpl;
	static Logger logger = LoggerFactory.getLogger(PropertyContentDAOImplTest.class);

	@BeforeEach
	public void init() {
		propertyContentDAOImpl = new PropertyContentDAOImpl(urlProperties, domainProperties, applicationProperties);
	}
	@Test
	public void getAllPropertiesContentTest() {
		when(propertyContentDAOImpl.getClient()).thenReturn(client);
		when(domainProperties.getContentapi()).thenReturn("");
		when(urlProperties.getPropertyContentApi()).thenReturn("");
		ResponseEntity<Property[]> entity =ResponseEntity.ok(new Property[] {new Property(), new Property()});
		when(client.getForEntity("", Property[].class)).thenReturn(entity);
		List<Property> propertyList =propertyContentDAOImpl.getAllPropertiesContent();
		assertNotNull(propertyList);
		verify(propertyContentDAOImpl.getClient()).getForEntity("", Property[].class);
	}
}
