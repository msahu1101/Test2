package com.mgm.services.booking.room.dao.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.PaymentDAO;
import com.mgm.services.booking.room.model.DestinationHeader;
import com.mgm.services.booking.room.model.crs.reservation.ReservationModifyPendingRes;
import com.mgm.services.booking.room.model.crs.reservation.ReservationPartialModifyReq;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRetrieveResReservation;
import com.mgm.services.booking.room.model.paymentorchestration.*;
import com.mgm.services.booking.room.model.paymentorchestration.WorkflowArgs.OrderEnum;
import com.mgm.services.booking.room.model.paymentorchestration.WorkflowDefInner.FunctionNameEnum;
import com.mgm.services.booking.room.model.paymentservice.*;
import com.mgm.services.booking.room.model.request.PaymentTokenizeRequest;
import com.mgm.services.booking.room.model.response.PaymentTokenizeResponse;
import com.mgm.services.booking.room.properties.*;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.util.TokenizationEncryptionUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.DefaultUriBuilderFactory.EncodingMode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@Log4j2
public class PaymentDAOImpl implements PaymentDAO {

    private final URLProperties urlProperties;
    private DomainProperties domainProperties;
    private final RestTemplate client;
    private final ApplicationProperties applicationProperties;
    private IDMSTokenDAO idmsTokenDAO;
    private SecretsProperties secretsProperties;
    
    private static final String CORRELATIONID = "correlationId";
	
	private static final String CRS_SOURCE = "CRS";
	private static final String PPS_FUNCTION_CALLS ="PPS";
	private static final String PET_FUNCTION_CALLS ="PET";

