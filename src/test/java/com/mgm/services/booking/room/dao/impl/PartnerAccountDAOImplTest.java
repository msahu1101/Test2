package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.File;
import java.text.ParseException;

import javax.net.ssl.SSLException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.PartnerAccountDAO;
import com.mgm.services.booking.room.dao.PartnerAuthTokenDAO;
import com.mgm.services.booking.room.model.partneraccount.partnercustomerbasicinforesponse.PartnerCustomerBasicInfoResponse;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequest;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchresponse.PartnerCustomerSearchResponse;
import com.mgm.services.booking.room.model.request.PartnerAccountV2Request;
import com.mgm.services.booking.room.model.response.PartnerAccountsSearchV2Response;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.PartnerProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;

@RunWith(MockitoJUnitRunner.class)
public class PartnerAccountDAOImplTest extends BaseRoomBookingTest{
	
	private static DomainProperties domainProps;
	
	protected static  RestTemplate client;
	
	private static  ApplicationProperties applicationProperties;
	
	private static URLProperties urlProperties;
	
	@Mock
	private static PartnerProperties partnerProperties;
	
	@InjectMocks
	private static PartnerAccountDAOImpl partnerAccountDAOImpl;
	
	
	private static RestTemplateBuilder restTemplateBuilder;

	private PartnerAccountDAO partnerAccountDao;
	
	@Mock
	private static PartnerAuthTokenDAO partnerAuthDAO;
	static Logger logger = LoggerFactory.getLogger(PartnerAccountDAOImplTest.class);


