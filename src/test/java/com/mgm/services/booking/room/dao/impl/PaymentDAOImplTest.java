package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseIntegrationTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.DestinationHeader;
import com.mgm.services.booking.room.model.TestData;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.crs.reservation.ModificationChangesItem.OpEnum;
import com.mgm.services.booking.room.model.paymentorchestration.Response;
import com.mgm.services.booking.room.model.paymentorchestration.ResponseWorkflowResponse;
import com.mgm.services.booking.room.model.paymentservice.*;
import com.mgm.services.booking.room.model.request.PaymentTokenizeRequest;
import com.mgm.services.booking.room.model.response.PaymentTokenizeResponse;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.*;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.authorization.AuthorizationTransactionDetails;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PaymentDAOImplTest extends BaseIntegrationTest {

	@InjectMocks
	private static PaymentDAOImpl paymentDAOImpl;

	static Logger logger = LoggerFactory.getLogger(PaymentDAOImplTest.class);

	protected static WebClient identityClient;

	protected static String BEARER_TOKEN;

	private static RestTemplate client;

	private static String idmsUrl;

	private static URLProperties urlProperties;

	private static DomainProperties domainProperties;

	private static ApplicationProperties applicationProperties;

	private static IDMSTokenDAO idmsTokenDAO;

	private static RestTemplateBuilder restTemplateBuilder;

	private static AcrsProperties acrsProperties;

	private static SecretsProperties secretsProperties;

	private static Map<String, String> azureKeyVaultSecrets = new HashMap<>();

	protected static String testDataFileName = "/test-data.json";

	protected static TestData defaultTestData;

	private static HttpHeaders headers;

	@BeforeClass
	public static void init() throws SSLException, ParseException {
		Configurator.setAllLevels("", Level.ALL);
		envPrefix = System.getenv("envPrefix");
		deploymentEnv = System.getenv(TestConstant.DEPLOYMENT_ENV);

		if (System.getProperty("spring.profiles.active") != null
				&& System.getProperty("spring.profiles.active").contains("local")) {
			loadLocalVariables();
		} else {
			initializeEnvVariables();
		}

		if (StringUtils.isEmpty(baseUrl)) {
			baseUrl = "http://localhost:8080";
		}

		logger.info("Environment: {}", envPrefix);
		defaultTestData = getDefaultTestData(testDataFileName, TestData.class);

		initializeSecretsManager();

		client = Mockito.mock(RestTemplate.class);
		idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);
		domainProperties = new DomainProperties();
		domainProperties.setCrs("");
		domainProperties.setPaymentPPSOrchestration("https://mgm-nonprod-preprod.apigee.net");
		domainProperties.setTokenize("token");
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		applicationProperties = new ApplicationProperties();
		applicationProperties.setCrsUcpRetrieveResvEnvironment("q");
		applicationProperties.setPaymentEnvironment("nonprod-dev");
		acrsProperties = new AcrsProperties();
		acrsProperties.setModifySpecialRequestPath("modifySpecialRequestPath");
		acrsProperties.setModifyPartyConfirmationNumberPath("modifyPartyConfirmationNumberPath");
		acrsProperties.setLiveCRS(true);

		urlProperties = new URLProperties();
		urlProperties.setPaymentService("/mpay/v1/payment");
		urlProperties.setTokenize("token");
		
		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(),
				acrsProperties.isLiveCRS(), applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsMaxConnectionPerDaoImpl(), applicationProperties.getConnectionTimeout(),
				applicationProperties.getReadTimeOut(), applicationProperties.getSocketTimeOut(),1, applicationProperties.getPetRestTTL())).thenReturn(client);

		secretsProperties = Mockito.mock(SecretsProperties.class);

		paymentDAOImpl = new PaymentDAOImpl(urlProperties, client, domainProperties, applicationProperties,
				secretsProperties, idmsTokenDAO, acrsProperties, restTemplateBuilder);

		identityClient = WebClient.builder().baseUrl(idmsUrl).build();

		headers = new HttpHeaders();
		headers.add(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
		headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + "23412345");
		headers.add(ServiceConstant.X_MGM_CORRELATION_ID, UUID.randomUUID().toString());
		headers.add(ServiceConstant.X_MGM_TRANSACTION_ID, UUID.randomUUID().toString());
		headers.add(ServiceConstant.HEADER_USER_AGENT, UUID.randomUUID().toString());
		headers.add(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN, UUID.randomUUID().toString());
	}

	protected static void initializeSecretsManager() {
		azureKeyVaultSecrets.put(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE,
				System.getProperty(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE));
	}

	protected static void loadLocalVariables() {
		envPrefix = "qa";
		idmsUrl = "https://azdeapi-dev.mgmresorts.com/stg/identity/authorization/v1/mgmsvc/token";
		baseUrl = "http://localhost:8080";
	}

	protected static void initializeEnvVariables() {
		baseUrl = System.getenv("baseUrl");
		idmsUrl = (System.getenv("idmsUrl") != null ? System.getenv("idmsUrl")
				: "https://azdeapi-dev.mgmresorts.com/stg/identity/authorization/v1/mgmsvc/token");
		envPrefix = "qa";
	}

	@Test(expected = BusinessException.class)
	public void afsAuthorizeTest_BusinessException() {
		AuthorizationTransactionRequest authorizationTransactionRequest = new AuthorizationTransactionRequest();
		AuthorizationTransactionDetails authorizationTransactionDetails = new AuthorizationTransactionDetails();

		failureResponseClient();

		AuthorizationTransactionResponse authorizationTransactionResponse = new AuthorizationTransactionResponse();
		authorizationTransactionResponse.setAuthorizationRemarks("paid");
		authorizationTransactionResponse.setAuthorized(true);
		authorizationTransactionResponse.setReference("123");
		authorizationTransactionResponse.setTransactionId("123");

		authorizationTransactionRequest.setTransaction(authorizationTransactionDetails);
		HttpEntity<AuthorizationTransactionRequest> afsReq = new HttpEntity<>(authorizationTransactionRequest, headers);
		AuthorizationTransactionResponse actualResponse = paymentDAOImpl.afsAuthorize(afsReq);
		assertEquals(authorizationTransactionResponse, actualResponse);
	}

	private void failureResponseClient() {
		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<Response>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<Response>) failureResponse());
	}

	private HttpEntity<?> getAuthorizationTransactionResponse() {
		AuthorizationTransactionResponse authorizationTransactionResponse = new AuthorizationTransactionResponse();
		authorizationTransactionResponse.setAuthorizationRemarks("paid");
		authorizationTransactionResponse.setAuthorized(true);
		authorizationTransactionResponse.setReference("123");
		authorizationTransactionResponse.setTransactionId("123");
		Response response = new Response();
		List<ResponseWorkflowResponse> workflowResponseList = new ArrayList<>();
		ResponseWorkflowResponse responseWorkflowResponse = new ResponseWorkflowResponse();
		responseWorkflowResponse.setBody(authorizationTransactionResponse);
		workflowResponseList.add(responseWorkflowResponse);
		response.setWorkflowResponse(workflowResponseList);
		ResponseEntity<?> finalResponse = new ResponseEntity<>(response, HttpStatus.OK);
		return finalResponse;

	}

	private HttpEntity<?> failureResponse() {
		Response response = new Response();
		List<ResponseWorkflowResponse> workflowResponseList = new ArrayList<>();
		ResponseWorkflowResponse responseWorkflowResponse = new ResponseWorkflowResponse();
		responseWorkflowResponse.setBody("value");
		workflowResponseList.add(responseWorkflowResponse);
		response.setWorkflowResponse(workflowResponseList);
		ResponseEntity<?> finalResponse = new ResponseEntity<>(response, HttpStatus.OK);
		return finalResponse;

	}

	@Test
	public void afsAuthorizeTest_positive() {
		AuthorizationTransactionRequest authorizationTransactionRequest = new AuthorizationTransactionRequest();
		AuthorizationTransactionDetails authorizationTransactionDetails = new AuthorizationTransactionDetails();

		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<Response>>any(), Mockito.anyMap()))
				.thenReturn((ResponseEntity<Response>) getAuthorizationTransactionResponse());

		AuthorizationTransactionResponse authorizationTransactionResponse = new AuthorizationTransactionResponse();
		authorizationTransactionResponse.setAuthorizationRemarks("paid");
		authorizationTransactionResponse.setAuthorized(true);
		authorizationTransactionResponse.setReference("123");
		authorizationTransactionResponse.setTransactionId("123");

		authorizationTransactionRequest.setTransaction(authorizationTransactionDetails);
		HttpEntity<AuthorizationTransactionRequest> afsReq = new HttpEntity<>(authorizationTransactionRequest, headers);
		AuthorizationTransactionResponse actualResponse = paymentDAOImpl.afsAuthorize(afsReq);
		assertEquals(authorizationTransactionResponse, actualResponse);
	}

	private HttpEntity<?> getAuthResponse() {
		Response response = new Response();
		List<ResponseWorkflowResponse> workflowResponseList = new ArrayList<>();
		ResponseWorkflowResponse responseWorkflowResponse = new ResponseWorkflowResponse();
		responseWorkflowResponse.setBody(getAuthResponseBody());
		workflowResponseList.add(responseWorkflowResponse);
		response.setWorkflowResponse(workflowResponseList);
		ResponseEntity<?> finalResponse = new ResponseEntity<>(response, HttpStatus.OK);
		return finalResponse;

	}

	private AuthResponse getAuthResponseBody() {
		AuthResponse authResponse = new AuthResponse();
		authResponse.setAmount("1000");
		authResponse.setAuthRequestId("123");
		authResponse.setDecision("paid");
		authResponse.setReasonCode(200);
		authResponse.setStatusMessage("success");
		return authResponse;
	}

	@Test
	public void authorizePayment_positive() {
		AuthRequest authRequest = new AuthRequest();
		authRequest.setAmount("10000");
		authRequest.setMerchantID("123");
		authRequest.setTransactionRefCode("123");

		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<Response>>any(), Mockito.anyMap()))
				.thenReturn((ResponseEntity<Response>) getAuthResponse());

		HttpEntity<AuthRequest> afsReq = new HttpEntity<>(authRequest, headers);
		AuthResponse actualResponse = paymentDAOImpl.authorizePayment(afsReq);
		assertEquals(getAuthResponseBody(), actualResponse);
	}

	@Test(expected = BusinessException.class)
	public void authorizePayment_Exception() {
		AuthRequest authRequest = new AuthRequest();
		authRequest.setAmount("10000");
		authRequest.setMerchantID("123");
		authRequest.setTransactionRefCode("123");

		failureResponseClient();

		HttpEntity<AuthRequest> afsReq = new HttpEntity<>(authRequest, headers);
		AuthResponse actualResponse = paymentDAOImpl.authorizePayment(afsReq);
		assertEquals(getAuthResponseBody(), actualResponse);
	}

	private HttpEntity<?> getCaptureResponseResponse() {
		Response response = new Response();
		List<ResponseWorkflowResponse> workflowResponseList = new ArrayList<>();
		ResponseWorkflowResponse responseWorkflowResponse = new ResponseWorkflowResponse();
		responseWorkflowResponse.setBody(getCaptureResponseBody());
		workflowResponseList.add(responseWorkflowResponse);
		response.setWorkflowResponse(workflowResponseList);
		ResponseEntity<?> finalResponse = new ResponseEntity<>(response, HttpStatus.OK);
		return finalResponse;

	}

	private CaptureResponse getCaptureResponseBody() {
		CaptureResponse response = new CaptureResponse();
		response.setAmount("1000");
		response.setDecision("paid");
		response.setReasonCode(200);
		response.setStatusMessage("success");
		return response;
	}

	@Test
	public void capturePayment_positive() {
		CaptureRequest request = new CaptureRequest();
		request.setAmount("10000");
		request.setMerchantID("123");
		request.setTransactionRefCode("123");

		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<Response>>any(), Mockito.anyMap()))
				.thenReturn((ResponseEntity<Response>) getCaptureResponseResponse());

		HttpEntity<CaptureRequest> afsReq = new HttpEntity<>(request, headers);
		CaptureResponse actualResponse = paymentDAOImpl.capturePayment(afsReq);
		assertEquals(getCaptureResponseBody(), actualResponse);
	}

	@Test(expected = BusinessException.class)
	public void capturePayment_exception() {
		CaptureRequest request = new CaptureRequest();
		request.setAmount("10000");
		request.setMerchantID("123");
		request.setTransactionRefCode("123");

		failureResponseClient();

		HttpEntity<CaptureRequest> afsReq = new HttpEntity<>(request, headers);
		CaptureResponse actualResponse = paymentDAOImpl.capturePayment(afsReq);
		assertEquals(getAuthResponseBody(), actualResponse);
	}

	@Test(expected = BusinessException.class)
	public void sendRequestToPayment_exception() {
		CaptureRequest request = new CaptureRequest();
		request.setAmount("10000");
		request.setMerchantID("123");
		request.setTransactionRefCode("123");

		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<Response>>any(), Mockito.anyMap()))
				.thenThrow(new RuntimeException());

		HttpEntity<CaptureRequest> afsReq = new HttpEntity<>(request, headers);
		CaptureResponse actualResponse = paymentDAOImpl.capturePayment(afsReq);
		assertEquals(getAuthResponseBody(), actualResponse);
	}

	private HttpEntity<?> getRefundResponseResponse() {
		Response response = new Response();
		List<ResponseWorkflowResponse> workflowResponseList = new ArrayList<>();
		ResponseWorkflowResponse responseWorkflowResponse = new ResponseWorkflowResponse();
		responseWorkflowResponse.setBody(getRefundResponseBody());
		workflowResponseList.add(responseWorkflowResponse);
		response.setWorkflowResponse(workflowResponseList);
		ResponseEntity<?> finalResponse = new ResponseEntity<>(response, HttpStatus.OK);
		return finalResponse;

	}

	private RefundResponse getRefundResponseBody() {
		RefundResponse response = new RefundResponse();
		response.setAmount("1000");
		response.setDecision("paid");
		response.setReasonCode(200);
		response.setStatusMessage("success");
		return response;
	}

	@Test
	public void refundPayment_positive() {
		RefundRequest request = new RefundRequest();
		request.setAmount("10000");
		request.setMerchantID("123");
		request.setTransactionRefCode("123");

		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<Response>>any(), Mockito.anyMap()))
				.thenReturn((ResponseEntity<Response>) getRefundResponseResponse());

		HttpEntity<RefundRequest> afsReq = new HttpEntity<>(request, headers);
		RefundResponse actualResponse = paymentDAOImpl.refundPayment(afsReq);
		assertEquals(getRefundResponseBody(), actualResponse);
	}

	@Test(expected = BusinessException.class)
	public void refundPayment_exception() {
		RefundRequest request = new RefundRequest();
		request.setAmount("10000");
		request.setMerchantID("123");
		request.setTransactionRefCode("123");

		failureResponseClient();

		HttpEntity<RefundRequest> afsReq = new HttpEntity<>(request, headers);
		RefundResponse actualResponse = paymentDAOImpl.refundPayment(afsReq);
		assertEquals(getAuthResponseBody(), actualResponse);
	}

	@Test
	public void sendRequestToPaymentExchangeToken_positive() {
		
		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationModifyPendingRes>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<ReservationModifyPendingRes>) new ResponseEntity<>(getReservationModifyPendingResBody(), HttpStatus.OK));
		
		TokenResponse tknRes = new TokenResponse();
        tknRes.setAccessToken("1234");
        Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
        
		ReservationModifyPendingRes actualResponse = paymentDAOImpl.sendRequestToPaymentExchangeToken(getReservationPartialModifyReq(),"/v1/modify",
				getDestinationHeader(),"123",true);
		assertEquals(getReservationModifyPendingResBody(),actualResponse);
	}

	@Test(expected = BusinessException.class)
	public void sendRequestToPaymentExchangeToken_failure() {
		
		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationModifyPendingRes>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<ReservationModifyPendingRes>) new ResponseEntity<>(getReservationModifyPendingResBody(), HttpStatus.BAD_REQUEST));
		
		TokenResponse tknRes = new TokenResponse();
        tknRes.setAccessToken("1234");
        Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
        
		ReservationModifyPendingRes actualResponse = paymentDAOImpl.sendRequestToPaymentExchangeToken(getReservationPartialModifyReq(),"/v1/modify",
				getDestinationHeader(),"123",true);
		assertEquals(getReservationModifyPendingResBody(),actualResponse);
	}

	private ReservationPartialModifyReq getReservationPartialModifyReq() {
		ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
		ModificationChanges modificationChanges = new ModificationChanges();
		ModificationChangesItem modificationChangesItem = new ModificationChangesItem();
		modificationChangesItem.setOp(OpEnum.APPEND);
		modificationChangesItem.setPath("/v1/modify");
		modificationChanges.add(modificationChangesItem);
		reservationPartialModifyReq.setData(modificationChanges);
		return reservationPartialModifyReq;
	}

	private DestinationHeader getDestinationHeader() {
		DestinationHeader destinationHeader = new DestinationHeader();
		destinationHeader.setAccept("Application/json");
		destinationHeader.setContentType("Application/json");
		destinationHeader.setHttpMethod("POST");
		destinationHeader.setXAuthorization(TestConstant.OKTA_AUTHORIZATION_HEADER_VALUE);
		return destinationHeader;
	}

	private ReservationModifyPendingRes getReservationModifyPendingResBody() {
		ReservationModifyPendingRes response = new ReservationModifyPendingRes();
		ReservationModifyPendingResData data = new ReservationModifyPendingResData();

		PointOfSale creator = new PointOfSale();
		creator.setOrigin("US");
		creator.setVendorCode("VendorCode");
		creator.setVendorName("VendorName");
		data.setCreator(creator);

		PointOfSale requestor = new PointOfSale();
		requestor.setOrigin("US");
		requestor.setVendorCode("VendorCode");
		requestor.setVendorName("VendorName");
		data.setRequestor(requestor);

		response.setData(data);
		return response;
	}

	@Test
	public void sendRetrieveRequestToPaymentExchangeToken_positive() {
		
		when(client.exchange(ArgumentMatchers.anyString(), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>>any(),
				Mockito.anyMap()))
						.thenReturn((ResponseEntity<ReservationRetrieveResReservation>) new ResponseEntity<>(getReservationRetrieveResReservationBody(), HttpStatus.OK));
		
		TokenResponse tknRes = new TokenResponse();
        tknRes.setAccessToken("1234");
        Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
        
        ReservationRetrieveResReservation actualResponse = paymentDAOImpl.sendRetrieveRequestToPaymentExchangeToken("/v1/modify",
				getDestinationHeader(),"123",new LocalDate().toString());
		assertEquals(getReservationRetrieveResReservationBody(),actualResponse);
	}

	private ReservationRetrieveResReservation getReservationRetrieveResReservationBody() {
		ReservationRetrieveResReservation response = new ReservationRetrieveResReservation();
		ReservationRetrieveResreservationData data = new ReservationRetrieveResreservationData();
		PointOfSale requestor = new PointOfSale();
		requestor.setOrigin("US");
		requestor.setVendorCode("VendorCode");
		requestor.setVendorName("VendorName");
		data.setRequestor(requestor);
		response.setData(data);
		return response;
	}

	@Test(expected = BusinessException.class)
	public void tokenizeCreditCard_exception() {
		PaymentTokenizeRequest tokenizeRequest = new PaymentTokenizeRequest();
		tokenizeRequest.setCardType("VISA");
		tokenizeRequest.setCreditCard("Platinum");
		tokenizeRequest.setCvv("123");
		tokenizeRequest.setExpirationMonth(LocalDate.now().plusDays(15).toString());
		tokenizeRequest.setExpirationYear(LocalDate.now().plusYears(2).toString());	
		
		TokenResponse tknRes = new TokenResponse();
        tknRes.setAccessToken("1234");
		Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
		when(secretsProperties.getSecretValue("freedompay-public-key")).thenReturn("test");
		
		when(client.postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(),ArgumentMatchers.<Class<PaymentTokenizeResponse>>any()))
						.thenReturn((ResponseEntity<PaymentTokenizeResponse>) new ResponseEntity<>(getPaymentTokenizeResponse(), HttpStatus.OK));
		
		String actualResponse = paymentDAOImpl.tokenizeCreditCard(tokenizeRequest);
	}
	
	@Test
	public void tokenizeCreditCard_positive() {
		PaymentTokenizeRequest tokenizeRequest = new PaymentTokenizeRequest();
		tokenizeRequest.setCardType("VISA");
		tokenizeRequest.setCreditCard("Platinum");
		tokenizeRequest.setCvv("123");
		tokenizeRequest.setExpirationMonth(LocalDate.now().plusDays(15).toString());
		tokenizeRequest.setExpirationYear(LocalDate.now().plusYears(2).toString());	
		
		TokenResponse tknRes = new TokenResponse();
        tknRes.setAccessToken("1234");
		Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
		when(secretsProperties.getSecretValue("freedompay-public-key")).thenReturn("test");
		
		when(client.postForEntity(ArgumentMatchers.anyString(), ArgumentMatchers.any(),ArgumentMatchers.<Class<PaymentTokenizeResponse>>any()))
						.thenReturn((ResponseEntity<PaymentTokenizeResponse>) new ResponseEntity<>(getPaymentTokenizePositiveResponse(), HttpStatus.OK));
		
		String actualResponse = paymentDAOImpl.tokenizeCreditCard(tokenizeRequest);
		assertEquals("token",actualResponse);
	}
	
	private PaymentTokenizeResponse getPaymentTokenizeResponse() {
		PaymentTokenizeResponse paymentTokenizeResponse=new PaymentTokenizeResponse();
		paymentTokenizeResponse.setCardType("VISA");
		return paymentTokenizeResponse;
	}
	
	private PaymentTokenizeResponse getPaymentTokenizePositiveResponse() {
		PaymentTokenizeResponse paymentTokenizeResponse=new PaymentTokenizeResponse();
		paymentTokenizeResponse.setCardType("VISA");
		paymentTokenizeResponse.setToken("token");
		return paymentTokenizeResponse;
	}

	@Test
	public void testHasError() throws IOException {
		PaymentDAOImpl.RestTemplateResponseErrorHandler errorHandler = 
				new PaymentDAOImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);
		
		// Assertion
		assertTrue(result);
	}
	
	@Test
	public void testHandleError() throws IOException {
		PaymentDAOImpl.RestTemplateResponseErrorHandler errorHandler = 
				new PaymentDAOImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn(null);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

		// Assertion
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.RESERVATION_NOT_FOUND, ex.getErrorCode());
	}
	
	@Test
	public void testHandleErrorElse() throws IOException {
		PaymentDAOImpl.RestTemplateResponseErrorHandler errorHandler = 
				new PaymentDAOImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getBody()).thenReturn(null);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);

		// Assertion
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
}
