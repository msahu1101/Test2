package com.mgm.services.booking.room.dao.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.loyalty.*;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.LoyaltyDao;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.util.CommonUtil;


import feign.FeignException;

import lombok.extern.log4j.Log4j2;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
@Log4j2
public class LoyaltyDaoImpl implements LoyaltyDao {

    @Autowired
    private IDMSTokenDAO idmsDao;

    private ApplicationProperties appProps;

    private final RestTemplate client;

    private String baseLoyaltyURL;

    private String loyaltyPlayerPromosURL;

    private String patronPromosURL;

    private String loyaltyPromoVersion;

    public LoyaltyDaoImpl(RestTemplateBuilder builder, DomainProperties domainProps, URLProperties urlProperties,
                          ApplicationProperties appProps) {
        super();
        this.appProps = appProps;
        this.client = CommonUtil.getRetryableRestTemplate(builder, appProps.isSslInsecure(), true,
                appProps.getLoyaltyClientMaxConnPerRoute(),
                appProps.getLoyaltyClientMaxConn(),
                appProps.getLoyaltyConnectionTimeoutInMilliSecs(),
                appProps.getLoyaltyReadTimeoutInMilliSecs(),
                appProps.getSocketTimeOut(),
                1,
                appProps.getCommonRestTTL());
        baseLoyaltyURL = domainProps.getLoyalty().trim();
        loyaltyPlayerPromosURL = urlProperties.getPlayerPromos();
        patronPromosURL = urlProperties.getPatronPromos();
        loyaltyPromoVersion = appProps.getLoyaltyGetPlayerPromoVersion();
      
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.LoyaltyDao#getPlayerPromos(java.lang.
     * String)
     */
    @Override
    public List<CustomerPromotion> getPlayerPromos(String mlifeNumber) {

        String authToken = idmsDao.generateToken().getAccessToken();

        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        String correlationId = httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID);
        if (StringUtils.isBlank(correlationId)) {
            correlationId = UUID.randomUUID().toString();
            log.info("CorrelationId was not found in header, generated Id is: {}", correlationId);
        }

        HttpHeaders headers = CommonUtil.createBasicHeaders(authToken,correlationId);

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put(ServiceConstant.MLIFE_NUM, mlifeNumber);
        uriVariables.put(ServiceConstant.API_VERSION, appProps.getLoyaltyGetPlayerPromoVersion());

        HttpEntity<String> request = new HttpEntity<>(headers);

        // Suppressing error here as it would be best to serve other offers even
        // when unable to get patron offers
        try {

            String url = baseLoyaltyURL + loyaltyPlayerPromosURL;
            log.info("Sent request to getPlayerPromos for mlife {} with correlation id {}", mlifeNumber, correlationId);
            PlayerPromotionResponse response = client.exchange(url,HttpMethod.GET,request,PlayerPromotionResponse.class,uriVariables).getBody();

            log.info("Received the response from getPlayerPromos as {}",
                    CommonUtil.convertObjectToJsonString(response));

            // filter to only include open promotions
            List<CustomerPromotion> promotions = filterPromotions(response.getCustomerPromotions());
            
            // set GSE property ids based on patron siteids
            setPropertyIds(promotions);
            
            return promotions;
        } catch (Exception e) {
        	log.info("Call to getPlayerPromos resulted in an error", e);
        }

        return new ArrayList<>();
    }

    @Override
    public void updatePlayerPromo(Collection<UpdatedPromotion> promos) {

        if (CollectionUtils.isEmpty(promos)) {
            log.warn("Call to updatePlayerPromo | No Patron promos to update");
            return;
        }

        final String authToken = idmsDao.generateToken().getAccessToken();

        for (UpdatedPromotion promotion : promos) {
            try {
                final UpdatePlayerPromotionRequest updatePlayerPromotionRequest = new UpdatePlayerPromotionRequest();
                updatePlayerPromotionRequest.setUpdatePromotion(promotion);
                final String correlationId = UUID.randomUUID().toString();
                HttpHeaders headers = CommonUtil.createBasicHeaders(authToken,correlationId);
                HttpEntity<UpdatePlayerPromotionRequest> request = new HttpEntity<>(updatePlayerPromotionRequest, headers);
                Map<String, String> uriVariables = new HashMap<>();
                uriVariables.put(ServiceConstant.MLIFE_NUM, String.valueOf(promotion.getPatronId()));
                uriVariables.put(ServiceConstant.CORE_VERSION, appProps.getLoyaltyUpdatePlayerPromoVersion());
                String url = baseLoyaltyURL + patronPromosURL;
                log.info("Request to updatePlayerPromo with correlationId: {}, mlife#: {}, request: {}",
                        correlationId, promotion.getPatronId(), CommonUtil.convertObjectToJsonString(request));

                UpdatePlayerPromotionResponse response  = client.exchange(url, HttpMethod.PUT,request,
                        UpdatePlayerPromotionResponse.class,uriVariables).getBody();

                log.info("Response from updatePlayerPromo as {}", CommonUtil.convertObjectToJsonString(response));

            } catch (Exception e) {
                // This error should be monitored and reported
                log.warn("Call to updatePlayerPromo resulted in an error {}", ExceptionUtils.getStackTrace(e));
            }
        }
    }
    
    private void setPropertyIds(List<CustomerPromotion> promotions) {
        promotions.forEach(
                promo -> promo.setPropertyId(appProps.getPropertyIdForSiteId(promo.getSiteInfo().getSiteId())));
    }

    private List<CustomerPromotion> filterPromotions(List<CustomerPromotion> promotions) {
        // return only promos which are open
        return promotions.stream().filter(this::isPromoOpen).collect(Collectors.toList());
    }

    private boolean isPromoOpen(CustomerPromotion promo) {
        final String promoStatus = promo.getStatus();
        return (promoStatus == null || promoStatus.equalsIgnoreCase("Mailer Sent")
                || promoStatus.equalsIgnoreCase("Cancelled") || promoStatus.equalsIgnoreCase("Host Invite")
                || promoStatus.equalsIgnoreCase("Enrolled") || promoStatus.equalsIgnoreCase("Email Sent"));
    }

}
