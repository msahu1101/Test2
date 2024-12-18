package com.mgm.services.booking.room.dao.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.ProgramContentDAO;
import com.mgm.services.booking.room.model.content.CuratedOfferResponse;
import com.mgm.services.booking.room.model.content.Program;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for ProgramContentDAO to fetch marketing content.
 */
@Component
@Log4j2
public class ProgramContentDAOImpl extends BaseContentDAOImpl implements ProgramContentDAO {

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
    public ProgramContentDAOImpl(URLProperties urlProperties, DomainProperties domainProperties,
             ApplicationProperties applicationProperties) {
        super(urlProperties, domainProperties, applicationProperties);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ProgramContentDAO#getProgramContent(
     * java.lang.String, java.lang.String)
     */
    @Override
    public Program getProgramContent(String propertyId, String programId) {
    	try {
        log.debug(getDomainProperties().getContentapi().concat(getUrlProperties().getProgramContentApi()));
        log.debug("Property Id: {}, Program Id: {}", propertyId, programId);
        return getClient()
                .getForEntity(getDomainProperties().getContentapi().concat(getUrlProperties().getProgramContentApi()),
                        Program.class, propertyId, programId)
                .getBody();
    	} catch(Exception ex) {
    		log.info("Error Occured while Inovking Contentent API: {}", ex);
    		return null;
    	}

    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.dao.ProgramContentDAO#getCuratedHotelOffers(java.lang.String)
     */
    @Override
    public CuratedOfferResponse getCuratedHotelOffers(String propertyId) {

        if (StringUtils.isEmpty(propertyId)) {
            propertyId = "mgmresorts";
        }

        log.debug("Property Id: {}", propertyId);
        CuratedOfferResponse response = getClient().getForEntity(
                getDomainProperties().getContentapi().concat(getUrlProperties().getCuratedOffersContentApi()),
                CuratedOfferResponse.class, propertyId).getBody();
        // null response is expected incase of 400 errors from content
        return (null == response ? new CuratedOfferResponse() : response);

    }


}
