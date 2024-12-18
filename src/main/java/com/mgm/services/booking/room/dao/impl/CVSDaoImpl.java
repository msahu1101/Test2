package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.CVSDao;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.crs.searchoffers.BodyParameterPricing;
import com.mgm.services.booking.room.model.response.CVSResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAwsCloudEnvironment;
import org.springframework.http.*;
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
@ConditionalOnMissingAwsCloudEnvironment
public class CVSDaoImpl implements CVSDao {

    private DomainProperties domainProps;

    private PropertyConfig propertyConfig;

    protected RestTemplate client;

    @Autowired(required=false)
    @Setter
    private IDMSTokenDAO idmsTokenDao;
    
    

    public CVSDaoImpl(PropertyConfig propertyConfig, DomainProperties domainProps, AcrsProperties acrsProperties, ApplicationProperties applicationProperties, RestTemplateBuilder builder) {
        this.propertyConfig = propertyConfig;
        this.domainProps = domainProps;
        this.client = CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),
                applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),
                1,
                applicationProperties.getCommonRestTTL());
        this.client.setErrorHandler(new CVSErrorHandler());
    }

    static class CVSErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            final String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            final HttpStatus statusCode = httpResponse.getStatusCode();
            log.error("Error received CVS: status code: {}, header: {}, body: {}",
                    statusCode, httpResponse.getHeaders().toString(), response);
            if (statusCode == HttpStatus.UNAUTHORIZED) {
                throw new BusinessException(ErrorCode.TRANSACTION_NOT_AUTHORIZED);
            }
            if (statusCode == HttpStatus.NOT_FOUND) {
                throw new BusinessException(ErrorCode.MLIFE_NUMBER_NOT_FOUND);
            }
            else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            } else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, response);
            }
        }
    }

    @Override
    public CVSResponse getCustomerValues(String mLifeNumber) {
        return getCustomerValues(mLifeNumber, 0);
    }

    private CVSResponse getCustomerValues(String mLifeNumber, int retryAttempt) {

        CVSResponse response = null;
        if (StringUtils.isNotEmpty(mLifeNumber)) {

            final String authToken = idmsTokenDao.generateToken().getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);
            headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
            headers.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_JSON);
            headers.set(ServiceConstant.HEADER_CHANNEL, ServiceConstant.RBS_CHANNEL_HEADER);

            final Map<String, String> uriParams = new HashMap<>();
            uriParams.put(ServiceConstant.CVS_MLIFENO_PARAM, mLifeNumber);

            if (log.isDebugEnabled()) {
                log.debug("Calling CVS Service : URI: {}, PathParam: {}", domainProps.getCvs(), uriParams.toString());
            }

            try {
                final ResponseEntity<CVSResponse> cvsResponse = client.exchange(
                        domainProps.getCvs(), HttpMethod.GET, new HttpEntity<BodyParameterPricing>(headers), CVSResponse.class, uriParams);

                if (log.isDebugEnabled()) {
                    log.debug("Received headers from CVS API: {}", CommonUtil.convertObjectToJsonString(cvsResponse.getHeaders()));
                    log.debug("Received response from CVS API: {}", CommonUtil.convertObjectToJsonString(cvsResponse.getBody()));
                }

                int status = cvsResponse.getStatusCodeValue();
                if (status >= 200 && status < 300) {
                    response = cvsResponse.getBody();
                }

            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.MLIFE_NUMBER_NOT_FOUND) {
                    // Skip as no CVS data found
                } else if (retryAttempt == 0 && e.getErrorCode() == ErrorCode.TRANSACTION_NOT_AUTHORIZED) {
                    // Retry once with token refresh in case token is stale
                    log.error("renewing cvs token");
                    return getCustomerValues(mLifeNumber, ++retryAttempt);
                } else {
                    log.error("Unable to receive CVS values | Exception : {}", ExceptionUtils.getStackTrace(e));
                }
            }
            catch (Exception e) {
                log.error("Unable to receive CVS values | Exception : {}", ExceptionUtils.getStackTrace(e));
            }
        }

        enhanceData(response);
        return response;
    }

    private void enhanceData(CVSResponse response) {
        if (null != response) {
            CVSResponse.CVSCustomer customer = response.getCustomer();
            if (null != customer) {
                final CVSResponse.CVSCustomerValue[] customerValues = customer.getCustomerValues();
                if (null != customerValues) {
                    for (CVSResponse.CVSCustomerValue cv : customerValues) {
                        final String operaCode = cv.getProperty();
                        propertyConfig.getPropertyValues().stream().forEach(x -> {
                            final String operaPropertyCode = x.getOperaCode();
                            if (StringUtils.equalsIgnoreCase(operaPropertyCode, operaCode)) {
                                cv.getGsePropertyIds().addAll(x.getGsePropertyIds());
                            }
                        });
                    }
                }
            }
        }
    }
}
