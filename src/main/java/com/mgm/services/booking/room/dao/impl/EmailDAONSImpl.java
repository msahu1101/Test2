package com.mgm.services.booking.room.dao.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.EmailDAO;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.Email;
import com.mgm.services.booking.room.model.request.EmailRequest;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for EmailDAO providing sendEmail functionality using
 * Notification Service.
 * 
 * @author vararora
 *
 */
@Component("EmailDAONSImpl")
@Log4j2
public class EmailDAONSImpl implements EmailDAO {

    @Autowired
    private IDMSTokenDAO idmsTokenDAO;

    private RestTemplate client;
    private String notificationServiceURL;
    private ApplicationProperties applicationProperties;

    private static final String RCS_NS_SCOPE_ALL = "rcs.ns:all";

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
    public EmailDAONSImpl(RestTemplateBuilder builder, DomainProperties domainProperties, URLProperties urlProperties,
            ApplicationProperties applicationProperties) {
        super();
        this.client = builder.build();
        String baseUrl = domainProperties.getNs().trim();
        this.notificationServiceURL = baseUrl.concat(urlProperties.getNotificationService().trim());
        this.applicationProperties = applicationProperties;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.EmailDAO#sendEmail(com.mgm.services.
     * booking.room.model.Email)
     */
    @Override
    public void sendEmail(Email email) {
        String nsEnvironment = applicationProperties.getNsEnvironment();
        List<String> sendToList = new ArrayList<>();
        sendToList.add(email.getTo());
        EmailRequest emailRequest = EmailRequest.builder().recipientEmailAddressList(sendToList)
                .source(ServiceConstant.ROOM_BOOKING_SERVICE).emailSubject(email.getSubject()).build();
        if (StringUtils.isNotEmpty(email.getBody())) {
            emailRequest.setEmailBody(email.getBody());
        }

        final String authToken = idmsTokenDAO.generateToken().getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);

        HttpEntity<EmailRequest> request = new HttpEntity<>(emailRequest, headers);

        log.info("Sent the request to sendEmail as : {}", CommonUtil.convertObjectToJsonString(emailRequest));
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put(ServiceConstant.NS_ENVIRONMENT_PH, nsEnvironment);
        String response = client.exchange(notificationServiceURL, HttpMethod.POST, request, String.class, uriVariables)
                .getBody();
        log.info("Received the response from sendEmail as : {}", response);
    }

}
