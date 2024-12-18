package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.content.CuratedOfferResponse;
import com.mgm.services.booking.room.model.content.Program;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

@RunWith(MockitoJUnitRunner.class)
public class ProgramContentDAOImplTest  extends BaseRoomBookingTest {
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
	private ProgramContentDAOImpl programContentDAOImpl;
	static Logger logger = LoggerFactory.getLogger(ProgramContentDAOImplTest.class);

	@BeforeEach
	public void init() {
		programContentDAOImpl = new ProgramContentDAOImpl(urlProperties, domainProperties, applicationProperties);
	}

	@Test
	public void getProgramContentTest() {
		String propertyId ="dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
		String programId = "RPCD-v-LSTD10N-d-PROP-v-MV275";
		when(programContentDAOImpl.getClient()).thenReturn(client);
		when(domainProperties.getContentapi()).thenReturn("");
		when(urlProperties.getProgramContentApi()).thenReturn("");
		Program expectedProgram = new Program();
		ResponseEntity<Program> entity =ResponseEntity.ok(expectedProgram);
		when(client.getForEntity("", Program.class,propertyId,programId)).thenReturn(entity);

		Program response = programContentDAOImpl.getProgramContent(propertyId, programId);
		assertNotNull(response);
		assertEquals(expectedProgram,response);
		verify(client).getForEntity("", Program.class,propertyId,programId);
	}

	@Test
	public void getCuratedHotelOffersTest() {
		String propertyId = "mgmresorts";
		
		when(programContentDAOImpl.getClient()).thenReturn(client);
		when(domainProperties.getContentapi()).thenReturn("");
		when(urlProperties.getCuratedOffersContentApi()).thenReturn("");
		CuratedOfferResponse offersResponse = new CuratedOfferResponse();
		ResponseEntity<CuratedOfferResponse> entity =ResponseEntity.ok(offersResponse);
		when(client.getForEntity("", CuratedOfferResponse.class,propertyId)).thenReturn(entity);
		
		CuratedOfferResponse response  =programContentDAOImpl.getCuratedHotelOffers(propertyId);
	    assertNotNull(response);
	    verify(client).getForEntity("",CuratedOfferResponse.class,propertyId);
		
	}
}
