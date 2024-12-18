package com.mgm.services.booking.room.dao.impl;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.mgm.services.booking.room.model.response.ItineraryResponse;
import com.mgm.services.booking.room.util.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.ItineraryDAO;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.request.ItineraryServiceRequest;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

import lombok.extern.log4j.Log4j2;

/**
 * DAO class to authorize and confirm a transaction
 * 
 * @author vararora
 *
 */
@Component
@Log4j2
public class ItineraryDAOImpl implements ItineraryDAO {

    @Autowired
    private IDMSTokenDAO idmsTokenDAO;

    private RestTemplate client;
    private String itineraryServiceURL;
    private String createItineraryURL;
    private String retrieveItineraryURL;
    private ApplicationProperties applicationProperties;

    protected static String BEARER_TOKEN;    

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     * 
     * @param builder
     *            Spring's auto-configured RestTemplateBuilder
     * @param domainProperties
     *            Domain Properties
     * @param urlProperties
     *            URL Properties
     * @param applicationProperties
     *            Application Properties
     */
    public ItineraryDAOImpl(RestTemplateBuilder builder, DomainProperties domainProperties, URLProperties urlProperties,
            ApplicationProperties applicationProperties) {
        super();
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), true,
                applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getItineraryConnectionTimeout(),
                applicationProperties.getItineraryReadTimeout(),
                applicationProperties.getSocketTimeOut(),
                1,
                applicationProperties.getCommonRestTTL());

        String baseUrl = domainProperties.getItinerary().trim();
        this.itineraryServiceURL = baseUrl.concat(urlProperties.getItineraryService().trim());
        this.applicationProperties = applicationProperties;
        this.createItineraryURL = baseUrl.concat(urlProperties.getItineraryCreate().trim());
        this.retrieveItineraryURL = baseUrl.concat(urlProperties.getItineraryRetrieve().trim());
    }

    @LogExecutionTime
    @Override
    public void updateCustomerItinerary(String itineraryId, ItineraryServiceRequest itineraryRequest) {
        String itineraryEnvironment = applicationProperties.getItineraryEnvironment();
        final String authToken = idmsTokenDAO.generateToken().getAccessToken();
        
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String correlationId = httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID);
        if (StringUtils.isBlank(correlationId)) {
            correlationId = UUID.randomUUID().toString();
            log.info("CorrelationId was not found in header, generated Id is: {}", correlationId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);
        headers.add(ServiceConstant.X_MGM_CORRELATION_ID, correlationId);

        HttpEntity<ItineraryServiceRequest> request = new HttpEntity<>(itineraryRequest, headers);

        log.info("Sending request to Itinerary service for ItineraryId({}) with correlation id({}): {}", itineraryId,
                correlationId, itineraryRequest);

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put(ServiceConstant.ITINERARY_ENVIRONMENT_PH, itineraryEnvironment);
        uriVariables.put(ServiceConstant.ITINERARY_ID_PH, itineraryId);
        String response = client.exchange(itineraryServiceURL, HttpMethod.PUT, request, String.class, uriVariables)
                .getBody();

        log.info("Received response from Itinerary service for ItineraryId({}): {}", itineraryId, response);

    }

    @LogExecutionTime
    @Override
    public String createCustomerItinerary(ItineraryServiceRequest itineraryRequest) {
        String itineraryEnvironment = applicationProperties.getItineraryEnvironment();
        final String authToken = idmsTokenDAO.generateToken().getAccessToken();

        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String correlationId = httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID);
        if (StringUtils.isBlank(correlationId)) {
            correlationId = UUID.randomUUID().toString();
            log.info("CorrelationId was not found in header, generated Id is: {}", correlationId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);
        headers.add(ServiceConstant.X_MGM_CORRELATION_ID, correlationId);

        HttpEntity<ItineraryServiceRequest> request = new HttpEntity<>(itineraryRequest, headers);

        log.info("Sending request to create Itinerary for creating ItineraryId({}) with correlation id({}): {}",
                correlationId, itineraryRequest);

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put(ServiceConstant.ITINERARY_ENVIRONMENT_PH, itineraryEnvironment);
        ItineraryResponse itineraryResponse = client.exchange(createItineraryURL, HttpMethod.POST, request, ItineraryResponse.class, uriVariables)
                .getBody();

        log.info("Received response from create Itinerary: {}", itineraryResponse);
        return itineraryResponse.getItinerary().getItineraryId();
    }
    
    
    @LogExecutionTime
    @Override
    public ItineraryResponse retreiveCustomerItineraryDetailsByConfirmationNumber(String roomConfirmationNumber) {
        ItineraryResponse itineraryResponse = null;
		try {
			String itineraryEnvironment = applicationProperties.getItineraryEnvironment();
			final String authToken = idmsTokenDAO.generateToken().getAccessToken();

			HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder
					.currentRequestAttributes()).getRequest();
			String correlationId = httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID);
			if (StringUtils.isBlank(correlationId)) {
				correlationId = UUID.randomUUID().toString();
				log.info("CorrelationId was not found in header, generated Id is: {}", correlationId);
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);
			headers.add(ServiceConstant.X_MGM_CORRELATION_ID, correlationId);

			HttpEntity<?> request = new HttpEntity<>(headers);

			log.info("Sending request to Retrieve Itinerary by passing correlationId {} and roomConfirmationNumber: {}",
					correlationId, roomConfirmationNumber);

			Map<String, String> uriVariables = new HashMap<>();
			uriVariables.put(ServiceConstant.ITINERARY_ENVIRONMENT_PH, itineraryEnvironment);
			uriVariables.put(ServiceConstant.ITINERARY_RETRIEVE_PH, roomConfirmationNumber);
			itineraryResponse = client
					.exchange(retrieveItineraryURL, HttpMethod.GET, request, ItineraryResponse.class, uriVariables).getBody();

			log.info("Received response from Retrieve Itinerary: {}", itineraryResponse);
		} catch (Exception e) {
			log.info("Failed to retrieve response from Itinerary Service for this confirmation Number: {}", roomConfirmationNumber);
		}
        return itineraryResponse;
    }

}