	@BeforeClass
	public static void init() throws SSLException, ParseException {
		client = Mockito.mock(RestTemplate.class);
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		applicationProperties = Mockito.mock(ApplicationProperties.class);
		partnerProperties = Mockito.mock(PartnerProperties.class);
		applicationProperties.setPartnerSearchLive(true);
		partnerAuthDAO = Mockito.mock(PartnerAuthTokenDAO.class);
		domainProps = new DomainProperties();
		domainProps.setPartnerAccountBasic("https://gatewaydsapuat3.marriott.com");
		domainProps.setPartnerAccountSearch("https://gatewaydsapuat3.marriott.com");
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		urlProperties = new URLProperties();
		urlProperties.setPartnerAccountCustomerBasicInfo("/partners/{partnerVersion}/queries/members/pattern1");
		urlProperties.setPartnerAccountCustomerSearch("/{partnerVersion}/customers/profile/search");
		partnerProperties.setClientMaxConn(1);
		partnerProperties.setClientMaxConnPerRoute(1);
		partnerProperties.setConnectionTimeOut(1);
		partnerProperties.setClientMaxConn(1);
		partnerProperties.setSocketTimeOut(1);
		partnerProperties.setReadTimeOut(1);
		partnerProperties.setRetryCount(1);
		partnerProperties.setTtl(5);
			CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(), true,
				partnerProperties.getClientMaxConnPerRoute(),
				partnerProperties.getClientMaxConn(),
				partnerProperties.getConnectionTimeOut(),
				partnerProperties.getReadTimeOut(),
				partnerProperties.getSocketTimeOut(),
				partnerProperties.getRetryCount(),
				partnerProperties.getTtl())).thenReturn(client);
				partnerAccountDAOImpl = new PartnerAccountDAOImpl(domainProps,
				applicationProperties, restTemplateBuilder, urlProperties, partnerProperties,partnerAuthDAO);


	}
	
	private TokenResponse buildTokenResponse() {
		TokenResponse res = new TokenResponse();
		res.setAccessToken("tokenTest");
		return res;
	}

	private HttpEntity<?> getCustomerBasicInfoResponse() {
		File file = new File(getClass().getResource("/PartnerCustomerBasicInfoSampleResponse.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<PartnerCustomerBasicInfoResponse>(
				convertCrs(file, PartnerCustomerBasicInfoResponse.class), HttpStatus.OK);
		return response;

	}
	
	private void setMockForCustomerBasicInfo() {
		when(client.exchange(ArgumentMatchers.contains("members/pattern1"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<PartnerCustomerBasicInfoResponse>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<PartnerCustomerBasicInfoResponse>) getCustomerBasicInfoResponse());
	}
	
	private HttpEntity<?> getCustomerProfileSearch() {
		File file = new File(getClass().getResource("/PartnerCustomerSearchSampleResponse.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<PartnerCustomerSearchResponse>(
				convertCrs(file, PartnerCustomerSearchResponse.class), HttpStatus.OK);
		return response;

	}
	
    private void setMockForCustomerProfileSearch() {
		when(client.exchange(ArgumentMatchers.contains("profile/search"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<PartnerCustomerSearchResponse>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<PartnerCustomerSearchResponse>) getCustomerProfileSearch());
	}
	
	@Test
	public void searchPartnerAccount_success() {
		when(partnerAuthDAO.generateAuthToken()).thenReturn(buildTokenResponse());
		setMockForCustomerBasicInfo();
		PartnerAccountsSearchV2Response finalRes = partnerAccountDAOImpl.searchPartnerAccount( 
        		buildPartnerSearchReq());
		assertNotNull(finalRes);
	}
	
    @Test
    public void searchPartnerAccount_withCodeAndAccountNo() {
    	when(partnerAuthDAO.generateAuthToken()).thenReturn(buildTokenResponse());
		setMockForCustomerBasicInfo();
		PartnerAccountsSearchV2Response finalRes = partnerAccountDAOImpl.searchPartnerAccount( 
        		buildPartnerSearchReq());
		assertNotNull(finalRes);	
    }

    @Test
    public void searchPartnerAccount_AccountNoNull() {
    	when(partnerAuthDAO.generateAuthToken()).thenReturn(buildTokenResponse());
		setMockForCustomerProfileSearch();
		setMockForCustomerBasicInfo();
		PartnerAccountsSearchV2Response finalRes = partnerAccountDAOImpl.searchPartnerAccount( 
        		buildPartnerSearchReq1());
		assertNotNull(finalRes);
		
    }
    
    @Test
    public void searchPartnerAccount_withCodeAndEmail() {
    	when(partnerAuthDAO.generateAuthToken()).thenReturn(buildTokenResponse());
		setMockForCustomerProfileSearch();
		setMockForCustomerBasicInfo();
		PartnerAccountsSearchV2Response finalRes = partnerAccountDAOImpl.searchPartnerAccount( 
        		buildPartnerSearchReq1());
		assertNotNull(finalRes);
		
    }

    @Test
    public void searchPartnerAccount_withNameAndAccountno() {
    	when(partnerAuthDAO.generateAuthToken()).thenReturn(buildTokenResponse());
		setMockForCustomerBasicInfo();
		PartnerAccountsSearchV2Response finalRes = partnerAccountDAOImpl.searchPartnerAccount( 
        		buildPartnerSearchReq());
		assertNotNull(finalRes);	
    }
    
    
    @Test
    public void searchPartnerAccount_withAccountNoAndProgramCode() {
    	when(partnerAuthDAO.generateAuthToken()).thenReturn(buildTokenResponse());
		setMockForCustomerBasicInfo();
		PartnerAccountsSearchV2Response finalRes = partnerAccountDAOImpl.searchPartnerAccount( 
        		buildPartnerSearchReq2());
		assertNotNull(finalRes);	
    }
    
    @Test
    public void searchPartnerAccount_withEmailandCode() {
    	when(partnerAuthDAO.generateAuthToken()).thenReturn(buildTokenResponse());
		setMockForCustomerBasicInfo();
		PartnerAccountsSearchV2Response finalRes = partnerAccountDAOImpl.searchPartnerAccount( 
        		buildPartnerSearchReq3());
		assertNotNull(finalRes);	
    }
    
    
    @Test
    public void searchPartnerAccount_withNameandAccountNo() {
    	when(partnerAuthDAO.generateAuthToken()).thenReturn(buildTokenResponse());
		setMockForCustomerBasicInfo();
		PartnerAccountsSearchV2Response finalRes = partnerAccountDAOImpl.searchPartnerAccount( 
        		buildPartnerSearchReq4());
		assertNotNull(finalRes);	
    }
    
    
	private PartnerAccountV2Request buildPartnerSearchReq() {
		PartnerAccountV2Request req = PartnerAccountV2Request.builder()
				.firstName("john")
				.lastName("Doe")
				.emailAddress("john.doe@mail.com")
				.partnerAccountNo("1234556")
				.programCode("bv")
				.build();
		return req;
	} 
	
    private PartnerAccountV2Request buildPartnerSearchReq1() {
		PartnerAccountV2Request req = PartnerAccountV2Request.builder()
				.firstName("john")
				.lastName("Doe")
				.emailAddress("john.doe@mail.com")
				.partnerAccountNo("")
				.programCode("bv")
				.build();
		return req;
	} 
    
    private PartnerAccountV2Request buildPartnerSearchReq2() {
		PartnerAccountV2Request req = PartnerAccountV2Request.builder()
				.firstName("")
				.lastName("")
				.emailAddress("")
				.partnerAccountNo("1234567")
				.programCode("bv")
				.build();
		return req;
	} 
    
    private PartnerAccountV2Request buildPartnerSearchReq3() {
		PartnerAccountV2Request req = PartnerAccountV2Request.builder()
				.firstName("")
				.lastName("")
				.emailAddress("john.doe@mail.com")
				.partnerAccountNo("")
				.programCode("bv")
				.build();
		return req;
	}
    
    private PartnerAccountV2Request buildPartnerSearchReq4() {
		PartnerAccountV2Request req = PartnerAccountV2Request.builder()
				.firstName("john")
				.lastName("Doe")
				.emailAddress("")
				.partnerAccountNo("1234567")
				.programCode("")
				.build();
		return req;
	} 
    
    @Test(expected=BusinessException.class)
    public void searchPartnerAccount_Stringnull() {
    	when(partnerAuthDAO.generateAuthToken()).thenReturn(buildTokenResponse());
		setMockForCustomerBasicInfo();
    	setMockForCustomerProfileSearch2();
		PartnerAccountsSearchV2Response finalRes = partnerAccountDAOImpl.searchPartnerAccount( 
        		buildPartnerSearchReq1());
    }
    
    private void setMockForCustomerProfileSearch2() {
		ResponseEntity<?> response = new ResponseEntity<>(null, HttpStatus.OK);
		when(client.exchange(ArgumentMatchers.contains("profile/search"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<PartnerCustomerSearchResponse>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<PartnerCustomerSearchResponse>) response);
	}
	

    

}


