/**
 * 
 */
package com.mgm.services.booking.room.dao.impl;

import javax.net.ssl.SSLException;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.dao.EmailTemplateDAO;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class to fetch email template from public AEM end points.
 * 
 */
@Component
@Log4j2
public class EmailTemplateDAOImpl implements EmailTemplateDAO {

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
     * @param applicationProperties
     *            Application Properties
     * @param builder
     *            Spring's auto-configured rest template builder
     * @throws SSLException
     *             Throws SSL Exception
     */
    public EmailTemplateDAOImpl(URLProperties urlProperties, DomainProperties domainProperties,
            ApplicationProperties applicationProperties, RestTemplateBuilder builder) throws SSLException {
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = CommonUtil.getRestTemplate(builder, applicationProperties.isSslInsecure(),true,
                applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),
                applicationProperties.getCommonRestTTL());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.EmailTemplateDAO#getEmailTemplate(java.
     * lang.String)
     */
    @Override
    public String getRoomConfirmationTemplate(String propertyId) {
        log.info("Getting room confirmation template for {} with uri {}", propertyId,
                urlProperties.getAemRoomConfirmationTemplate());

        return client.getForEntity(domainProperties.getAem().concat(urlProperties.getAemRoomConfirmationTemplate()),
                String.class, propertyId).getBody();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.EmailTemplateDAO#
     * getRoomCancellationTemplate(java.lang.String)
     */
    @Override
    public String getRoomCancellationTemplate(String propertyId) {
        log.info("Getting room cancellation template for {} with uri {}", propertyId,
                urlProperties.getAemRoomCancellationTemplate());

        return client.getForEntity(domainProperties.getAem().concat(urlProperties.getAemRoomCancellationTemplate()),
                String.class, propertyId).getBody();
    }

    @Override
    public String getSignupCompletionTemplate(String propertyId) {
        log.info("Getting signup completion template for {} with uri {}", propertyId,
                urlProperties.getAemSignupCompletionTemplate());

        return client.getForEntity(domainProperties.getAem().concat(urlProperties.getAemSignupCompletionTemplate()),
                String.class, propertyId).getBody();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.EmailTemplateDAO#getHDEPackageConfirmationTemplate(java.
     * lang.String)
     */

	@Override
	public String getHDEPackageConfirmationTemplate(String propertyId) {
		log.info("Getting HDE Package room confirmation template for {} with uri {}", propertyId,
                urlProperties.getAemHDEPackageRoomConfirmationTemplate());

        return client.getForEntity(domainProperties.getAem().concat(urlProperties.getAemHDEPackageRoomConfirmationTemplate()),
                String.class, propertyId).getBody();

	}

}
