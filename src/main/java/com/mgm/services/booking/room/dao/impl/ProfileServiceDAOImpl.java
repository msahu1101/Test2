package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.ProfileServiceDAO;
import com.mgm.services.booking.room.model.crs.searchoffers.BodyParameterPricing;
import com.mgm.services.booking.room.model.response.ProfileServiceResponse;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.service.CustomerInformationService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
public class ProfileServiceDAOImpl implements ProfileServiceDAO {

    private final DomainProperties domainProps;
    private final PropertyConfig propertyConfig;
    protected RestTemplate client;
    private final IDMSTokenDAO idmsTokenDao;

    @Autowired
    public ProfileServiceDAOImpl(DomainProperties domainProps, PropertyConfig propertyConfig, RestTemplate client, IDMSTokenDAO idmsTokenDao){
        this.domainProps = domainProps;
        this.propertyConfig = propertyConfig;
        this.client = client;
        this.idmsTokenDao = idmsTokenDao;
    }


    @Override
    public ProfileServiceResponse getCustomerIdByMlifeId(String mlifeId) {
        return getCustomerIdByMlifeId(mlifeId, 0);
    }

    private ProfileServiceResponse getCustomerIdByMlifeId(String mlifeId, int retryAttempt) {
        ProfileServiceResponse response = null;
        if (StringUtils.isNotEmpty(mlifeId)) {
            final String authToken = idmsTokenDao.generateToken().getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);
            headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
            headers.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_JSON);
            headers.set(ServiceConstant.HEADER_CHANNEL, ServiceConstant.RBS_CHANNEL_HEADER);

            final Map<String, String> uriParams = new HashMap<>();
            uriParams.put("mlifeId", mlifeId);

            if (log.isDebugEnabled()) {
                log.debug("Calling Profile Service : URI: {}, PathParam: {}", domainProps.getProfileService(), uriParams.toString());
            }

            try {
                final ResponseEntity<ProfileServiceResponse> profileResponse = client.exchange(
                        domainProps.getProfileService(), HttpMethod.GET, new HttpEntity<BodyParameterPricing>(headers), ProfileServiceResponse.class, uriParams);

                if (log.isDebugEnabled()) {
                    log.debug("Received headers from Profile Services API: {}", CommonUtil.convertObjectToJsonString(profileResponse.getHeaders()));
                    log.debug("Received response from Profile Services API: {}", CommonUtil.convertObjectToJsonString(profileResponse.getBody()));
                }

                int status = profileResponse.getStatusCodeValue();
                if (status >= 200 && status < 300) {
                    response = profileResponse.getBody();
                }
            }
            catch (Exception e) {
                log.error("Unable to receive Profile Service Values | Exception : {}", e.getMessage());
            }
        }

        return response;
    }
}
