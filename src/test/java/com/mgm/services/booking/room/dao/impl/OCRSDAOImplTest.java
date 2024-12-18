package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.net.ssl.SSLException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.UserProfile;
import com.mgm.services.booking.room.model.ocrs.OcrsReservation;
import com.mgm.services.booking.room.model.ocrs.OcrsReservationList;
import com.mgm.services.booking.room.model.request.UpdateProfileRequest;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

public class OCRSDAOImplTest extends BaseRoomBookingTest{
	
	@Mock
    private static RestTemplate client;

    @Mock
    private static DomainProperties domainProperties;

    @Mock
    private static ApplicationProperties applicationProperties;

    @Mock
    private static RestTemplateBuilder restTemplateBuilder;

    @Mock
    private static URLProperties urlProperties;

    @Mock
    private static IDMSTokenDAO idmsTokenDAO;

    @Mock
    private static SecretsProperties secretsProperties;

    @InjectMocks
    private static OCRSDAOImpl ocrsDAOImpl;
    static Logger logger = LoggerFactory.getLogger(OCRSDAOImplTest.class);

    @BeforeClass
    public static void init() throws SSLException {
    	 client = Mockito.mock(RestTemplate.class);
    	 domainProperties = Mockito.mock(DomainProperties.class);
         urlProperties = Mockito.mock(URLProperties.class);
         restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
         applicationProperties = Mockito.mock(ApplicationProperties.class);
         applicationProperties.setApigeeEnvironment("nonprod-dev");
         idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);
         CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
         when(CommonUtil.getRestTemplate(restTemplateBuilder,
         		applicationProperties.isSslInsecure(),true,
 				applicationProperties.getConnectionPerRouteDaoImpl(),
 				applicationProperties.getMaxConnectionPerDaoImpl(),
 				applicationProperties.getConnectionTimeout(),
 				applicationProperties.getReadTimeOut(),
 				applicationProperties.getSocketTimeOut())).thenReturn(client);
         ocrsDAOImpl = new OCRSDAOImpl(urlProperties,restTemplateBuilder,domainProperties,applicationProperties,idmsTokenDAO,secretsProperties);
    }
   
    @Test
    public  void getOCRSResvPrimaryProfileTest() {
    	String cnfNumber = "3H2CRBLJ16";
    	
    	MockHttpServletRequest request = new MockHttpServletRequest();
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    	
    	when(domainProperties.getOcrsSearchReservation()).thenReturn("");
    	when(urlProperties.getOcrsSearchReservation()).thenReturn("");
    	
        OcrsReservationList list = new OcrsReservationList();
    	ResponseEntity<OcrsReservationList>  entity = ResponseEntity.ok(list);
    	when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<OcrsReservationList>>any(), Mockito.anyMap()))
                .thenReturn(entity);
    	
    	 UserProfile response = null;
         try {
        response= ocrsDAOImpl.getOCRSResvPrimaryProfile(entity, cnfNumber);
         assertNotNull(response);
         }
         catch (Exception e){
         	assertNull (response);
         }
    }
    
    @Test
    public void getOCRSReservationTest() {
    	String cnfNumber = "3H2CRBLJ16";
    	MockHttpServletRequest request = new MockHttpServletRequest();
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    	
    	TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("token");
        when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
    	when(domainProperties.getOcrsSearchReservation()).thenReturn("");
    	when(urlProperties.getOcrsSearchReservation()).thenReturn("");
    	
        OcrsReservationList list = new OcrsReservationList();
    	ResponseEntity<OcrsReservationList> entity = ResponseEntity.ok(list);
    	when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<OcrsReservationList>>any(), Mockito.anyMap()))
                .thenReturn(entity);
    	OcrsReservation response =ocrsDAOImpl.getOCRSReservation(cnfNumber);
      	assertNull(response);
      	verify(client).exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<OcrsReservation>>any(), Mockito.anyMap());
    }
    
    @Test
    public void updateProfileTest() {
    	UpdateProfileRequest request = new UpdateProfileRequest();
    	request.setOperaConfirmationNumber("3H2CRBLJ16");
    	MockHttpServletRequest request1 = new MockHttpServletRequest();
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request1));
    	
    	TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("token");
        when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
    	
    	when(domainProperties.getOcrsPartialReservationUpdate()).thenReturn("");
    	when(urlProperties.getOcrsPartialReservationUpdate()).thenReturn("");
    	
    	OcrsReservation updatedReservation = new OcrsReservation();
    	
    	updatedReservation.setReservationID("3H2CRBLJ16");
    	ResponseEntity<OcrsReservation> entity = ResponseEntity.ok(updatedReservation);

    	when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<OcrsReservation>>any(), Mockito.anyMap()))
                .thenReturn(entity);   
    	OcrsReservation response =ocrsDAOImpl.updateProfile(request);
      	assertNotNull(response);
        assertEquals("3H2CRBLJ16",response.getReservationID());
    }
    
}
