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
import com.mgm.services.booking.room.service.cache.SignupEmailCacheService;
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
public class SignupEmailCacheServiceImpl extends AbstractCacheService implements SignupEmailCacheService {

    private static final String CACHE_NAME = "signupEmail";

    private static final String SIGNUP_KEY = "_signup";

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    private AuroraProperties auroraProperties;

    @Autowired
    private EmailTemplateDAO emailTemplateDao;

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    protected Map<Object, Object> fetchData(String key) {
        Map<Object, Object> emailMap = new HashMap<>();
        try {
            log.info("[EhCacheLogger]|'{}' Cache -> Fetching emails", getCacheName());

            EmailCacheServiceHelper.populateEmailMap(key + SIGNUP_KEY, emailMap,
                    emailTemplateDao.getSignupCompletionTemplate(key));

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
        return cacheProperties.getSignupEmailRefreshFreqInSecs();
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

    @Override
    protected List<String> getKeys() {
        return auroraProperties.getSignupEmailPropertyIds();
    }

    @Override
    public Email getSignupEmailTemplate(String propertyId) {
        return BaseCommonUtil.copyProperties(getCachedObject(propertyId + SIGNUP_KEY), Email.class);
    }

}
