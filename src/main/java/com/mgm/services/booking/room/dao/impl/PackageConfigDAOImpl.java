package com.mgm.services.booking.room.dao.impl;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.PackageConfigDAO;
import com.mgm.services.booking.room.model.content.PackageConfig;
import com.mgm.services.booking.room.model.content.PackageConfigParam;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

import lombok.extern.log4j.Log4j2;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation class for PackageConfigDAO to fetch package config.
 */
@Component
@Log4j2
public class PackageConfigDAOImpl extends BaseContentDAOImpl implements PackageConfigDAO {

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's pre-configured HttpClient.
     * 
     * @param urlProperties
     *            URL Properties
     * @param domainProperties
     *            Domain Properties
     * @param clientHttpRequestFactory
     *            Spring's pre-configured HttpClient
     * @param appProps
     *            Application Properties
     */
    public PackageConfigDAOImpl(URLProperties urlProperties, DomainProperties domainProperties,
                                 ApplicationProperties appProps) {
        super(urlProperties, domainProperties, appProps);
    }

    @Override
    public PackageConfig[] getPackageConfigs(PackageConfigParam key, String value) {
        log.info(getDomainProperties().getContentapi().concat(getUrlProperties().getPackageConfigApi()));
        log.info("Param key: {}, Param value: {}", key, value);
        return getClient()
                .getForEntity(getDomainProperties().getContentapi().concat(getUrlProperties().getPackageConfigApi()),
                        PackageConfig[].class, key, value)
                .getBody();
    }

}
