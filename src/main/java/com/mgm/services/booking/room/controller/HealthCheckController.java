package com.mgm.services.booking.room.controller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import com.mgm.services.booking.room.model.response.AcrsPropertiesResponse;
import com.mgm.services.booking.room.properties.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.impl.ACRSOAuthTokenDAOImpl;
import com.mgm.services.booking.room.exception.AuroraError;
import com.mgm.services.booking.room.model.crs.calendarsearches.BodyParameterCalendar;
import com.mgm.services.booking.room.model.crs.calendarsearches.SuccessfulCalendarAvailability;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgmresorts.aurora.messages.Credentials;
import com.mgmresorts.aurora.messages.GetRoomPricingAndAvailabilityExRequest;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.service.Client;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * Controller to handle health-check end points.
 * 
 * @author jayveera
 */
@RestController
@RequestMapping(value = "/health-check")
@Log4j2
public class HealthCheckController {

    @Autowired
    private AuroraProperties properties;
    @Autowired
    private AcrsProperties acrsProperties;
    @Autowired
    private URLProperties urlProperties;
    @Autowired
    private DomainProperties domainProperties;
    @Autowired
    private ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;
    @Autowired
    private SecretsProperties secretProperties;
    @Autowired
    private ApplicationProperties applicationProperties;


    private static final int auroraClientTimeout = 5;

    private static Client auroraClient = null;
    
    private RestTemplate restClient = null;
    
    private static final String DURATION = "2D";

    private final Map<String, ACRSAuthTokenResponse> acrsOAuthTokenMaps = new ConcurrentHashMap<>();

    /**
     * Api to check the service health.
     * 
     * @return HttpStatus ok for healthy, otherwise non-healthy.
     */
    @GetMapping("/ping")
    public HttpStatus ping() {
        return HttpStatus.OK;
    }

