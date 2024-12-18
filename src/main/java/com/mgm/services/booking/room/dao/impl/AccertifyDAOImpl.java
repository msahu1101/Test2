package com.mgm.services.booking.room.dao.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.AccertifyDAO;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;

import lombok.extern.log4j.Log4j2;

/**
 * DAO class to authorize and confirm a transaction
 * 
 * @author nitpande0
 *
 */
@Component
@Log4j2
public class AccertifyDAOImpl implements AccertifyDAO {

    private RestTemplate client;
    private String accertifyAuthorizationURL;
    private String accertifyConfirmationURL;
    private SecretsProperties secretsProperties;
    private IDMSTokenDAO idmsTokenDAO;

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     * 
     * @param restTemplate
     *            Configured rest template
     * @param domainProperties
     *            Domain Properties
     * @param urlProperties
     *            URL Properties
     * @param secretsProperties
     *            Secrets Properties
     */
    public AccertifyDAOImpl(RestTemplate restTemplate, DomainProperties domainProperties, URLProperties urlProperties,
            SecretsProperties secretsProperties, IDMSTokenDAO idmsTokenDAO) {
        super();
        this.client = restTemplate;
        String baseUrl = domainProperties.getAccertify().trim();
        this.accertifyAuthorizationURL = baseUrl.concat(urlProperties.getAccertifyAuthorization().trim());
        this.accertifyConfirmationURL = baseUrl.concat(urlProperties.getAccertifyConfirmation().trim());
        this.secretsProperties = secretsProperties;
        this.idmsTokenDAO = idmsTokenDAO;
        
    }
    
    private String getAuthorizationHeader() {
        return ServiceConstant.HEADER_AUTH_BEARER + idmsTokenDAO.generateToken().getAccessToken();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.AccertifyDAO#authorize(com.mgm.services
     * .booking.room.model.authorization.AuthorizationTransactionRequest)
     */
    @Override
    public AuthorizationTransactionResponse authorize(AuthorizationTransactionRequest transactionRequest) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add(ServiceConstant.X_API_KEY,
                secretsProperties.getSecretValue(ServiceConstant.ACCERTIFY_APIKEY));        
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, getAuthorizationHeader());
        CommonUtil.setAdditionalPaymentHeaders(headers);
        CommonUtil.setPaymentSourceAndChannelHeaders(headers);
        Map.Entry<String, String> clientRef = CommonUtil.getClientRefForPayment();
        headers.set(clientRef.getKey(),clientRef.getValue());

        log.debug("AFS API Key: {}", secretsProperties.getSecretValue(ServiceConstant.ACCERTIFY_APIKEY));
        log.debug("AFS Auth: {}", headers);
        log.info("AFS Authorization Headers: {}", CommonUtil.convertObjectToJsonString(headers));

        HttpEntity<AuthorizationTransactionRequest> request = new HttpEntity<>(transactionRequest, headers);

        log.info("Sending request to accertify for authorization: {}", CommonUtil.convertObjectToJsonString(transactionRequest));

        AuthorizationTransactionResponse response = client
                .postForEntity(accertifyAuthorizationURL, request, AuthorizationTransactionResponse.class).getBody();

        log.info("Received response from accertify for authorization: {}", response);
        return response;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.AccertifyDAO#confirm(com.mgm.services.
     * booking.room.model.authorization.AuthorizationTransactionRequest)
     */
    @Override
    public void confirm(AuthorizationTransactionRequest transactionRequest, HttpHeaders threadHeaders) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, getAuthorizationHeader());
        headers.add(ServiceConstant.X_API_KEY,
                secretsProperties.getSecretValue(ServiceConstant.ACCERTIFY_APIKEY));
        headers.setContentType(MediaType.APPLICATION_JSON);
        if(null != threadHeaders) {
            headers.addAll(threadHeaders);
        }

        List<String> userAgentHeader = threadHeaders.get(ServiceConstant.HEADER_USER_AGENT);
		List<String> fraudAgenTokenHeader = threadHeaders.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN);
		
		if(CollectionUtils.isNotEmpty(userAgentHeader)) {
			headers.set(ServiceConstant.HEADER_USER_AGENT, userAgentHeader.get(0));
		}
		
		if(CollectionUtils.isNotEmpty(fraudAgenTokenHeader)) {
			headers.set(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN, fraudAgenTokenHeader.get(0));
		}

        
        HttpEntity<AuthorizationTransactionRequest> request = new HttpEntity<>(transactionRequest, headers);

        log.info("Sending request to accertify for confirmation: {}", CommonUtil.convertObjectToJsonString(transactionRequest));
        log.info("Sending headers to accertify for confirmation: {}", CommonUtil.convertObjectToJsonString(headers));
        AuthorizationTransactionResponse response = client
                .postForEntity(accertifyConfirmationURL, request, AuthorizationTransactionResponse.class).getBody();

        log.info("Received response from accertify for confirmation: {}", response);

    }

}
