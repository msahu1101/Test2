package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mgm.services.booking.room.util.ReservationUtil;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.RefDataDAO;
import com.mgm.services.booking.room.model.refdata.*;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

import lombok.extern.log4j.Log4j2;
@Component
@Log4j2
public class RefDataDAOImpl implements RefDataDAO{

    private final URLProperties urlProperties;
    private DomainProperties domainProperties;
    private final RestTemplate client;
    private IDMSTokenDAO idmsTokenDAO;
    private ApplicationProperties applicationProperties;

    protected RefDataDAOImpl(URLProperties urlProperties, RestTemplateBuilder builder, DomainProperties domainProperties,
                             ApplicationProperties applicationProperties, IDMSTokenDAO idmsTokenDAO) {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), true,
                applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),
                1,
                applicationProperties.getCommonRestTTL());
        this.idmsTokenDAO = idmsTokenDAO;
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
        this.applicationProperties = applicationProperties;
    }
    
    @Override
    public String getRoutingAuthAppUserId(HttpEntity<?> request, String phoenixId) {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("phoenixId", phoenixId);
        uriParams.put(ServiceConstant.APIGEE_ENVIRONMENT, applicationProperties.getApigeeEnvironment());
        log.info("routingAuth request headers: {}", request.getHeaders());

        try {
            ResponseEntity<RoutingAuthDataEntityList> routingAuthResponse = client.exchange(
                    domainProperties.getRefData() + urlProperties.getRefDataRoutingAuthByPhoenixId(), HttpMethod.GET,
                    request, RoutingAuthDataEntityList.class, uriParams);
            logAndReturnRefDataResponseBody(routingAuthResponse,"Routing Auth App User Id", applicationProperties.isPermanentInfoLogEnabled());
            return routingAuthResponse.getBody().get(0).getAppUserId();
        } catch (Exception e) {
           log.error("Error while retrieving appUserId  from PhoenixId");
           log.error(e.getMessage());
           throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        
        }

    }

    @Override
    public String getRoutingAuthAppPhoenixId(HttpEntity<?> request, String appUserId) {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("appUserId", appUserId);
        uriParams.put(ServiceConstant.APIGEE_ENVIRONMENT, applicationProperties.getApigeeEnvironment());
        
        log.info("routingAuth request headers: {}", request.getHeaders());

        try {
            ResponseEntity<RoutingAuthDataEntityList> routingAuthResponse = client.exchange(
                    domainProperties.getRefData() + urlProperties.getRefDataRoutingAuthByAppUserId(), HttpMethod.GET,
                    request, RoutingAuthDataEntityList.class, uriParams);
            logAndReturnRefDataResponseBody(routingAuthResponse,"Routing Auth App Phoenix Id", applicationProperties.isPermanentInfoLogEnabled());
            return routingAuthResponse.getBody().get(0).getPhoenixId();
        } catch (Exception e) {
            log.error("Error while retrieving PhoenixId from appUserId");
            log.error(e.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }

    }

    @Override
    public RoutingInfoResponseList getRoutingInfo(HttpEntity<List<RoutingInfoRequest>> request) {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put(ServiceConstant.APIGEE_ENVIRONMENT, applicationProperties.getApigeeEnvironment());
        
        log.info("RefData call request headers: {}", request.getHeaders());
        try {
            ResponseEntity<RoutingInfoResponseList> routingDetailsResponse = client.exchange(
                    domainProperties.getRefData() + urlProperties.getRefDataRoutingInfoSearch(), HttpMethod.POST,
                    request, RoutingInfoResponseList.class, uriParams);
            return logAndReturnRefDataResponseBody(routingDetailsResponse,"Routing Info", applicationProperties.isPermanentInfoLogEnabled());
        } catch (Exception e) {
            log.error("Error while invoking Ref Data API");
            log.error(e.getMessage());
            return new RoutingInfoResponseList();
        }
    }

	static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

		@Override
		public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
			return httpResponse.getStatusCode().isError();
		}

		@Override
		public void handleError(ClientHttpResponse httpResponse) throws IOException {
			String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
			log.error("Error received from  Ref Data API : header: {} body: {}", httpResponse.getHeaders().toString(), response);
			// TODO handle errors
			if (httpResponse.getStatusCode().value() >= 500) {
				throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
			} else {
			    throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION,response);
			}
		}
	}


    @Override
    public AlertAndTraceSearchRefDataRes searchRefDataEntity(
            RefDataEntitySearchRefReq alertAndTraceSearchRefDataReq) {
        HttpEntity<RefDataEntitySearchRefReq> request = getHttpEntity(alertAndTraceSearchRefDataReq);
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put(ServiceConstant.APIGEE_ENVIRONMENT, applicationProperties.getApigeeEnvironment());

        log.info("AlertAndTraceSearchRefData request {}", CommonUtil.convertObjectToJsonString(request));

        ResponseEntity<AlertAndTraceSearchRefDataRes> routingAuthResponse = client.exchange(
                domainProperties.getRefData() + urlProperties.getRefDataAlertAndTraceSearch(), HttpMethod.POST,
                request, AlertAndTraceSearchRefDataRes.class, uriParams);

        return logAndReturnRefDataResponseBody(routingAuthResponse,"Alert And Trace Search RefData",
                applicationProperties.isPermanentInfoLogEnabled());
    }
    
    private <T> HttpEntity<T> getHttpEntity(T request) {
        final String refDataToken = idmsTokenDAO.generateToken().getAccessToken();
        return new HttpEntity<>(request, CommonUtil.createHeaders(refDataToken));

    }

    @Override
    public String getRoutingAuthPhoenixId(String authorizer, String propertyId) {
        HttpEntity<?> request = new HttpEntity<>(CommonUtil.createHeaders(idmsTokenDAO.generateToken().getAccessToken()));
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put("authorizer", authorizer);
        uriParams.put("propertyId", propertyId);
        uriParams.put(ServiceConstant.APIGEE_ENVIRONMENT, applicationProperties.getApigeeEnvironment());
        log.info("RoutingAuth request headers: {}", request.getHeaders());
        try {
            ResponseEntity<RoutingAuthDataEntityList> routingAuthResponse = client.exchange(
                    domainProperties.getRefData() + urlProperties.getRefDataRoutingAuthByAuthorizer(), HttpMethod.GET,
                    request, RoutingAuthDataEntityList.class, uriParams);
            logAndReturnRefDataResponseBody(routingAuthResponse,"Routing Auth Phoenix Id", applicationProperties.isPermanentInfoLogEnabled());
            return routingAuthResponse.getBody().get(0).getPhoenixId();
        } catch (Exception e) {
            log.error("Error while retrieving PhoenixId from Routing Auth Authorizer and PropertyId : {}", e.getMessage());
            return null;
        }
    }

    protected <T> T logAndReturnRefDataResponseBody(ResponseEntity<T> refDataResponse, String apiName, boolean isInfoLogEnabled) {
        return ReservationUtil.logAndReturnRefDataResponseBody(refDataResponse, apiName, log, isInfoLogEnabled);
    }
}
