package com.mgm.services.booking.room.dao.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.PropertyContentDAO;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation class for PropertyContentDAO to fetch property marketing
 * content.
 */
@Component
public class PropertyContentDAOImpl extends BaseContentDAOImpl implements PropertyContentDAO {

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     * 
     * @param urlProperties
     *            URL Properties
     * @param domainProperties
     *            Domain Properties
     * @param builder
     *            Spring's auto-configured rest template builder
     * @param applicationProperties
     *            Application Properties
     */
    public PropertyContentDAOImpl(URLProperties urlProperties, DomainProperties domainProperties,
                                  ApplicationProperties applicationProperties) {
        super(urlProperties, domainProperties, applicationProperties);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.PropertyContentDAO#
     * getAllPropertiesContent()
     */
    @Override
    public List<Property> getAllPropertiesContent() {

        return Arrays.asList(getClient()
                .getForEntity(getDomainProperties().getContentapi().concat(getUrlProperties().getPropertyContentApi()),
                        Property[].class)
                .getBody());
    }

}
