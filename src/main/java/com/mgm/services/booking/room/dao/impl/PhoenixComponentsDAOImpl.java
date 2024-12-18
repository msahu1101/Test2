package com.mgm.services.booking.room.dao.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.dao.PhoenixComponentsDAO;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class to fetch component details from Phoenix
 * 
 * @author laknaray
 */
@Component
@Log4j2
public class PhoenixComponentsDAOImpl implements PhoenixComponentsDAO {

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
    public PhoenixComponentsDAOImpl(URLProperties urlProperties, DomainProperties domainProperties,
            RestTemplateBuilder builder) {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = builder.build();
    }

    @Override
    public Map<String, RoomComponent> getRoomComponents() {

        final Map<String, RoomComponent> data = new HashMap<>();
        final RoomComponent[] allComponents = client.getForEntity(domainProperties.getPhoenix().concat(urlProperties.getPhoenixRoomComponents()),
                RoomComponent[].class).getBody();

        for (final RoomComponent component : allComponents) {
            data.put(component.getId(), component);
        }
        log.info("Total number of room components {}", allComponents.length);
        return data;

    }

}
