package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.PIMDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.PIMPkgComponent;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.ReservationUtil;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Log4j2
public class PIMDAOImpl implements PIMDAO {

    @Autowired
    @Setter
    private IDMSTokenDAO idmsTokenDao;
    private final RestTemplate client;

    protected URLProperties urlProperties;
    protected DomainProperties domainProperties;
    protected AcrsProperties acrsProperties;
    protected ApplicationProperties applicationProperties;
    protected ReferenceDataDAOHelper referenceDataDAOHelper;
    protected ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;

    public PIMDAOImpl(URLProperties urlProperties, DomainProperties domainProperties, ApplicationProperties applicationProperties, AcrsProperties acrsProperties,
                      RestTemplate restTemplate, ReferenceDataDAOHelper referenceDataDAOHelper, ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl) {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.acrsProperties = acrsProperties;
        this.applicationProperties = applicationProperties;
        this.client = restTemplate;
        this.referenceDataDAOHelper = referenceDataDAOHelper;
		this.acrsOAuthTokenDAOImpl = acrsOAuthTokenDAOImpl;
        this.client.setMessageConverters(Collections.singletonList(ReservationUtil.createHttpMessageConverter()));
    }

	@Override
	public List<PIMPkgComponent> searchPackageComponents(String propertyCode, String type) {
        List<PIMPkgComponent> listOfPkgComponents = null;
        if(StringUtils.isNotEmpty(propertyCode)){
            listOfPkgComponents = getListOfPkgComponentsByPropertyCode(propertyCode, type);
        }
		return listOfPkgComponents;
	}

    private List<PIMPkgComponent> getListOfPkgComponentsByPropertyCode(String propertyCode, String type){
        final String authToken = idmsTokenDao.generateToken().getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
        HttpEntity<?> request = new HttpEntity<>(headers);

        Map<String, String> uriParams = new HashMap<>();
        uriParams.put(ServiceConstant.APIGEE_ENVIRONMENT, applicationProperties.getApigeeEnvironment());
        uriParams.put(ServiceConstant.ACRS_PROPERTY_CODE, propertyCode);
        uriParams.put(ServiceConstant.NON_ROOM_INVENTORY_TYPE, type);

        final ResponseEntity<PIMPkgComponent[]> packageComponentsResponse = client.exchange(domainProperties.getPackageComponentsSearch() + urlProperties.getPackageComponents(),
                HttpMethod.GET, request, PIMPkgComponent[].class, uriParams);
        return Arrays.asList(packageComponentsResponse.getBody());

    }

}
