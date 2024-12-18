package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mgm.services.booking.room.model.ocrs.SelectedMembership;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAwsCloudEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.PartnerAuthTokenDAO;
import com.mgm.services.booking.room.dao.PartnerAccountDAO;
import com.mgm.services.booking.room.model.PartnerAccountDetails;
import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.booking.room.model.partneraccount.partnercustomerbasicinforequest.PartnerCustomerBasicInfoRequest;
import com.mgm.services.booking.room.model.partneraccount.partnercustomerbasicinforesponse.PartnerCustomerBasicInfoResponse;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchrequest.PartnerCustomerSearchRequest;
import com.mgm.services.booking.room.model.partneraccount.partnercustomersearchresponse.PartnerCustomerSearchResponse;
import com.mgm.services.booking.room.model.request.PartnerAccountV2Request;
import com.mgm.services.booking.room.model.response.PartnerAccountsSearchV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.PartnerProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.transformer.PartnerAccountSearchTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class PartnerAccountDAOImpl implements PartnerAccountDAO{

	private DomainProperties domainProps;
	
	protected RestTemplate client;
	
	private ApplicationProperties applicationProperties;
	
	private URLProperties urlProperties;
	
	private PartnerProperties partnerProperties;
	
	private PartnerAuthTokenDAO customerSearchAuthToken;
	public PartnerAccountDAOImpl(DomainProperties domainProps,
			ApplicationProperties applicationProps, RestTemplateBuilder builder,URLProperties urlProperties, PartnerProperties partnerProperties, PartnerAuthTokenDAO customerSearchAuthToken) {
		this.domainProps = domainProps;
		this.applicationProperties = applicationProps;
		this.urlProperties = urlProperties;
		this.partnerProperties = partnerProperties;
		this.customerSearchAuthToken = customerSearchAuthToken;
		this.client = CommonUtil.getRetryableRestTemplate(builder, true, true,
				partnerProperties.getClientMaxConnPerRoute(),
				partnerProperties.getClientMaxConn(),
				partnerProperties.getConnectionTimeOut(),
				partnerProperties.getReadTimeOut(),
				partnerProperties.getSocketTimeOut(),
				partnerProperties.getRetryCount(),
				partnerProperties.getTtl());
		this.client.setErrorHandler(new SearchProfileErrorHandler());
	}
	
	static class SearchProfileErrorHandler  implements ResponseErrorHandler{

		@Override
		public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
			return httpResponse.getStatusCode().isError();
		}

		@Override
		public void handleError(ClientHttpResponse httpResponse) throws IOException {
			validateException(httpResponse);
		}
		public void validateException(ClientHttpResponse httpResponse) throws IOException {
			final String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
			final HttpStatus statusCode = httpResponse.getStatusCode();
            log.error("Error received Search Profile: status code: {}, header: {}, body: {}",
                    statusCode, httpResponse.getHeaders().toString(), response);
            if (statusCode == HttpStatus.UNAUTHORIZED) {
                throw new BusinessException(ErrorCode.TRANSACTION_NOT_AUTHORIZED, "Transaction is not Authorized");
            }
            if (statusCode == HttpStatus.NOT_FOUND) {
                throw new BusinessException(ErrorCode.INVALID_EMAIL, "Partner Account not found. Please verify the information provided");
            }
            else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }
			else if (statusCode == HttpStatus.UNPROCESSABLE_ENTITY) {
	            throw new BusinessException(ErrorCode.INVALID_PROFILE, "Invalid partner account provided");
            }
			else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, response);
            }
		}
		
    }
	@Override
	public PartnerAccountsSearchV2Response searchPartnerAccount(PartnerAccountV2Request partnerAccountRequest) {
		PartnerAccountsSearchV2Response partnerAccountExternalResponse = null;
		if(StringUtils.isEmpty(partnerAccountRequest.getPartnerAccountNo())) {
			// compose profile search api request data
			PartnerCustomerSearchRequest request = PartnerAccountSearchTransformer.composeProfileSearchRequest(partnerAccountRequest);
			

			log.info("Received request object from composeProfileSearchRequest method: {}", CommonUtil.convertObjectToJsonString(request));

			//make request to profile search API for customer profile
			PartnerCustomerSearchResponse profileSearchRes = customerProfileSearch(request);
			
			if(null == profileSearchRes ||
					(null != profileSearchRes
							&& CollectionUtils.isEmpty(profileSearchRes.getCustomers()))) {
				throw new BusinessException(ErrorCode.ITEM_NOT_FOUND,"Partner Account not found. Please search with account number");
			}

			partnerAccountExternalResponse = new PartnerAccountsSearchV2Response();
			PartnerAccountDetails accountDetails = new PartnerAccountDetails();
			List<PartnerAccounts> partnerAccountsList = new ArrayList<>();
			
			accountDetails.setPartnerAccounts(partnerAccountsList);
			
			profileSearchRes.getCustomers().stream().forEach(customer -> {
				PartnerCustomerBasicInfoResponse customerBasicInfoResponse = getCustomerBasicInfoData(customer.getCustomerIdentifiers().getCustomerId());
				updateNamingDetails(accountDetails,customerBasicInfoResponse);
				PartnerAccounts account = PartnerAccountSearchTransformer.composeAccountDataResponse(customerBasicInfoResponse,partnerAccountRequest,customer.getCustomerIdentifiers().getCustomerId());
				account.setMembershipLevel(partnerProperties.getMembershipLevel().get(account.getProgramSubcategory()));
				accountDetails.getPartnerAccounts().add(account);
			});
			partnerAccountExternalResponse.setPartnerAccountDetails(accountDetails);
		}else {
			//compose Basic Member data API request data
			PartnerCustomerBasicInfoRequest memberDataReq = PartnerAccountSearchTransformer.composeBasicMemberDataRequest(partnerAccountRequest.getPartnerAccountNo(), applicationProperties.getPartnerSearchMGMId());
			
			if (log.isDebugEnabled()) {
				log.debug("Received request object from composeBasicMemberDataRequest method: {}", CommonUtil.convertObjectToJsonString(memberDataReq));
			}
			//Make request to BasicMemberData API for CustomerInfo
			PartnerCustomerBasicInfoResponse basicMemberDataRes = retrieveBasicMemberData(memberDataReq);
			
			// Compose Response from basic member data to final partenAccount response
			partnerAccountExternalResponse = PartnerAccountSearchTransformer.composePartnerAccountResponse(basicMemberDataRes,memberDataReq, partnerAccountRequest,partnerProperties.getMembershipLevel());
		}
		
		return partnerAccountExternalResponse;
	}

	private void updateNamingDetails(PartnerAccountDetails accountDetails, PartnerCustomerBasicInfoResponse customerBasicInfoResponse) {
		accountDetails.setFirstName(customerBasicInfoResponse.getCustomerInfo().getName().getGivenName());
		accountDetails.setLastName(customerBasicInfoResponse.getCustomerInfo().getName().getSurname());

	}

	private PartnerCustomerBasicInfoResponse getCustomerBasicInfoData(String accountNumber) {
		
		//compose Basic Member data API request data
		PartnerCustomerBasicInfoRequest memberDataReq = PartnerAccountSearchTransformer.composeBasicMemberDataRequest(accountNumber,applicationProperties.getPartnerSearchMGMId());
	
		//Make request to BasicMemberData API for CustomerInfo
		return retrieveBasicMemberData(memberDataReq);
	}

	public PartnerCustomerBasicInfoResponse retrieveBasicMemberData(PartnerCustomerBasicInfoRequest memberDataReq) {
		final String authToken = customerSearchAuthToken.generateAuthToken().getAccessToken();
		
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+authToken);
		httpHeaders.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
		httpHeaders.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_JSON);
		httpHeaders.set(ServiceConstant.HEADER_CHANNEL, ServiceConstant.RBS_CHANNEL_HEADER);
		Map.Entry<String, String> clientRefId = CommonUtil.getPartnerClientRef();
		if(clientRefId != null) {
			httpHeaders.set(clientRefId.getKey(), clientRefId.getValue());
		}
		if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.X_USER_ID))) {
			httpHeaders.set(ServiceConstant.X_USER_ID, ThreadContext.get(ServiceConstant.X_USER_ID));
		}
		Map<String, String> uriParams = new HashMap<>();
		uriParams.put(ServiceConstant.PARTNER_VERSION_PARAM, applicationProperties.getPartnerVersion());
		HttpEntity<PartnerCustomerBasicInfoRequest> httpEntity = new HttpEntity<>(memberDataReq,httpHeaders);
		//@TODO add if(log.isDebugEnabled()) {
			log.info("Sending request to Partner customer basic info data, Request headers {}:", CommonUtil.convertObjectToJsonString(httpEntity.getHeaders()));
			log.info("Sending request to Partner customer basic info data, Request body {}:", CommonUtil.convertObjectToJsonString(httpEntity.getBody()));
			log.info("Calling Partner customer basic info data API : URI {} ", urlProperties.getPartnerAccountCustomerBasicInfo());

			final ResponseEntity<PartnerCustomerBasicInfoResponse> response = client.exchange(
					domainProps.getPartnerAccountBasic().concat(urlProperties.getPartnerAccountCustomerBasicInfo()),
					HttpMethod.POST,
					httpEntity,
					PartnerCustomerBasicInfoResponse.class,
					uriParams);


			log.info("Received headers from Partner customer basic info data API : {}", CommonUtil.convertObjectToJsonString(response.getHeaders()));
			log.info("Received response from Partner customer basic info data API: {}", CommonUtil.convertObjectToJsonString(response.getBody()));


			if (response.getStatusCodeValue() >= 200 && response.getStatusCodeValue() <= 300) {
				return response.getBody();
			}
			return null;
	}

	private PartnerCustomerSearchResponse customerProfileSearch(PartnerCustomerSearchRequest request) {		
		
		PartnerCustomerSearchResponse profileSearchRes = null;
		final String authToken = customerSearchAuthToken.generateAuthToken().getAccessToken();
		
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);
		httpHeaders.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
		httpHeaders.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_JSON);
		httpHeaders.set(ServiceConstant.HEADER_CHANNEL, ServiceConstant.RBS_CHANNEL_HEADER);
		httpHeaders.set("api-version", "1.0");
		Map.Entry<String, String> clientRefId = CommonUtil.getPartnerClientRef();
		if(clientRefId != null) {
			httpHeaders.set(clientRefId.getKey(), clientRefId.getValue());
		}
		if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.X_USER_ID))) {
			httpHeaders.set(ServiceConstant.X_USER_ID, ThreadContext.get(ServiceConstant.X_USER_ID));
		}
		final Map<String, String> uriParams = new HashMap<>();
        uriParams.put(ServiceConstant.PARTNER_VERSION_PARAM, applicationProperties.getPartnerVersion());
		HttpEntity<PartnerCustomerSearchRequest> entity = new HttpEntity<>(request,httpHeaders);
		//@TODO add if(log.isDebugEnabled()) {
			log.info("Calling Partner customer search API : URI {} ", urlProperties.getPartnerAccountCustomerSearch());
			log.info("Sending headers to Partner customer search  API : {}", CommonUtil.convertObjectToJsonString(entity.getHeaders()));
			log.info("Sending request body to Partner customer search  API : {}", CommonUtil.convertObjectToJsonString(entity.getBody()));
			final ResponseEntity<PartnerCustomerSearchResponse> response = client.exchange(
					domainProps.getPartnerAccountSearch().concat(urlProperties.getPartnerAccountCustomerSearch()),
					HttpMethod.POST,
					entity,
					PartnerCustomerSearchResponse.class,
					uriParams);
			log.info("Received headers from Partner customer search API: {}", CommonUtil.convertObjectToJsonString(response.getHeaders()));
			log.info("Received response from Partner customer search API: {}", CommonUtil.convertObjectToJsonString(response.getBody()));
			int status = response.getStatusCodeValue();
			if (status >= 200 && status <= 300) {
				return response.getBody();
			}
			return null;
	}

}
