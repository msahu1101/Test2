package com.mgm.services.booking.room.properties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration class to read properties from application.properties file with
 * "application" prefix
 *
 */
@Component
@ConfigurationProperties(
        prefix = "application")
public @Data class ApplicationProperties {

    private Map<String, String> timezone;
    private boolean sslInsecure;
    private String secretName;
    private String awsSecretsRegion;
    private List<String> paymentErrorCodes;
    private List<String> validChannels;
    private List<String> cookieChannels;
    private String[] corsOrigin;
    private String corsHeaders;
    private List<String> whitelisturl;
    private boolean room;
    private String myvegasAuthCode;
    private String cookieSameSite;
    private boolean cookieSecure;
    private String sharedCookieDomain;
    private List<String> testRedemptionCodes;    

    private int minimumAge;
    private List<String> whitelistChannels;
    private String secretKeyVaultUrl;
    private String subscription;
    private String resourceGroup;
    private String resourceName;
    private String paymentEnvironment;
    private String apigeeEnvironment;
    private String itineraryEnvironment;
    private String nsEnvironment;
    private int itineraryConnectionTimeout;
    private int itineraryReadTimeout;
    private String decryptPrivateKey;
    private List<String> bypassAfsChannels = new ArrayList<>();
    private List<String> bypassMyvegasChannels = new ArrayList<>();
    private int myVegasConnectionTimeoutInSecs;
    private int myVegasReadTimeoutInSecs;
    private int contentConnectionTimeoutInSecs;
    private int contentReadTimeoutInSecs;
    private int loyaltyConnectionTimeoutInMilliSecs;
    private int loyaltyReadTimeoutInMilliSecs;
    private int loyaltyClientMaxConn;
    private int loyaltyClientMaxConnPerRoute;
    private int customerOfferRequestTimeoutInSec;
    private List<String> excludeEmailForChannels;
    private String myVegasEnvironment;
    private String crsUcpRetrieveResvEnvironment;
    private String ocrsOcpApimSubscriptionKey;
    private String ocrsEnv;
    private List<String> borgataSpecialRequests;
    private String borgataPropertyId;
    private boolean skipCapiLookupsForAFS;
    private boolean gseDisabled;
    private Map<String, String> hotelCode;
    private Map<String, String> siteId;
    private boolean enableJwtLogging;
    private String rtcEmailsOnboardedListSecretKey;
    private String rbsEnv;
    private boolean enableFuzzyNameMatch;
    private String rtcHDEPackageEmailsOnboardedListSecretKey;
    private String loyaltyGetPlayerPromoVersion;
    private String loyaltyUpdatePlayerPromoVersion;
    private String acrsPropertyListSecretKey;
    private List<String> validF1ProductCodes;
    private List<String> tcolvF1ComponentCodes;
    private String f1PackageTag;
    private String soostoneProgramTag;
    private int commitInventoryMaxCount;
    private int maxF1TicketCount;
    private int minF1TicketCount;
    private String publicF1ProductPrefix;
    private List<String> transferredPropertyIds;
    private Map<String, Date> handOverDate;
    private Map<String, String> handOverErrorCode;
    private Map<String, String> f1N24AdditionalTicket;
    private String tcolvHotelCode;

    private String baseTCOLVRatePlan;
    private String tcolvPropertyId;
    private String enableNewSegmentsKey;

    private int acrsMaxConnectionPerDaoImpl;
    private int acrsPricingMaxConnectionPerDaoImpl;
    private int acrsConnectionPerRouteDaoImpl;
    private int acrsPricingConnectionPerRouteDaoImpl;
    private int enrMaxConnectionPerDaoImpl;
    private int enrConnectionPerRouteDaoImpl;
    private int petMaxConnectionPerDaoImpl;
    private int petConnectionPerRouteDaoImpl;
    private int maxConnectionPerDaoImpl;
    private int connectionPerRouteDaoImpl;
    private int contentMaxConnectionPerDaoImpl;
    private int ContentConnectionPerRouteDaoImpl;
    // The timeout when requesting a connection from the connection manager.
    private int readTimeOut;
    // Determines the timeout in milliseconds until a connection is established.
    private int connectionTimeout;
    // The timeout for waiting for data
    private int socketTimeOut;
    private int readTimeOutACRS;
    private int connectionTimeoutACRS;
    private int socketTimeOutACRS;
    private int readTimeOutOperaENR;
    private int connectionTimeOutOperaENR;
    private int socketTimeOutOperaENR;
    private int readTimeOutENR;
    private int connectionTimeoutENR;
    private int socketTimeOutENR;
    private int readTimeOutContent;
    private int connectionTimeoutContent;
    private int socketTimeOutContent;
    private int readTimeOutPET;
    private int connectionTimeoutPET;
    private int socketTimeOutPET;
    private String partnerVersion;
    private boolean partnerSearchLive;
    private String partnerBasicAuthUsername;
    private String partnerBasicAuthPassword;
    private String partnerSearchMGMId;
    private boolean permanentInfoLogEnabled;
    private boolean phoenixCacheEnabled;
    private String tempInfoLogEnabled;
    private long contentRestTTL;
    private long crsRestTTL;
    private long enrRestTTL;
    private long commonRestTTL;
    private long petRestTTL;
    private String enableZeroAmountAuthKey;
    private String enableZeroAmountAuthKeyTCOLVCreate;
    private String enableZeroAmountAuthKeyTCOLVModify;
    // eVar141 property map
    private Map<String, String> propertyCodeVar141Map;
    private Map<String, String> tcolvF1GrandStandComponentCode;

    private String poDeepLinkKey;
    private String poDeepLinkIV;
    private String poDeepLinkEncAlgorithm;
    private String rbsENRRedisIntegrationEnabled;


    /**
     * Returns default time zone to be used application wide.
     * 
     * @return Returns default time zone to be used application wide.
     */
    public String getDefaultTimezone() {
        return timezone.get("default");
    }

    /**
     * Returns time zone based on the property Id or default time zone
     * 
     * @param propertyId
     *            Property Identifier
     * @return Returns time zone based on the property or default time zone when
     *         time zone for property is not available
     */
    public String getTimezone(String propertyId) {
        if (timezone.containsKey(propertyId)) {
            return timezone.get(propertyId);
        } else {
            return getDefaultTimezone();
        }
    }

    public String getHotelCode(String propertyId) {
        return hotelCode.get(propertyId);
    }

    /**
     * Return the properyId matching the given hotel code.
     * 
     * @param code
     *            hotel code string
     * @return propertyId GUID
     */
    public String getPropertyIdFromHotelCode(String code) {
        for (Map.Entry<String, String> hotelCodeEntry : hotelCode.entrySet()) {
            if (hotelCodeEntry.getValue().equals(code)) {
                return hotelCodeEntry.getKey();
            }
        }
        return null;
    }

    /**
     * Return the propertyIds matching the given hotel code.
     *
     * @param code
     *            hotel code string
     * @return propertyId GUID
     */
    public List<String> getPropertyIdsFromHotelCode(String code) {
        List<String> propertyIds = new ArrayList<>();
        for (Map.Entry<String, String> hotelCodeEntry : hotelCode.entrySet()) {
            if (hotelCodeEntry.getValue().equals(code)) {
                propertyIds.add( hotelCodeEntry.getKey());
            }
        }
        return propertyIds;
    }
    
    public String getPropertyIdForSiteId(String siteId) {
        return this.siteId.get(siteId);
    }
    
    public Date getHandoverDate(String propertyId) {
        return handOverDate.get(propertyId);
    }
    
    public String getHandOverErrorCode(String propertyId) {
        return handOverErrorCode.get(propertyId);
    }

    public String getF1N24AdditionalTicketByGrandStand(String grandstandCode) {
        return f1N24AdditionalTicket.get(grandstandCode);
    }

    public String getTcolvF1ComponentCodeByGrandStand(String grandstandCode) {
        return tcolvF1GrandStandComponentCode.get(grandstandCode);
    }

    public String getVar141ValueFromHotelCode(String hotelCode) { return propertyCodeVar141Map.get(hotelCode); }
}
