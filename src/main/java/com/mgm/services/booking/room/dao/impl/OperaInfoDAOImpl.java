package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.OperaInfoDAO;
import com.mgm.services.booking.room.model.opera.OperaInfoResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Component
@Log4j2
public class OperaInfoDAOImpl implements OperaInfoDAO {

    private final URLProperties urlProperties;
    private DomainProperties domainProperties;
    private final RestTemplate client;
    private IDMSTokenDAO idmsTokenDAO;
    ApplicationProperties applicationProperties;

    protected OperaInfoDAOImpl(URLProperties urlProperties, RestTemplateBuilder builder, DomainProperties domainProperties,
                               ApplicationProperties applicationProperties, IDMSTokenDAO idmsTokenDAO, SecretsProperties secretsProperties) {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.applicationProperties = applicationProperties;
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), true,
                applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeOutOperaENR(),
                applicationProperties.getReadTimeOutOperaENR(),
                applicationProperties.getSocketTimeOutOperaENR(),
                1,
                applicationProperties.getCommonRestTTL());
        this.idmsTokenDAO = idmsTokenDAO;
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
    }

    @Override
    public OperaInfoResponse getOperaInfo(String cnfNumber, String propertyCode) {
        Map<String, String> uriParams = new HashMap<>();
        uriParams.put(ServiceConstant.APIGEE_ENVIRONMENT, applicationProperties.getApigeeEnvironment());
        uriParams.put("cnfNumber", cnfNumber);
        String operaRetrieveURI;
        if (StringUtils.isNotEmpty(propertyCode)) {
            operaRetrieveURI = urlProperties.getOperaDetailsRetrieveProperty();
            uriParams.put("propCode", propertyCode);
        } else {
            operaRetrieveURI = urlProperties.getOperaDetailsRetrieve();
        }
        HttpEntity<?> request = getHttpEntity();
        try {
            ResponseEntity<OperaInfoResponse> response = client.exchange(
                    domainProperties.getOperaRetrieve() + operaRetrieveURI, HttpMethod.GET,
                    request, OperaInfoResponse.class, uriParams);

            log.info("Opera Details response headers: {}", CommonUtil.convertObjectToJsonString(response.getHeaders()));
            log.info("Opera Details response body: {}", CommonUtil.convertObjectToJsonString(response.getBody()));

            return response.getBody();
        } catch (Exception e) {
            log.error("Error while retrieving Opera details: {}", e.getMessage());
            return null;
        }
    }

    private <T> HttpEntity<T> getHttpEntity() {
        final String authToken = idmsTokenDAO.generateToken().getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);
        return new HttpEntity<>(headers);
    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            final String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received Opera Retrieve: status code: {}, header: {}, body: {}",
                    httpResponse.getStatusCode().value(), httpResponse.getHeaders().toString(), response);
            if (httpResponse.getStatusCode().value() >= 500) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            } else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, response);
            }
        }
    }
}
