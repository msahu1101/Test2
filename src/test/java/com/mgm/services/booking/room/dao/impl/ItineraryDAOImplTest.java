package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mgm.services.booking.room.util.CommonUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.Itinerary;
import com.mgm.services.booking.room.model.request.ItineraryServiceRequest;
import com.mgm.services.booking.room.model.response.ItineraryResponse;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

/**
 * Test class to provide unit test cases for ItineraryDAOImpl
 * 
 * @author vararora
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class ItineraryDAOImplTest {

    @Mock
    private static RestTemplate client;

    @Mock
    private IDMSTokenDAO idmsTokenDAO;

    @InjectMocks
    private static DomainProperties domainProperties;

    @InjectMocks
    private static ApplicationProperties applicationProperties;

    @InjectMocks
    private static RestTemplateBuilder restTemplateBuilder;

    @InjectMocks
    private static URLProperties urlProperties;

    @InjectMocks
    private static ItineraryDAOImpl itineraryDAOImpl;

   @BeforeClass
    public static void init() {
        client = Mockito.mock(RestTemplate.class);
        domainProperties = new DomainProperties();
        domainProperties.setItinerary("https://itinerary-service.com/service/{itineraryEnvironment}");
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
        applicationProperties = new ApplicationProperties();
        applicationProperties.setItineraryConnectionTimeout(3);
        applicationProperties.setItineraryReadTimeout(3);
        applicationProperties.setItineraryEnvironment("test123");
        urlProperties = new URLProperties();
        urlProperties.setItineraryService("/v2/{itineraryId}");
        urlProperties.setItineraryCreate("/v2/itinerary");
        urlProperties.setItineraryRetrieve("/v2/itinerary/room/{roomConfirmationNumber}");
        CommonUtil commonUtil = Mockito.spy(CommonUtil.class);

        when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(), true,
                applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getItineraryConnectionTimeout(),
                applicationProperties.getItineraryReadTimeout(),
                applicationProperties.getSocketTimeOut(),
                1,
                applicationProperties.getCommonRestTTL())).thenReturn(client);
        itineraryDAOImpl = new ItineraryDAOImpl(restTemplateBuilder, domainProperties, urlProperties,
                applicationProperties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    public void testUpdateItinerary() {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("accessToken");
        when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
        when(client.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), Mockito.<Class<String>> any(),
                Mockito.anyMap())).thenReturn(new ResponseEntity<String>("response", HttpStatus.OK));
        try {
            itineraryDAOImpl.updateCustomerItinerary("itineraryId", new ItineraryServiceRequest());
        } catch (Exception e) {
            fail("The updateCustomerItinerary method should not have thrown an exception");
        }

        verify(client, times(1)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                Mockito.<Class<String>> any(), Mockito.anyMap());
    }
    
    @Test
    public void testCreateCustomerItinerary() {
        // Mocking token generation
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("accessToken");
        when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
        
        ItineraryResponse itineraryResponse = new ItineraryResponse();
        itineraryResponse.setItinerary(new Itinerary());
        itineraryResponse.getItinerary().setItineraryId("itineraryId");
        when(client.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ItineraryResponse.class), anyMap()))
            .thenReturn(new ResponseEntity<>(itineraryResponse, HttpStatus.OK));
        
        
        try {
        	itineraryDAOImpl.createCustomerItinerary(new ItineraryServiceRequest());
        }  catch (Exception e) {
            fail("The testCreateCustomerItinerary method should not have thrown an exception");
        }

        verify(client, times(1)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                Mockito.<Class<ItineraryResponse>> any(), Mockito.anyMap());      
    }
    
    @Test
    public void testRetreiveCustomerItineraryDetailsByConfirmationNumber() {
        // Mocking
        String roomConfirmationNumber = "123456789";
       
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("accessToken");
        when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
        when(client.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), Mockito.<Class<ItineraryResponse>> any(),
                Mockito.anyMap())).thenReturn(new ResponseEntity<ItineraryResponse>(HttpStatus.OK));
        try {
             itineraryDAOImpl.retreiveCustomerItineraryDetailsByConfirmationNumber(roomConfirmationNumber);
        } catch (Exception e) {
            fail("The updateCustomerItinerary method should not have thrown an exception");
        }

        verify(client, times(1)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                Mockito.<Class<ItineraryResponse>> any(), Mockito.anyMap());
    }
}
