package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.refdata.AlertAndTraceSearchRefDataRes;
import com.mgm.services.booking.room.model.refdata.RefDataEntityRes;
import com.mgm.services.booking.room.model.refdata.RefDataEntitySearchRefReq;
import com.mgm.services.booking.room.model.refdata.RoutingAuthDataEntity;
import com.mgm.services.booking.room.model.refdata.RoutingAuthDataEntityList;
import com.mgm.services.booking.room.model.refdata.RoutingInfoRequest;
import com.mgm.services.booking.room.model.refdata.RoutingInfoResponseList;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

public class RefDataDAOImplTest extends BaseRoomBookingTest {
	
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
    
    @InjectMocks
    private static RefDataDAOImpl refDataDAOImpl;
    static Logger logger = LoggerFactory.getLogger(ComponentDAOStrategyACRSImplTest.class);

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
		refDataDAOImpl = new RefDataDAOImpl(urlProperties,restTemplateBuilder,domainProperties,applicationProperties,idmsTokenDAO);
   }
    
	@Test
    public void getRoutingAuthPhoenixIdTest() {
    	
    	String authorizer = "";
    	String propertyId ="dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
    	
    	MockHttpServletRequest request = new MockHttpServletRequest();
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    	
    	TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("token");
        when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
        when(domainProperties.getRefData()).thenReturn("");
        when(urlProperties.getRefDataRoutingAuthByAuthorizer()).thenReturn("");
    	
    	RoutingAuthDataEntity dataEntity = new RoutingAuthDataEntity();
    	dataEntity.setPhoenixId("phoenixId");
        RoutingAuthDataEntityList authResponse = new RoutingAuthDataEntityList();
        authResponse.add(dataEntity);
       

        ResponseEntity<RoutingAuthDataEntityList> entity = ResponseEntity.ok(authResponse);
        when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<RoutingAuthDataEntityList>>any(), Mockito.anyMap()))
                .thenReturn(entity);
        
         String response  =refDataDAOImpl.getRoutingAuthPhoenixId(authorizer, propertyId);
         assertNotNull(response);
         assertEquals("phoenixId",response.toString());             
    }
    
    @Test
    public void getRoutingAuthAppUserIdTest() {
    	String phoenixId = "";
    	
    	MockHttpServletRequest request = new MockHttpServletRequest();
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    
        when(domainProperties.getRefData()).thenReturn("");
        when(urlProperties.getRefDataRoutingAuthByPhoenixId()).thenReturn("");
        
    	RoutingAuthDataEntity dataEntity = new RoutingAuthDataEntity();
    	dataEntity.setAppUserId("appUserId");
        
        RoutingAuthDataEntityList authResponse = new RoutingAuthDataEntityList();
        authResponse.add(dataEntity);

        ResponseEntity<RoutingAuthDataEntityList> entity = ResponseEntity.ok(authResponse);
        when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<RoutingAuthDataEntityList>>any(), Mockito.anyMap()))
                .thenReturn(entity);
      
        String response  =refDataDAOImpl.getRoutingAuthAppUserId(entity, phoenixId);
        assertNotNull(response);
        assertEquals("appUserId",response.toString());
      
    }
    
    @Test
    public void getRoutingAuthAppPhoenixIdTest() {
    	String appUserId = "";
    	
    	MockHttpServletRequest request = new MockHttpServletRequest();
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
       
        when(domainProperties.getRefData()).thenReturn("");
        when(urlProperties.getRefDataRoutingAuthByAppUserId()).thenReturn("");
        
    	RoutingAuthDataEntity dataEntity = new RoutingAuthDataEntity();
        dataEntity.setPhoenixId("phoenixId");
        
        RoutingAuthDataEntityList authResponse = new RoutingAuthDataEntityList();
        authResponse.add(dataEntity);
        
        ResponseEntity<RoutingAuthDataEntityList> entity = ResponseEntity.ok(authResponse);
        when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<RoutingAuthDataEntityList>>any(), Mockito.anyMap()))
                .thenReturn(entity);
        String response = null;
        try {
       response= refDataDAOImpl.getRoutingAuthAppPhoenixId(entity, appUserId);
        assertNotNull(response);
        assertEquals("phoenixId",response.toString());
        }
        catch (Exception e){
        	assertNull (response);
        }
        
    }
        
    
    @Test
    public void searchRefDataEntityTest() {
    	RefDataEntitySearchRefReq alertAndTraceSearchRefDataReq = new RefDataEntitySearchRefReq();
    	TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("token");
        when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
    	
    	MockHttpServletRequest request = new MockHttpServletRequest();
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(domainProperties.getRefData()).thenReturn("");
        when(urlProperties.getRefDataAlertAndTraceSearch()).thenReturn("");
        RefDataEntityRes res = new RefDataEntityRes();
        res.setElements(new ArrayList<>());
        
        AlertAndTraceSearchRefDataRes authResponse = new AlertAndTraceSearchRefDataRes();
        
        authResponse.add(res);
        
        ResponseEntity<AlertAndTraceSearchRefDataRes> entity = ResponseEntity.ok(authResponse);
        when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<AlertAndTraceSearchRefDataRes>>any(), Mockito.anyMap()))
                .thenReturn(entity);
        
        AlertAndTraceSearchRefDataRes response =refDataDAOImpl.searchRefDataEntity(alertAndTraceSearchRefDataReq);
        assertNotNull(response);                
    }
    
    @Test
    public void  getRoutingInfoTest() {
    	HttpEntity<List<RoutingInfoRequest>> request =new HttpEntity<>(Collections.singletonList(new RoutingInfoRequest()));
    	
    	when(domainProperties.getRefData()).thenReturn("");
        when(urlProperties.getRefDataRoutingInfoSearch()).thenReturn("");
        
        RoutingInfoResponseList routingInfoResponseList = new RoutingInfoResponseList();
        ResponseEntity<RoutingInfoResponseList> entity = ResponseEntity.ok(routingInfoResponseList);
        when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(), ArgumentMatchers.<Class<RoutingInfoResponseList>>any(), Mockito.anyMap()))
                .thenReturn(entity);
        
        RoutingInfoResponseList response = refDataDAOImpl.getRoutingInfo(request);
        assertNotNull(response);

    }
}
