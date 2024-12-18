package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.GroupSearchDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.crs.groupretrieve.GroupSearchReq;
import com.mgm.services.booking.room.model.crs.groupretrieve.GroupSearchResGroupBookingReservationSearch;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.properties.*;
import com.mgm.services.booking.room.transformer.GroupSearchTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
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

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation class for ComponentDAO providing functionality to provide room
 * component related functionalities.
 *
 */
@Component
@Log4j2
public class GroupSearchDAOStrategyACRSImpl extends BaseAcrsDAO implements GroupSearchDAOStrategy {

	final PropertyConfig propertyConfig;

    private SecretsProperties secretsProperties;

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     *
     * @param urlProperties
     *            URL Properties
     * @param domainProperties
     *            Domain Properties
     * @param applicationProperties
     *            Application Properties
     * @param secretsProperties
     *            Secrets Properties
     * @param builder
     *            Spring's auto-configured rest template builder
     * @throws SSLException
     *             Throws SSL Exception
     */
    protected GroupSearchDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties,
                                             ApplicationProperties applicationProperties, AcrsProperties acrsProperties,
					     RestTemplateBuilder builder, ReferenceDataDAOHelper referenceDataDAOHelper,
					     ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl,
					     PropertyConfig propertyConfig, SecretsProperties secretsProperties) throws SSLException {
        super(urlProperties, domainProperties, applicationProperties, acrsProperties, CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),
                applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
                applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeoutACRS(),
                applicationProperties.getReadTimeOutACRS(),
                applicationProperties.getSocketTimeOutACRS(),
                1,
                applicationProperties.getCrsRestTTL()), referenceDataDAOHelper, acrsOAuthTokenDAOImpl);
        this.propertyConfig = propertyConfig;
        this.secretsProperties = secretsProperties;
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
    }

    @Override
    public List<GroupSearchV2Response> searchGroup(GroupSearchV2Request groupSearchRequest) {
        overrideRequestData(groupSearchRequest);

		GroupSearchReq request = GroupSearchTransformer.composeGroupSearchRequest(groupSearchRequest, propertyConfig);
		GroupSearchResGroupBookingReservationSearch acrsResponse = groupSearch(request, groupSearchRequest.getSource());
		final List<GroupSearchV2Response> responses = GroupSearchTransformer.transform(acrsResponse);

        referenceDataDAOHelper.updateGroupAcrsReferencesToGse(responses);

        // filter the response if any property is not is configured in properties file but in response it is there
		return responses.stream()
				.filter(res -> referenceDataDAOHelper.isPropertyManagedByAcrs(res.getPropertyId()))
				.collect(Collectors.toList());
    }

    private GroupSearchResGroupBookingReservationSearch groupSearch(GroupSearchReq request, String source) {
        HttpHeaders httpHeaders = null;
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);
        String acrsPropertyCode = null;
        if(CollectionUtils.isNotEmpty(request.getData().getPropertyCodes())) {
            acrsPropertyCode = request.getData().getPropertyCodes().get(0);
        }else {
            //for resorts price withgroupCode propId will not be there.
            acrsPropertyCode = source;
        }
        final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        httpHeaders = CommonUtil.createCrsHeadersNoVersion(acrsPropertyCode,acrsVendor);
        httpHeaders.set(ServiceConstant.HEADER_X_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER+acrsAuthTokenResponseMap.get(acrsVendor).getToken());

        HttpEntity<GroupSearchReq> httpRequest = new HttpEntity<>(request, httpHeaders);

        Map<String, String> uriParam = CommonUtil.composeUriParams(acrsProperties.getEnvironment(),
                acrsProperties.getGroupSearchVersion(), acrsProperties.getChainCode());
        boolean  doTempInfoLogging = CommonUtil.isTempLogEnabled(secretsProperties.getSecretValue(applicationProperties.getTempInfoLogEnabled()));
        logRequestHeaderAndBody(doTempInfoLogging,
                "Sending request to ACRS Group Search, Request headers : {}",
                "Sending request to ACRS Group Search, Request body : {}",
                "Sending request to ACRS Group Search, Request query parameters: {}",
                CommonUtil.convertObjectToJsonString(httpRequest.getHeaders()),
                CommonUtil.convertObjectToJsonString(httpRequest.getBody()),
                uriParam
        );
        ThreadContext.put(ServiceConstant.DEPENDENT_SYSTEM_TYPE, ServiceConstant.ACRS_DEPENDENT_SYSTEM);
        ThreadContext.put(ServiceConstant.API_NAME_TYPE, "AcrsGroupSearch");
        ThreadContext.put(ServiceConstant.API_URL_TYPE, urlProperties.getAcrsGroupSearch());
        LocalDateTime start = LocalDateTime.now();
        ThreadContext.put(ServiceConstant.TIME_TYPE, start.toString());

        ResponseEntity<GroupSearchResGroupBookingReservationSearch> crsResponse = client.exchange(
                domainProperties.getCrs() + urlProperties.getAcrsGroupSearch(), HttpMethod.POST, httpRequest,
                GroupSearchResGroupBookingReservationSearch.class, uriParam);

        long duration = ChronoUnit.MILLIS.between(start,LocalDateTime.now());
        ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
        ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(crsResponse.getStatusCodeValue()));
        log.info("Custom Dimensions updated after ACRS call");

        return logAndReturnCrsResponseBody(crsResponse, "Group Search", doTempInfoLogging);
    }
    
    private void overrideRequestData(GroupSearchV2Request groupSearchRequest) {
        final String id = groupSearchRequest.getId();
        if (ACRSConversionUtil.isAcrsGroupCodeGuid(id)) {
            groupSearchRequest.setPropertyId(ACRSConversionUtil.getPropertyCode(id));
            groupSearchRequest.setId(ACRSConversionUtil.getGroupCode(id));
        } else if (StringUtils.isNotEmpty(groupSearchRequest.getPropertyId())
                && CommonUtil.isUuid(groupSearchRequest.getPropertyId())) {
            groupSearchRequest
                    .setPropertyId(referenceDataDAOHelper.retrieveAcrsPropertyID(groupSearchRequest.getPropertyId()));
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
            log.error("Error received Amadeus: header: {} body: {}", httpResponse.getHeaders().toString(), response);
            ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpResponse.getStatusCode()));
            try {
                LocalDateTime start = LocalDateTime.parse(ThreadContext.get(ServiceConstant.TIME_TYPE));
                long duration = ChronoUnit.MILLIS.between(start, LocalDateTime.now());
                ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
                log.info("Custom Dimensions updated after ACRS call");
            } catch (Exception e) {
                // Do nothing
            }
            //TODO https://mgmdigitalventures.atlassian.net/browse/CBSR-2288: Error Handling for Group Search
            throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
        }
    }
}
