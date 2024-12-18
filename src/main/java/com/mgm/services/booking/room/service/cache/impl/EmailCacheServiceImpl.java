package com.mgm.services.booking.room.service.cache.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.EmailTemplateDAO;
import com.mgm.services.booking.room.model.Email;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.properties.CacheProperties;
import com.mgm.services.booking.room.service.cache.EmailCacheService;
import com.mgm.services.booking.room.service.cache.helper.EmailCacheServiceHelper;
import com.mgm.services.common.util.BaseCommonUtil;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for EmailCacheService providing functions for caching
 * and returns email templates.
 *
 */
@Component
@Log4j2
public class EmailCacheServiceImpl extends AbstractCacheService implements EmailCacheService {

    private static final String CACHE_NAME = "email";

    private static final String CONFIRM_KEY = "_confirm";

    private static final String CANCEL_KEY = "_cancel";
    
    private static final String HDE_PACKAGE_CONFIRM_KEY = "_hde_package_confirm";

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    private AuroraProperties auroraProperties;

    @Autowired
    private EmailTemplateDAO emailTemplateDao;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getCacheName()
     */
    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * fetchData(java.lang.String)
     */
    @Override
    protected Map<Object, Object> fetchData(String key) {
        Map<Object, Object> emailMap = new HashMap<>();
        try {
            // using some block statements since this is background process
            // anyway
            log.info("[EhCacheLogger]|'{}' Cache -> Fetching emails", getCacheName());

            EmailCacheServiceHelper.populateEmailMap(key + CONFIRM_KEY, emailMap,
                    emailTemplateDao.getRoomConfirmationTemplate(key));

            EmailCacheServiceHelper.populateEmailMap(key + CANCEL_KEY, emailMap,
                    emailTemplateDao.getRoomCancellationTemplate(key));
            
            EmailCacheServiceHelper.populateEmailMap(key + HDE_PACKAGE_CONFIRM_KEY, emailMap,
            			emailTemplateDao.getHDEPackageConfirmationTemplate(key));
            

            log.info("[EhCacheLogger]|'{}' Cache -> Successfully fetched emails", getCacheName());

        } catch (Exception e) {
            failureCount++;
            log.error("[EhCacheLogger]|'{}' Cache -> Exception while retrieving emails from AEM for key {}",
                    getCacheName(), key, e);
        }
        return emailMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getRefreshPeriodInSeconds()
     */
    @Override
    protected long getRefreshPeriodInSeconds() {
        return cacheProperties.getEmailRefreshFreqInSecs();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getRetryAttempts()
     */
    @Override
    protected int getRetryAttempts() {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.cache.impl.AbstractCacheService#
     * getKeys()
     */
    @Override
    protected List<String> getKeys() {
        return auroraProperties.getEmailPropertyIds();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.cache.EmailCacheService#
     * getEmailTemplate(java.lang.String)
     */
    @Override
    public Email getConfirmationEmailTemplate(String propertyId) {
        return BaseCommonUtil.copyProperties(getCachedObject(propertyId + CONFIRM_KEY), Email.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.cache.EmailCacheService#
     * getCancellationEmailTemplate(java.lang.String)
     */
    @Override
    public Email getCancellationEmailTemplate(String propertyId) {
        return BaseCommonUtil.copyProperties(getCachedObject(propertyId + CANCEL_KEY), Email.class);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.cache.EmailCacheService#
     * getHDEPackageConfirmationEmailTemplate(java.lang.String)
     */
    @Override
    public Email getHDEPackageConfirmationEmailTemplate(String propertyId) {
        return BaseCommonUtil.copyProperties(getCachedObject(propertyId + HDE_PACKAGE_CONFIRM_KEY), Email.class);
    }


}
