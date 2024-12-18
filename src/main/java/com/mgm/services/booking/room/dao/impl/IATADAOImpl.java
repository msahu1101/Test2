package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.mgm.services.booking.room.model.crs.guestprofiles.*;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.exception.ValidationException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IATADAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.crs.guestprofiles.OrganizationalSearchRequest;
import com.mgm.services.booking.room.model.crs.guestprofiles.OrganizationalSearchResponse;
import com.mgm.services.booking.room.model.crs.guestprofiles.OrganizationalSummaryArray;
import com.mgm.services.booking.room.model.crs.guestprofiles.Status;
import com.mgm.services.booking.room.model.request.OrganizationSearchV2Request;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.model.response.IATAResponse;
import com.mgm.services.booking.room.model.response.OrganizationSearchV2Response;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.transformer.OrganizationalSearchTransformer;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for validating the iata code and organization search
 */
@Component
@Log4j2
public class IATADAOImpl implements IATADAO {

    private RestTemplate client;
    private URLProperties urlProperties;
    private DomainProperties domainProperties;
    private AcrsProperties acrsProperties;
    private ReferenceDataDAOHelper referenceDataDAOHelper;
    private ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;
    private ApplicationProperties applicationProperties;
    private SecretsProperties secretsProperties;
    private static final String ICE = "ice";

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     * 
     * @param builder
     *            Spring's auto-configured rest template builder
     * @param domainProperties
     *            Domain Properties
     * @param applicationProperties
     *            application Properties
     * @param urlProperties
     *            url Properties
     * @param secretsProperties
     *            Secrets Properties
     */
	@Autowired
    public IATADAOImpl(RestTemplateBuilder builder, DomainProperties domainProperties, ApplicationProperties applicationProperties, AcrsProperties acrsProperties, URLProperties urlProperties, 
    		ReferenceDataDAOHelper referenceDataDAOHelper, ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl, SecretsProperties secretsProperties) {
        super();
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),
                1,
                applicationProperties.getCrsRestTTL());
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
        this.acrsProperties = acrsProperties;
        this.applicationProperties = applicationProperties;
        this.secretsProperties = secretsProperties;
        this.referenceDataDAOHelper = referenceDataDAOHelper;
        this.acrsOAuthTokenDAOImpl = acrsOAuthTokenDAOImpl;
        // Only send non-null
        ObjectMapper objMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL) 
        		.registerModule(ReservationUtil.getJavaTimeModuleISO())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.client.setMessageConverters(Collections.singletonList(new MappingJackson2HttpMessageConverter(objMapper)));
    }

    protected boolean isLiveCRSIATAEnabled() {
        if (referenceDataDAOHelper.isAcrsEnabled()) {
            return acrsProperties.isLiveCRSIata();
        }
        return false;
    }

    @Override
    public boolean validateCode(String iataCode) {
    	if (!isLiveCRSIATAEnabled()){
    		final IATAResponse response = getIATAResponse(iataCode);
			return response != null && StringUtils.isNotBlank(response.getId());
    	}else {
    		 final HttpEntity<OrganizationalSearchResponse> crsResponse = getOrganizationalSearchResponse(iataCode, null, ICE);
			OrganizationalSearchResponse crsResponseBody = crsResponse.getBody();
			if (null == crsResponseBody) {
				log.error("CRS Response body is empty in validateCode.");
				throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
			}
			return !CollectionUtils.isEmpty(crsResponseBody.getData()) && !CollectionUtils.isEmpty(crsResponseBody.getData().get(0).getIataCodes());
    	}
	}
    
    private IATAResponse getIATAResponse(String iataCode) {
    	HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        return client
                .getForEntity(domainProperties.getPhoenix().concat(urlProperties.getIataValidation()), IATAResponse.class, iataCode)
                .getBody();
    }
    
    private HttpEntity<OrganizationalSearchResponse> getOrganizationalSearchResponse(String iataCode, String orgName, String source) {
    	 OrganizationalSearchRequest organizationalSearchRequest = new OrganizationalSearchRequest();
         OrganizationalSummaryArray organizationalSummaryArray = new OrganizationalSummaryArray();
         FiltersType filters = new FiltersType();
         List<Filter> simpleFilters = new ArrayList<>();
         Filter filter = new Filter();

         if (StringUtils.isNotBlank(iataCode)) {
             filter.setKey(acrsProperties.getIataSimpleFiltersKey());
             filter.setOperator(Filter.OperatorEnum.valueOf("EQUALS"));
             filter.setValues(Collections.singletonList(iataCode));
             simpleFilters.add(filter);
             filters.setSimpleFilters(simpleFilters);
             organizationalSummaryArray.setFilters(Collections.singletonList(filters));
         } else if (StringUtils.isNotBlank(orgName)) {
             organizationalSummaryArray.setShortNames(Collections.singletonList(orgName));
         }        
         organizationalSummaryArray.setStatuses(Collections.singletonList(Status.ACTIVE));
         organizationalSearchRequest.setData(organizationalSummaryArray);
         
         Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(), acrsProperties.getOrganizationVersion(), acrsProperties.getChainCode());

		 final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);

		final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
		final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersPRFChannel();
		httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION,
						ServiceConstant.HEADER_AUTH_BEARER + acrsAuthTokenResponseMap.get(acrsVendor).getToken());
				
         final HttpEntity<OrganizationalSearchRequest> request = new HttpEntity<>(organizationalSearchRequest, httpHeaders);

		if (log.isDebugEnabled()) {
             log.debug("Sending request to Organization Search, Request headers {}:", CommonUtil.convertObjectToJsonString(request.getHeaders()));
             log.debug("Sending request to Organization Search, Request body {}: ", CommonUtil.convertObjectToJsonString(request.getBody()));
         }
		log.debug("Sending request to Organization Search, Request query parameters: " + uriParam);
		final HttpEntity<OrganizationalSearchResponse> crsResponse = client.exchange(
                 domainProperties.getCrs() + urlProperties.getAcrsOrganizationSearch(), HttpMethod.POST, request,
                 OrganizationalSearchResponse.class, uriParam);

         if (log.isDebugEnabled()) {
 			log.debug("Received headers from crs organization Search API: {}",
 					CommonUtil.convertObjectToJsonString(crsResponse.getHeaders()));
 			log.debug("Received response from crs organization Search API: {}",
 					CommonUtil.convertObjectToJsonString(crsResponse.getBody()));
 		 } else if(CommonUtil.isTempLogEnabled(secretsProperties.getSecretValue(applicationProperties.getTempInfoLogEnabled()))){
             log.info("Received headers from crs organization Search API: {}",
                     CommonUtil.convertObjectToJsonString(crsResponse.getHeaders()));
             log.info("Received response from crs organization Search API: {}",
                     CommonUtil.convertObjectToJsonString(crsResponse.getBody()));
        }
         return crsResponse;
    }

    @Override
    public List<OrganizationSearchV2Response> organizationSearch(OrganizationSearchV2Request organizationSearchRequest) {

		if (log.isDebugEnabled()) {
			log.debug("Incoming Request for organizationSearch: {}",
					CommonUtil.convertObjectToJsonString(organizationSearchRequest));
		}

		// IATA search GSE call
		if (!isLiveCRSIATAEnabled() && StringUtils.isNotBlank(organizationSearchRequest.getIataCode())) {
			final IATAResponse iataResponse = getIATAResponse(organizationSearchRequest.getIataCode());
			if (iataResponse == null) {
				throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_IATA_CODE.getErrorCode()));
			}
			return OrganizationalSearchTransformer.transform(iataResponse);
		}
        HttpEntity<OrganizationalSearchResponse> crsResponse = getOrganizationalSearchResponse(organizationSearchRequest.getIataCode(), organizationSearchRequest.getOrgName(),
        		organizationSearchRequest.getSource());

		OrganizationalSearchResponse crsResponseBody = crsResponse.getBody();
		if (null == crsResponseBody) {
			log.error("CRS Response body is empty in organizationSearch.");
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}

        if(!CollectionUtils.isEmpty(crsResponseBody.getData()) && crsResponseBody.getData().size() > 1)
        {
            throw new ValidationException(Collections.singletonList(ErrorCode.MULTIPLE_IATA_CODE_FOUND.getErrorCode()));
        }
		
        return OrganizationalSearchTransformer.transform(crsResponse.getBody());   
    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received during IATA operation: header: {} body: {}", httpResponse.getHeaders().toString(), response);
            if ( httpResponse.getStatusCode().value() >= 500 ) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
                //TODO handle errors
            }
        }
    }
}
