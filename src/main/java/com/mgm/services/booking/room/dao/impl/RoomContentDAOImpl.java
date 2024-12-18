package com.mgm.services.booking.room.dao.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.RoomContentDAO;
import com.mgm.services.booking.room.model.content.Room;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

import lombok.extern.log4j.Log4j2;

import org.springframework.web.client.RestTemplate;

/**
 * Implementation class for RoomContentDAO to fetch room marketing content.
 */
@Component
@Log4j2
public class RoomContentDAOImpl extends BaseContentDAOImpl implements RoomContentDAO {

    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     * 
     * @param urlProperties
     *            URL Properties
     * @param domainProperties
     *            Domain Properties
     * @param restTemplate
     *            Configured Rest template
     * @param applicationProperties
     *            Application Properties
     */
    public RoomContentDAOImpl(URLProperties urlProperties, DomainProperties domainProperties,
                              ApplicationProperties applicationProperties) {
        super(urlProperties, domainProperties, applicationProperties);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.RoomContentDAO#getRoomContent(java.lang
     * .String)
     */
    @Override
    public Room getRoomContent(String roomId) {

        return Arrays.asList(getClient()
                .getForEntity(getDomainProperties().getContentapi().concat(getUrlProperties().getRoomContentApi()),
                        Room[].class, roomId)
                .getBody()).get(0);
    }
    
    @Override
    public Room getRoomContent(String operaCode, String operaPropertyCode, boolean isPrimary) {
    	try {
    		Map<String, Object> uriParams = new HashMap<>();
    		uriParams.put("operaCode", operaCode);
    		uriParams.put("operaPropertyCode", operaPropertyCode);
    		uriParams.put("primary", isPrimary);
    		return Arrays.asList(getClient()
                    .getForEntity(getDomainProperties().getContentapi().concat(getUrlProperties().getPropertyRoomContentApi()),
                            Room[].class, uriParams)
                    .getBody()).get(0);
    	} catch (Exception ex) {
    		log.error("Error while invoking content API with Opera Room Type Code and Opera Property Code : ", ex.getMessage());
    		return null;
    	}
        
    }


    
}