    protected PaymentDAOImpl(URLProperties urlProperties, RestTemplate restTemplate, DomainProperties domainProperties,
            ApplicationProperties applicationProperties, SecretsProperties secretsProperties,
            IDMSTokenDAO idmsTokenDAO,AcrsProperties acrsProperties, RestTemplateBuilder builder) {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),
				applicationProperties.getPetConnectionPerRouteDaoImpl(),
				applicationProperties.getPetMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeoutPET(),
				applicationProperties.getReadTimeOutPET(),
				applicationProperties.getSocketTimeOutPET(),
				1,
				applicationProperties.getPetRestTTL());
        this.applicationProperties = applicationProperties;
        this.idmsTokenDAO = idmsTokenDAO;
        this.secretsProperties = secretsProperties;
        this.client.setMessageConverters(Collections.singletonList(ReservationUtil.createHttpMessageConverter()));
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
    }

	private Response sendRequestToPayment(Workflow workflow, HttpHeaders header) {

		HttpEntity<Response> orchestrationRes = null;		

		final HttpHeaders headers = new HttpHeaders();
		headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
		headers.set(ServiceConstant.HEADER_AUTHORIZATION, header.get("Authorization").get(0));
		List<String> txnId = header.get(ServiceConstant.X_MGM_TRANSACTION_ID);
		List<String> corrId = header.get(ServiceConstant.X_MGM_CORRELATION_ID);
		if(CollectionUtils.isNotEmpty(txnId)) {
			headers.set(ServiceConstant.X_MGM_TRANSACTION_ID, txnId.get(0));
		}else if(CollectionUtils.isNotEmpty(corrId)){
			headers.set(ServiceConstant.X_MGM_CORRELATION_ID, corrId.get(0));
		}
		CommonUtil.setAdditionalPaymentHeaders(headers);
		CommonUtil.setPaymentSourceAndChannelHeaders(headers);

		final String functionName = workflow.getDefinitions().get(0).getFunctionName().getValue();

		final HttpEntity<Workflow> httpEntity = new HttpEntity<>(workflow, headers);

			log.info(functionName + " : Orchestration request headers: {}",
					CommonUtil.convertObjectToJsonString(httpEntity.getHeaders()));
			log.info(functionName + " : Orchestration request body: {}",
					CommonUtil.convertObjectToJsonString(httpEntity.getBody()));

		try {
			LocalDateTime start = LocalDateTime.now();
			setThreadContextBeforeAPICall("PaymentService",
					urlProperties.getPaymentService(), start);

			orchestrationRes = client.exchange(
					domainProperties.getPaymentPPSOrchestration() + urlProperties.getPaymentService(), HttpMethod.POST,
					httpEntity, Response.class, createUriParam());

			setThreadContextAfterAPICall(start,HttpStatus.OK.toString(),PPS_FUNCTION_CALLS);


			log.info(functionName + " : Received Orchestration headers response: {}",
						CommonUtil.convertObjectToJsonString(orchestrationRes.getHeaders()));
			log.info(functionName + ": Received Orchestration body response: {}",
						CommonUtil.convertObjectToJsonString(orchestrationRes.getBody()));

		} catch (Exception e) {
			log.error("Fatal Exception during Payment Orchestration: " + functionName, e);
			throw new BusinessException(ErrorCode.PAYMENT_FAILED);
		}
		return orchestrationRes.getBody();
	}
	
    
    @Override
	public AuthResponse authorizePayment(HttpEntity<AuthRequest> authReq) {


			log.info("authorizePayment request headers: {}",
					CommonUtil.convertObjectToJsonString(authReq.getHeaders()));
			log.info("authorizePayment request body: {}", CommonUtil.convertObjectToJsonString(authReq.getBody()));

		
		final Workflow workflow = new Workflow();
		final WorkflowArgs workflowArgs = new WorkflowArgs();
		final WorkflowDef workflowDef = new WorkflowDef();
		final WorkflowDefInner workflowDefInner = new WorkflowDefInner();
		workflowArgs.addOrderItem(OrderEnum.PPSAUTHORIZE);
		workflowDefInner.setFunctionName(FunctionNameEnum.PPSAUTHORIZE);
		final Properties p = new Properties();
		if (!authReq.getHeaders().isEmpty() && authReq.getHeaders().containsKey(ServiceConstant.X_MGM_CORRELATION_ID)) {
			List<String> correlationIDList = authReq.getHeaders().get(ServiceConstant.X_MGM_CORRELATION_ID);
			if (CollectionUtils.isNotEmpty(correlationIDList)) {
				p.put(CORRELATIONID, correlationIDList.get(0));
			}
		} else if (null != authReq.getBody() && StringUtils.isNotEmpty(authReq.getBody().getTransactionRefCode())) {
			p.put(CORRELATIONID, authReq.getBody().getTransactionRefCode());
		}
		workflowDefInner.setHeaders(p);
		workflowDef.add(workflowDefInner);
		workflow.setArguments(workflowArgs);
		workflow.setDefinitions(workflowDef);
		workflowDefInner.setBody(authReq.getBody());

		final Response response = sendRequestToPayment(workflow, authReq.getHeaders());
		try {
			final Object body = response.getWorkflowResponse().get(0).getBody();
			final ObjectMapper objectMapper = new ObjectMapper();
			final AuthResponse authRes = objectMapper.readValue(objectMapper.writeValueAsString(body),
					AuthResponse.class);

			log.info("PPS AuthorizePayment body response: {}", CommonUtil.convertObjectToJsonString(authRes));

			return authRes;
		} catch (Exception e) {
			log.error("unable to deserialize Payment authorization: ", e);
			throw new BusinessException(ErrorCode.PAYMENT_AUTHORIZATION_FAILED);
		}
	}

	@Override
	public CaptureResponse capturePayment(HttpEntity<CaptureRequest> capReq) {


			log.info("capturePayment request headers: {}", CommonUtil.convertObjectToJsonString(capReq.getHeaders()));
			log.info("capturePayment request body: {}", CommonUtil.convertObjectToJsonString(capReq.getBody()));

		
		final Workflow workflow = new Workflow();
		final WorkflowArgs workflowArgs = new WorkflowArgs();
		final WorkflowDef workflowDef = new WorkflowDef();
		final WorkflowDefInner workflowDefInner = new WorkflowDefInner();
		workflowArgs.addOrderItem(OrderEnum.PPSCAPTURE);
		workflowDefInner.setFunctionName(FunctionNameEnum.PPSCAPTURE);
		final Properties p = new Properties();
		if (!capReq.getHeaders().isEmpty() && capReq.getHeaders().containsKey(ServiceConstant.X_MGM_CORRELATION_ID)) {
			List<String> correlationIDList = capReq.getHeaders().get(ServiceConstant.X_MGM_CORRELATION_ID);
			if (CollectionUtils.isNotEmpty(correlationIDList)) {
				p.put(CORRELATIONID, correlationIDList.get(0));
			}
		} else if (null != capReq.getBody() && StringUtils.isNotEmpty(capReq.getBody().getTransactionRefCode())) {
			p.put(CORRELATIONID, capReq.getBody().getTransactionRefCode());
		}
		workflowDefInner.setHeaders(p);
		workflowDef.add(workflowDefInner);
		workflow.setArguments(workflowArgs);
		workflow.setDefinitions(workflowDef);
		workflowDefInner.setBody(capReq.getBody());
		
		final Response response = sendRequestToPayment(workflow, capReq.getHeaders());
		try {
			final Object body = response.getWorkflowResponse().get(0).getBody();
			final ObjectMapper objectMapper = new ObjectMapper();
			final CaptureResponse capRes = objectMapper.readValue(objectMapper.writeValueAsString(body),
					CaptureResponse.class);

			log.info("PPS CapturePayment request body response: {}", CommonUtil.convertObjectToJsonString(capRes));

			return capRes;
		} catch (Exception e) {
			log.error("unable to deserialize capture Payment: ", e);
			throw new BusinessException(ErrorCode.PAYMENT_CAPTURE_FAILED);
		}

	}

    @Override
	public RefundResponse refundPayment(HttpEntity<RefundRequest> refReq) {

			log.info("refundPayment request headers: {}", CommonUtil.convertObjectToJsonString(refReq.getHeaders()));
			log.info("refundPayment request body: {}", CommonUtil.convertObjectToJsonString(refReq.getBody()));

		
		final Workflow workflow = new Workflow();
		final WorkflowArgs workflowArgs = new WorkflowArgs();
		final WorkflowDef workflowDef = new WorkflowDef();
		final WorkflowDefInner workflowDefInner = new WorkflowDefInner();
		workflowArgs.addOrderItem(OrderEnum.PPSREFUND);
		workflowDefInner.setFunctionName(FunctionNameEnum.PPSREFUND);
		final Properties p = new Properties();
		p.put(CORRELATIONID, refReq.getBody().getTransactionRefCode());
		workflowDefInner.setHeaders(p);
		workflowDef.add(workflowDefInner);
		workflow.setArguments(workflowArgs);
		workflow.setDefinitions(workflowDef);
		workflowDefInner.setBody(refReq.getBody());
		
		final Response response = sendRequestToPayment(workflow, refReq.getHeaders());
		try {

			final Object body = response.getWorkflowResponse().get(0).getBody();
			final ObjectMapper objectMapper = new ObjectMapper();
			final RefundResponse refRes = objectMapper.readValue(objectMapper.writeValueAsString(body),
					RefundResponse.class);

			log.info("PPS RefundPayment body response: {}", CommonUtil.convertObjectToJsonString(refRes));

			return refRes;
		} catch (Exception e) {
			log.error("unable to deserialize Payment refund: ", e);
			throw new BusinessException(ErrorCode.PAYMENT_REFUND_FAILED);
		}
	}

    @Override
    public String deTokenizeCreditCard(String token) {
        return "5555555555554444";
    }

    private Map<String, String> createUriParam() {
        final Map<String, String> uriParam = new HashMap<>();
        uriParam.put(ServiceConstant.PAYMENT_ENVIRONMENT, applicationProperties.getPaymentEnvironment());
        return uriParam;
    }
    
    private Map<String, String> createUriParamForToken(String destTokenPath, String cfNumber, String cardExpireDateXpath) {
        final Map<String, String> uriParam = new HashMap<>();
        uriParam.put(ServiceConstant.PAYMENT_ENVIRONMENT, applicationProperties.getPaymentEnvironment());
        uriParam.put(ServiceConstant.TOKEN_PATH, destTokenPath);
        uriParam.put(ServiceConstant.RESERVATION_ID, cfNumber);
        if(StringUtils.isNotEmpty(cardExpireDateXpath)) {
            uriParam.put(ServiceConstant.CARD_EXP_PATH, cardExpireDateXpath);  
        }
        return uriParam;
    }

    @Override
    public String tokenizeCreditCard(PaymentTokenizeRequest tokenizeRequest) {
        log.info("Sending request to tokenization to generate token");
        return Optional.ofNullable(getPaymentTokenizeResponse(tokenizeRequest))
                .map(this::transformPaymentTokenizeResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDIT_DETAILS));
    }

    private PaymentTokenizeResponse getPaymentTokenizeResponse(PaymentTokenizeRequest tokenizeRequest) {
        HttpEntity<Map<String, String>> request = getRequest(tokenizeRequest);
		LocalDateTime start = LocalDateTime.now();
		setThreadContextBeforeAPICall("Tokenize",
				urlProperties.getTokenize(), start);
		PaymentTokenizeResponse response = client.postForEntity(domainProperties.getTokenize().concat(urlProperties.getTokenize()), request,
                PaymentTokenizeResponse.class).getBody();
		setThreadContextAfterAPICall(start, HttpStatus.OK.toString(),"TOKENIZE");
		return response;
    }

    private String transformPaymentTokenizeResponse(PaymentTokenizeResponse response) {
        log.info("Received response from tokenization for generated token");
        return response.getToken();
    }

    private HttpEntity<Map<String, String>> getRequest(PaymentTokenizeRequest tokenizeRequest) {
        HttpHeaders headers = new HttpHeaders();
        String token = idmsTokenDAO.generateToken().getAccessToken();
        headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + token);
        headers.add(ServiceConstant.HEADER_KEY_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
        headers.add(ServiceConstant.HEADER_KEY_API_KEY,
                secretsProperties.getSecretValue(ServiceConstant.PAYMENT_TOKENIZE_API_KEY));

        Map<String, String> paymentInfo = new HashMap<>();
        paymentInfo.put(ServiceConstant.KEY_TRACKE, encryptPaymentDetails(tokenizeRequest));

        return new HttpEntity<Map<String, String>>(paymentInfo, headers);
    }

    private String encryptPaymentDetails(PaymentTokenizeRequest tokenizeRequest) {
        StringBuilder paymentInfoBuilder = new StringBuilder();

        paymentInfoBuilder.append("M");
        paymentInfoBuilder.append(tokenizeRequest.getCreditCard());
        paymentInfoBuilder.append(ServiceConstant.EQUAL);
        paymentInfoBuilder.append(tokenizeRequest.getExpirationYear());
        paymentInfoBuilder.append(tokenizeRequest.getExpirationMonth());

        if (tokenizeRequest.getCvv() != null) {
            paymentInfoBuilder.append(ServiceConstant.COLON);
            paymentInfoBuilder.append(tokenizeRequest.getCvv());
        }
        return TokenizationEncryptionUtil.encrypt(paymentInfoBuilder.toString(),
                secretsProperties.getSecretValue(ServiceConstant.PAYMENT_ENCRYPTION_PUBLIC_KEY));
    }

	@Override
	public AuthorizationTransactionResponse afsAuthorize(HttpEntity<AuthorizationTransactionRequest> afsReq) {

		log.info("afs authorize request headers: {}", CommonUtil.convertObjectToJsonString(afsReq.getHeaders()));
		log.info("afs authorize request body: {}", CommonUtil.convertObjectToJsonString(afsReq.getBody()));

		
		final Workflow workflow = new Workflow();
		final WorkflowArgs workflowArgs = new WorkflowArgs();
		final WorkflowDef workflowDef = new WorkflowDef();
		final WorkflowDefInner workflowDefInner = new WorkflowDefInner();
		workflowArgs.addOrderItem(OrderEnum.AFSAUTHORIZE);
		workflowDefInner.setFunctionName(FunctionNameEnum.AFSAUTHORIZE);
		final Properties p = CommonUtil.getForterHeaders();
		p.put(CORRELATIONID, UUID.randomUUID().toString());
		workflowDefInner.setHeaders(p);
		workflowDef.add(workflowDefInner);
		workflow.setArguments(workflowArgs);
		workflow.setDefinitions(workflowDef);
		workflowDefInner.setBody(afsReq.getBody());
		
		final Response response = sendRequestToPayment(workflow, afsReq.getHeaders());
		try {
			final Object body = response.getWorkflowResponse().get(0).getBody();
			final ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			final AuthorizationTransactionResponse afsRes = objectMapper.readValue(objectMapper.writeValueAsString(body),
					AuthorizationTransactionResponse.class);

				log.info("AFS AuthorizePayment body response: {}", CommonUtil.convertObjectToJsonString(afsRes));

			return afsRes;
		} catch (Exception e) {
			log.error("unable to deserialize afs authorize Payment: ", e);
			throw new BusinessException(ErrorCode.AFS_FAILURE);
		}
	}
	
	@Override
	public ReservationModifyPendingRes  sendRequestToPaymentExchangeToken(ReservationPartialModifyReq reservationPartialModifyReq,
            String tokenPath, DestinationHeader destinationHeader, String confirmationNo, boolean isPoFlow) {

		final String token = idmsTokenDAO.generateToken().getAccessToken();
        final HttpHeaders headers = CommonUtil.createPETHeaders(destinationHeader, CRS_SOURCE, token, isPoFlow);

        final HttpEntity<ReservationPartialModifyReq> httpEntity = new HttpEntity<>(reservationPartialModifyReq,
                headers);
        DefaultUriBuilderFactory handler = new DefaultUriBuilderFactory();
        client.setUriTemplateHandler(handler);
        handler.setEncodingMode(EncodingMode.NONE);
        final Map<String, String> uriParamForToken = createUriParamForToken(tokenPath, confirmationNo, null);


            log.info("Sending modify pending request to Payment Token, Request headers: {}",
                    CommonUtil.convertObjectToJsonString(httpEntity.getHeaders()));
            log.info("Sending modify pending request to Payment Token, Request body: {}", CommonUtil.convertObjectToJsonString(httpEntity.getBody()));
            log.info("Sending modify pending request to Payment Token, Request query parameters: {}", CommonUtil.convertObjectToJsonString(uriParamForToken));


		LocalDateTime start = LocalDateTime.now();
		setThreadContextBeforeAPICall("ModifyPendingPaymentExchangeToken",
				urlProperties.getModifyPendingPaymentExchangeToken(), start);

        ResponseEntity<ReservationModifyPendingRes> response = client.exchange(
                domainProperties.getPaymentOrchestration() + urlProperties.getModifyPendingPaymentExchangeToken(),
                HttpMethod.POST, httpEntity, ReservationModifyPendingRes.class, uriParamForToken);

		setThreadContextAfterAPICall(start, String.valueOf(response.getStatusCodeValue()),PET_FUNCTION_CALLS);

        if (response.getStatusCodeValue() >= 200 && response.getStatusCodeValue() < 300) {
            ReservationModifyPendingRes reservationRes = response.getBody();
            HttpHeaders httpHeaders = response.getHeaders();


            log.info("Received headers from payment token exchange partial Modify Pending Reservation : {}.",
                        CommonUtil.convertObjectToJsonString(httpHeaders));
            log.info("Received response from payment token exchange partial Modify Pending Reservation : {}.",
                        CommonUtil.convertObjectToJsonString(reservationRes));

            return response.getBody();
        } else {
            log.error("Fatal Exception during payment token exchange partial Modify Pending Reservation: ");
            throw new BusinessException(ErrorCode.RESERVATION_NOT_SUCCESSFUL,
                    CommonUtil.convertObjectToJsonString(response.getBody()));
        }

    }

    @Override
    public ReservationRetrieveResReservation sendRetrieveRequestToPaymentExchangeToken(String tokenPath,
            DestinationHeader destinationHeader, String confirmationNo, String cardExpireDateXpath) {


	    final String token = idmsTokenDAO.generateToken().getAccessToken();
	    final HttpHeaders headers = CommonUtil.createPETHeaders(destinationHeader, CRS_SOURCE, token, null);

        final HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        DefaultUriBuilderFactory handler = new DefaultUriBuilderFactory();
        client.setUriTemplateHandler(handler);
        handler.setEncodingMode(EncodingMode.NONE);
        final Map<String, String> uriParamForToken = createUriParamForToken(tokenPath, confirmationNo,
                cardExpireDateXpath);


            log.info("Sending find reservation request to Payment Token exchange, Request headers: {}",
                    CommonUtil.convertObjectToJsonString(httpEntity.getHeaders()));
            log.info("Sending find reservation request to Payment Token exchange, Request body: {}", CommonUtil.convertObjectToJsonString(httpEntity.getBody()));
            log.info("Sending find reservation request to Payment Token exchange, Request query parameters: {} ", CommonUtil.convertObjectToJsonString(uriParamForToken));


		LocalDateTime start = LocalDateTime.now();
		setThreadContextBeforeAPICall("FindReservationPaymentExchangeToken",
				urlProperties.getFindReservationPaymentExchangeToken(), start);

        ResponseEntity<ReservationRetrieveResReservation> response = client.exchange(
                domainProperties.getPaymentOrchestration() + urlProperties.getFindReservationPaymentExchangeToken(),
                HttpMethod.POST, httpEntity, ReservationRetrieveResReservation.class, uriParamForToken);

		setThreadContextAfterAPICall(start, String.valueOf(response.getStatusCodeValue()),PET_FUNCTION_CALLS);

		return logAndReturnPetResponseBody(response, "Payment Exchange Token", applicationProperties.isPermanentInfoLogEnabled());

    }

	private void setThreadContextBeforeAPICall(String apiName, String apiUrl, LocalDateTime start) {
		ThreadContext.put(ServiceConstant.DEPENDENT_SYSTEM_TYPE, ServiceConstant.PAYMENT_DEPENDENT_SYSTEM);
		ThreadContext.put(ServiceConstant.API_NAME_TYPE, apiName);
		ThreadContext.put(ServiceConstant.API_URL_TYPE, apiUrl);
		ThreadContext.put(ServiceConstant.TIME_TYPE, start.toString());
	}

	private void setThreadContextAfterAPICall(LocalDateTime start, String httpStatusCode, String paymentCallName) {
		long duration = ChronoUnit.MILLIS.between(start, LocalDateTime.now());
		ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
		ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpStatusCode));
		log.info("Custom Dimensions updated after Payment " + paymentCallName + " call");
	}

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received from Payment exchange token or payment API: header: {} body: {}", httpResponse.getHeaders().toString(), response);
			ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpResponse.getStatusCode()));
			try {
				LocalDateTime start = LocalDateTime.parse(ThreadContext.get(ServiceConstant.TIME_TYPE));
				long duration = ChronoUnit.MILLIS.between(start, LocalDateTime.now());
				ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
				log.info("Custom Dimensions updated after Payment call");
			} catch (Exception e) {
				// Do nothing
				log.warn("Caught Exception while updating custom dimensions in Payment Error Handling: ", e);
			}
            if (httpResponse.getStatusCode().value() >= 500) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
			} else if (httpResponse.getStatusCode().value() == 404) {
				throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, response);
			} else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, response);
            }

        }
    }

	protected <T> T logAndReturnPetResponseBody(ResponseEntity<T> petResponse, String apiName, boolean isInfoLogEnabled) {
		return ReservationUtil.logAndReturnPetResponseBody(petResponse, apiName, log, isInfoLogEnabled);
	}
}