    /**
     * Service to check the GSE backend system health by calling GSE pricing api.
     * 
     * @param servletRequest request
     * @return HttpStatus ok for healthy, otherwise non-healthy.
     */
    @GetMapping("/gse")
    public HttpStatus doHealthCheck(HttpServletRequest servletRequest) {
        HttpStatus result = null;
        try {
            if (auroraClient == null) {
                auroraClient = getAuroraClient();
            }
            auroraClient.getRoomPricingAndAvailabilityEx(createPricingRequest());
            result = HttpStatus.OK;
        } catch (EAuroraException e) {
            log.error("Exception occured while Aurora health check", e);
            String errorType = AuroraError.getErrorType(e.getErrorCode().name());
            if (!AuroraError.FUNCTIONAL_ERROR.equals(errorType)) {
                result = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        return result;
    }
    
    /**
     * Service to check the ACRS backend system health by calling ACRS Calendar pricing api.
     * 
     * @return HttpStatus ok for healthy, otherwise non-healthy.
     */
	@GetMapping("/acrs")
	public HttpStatus doAcrsHealthCheck() {
		try {
			if (restClient == null) {
				restClient = CommonUtil.getRestTemplate(new RestTemplateBuilder(), true, acrsProperties.isLiveCRS(),
                        applicationProperties.getConnectionPerRouteDaoImpl(),
                        applicationProperties.getMaxConnectionPerDaoImpl(),
                        applicationProperties.getConnectionTimeout(),
                        applicationProperties.getReadTimeOut(),
                        applicationProperties.getSocketTimeOut(),
                        applicationProperties.getCrsRestTTL());
			}
            String acrsEnabledProperties = secretProperties.getSecretValue(acrsProperties.getAcrsPropertyListSecretKey());
            if (null != acrsEnabledProperties) {
                List<String> availableProperties = Arrays.asList(acrsEnabledProperties.trim().split(","));
                if (!availableProperties.isEmpty() && StringUtils.isNotEmpty(availableProperties.get(0))) {
                    final HttpEntity<BodyParameterCalendar> request = createBodyParameterCalendarRequest(availableProperties.get(0),ServiceConstant.ICECC);
                    final Map<String, String> uriParam = createUriParam(availableProperties.get(0));
                    restClient.exchange(domainProperties.getCrs() + urlProperties.getAcrsCalendarAvailabilitySearch(),
                            HttpMethod.POST, request, SuccessfulCalendarAvailability.class, uriParam);
                    return HttpStatus.OK;
                } else {
                    return HttpStatus.NO_CONTENT;
                }
            } else {
                return HttpStatus.NO_CONTENT;
            }
		} catch (Exception e) {
			log.error("Calendar search failed", e);
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
	}

    /**
     * API to return a list of property codes that are configured to ACRS in this instance of RBS
     *
     * @return
     *  List of strings in the form of property codes (i.e. MV021, MV285, etc.) that are configured to send requests to
     *  ACRS instead of GSE in this environment.
     */
    @GetMapping("/acrs/properties")
    public AcrsPropertiesResponse getAcrsProperties() {
        String acrsEnabledProperties = secretProperties.getSecretValue(acrsProperties.getAcrsPropertyListSecretKey());
        List<String> availableProperties = new ArrayList<>();
        if (null != acrsEnabledProperties) {
            availableProperties = Arrays.asList(acrsEnabledProperties.trim().split(","));
        }
        AcrsPropertiesResponse response = new AcrsPropertiesResponse();
        response.setAcrsProperties(availableProperties);
        return response;
    }

    /**
     * This will return acrs property wise charge and tax code configurations.
     * @return
     */
    @GetMapping("/acrs/chargeAndTaxCodes")
    public String getAcrsChargeAndTaxCodes() {
        return acrsProperties.getAcrsPropertyChargeCodeMap().toString();
    }
    
	private HttpEntity<BodyParameterCalendar> createBodyParameterCalendarRequest(String propertyCode ,String channel) {
        final ACRSAuthTokenResponse acrsAuthTokenResponseIce = acrsOAuthTokenDAOImpl.generateTokenIce();
        log.info("Token call for ICE is successful");
        final ACRSAuthTokenResponse acrsAuthTokenResponseWeb = acrsOAuthTokenDAOImpl.generateTokenWeb();
        log.info("Token call for WEB is successful");
        acrsOAuthTokenMaps.put(ServiceConstant.ICECC, acrsAuthTokenResponseIce);
        acrsOAuthTokenMaps.put(ServiceConstant.WEBBE, acrsAuthTokenResponseWeb);
        final HttpHeaders httpHeaders = CommonUtil.createCrsHeadersNoVersion(propertyCode, channel);
    	httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsOAuthTokenMaps.get(channel).getToken());
    		
		final HttpEntity<BodyParameterCalendar> request = new HttpEntity<BodyParameterCalendar>(new BodyParameterCalendar(), httpHeaders);

		return request;
	}

	private Map<String, String> createUriParam(String propertyCode) {
		final Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
				acrsProperties.getSearchVersion(), acrsProperties.getChainCode());
		uriParam.put(ServiceConstant.CRS_DURATION, DURATION);
		uriParam.put(ServiceConstant.CRS_PROPERTY_CODE, propertyCode);
		uriParam.put(ServiceConstant.CRS_START_DATE,
				ReservationUtil.convertDateToLocalDate(getFutureDate(7)).toString());
		uriParam.put(ServiceConstant.CRS_END_DATE, ReservationUtil.convertDateToLocalDate(getFutureDate(9)).toString());
		return uriParam;
	}

	/**
     * Create aurora client.
     * 
     * @return Client aurora client.
     */
    private Client getAuroraClient() {
        System.setProperty("aurora.app.name", properties.getAppName());
        System.setProperty("aurora.app.numpartitions", properties.getAppPartitions());
        System.setProperty("aurora.security.enabled", properties.getSecurityEnabled());
        System.setProperty("aurora.publickey", properties.getPublicKey());

        log.info(System.getProperty("aurora.app.name"));
        log.info(System.getProperty("aurora.app.numpartitions"));
        log.info(properties.getUrl());
        log.info(System.getProperty("aurora.security.enabled"));
        log.info(System.getProperty("aurora.publickey"));

        Credentials credentialsObj = new Credentials();
        credentialsObj.setUsername(properties.getChannelCredentials().get(1).getName());
        credentialsObj.setPassword(properties.getChannelCredentials().get(1).getCode());
        Client createdAuroraClient = new Client(properties.getChannelCredentials().get(1).getName());
        createdAuroraClient.setResponseTimeout(auroraClientTimeout);
        createdAuroraClient = createdAuroraClient.open(properties.getUrl(), credentialsObj);
        if (null == createdAuroraClient) {
            log.error("Unable to open Aurora client connetion for : {}",
                    properties.getChannelCredentials().get(1).getName());
        } else {
            log.info("Opened Aurora client connetion for : {}", properties.getChannelCredentials().get(1).getName());
        }
        return createdAuroraClient;
    }

    /**
     * Create pricing request.
     * 
     * @return GetRoomPricingAndAvailabilityExRequest request.
     */
    private GetRoomPricingAndAvailabilityExRequest createPricingRequest() {
        GetRoomPricingAndAvailabilityExRequest request = MessageFactory.createGetRoomPricingAndAvailabilityExRequest();
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
        request.setCheckInDate(getFutureDate(7));
        request.setCheckOutDate(getFutureDate(8));
        request.setNumAdults(2);
        request.setCustomerId(-1);
        return request;
    }

    /**
     * return future date from the given input number of days.
     * 
     * @param days - number of days.
     * @return Date - future date.
     */
    private Date getFutureDate(int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }

    /**
     * Service to check the ACRS backend system connectivity by calling ACRS Calendar pricing api.
     *
     * @return response Object.
     */
    @GetMapping("/acrsconnection")
    public HttpStatus doAcrsConnectionCheck() {
        try {
            if (restClient == null) {
                restClient = CommonUtil.getRestTemplate(new RestTemplateBuilder(), true, acrsProperties.isLiveCRS(),
                        applicationProperties.getConnectionPerRouteDaoImpl(),
                        applicationProperties.getMaxConnectionPerDaoImpl(),
                        applicationProperties.getConnectionTimeout(),
                        applicationProperties.getReadTimeOut(),
                        applicationProperties.getSocketTimeOut(),
                        applicationProperties.getCrsRestTTL());
            }
            List<String> availableProperty = Arrays.asList("MV021");
            try {
                HttpEntity<BodyParameterCalendar> request = createBodyParameterCalendarRequest(availableProperty.get(0), ServiceConstant.ICECC);
                final Map<String, String> uriParam = createUriParam(availableProperty.get(0));
                log.info("Calender Search for ICE returned is : {}", restClient.exchange(domainProperties.getCrs() + urlProperties.getAcrsCalendarAvailabilitySearch(),
                        HttpMethod.POST, request, SuccessfulCalendarAvailability.class, uriParam));
            } catch (Exception e) {
                log.error("Calendar search failed for ICE : {}", e.getMessage());
            }
            try {
                HttpEntity<BodyParameterCalendar> requestWeb = createBodyParameterCalendarRequest(availableProperty.get(0), ServiceConstant.WEBBE);
                final Map<String, String> uriParamWeb = createUriParam(availableProperty.get(0));
                log.info("Calender Search for WEB returned is : {}", restClient.exchange(domainProperties.getCrs() + urlProperties.getAcrsCalendarAvailabilitySearch(),
                        HttpMethod.POST, requestWeb, SuccessfulCalendarAvailability.class, uriParamWeb));
            } catch (Exception e) {
                log.error("Calendar search failed for WEB : {}", e.getMessage());
            }

            return HttpStatus.OK;

        } catch (Exception e) {
            log.error("Calendar search failed", e);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    
}
