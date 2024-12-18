package com.mgm.services.booking.room.dao.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.dao.PhoenixRoomDAO;
import com.mgm.services.booking.room.model.phoenix.Room;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

/**
 * Implementation class to retrieve room information from Phoenix.
 */
@Component
public class PhoenixRoomDAOImpl implements PhoenixRoomDAO {

    private URLProperties urlProperties;
    private DomainProperties domainProperties;
    private RestTemplate client;

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
     */
    public PhoenixRoomDAOImpl(URLProperties urlProperties, DomainProperties domainProperties,
            RestTemplateBuilder builder) {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = builder.build();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.PhoenixRoomDAO#getRoomsByProperty(java.
     * lang.String)
     */
    @Override
    public List<Room> getRoomsByProperty(String propertyId) {

        return Arrays.asList(
                client.getForEntity(domainProperties.getPhoenix().concat(urlProperties.getPhoenixRoomByProperty()),
                        Room[].class, propertyId).getBody());

    }

}
