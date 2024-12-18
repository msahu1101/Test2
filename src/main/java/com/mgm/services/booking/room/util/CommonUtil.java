package com.mgm.services.booking.room.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.joda.cfg.FormatConfig;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.filter.RequestHeadersEnum;
import com.mgm.services.booking.room.model.DestinationHeader;
import com.mgm.services.booking.room.model.RoomCartItem;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.util.permormance.RestTemplateMock;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.model.CartItem;
import com.mgm.services.common.model.Customer;
import com.mgm.services.common.model.RedemptionValidationResponse;
import com.mgm.services.common.model.ServicesSession;
import com.mgm.services.common.util.BaseCommonUtil;
import com.mgm.services.common.util.DateUtil;
import com.mgmresorts.aurora.common.Payment;
import com.mgmresorts.aurora.common.RoomReservation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.joda.time.LocalDateTime;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Final class to define common utility methods required by the application.
 * 
 */
/**
 * @author KSAMMANDAN
 *
 */
@Log4j2
public final class CommonUtil extends BaseCommonUtil {

    /**
     * The code Value
     */
    private static String codeValue = "474bf9f97fd67b12";

    private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS = 4 * 1000;
    private static final int VALIDATE_INACTIVE_CONNECTION_TIME_MILLIS =2 * 1000;
    private static final int MAX_IDEAL_TIME_SEC = 2 ;
    private CommonUtil() {
        // Hiding implicit constructor
    }
    
    /**
     * Return the encoded authorization header
     *
     * @param clientId
     *            client id
     * @param clientSecret
     *            client secret
     * @return headers
     */
    public static HttpHeaders createAuthorizationHeader(String clientId, String clientSecret) {
        String encodedCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        String authHeader = ServiceConstant.HEADER_AUTH_BASIC + encodedCredentials;
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, authHeader);
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_URLENCODED);
        return headers;
    }
    
    public static HttpHeaders createIdmsRequestHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_URLENCODED);
         return headers;
    }

    /**
     * Return the authorization header with access token
     *
     * @param accessToken
     *            the access token
     * @return headers
     */
    public static HttpHeaders createAccessTokenAuthorizationHeader(String accessToken) {
        String authHeader = ServiceConstant.HEADER_AUTH_BEARER + accessToken;
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, authHeader);
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_URLENCODED);
        headers.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_JSON);
        return headers;
    }

    public static DestinationHeader createDestinationHeader(String hotelCode, String channelIdentifier, String httpMethod, String agentId, String rrUpSell) {
    	DestinationHeader destinationHeader = new DestinationHeader();
        Map.Entry<String, String> amaClientRef = getClientRef();

        if (StringUtils.isNotEmpty(hotelCode)) {
            destinationHeader.setAmaPos(ServiceConstant.HOTEL_CODE + hotelCode + "\"}");
        }
    	destinationHeader.setAmaChannelIdentifiers(ServiceConstant.VENDOR_CODE + channelIdentifier +"\"}");
    	destinationHeader.setContentType(ServiceConstant.CONTENT_TYPE_JSON);
    	destinationHeader.setAccept(ServiceConstant.CONTENT_TYPE_JSON);
    	destinationHeader.setHttpMethod(httpMethod);

    	if (StringUtils.isNotEmpty(rrUpSell)) {
    		destinationHeader.setAmaPos(String.format(ServiceConstant.RRUPSELL_DETAILS, hotelCode, rrUpSell));            
        }
        if (StringUtils.isNotEmpty(agentId)) {
        	destinationHeader.setAmaReservationOwner(String.format(ServiceConstant.IATA_AGENT_DETAILS, agentId));           
        }
        if(amaClientRef != null) {
            destinationHeader.setAmaClientRef(amaClientRef.getValue());
        }

    	return destinationHeader;
    }
    public static Map.Entry<String, String> getPartnerClientRef() {
        String idType = null;
        String value = null;
        try {
            HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
            value = httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID);
            idType = ServiceConstant.X_PARTNER_CORRELATION_ID;
            if(StringUtils.isEmpty(value)) {
                value = httpRequest.getHeader(ServiceConstant.X_MGM_TRANSACTION_ID);
            }
            if(StringUtils.isEmpty(value)){
                value = UUID.randomUUID().toString();
            }
        }catch(IllegalStateException e) {
            log.warn("Error While fetching client reference ID, setting random id.");
            return new AbstractMap.SimpleEntry<String, String>(ServiceConstant.X_PARTNER_CORRELATION_ID, UUID.randomUUID().toString());
        }
        return new AbstractMap.SimpleEntry<String, String>(idType, value);
    }

    /**
     * This returns a pair of values containing the reference ID of the transaction
     *
     * The pair's key is set to what type of ID it is and the value is the actual
     * value of the ID
     * @return type-value pair
     */
    private static Map.Entry<String, String> getClientRef() {
        String idType = null;
        String value = null;
        try {
            HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
            value = httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID);
            idType = ServiceConstant.X_MGM_CORRELATION_ID;
            if(StringUtils.isEmpty(value)) {
                value = httpRequest.getHeader(ServiceConstant.X_MGM_TRANSACTION_ID);
                idType = ServiceConstant.X_MGM_TRANSACTION_ID;
            }
        }catch(IllegalStateException e) {
            log.warn("Error While fetching client reference ID");
            return null;
        }
        return new AbstractMap.SimpleEntry<String, String>(idType, value);
    }

    public static Map.Entry<String, String> getClientRefForPayment() {
        return getClientRef();
    }

    /**
     * Common method to create headers for PET
     *
     * @param destinationHeader
     * @param source
     * @param token
     * @param isPoFlow
     * @return
     */
    public static HttpHeaders createPETHeaders(DestinationHeader destinationHeader, String source, String token, Boolean isPoFlow) {

        Map.Entry<String, String> clientRefId = getClientRefForPayment();
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
        headers.set(ServiceConstant.DESTINATION_HEADER, CommonUtil.convertObjectToJsonString(destinationHeader));
        headers.set(ServiceConstant.HEADER_SOURCE, source);
        headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + token);
        if(isPoFlow != null) {
            headers.set(ServiceConstant.PO_FLAG, String.valueOf(isPoFlow));
        }
        if(clientRefId != null) {
            headers.set(clientRefId.getKey(), clientRefId.getValue());
        }
        setPaymentSourceAndChannelHeaders(headers);
        return headers;
    }
        
    public static HttpHeaders createCrsHeaders(String hotelCode, String channelIdentifier, String agentId, String rrUpSell) {
        HttpHeaders headers = createCrsHeadersNoVersion(hotelCode, channelIdentifier);
        if (StringUtils.isNotEmpty(rrUpSell)) {
            headers.set(ServiceConstant.HEADER_CRS_AMA_POS,
                    String.format(ServiceConstant.RRUPSELL_DETAILS, hotelCode, rrUpSell));
        }
        if (StringUtils.isNotEmpty(agentId)) {
            headers.set(ServiceConstant.HEADER_CRS_AMA_OWNER,
                    String.format(ServiceConstant.IATA_AGENT_DETAILS, agentId));
        }
        return headers;
    }

    public static HttpHeaders createCrsHeadersNoVersion(String hotelCode, String channelIdentifier) {
        // Getting details regarding correlationId/transactionId from the current call and setting it amadeus client ref
        Map.Entry<String, String> amaClientRef = getClientRef();

        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_CRS_AMA_POS, ServiceConstant.HOTEL_CODE + hotelCode + "\"}");
        headers.set(ServiceConstant.HEADER_CRS_AMA_CHANNEL_IDENTIFIERS, ServiceConstant.VENDOR_CODE + channelIdentifier +"\"}");
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
        headers.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_JSON);
        if(amaClientRef != null) {
            headers.set(ServiceConstant.HEADER_CLIENT_REF, amaClientRef.getValue());
        }
        return headers;
    }

     public static HttpHeaders createCrsHeadersPRFChannel() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
        headers.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_JSON);
        return headers;
    }

    /**
     * Return Crs Access Token
     *
     * @param token
     * @param source
     * @return access token
     */

    public static HttpHeaders createPaymentHeaders(String token, String source) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_SOURCE, source);
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + token);
        headers.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_JSON);
        setAdditionalPaymentHeaders(headers);
        Map.Entry<String, String> clientRef = getClientRefForPayment();
        headers.set(clientRef.getKey(),clientRef.getValue());
        return headers;
    }
    
    public static HttpHeaders createHeaders(String refDataToken) {
        
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + refDataToken);
        headers.set(ServiceConstant.HEADER_KEY_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
        headers.set(ServiceConstant.X_MGM_TRANSACTIONID, httpRequest.getHeader(ServiceConstant.X_MGM_TRANSACTION_ID));
        headers.set(ServiceConstant.X_MGM_CORRELATION_ID, httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID));
        headers.set(ServiceConstant.X_MGM_SOURCE, httpRequest.getHeader(ServiceConstant.X_MGM_SOURCE));
        headers.set(ServiceConstant.X_MGM_CHANNEL, httpRequest.getHeader(ServiceConstant.X_MGM_CHANNEL));
        return headers;
    }

    public static HttpHeaders createProductInventoryHeaders(String token) {

        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + token);
        headers.set(ServiceConstant.HEADER_KEY_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
        headers.set(ServiceConstant.X_MGM_TRANSACTION_ID, httpRequest.getHeader(ServiceConstant.X_MGM_TRANSACTION_ID));
        headers.set(ServiceConstant.X_MGM_SOURCE, httpRequest.getHeader(ServiceConstant.X_MGM_SOURCE));
        headers.set(ServiceConstant.X_MGM_CHANNEL, httpRequest.getHeader(ServiceConstant.X_MGM_CHANNEL));
        return headers;
    }
    
    public static HttpHeaders createOcrsHeaders(String refDataToken) {
        
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + refDataToken);
        headers.set(ServiceConstant.HEADER_KEY_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
        headers.set(ServiceConstant.X_MGM_TRANSACTIONID, httpRequest.getHeader(ServiceConstant.X_MGM_TRANSACTION_ID));
        headers.set(ServiceConstant.X_MGM_CORRELATION_ID, httpRequest.getHeader(ServiceConstant.X_MGM_CORRELATION_ID));
        headers.set(ServiceConstant.X_MGM_SOURCE, httpRequest.getHeader(ServiceConstant.X_MGM_SOURCE));
        headers.set(ServiceConstant.X_MGM_CHANNEL, httpRequest.getHeader(ServiceConstant.X_MGM_CHANNEL));
    
        return headers;
    }

    /**
     * Return the authorization header with session id
     *
     * @param sessionId
     *            the session id
     * @return headers
     */
    public static HttpHeaders createOktaSessionAuthorizationHeader(String sessionId) {
        String authHeader = ServiceConstant.HEADER_AUTH_OKTA_CUSTOM + sessionId;
        HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_AUTHORIZATION, authHeader);
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_JSON);
        headers.set(ServiceConstant.HEADER_ACCEPT, ServiceConstant.CONTENT_TYPE_ALL);
        return headers;
    }

    /**
     * If program is a myvegas program, ensure that there's corresponding
     * redemption code in session. Also, check that the customer details matches
     * with the profile returned by MyVegas
     *
     * @param myVegasRedemptionItems
     *            myvegasredemption items in session
     * @param roomProgram
     *            the room program
     * @param customer
     *            the customer profile
     * @return true if the program is a myVegas program and user is eligible for
     *         it. Return false if the program is null or is not a valid myvegas
     *         program. Throw exception if the program is valid but the user is
     *         ineligible
     */
    public static boolean isEligibleForMyVegasRedemption(
            Map<String, RedemptionValidationResponse> myVegasRedemptionItems, RoomProgram roomProgram,
            Customer customer) {
        boolean isValidProgram = roomProgram != null && roomProgram.getId() != null;
        if (isValidProgram && isContainMyVegasTags(roomProgram.getTags())) {
            boolean isMyVegasInCache = myVegasRedemptionItems != null
                    && myVegasRedemptionItems.containsKey(roomProgram.getId());
            boolean isMatchingMyVegasProfile = isMyVegasInCache && isMatchingMyVegasProfile(customer,
                    myVegasRedemptionItems.get(roomProgram.getId()).getCustomer());
            log.info("Checking myvegas validity for {}. Cache Presence is {}. Matching Profile is {} ",
                    roomProgram.getId(), isMyVegasInCache, isMatchingMyVegasProfile);
            if (isMyVegasInCache && isMatchingMyVegasProfile) {
                return true;
            } else {
                throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
            }
        }
        return false;
    }
    
    /**
     * This method should be invoked if the myvegas tags check already happened.
     * If program is a myvegas program, ensure that there's corresponding
     * redemption code in session. Also, check that the customer details matches
     * with the profile returned by MyVegas.
     * 
     * @param myVegasRedemptionItems
     *            myvegas redemption items in session
     * @param programId
     *            the room program GUID
     * @param customer
     *            the customer profile
     * @return true if the program is a myVegas program and user is eligible for
     *         it. Return false if the program is null or is not a valid myvegas
     *         program. Throw exception if the program is valid but the user is
     *         ineligible
     */
    public static boolean isEligibleForMyVegasRedemption(
            Map<String, RedemptionValidationResponse> myVegasRedemptionItems, String programId, Customer customer) {
        boolean isMyVegasInCache = myVegasRedemptionItems != null && myVegasRedemptionItems.containsKey(programId);
        boolean isMatchingMyVegasProfile = isMyVegasInCache
                && isMatchingMyVegasProfile(customer, myVegasRedemptionItems.get(programId).getCustomer());
        log.info("Checking myvegas validity for {}. Cache Presence is {}. Matching Profile is {} ", programId,
                isMyVegasInCache, isMatchingMyVegasProfile);
        return (isMyVegasInCache && isMatchingMyVegasProfile);
    }

    /**
     * Returns the room cart items from the cart list. All items are returned if
     * the source is empty non UUID. If source is a UUID, only room items belong
     * to the UUID will be returned.
     * 
     * @param cartItems
     *            the cart items
     * @param source
     *            Source invoking the API
     * @return the room cart items
     */
    public static List<RoomCartItem> getRoomCartItems(List<CartItem> cartItems, String source) {
        List<RoomCartItem> roomCartItems = new ArrayList<>();

        if (null == cartItems) {
            return roomCartItems;
        }

        boolean filterBySource = false;
        if (StringUtils.isNotBlank(source) && isUUID(source)) {
            filterBySource = true;
        }

        for (CartItem item : cartItems) {

            if (item instanceof RoomCartItem) {
                if (filterBySource) {
                    if (((RoomCartItem) item).getReservation().getPropertyId().equals(source)) {
                        roomCartItems.add((RoomCartItem) item);
                    }
                } else {
                    roomCartItems.add((RoomCartItem) item);
                }
            }
        }

        return roomCartItems;
    }

    /**
     * This method retrieves the itinerary ids corresponding to the items in the
     * cart. The itinerary id correspond to the property saved on the aurora
     * side as a pre-reservation step
     * 
     * @param cartItems
     *            the cart items for room reservation
     * @return list of aurora itinerary ids
     */
    public static List<String> getAuroraItinerariesInCart(List<CartItem> cartItems) {

        List<RoomCartItem> roomCartItems = getRoomCartItems(cartItems, null);

        // Iterate through cart items and get aurora itinerary ids
        List<String> auroraItineraries = new ArrayList<>();
        for (RoomCartItem cartItem : roomCartItems) {
            if (cartItem.getAuroraItineraryId() != null) {
                auroraItineraries.add(cartItem.getAuroraItineraryId());
            }
        }
        return auroraItineraries;
    }


    public static RestTemplate getRetryableRestTemplate(RestTemplateBuilder builder, boolean insecure, boolean isLiveCRS, int connectionPerRoute, int maxConnection, int connectionTimeOut, int readTimeOut, int socketTimeOut,int retryCount, long ttl, boolean... noReuseConnection) {

        if (!isLiveCRS) {
            return new RestTemplateMock();
        }
        RestTemplate restTemplate = builder.build();
        if(null != restTemplate) {
            boolean ignoreReuseConnection = noReuseConnection.length > 0 && noReuseConnection[0];
            restTemplate.setRequestFactory(clientHttpRequestFactory(insecure, maxConnection, connectionPerRoute, connectionTimeOut, readTimeOut, socketTimeOut, retryCount, ttl,ignoreReuseConnection));
            if (insecure) {
                restTemplate.getInterceptors().add((request, body, execution) -> {
                    final StopWatch stopWatch = new StopWatch();
                    stopWatch.start();
                    try {
                        return execution.execute(request, body);
                    } finally {
                        stopWatch.stop();
                        log.info(ServiceConstant.HTTP_CALL_TIME_TAKEN,
                                request.getMethod(), request.getURI(), stopWatch.getTotalTimeMillis());
                    }
                });
            }
        }
        return restTemplate;
    }

    public static HttpComponentsClientHttpRequestFactory clientHttpRequestFactory(boolean insecure, int maxConnection, int connectionPerRoute, int connectionTimeOut, int readTimeOut, int socketTimeOut,int retryCount, long ttl, boolean noReuseConnection) {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(httpClient(insecure,maxConnection, connectionPerRoute, connectionTimeOut, readTimeOut, socketTimeOut,retryCount,ttl, noReuseConnection));
        return clientHttpRequestFactory;
    }
    public static CloseableHttpClient httpClient(boolean insecure, int maxConnection, int connectionPerRoute, int connectionTimeOut, int readTimeOut, int socketTimeOut,int retryCount, long ttl, boolean noReuseConnection) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(readTimeOut > 0 ? readTimeOut : 5000)
                .setConnectTimeout(connectionTimeOut > 0 ? connectionTimeOut : 5000)
                .setSocketTimeout(socketTimeOut > 0 ? socketTimeOut : 5000)
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();
        CloseableHttpClient httpClient;
        HttpClientBuilder clientBuilder;
        //set NoConnectionReuseStrategy
        if(noReuseConnection) {
            clientBuilder = HttpClients.custom()
                    .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
                    .setDefaultRequestConfig(requestConfig)
                    .setConnectionManager(poolingConnectionManager(insecure, maxConnection, connectionPerRoute));
        }else{
            clientBuilder = HttpClients.custom()
                    //evict expired connections from the connection pool after some time
                    .evictExpiredConnections()
                    //Evict idle connections from the connection pool after the set time
                    .evictIdleConnections(MAX_IDEAL_TIME_SEC, TimeUnit.SECONDS)
                    .setDefaultRequestConfig(requestConfig)
                    .setConnectionManager(poolingConnectionManager(insecure, maxConnection, connectionPerRoute)).setKeepAliveStrategy(connectionKeepAliveStrategy(ttl));
        }
        if(retryCount>0){
            httpClient = clientBuilder.setRetryHandler(new SocketTimeoutRetryHandler(retryCount, true))
            .build();
        }else{
            httpClient = clientBuilder.build();
        }
        return httpClient;
    }


    private static class SocketTimeoutRetryHandler extends DefaultHttpRequestRetryHandler {
        SocketTimeoutRetryHandler(int retryCount, boolean requestSentRetryEnabled) {
            super(retryCount, requestSentRetryEnabled, Arrays.asList(NoHttpResponseException.class, UnknownHostException.class, ConnectException.class, NoRouteToHostException.class, SSLException.class));
        }

        @Override
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            log.warn("Retrying request after error {}. Retry count: {}", exception.getClass().toString(), executionCount);
            return super.retryRequest(exception, executionCount, context);
        }
    }

    /**
     * Builds and returns rest template from builder. Also, configures rest
     * template to ignore SSL errors based on insecure flag. This insecure flag
     * will be set as false for lower environments with self-signed
     * certificates.
     * 
     * @param builder
     *            Spring's auto-configured rest template builder
     * @param insecure
     *            Flag to indicate if rest template should skip ssl issues
     * @return returns rest template created from builder
     */

    public static RestTemplate getRestTemplate(RestTemplateBuilder builder, boolean insecure, boolean isLiveCRS, int connectionPerRoute, int maxConnection, int connectionTimeOut, int readTimeOut, int socketTimeOut) {
        return createRestTemplate( builder,  insecure,  isLiveCRS,  connectionPerRoute,  maxConnection,  connectionTimeOut,  readTimeOut,  socketTimeOut,  0);

    }
    public static RestTemplate getRestTemplate(RestTemplateBuilder builder, boolean insecure, boolean isLiveCRS, int connectionPerRoute, int maxConnection, int connectionTimeOut, int readTimeOut, int socketTimeOut, long ttl) {
        return createRestTemplate( builder,  insecure,  isLiveCRS,  connectionPerRoute,  maxConnection,  connectionTimeOut,  readTimeOut,  socketTimeOut,  ttl);

    }
    public static RestTemplate createRestTemplate(RestTemplateBuilder builder, boolean insecure, boolean isLiveCRS, int connectionPerRoute, int maxConnection, int connectionTimeOut, int readTimeOut, int socketTimeOut, long ttl) {
        if (!isLiveCRS) {
            return new RestTemplateMock();
        }
        RestTemplate restTemplate = builder.build();
        if(null != restTemplate) {
            restTemplate.setRequestFactory(clientHttpRequestFactory(insecure, maxConnection, connectionPerRoute, connectionTimeOut, readTimeOut, socketTimeOut,0,ttl,false));
            if (insecure) {
                restTemplate.getInterceptors().add((request, body, execution) -> {
                    final StopWatch stopWatch = new StopWatch();
                    stopWatch.start();
                    try {
                        return execution.execute(request, body);
                    } finally {
                        stopWatch.stop();
                        log.info(ServiceConstant.HTTP_CALL_TIME_TAKEN,
                                request.getMethod(), request.getURI(), stopWatch.getTotalTimeMillis());
                    }
                });
            }
        }
        return restTemplate;
    }
    public static PoolingHttpClientConnectionManager poolingConnectionManager(boolean insecure, int maxConnection, int connectionPerRoute) {
        PoolingHttpClientConnectionManager poolingConnectionManager = null;
        if (insecure) {
            SSLContextBuilder builder = new SSLContextBuilder();
            TrustStrategy acceptingTrustStrategy = (java.security.cert.X509Certificate[] chain,
                                                    String authType) -> true;
            try {
                builder.loadTrustMaterial(null, acceptingTrustStrategy);
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                log.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
            }

            SSLConnectionSocketFactory sslsf = null;
            try {
                sslsf = new SSLConnectionSocketFactory(builder.build());
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                log.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
            }

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                    .register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();

            poolingConnectionManager = new PoolingHttpClientConnectionManager(
                    socketFactoryRegistry);

        } else {
            poolingConnectionManager = new PoolingHttpClientConnectionManager();

        }
        poolingConnectionManager.setMaxTotal(maxConnection > 0 ? maxConnection : 20);
        poolingConnectionManager.setDefaultMaxPerRoute(connectionPerRoute > 0? connectionPerRoute : 5);
        // by default 2 sec
        poolingConnectionManager.setValidateAfterInactivity(VALIDATE_INACTIVE_CONNECTION_TIME_MILLIS);
        return poolingConnectionManager;
    }


    public static ConnectionKeepAliveStrategy connectionKeepAliveStrategy(long ttl) {

        return (HttpResponse response, HttpContext context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator(
                    response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();

                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return ttl>0 ? ttl : DEFAULT_KEEP_ALIVE_TIME_MILLIS;
        };

    }


    /**
     * This method encrypts the plain text using the above code value and random
     * generated salt with AES algorithm
     * 
     * @param text
     *            String or text to encrypt
     * @return Returns the encrypted string
     */
    public static String encrypt(String text) {
        try {

            byte[] bytes = new byte[ServiceConstant.NUM_16];
            new SecureRandom().nextBytes(bytes);
            byte[] saltBytes = bytes;

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(codeValue.toCharArray(), saltBytes, ServiceConstant.NUM_65536,
                    ServiceConstant.NUM_128);
            SecretKey secretKey = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/OFB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();
            byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] encryptedTextBytes = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));

            byte[] buffer = new byte[saltBytes.length + ivBytes.length + encryptedTextBytes.length];
            System.arraycopy(saltBytes, 0, buffer, 0, saltBytes.length);
            System.arraycopy(ivBytes, 0, buffer, saltBytes.length, ivBytes.length);
            System.arraycopy(encryptedTextBytes, 0, buffer, saltBytes.length + ivBytes.length,
                    encryptedTextBytes.length);
            return Base64.getEncoder().encodeToString(buffer);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException
                | InvalidParameterSpecException | BadPaddingException | IllegalBlockSizeException e) {
            log.error("Error while encrypting: ", e);
        }
        return null;
    }

    /**
     * This method returns the custom encodes of the given plainText. This
     * method replaces all the occurrences of the following characters with
     * their corresponding replacement text in the given input text. '/'
     * replacement text: 'SLAsla', '+' replacement text: 'PLpl' and '='
     * replacement text: 'EQeq'
     * 
     * @param plainText
     *            the plain text to be encrypted
     * @return encodedEncryptedValue the encoded and encrypted plain text
     */
    public static String encryptAndCustomEncode(String plainText) {
        String encryptedValue = encrypt(plainText);

        String encodedEncryptedValue = RegExUtils.replaceAll(encryptedValue, "/", "SLAsla");
        encodedEncryptedValue = RegExUtils.replaceAll(encodedEncryptedValue, "\\+", "PLpl");
        encodedEncryptedValue = RegExUtils.replaceAll(encodedEncryptedValue, "=", "EQeq");

        return encodedEncryptedValue;
    }

    /**
     * Iterates through all the items in the cart and return is customer
     * eligible for account creation or not.
     * 
     * @param sSession
     *            services session object
     * @param source
     *            source string
     * @return true, if the customer is not logged and at least one room cart
     *         has the JWB flag (i.e., promotedMlifePrice) as true
     */
    public static boolean isEligibleForAccountCreation(ServicesSession sSession, String source) {
        List<RoomCartItem> roomCartItems = CommonUtil.getRoomCartItems(sSession.getCartItems(), source);
        boolean eligibleForAccountCreation = false;
        if ((sSession.getCustomer() == null || sSession.getCustomer().getCustomerId() <= 0)
                && CollectionUtils.isNotEmpty(roomCartItems)) {
            for (RoomCartItem roomCartItem : roomCartItems) {
                if (roomCartItem.isPromotedMlifePrice()) {
                    eligibleForAccountCreation = true;
                    break;
                }
            }
        }
        return eligibleForAccountCreation;
    }

    /**
     * Get's check in time.
     * 
     * @param date
     *            Arrival Date
     * @param tz
     *            time zone
     * @return check in time.
     */
    public static Date getCheckinTime(final Date date, TimeZone tz) {

        Date dateCopy = (Date) date.clone();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(tz);
        calendar.setTime(dateCopy);
        calendar.set(Calendar.HOUR_OF_DAY, ServiceConstant.NUM_3PM);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    /**
     * Convert the given date into string format.
     *
     * @param format
     *            the format
     * @param dateInput
     *            the date input
     * @param tz
     *            the tz
     * @return String
     * @since
     */
    public static String convertDateToString(String format, Date dateInput, TimeZone tz) {
        if (null != dateInput) {
            final DateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
            dateFormat.setTimeZone(tz);
            return dateFormat.format(dateInput);
        }
        return null;
    }
    
    /**
     * compose Uri Params - Set environment, version, chaincode before calling
     * ACRS api
     * 
     * @param environment
     *            environment to set
     * @param version
     *            version to set
     * @param chainCode
     *            chainCode to set
     * @return composed uri params
     */
    public static Map<String, String> composeUriParams(String environment, String version, String chainCode) {
        Map<String, String> uriParams = new HashMap<>();
        if (StringUtils.isNotEmpty(environment)) {
            uriParams.put(ServiceConstant.ACRS_ENVIRONMENT, environment);
        }
        uriParams.put(ServiceConstant.ACRS_VERSION, version);
        uriParams.put(ServiceConstant.ACRS_CHAINCODE, chainCode);
        return uriParams;
    }
    /**
     * {routingCode} is mgm-ga for gaming flows and mgm for the rest 
     * @param environment
     * @param version
     * @param chainCode
     * @param isPoFlow
     * @return
     */
    public static Map<String, String> composeUriParams(String environment, String version, String chainCode, boolean isPoFlow) {
        Map<String, String> uriParams = new HashMap<>();
        if (StringUtils.isNotEmpty(environment)) {
            uriParams.put(ServiceConstant.ACRS_ENVIRONMENT, environment);
        }
        uriParams.put(ServiceConstant.ACRS_VERSION, version);
        uriParams.put(ServiceConstant.ACRS_CHAINCODE, chainCode);
        if(isPoFlow) {
            uriParams.put(ServiceConstant.ACRS_GAMING_ROUTING, ServiceConstant.ACRS_PO_ROUTING); 
        }else {
            uriParams.put(ServiceConstant.ACRS_GAMING_ROUTING, ServiceConstant.ACRS_NONPO_ROUTING);
        }
        
        
        return uriParams;
    }
    
    /**
     * Get the current date without time.
     * 
     * @param inputDateStr
     *            input date string
     * @param format
     *            format
     * @return the date
     */
    public static Date getDate(String inputDateStr, String format) {
        DateFormat formatter = new SimpleDateFormat(format, Locale.ENGLISH);
        if(StringUtils.isEmpty(inputDateStr)){
            return null;
        }
        try {
            return formatter.parse(inputDateStr);
        } catch (ParseException e) {
        	log.error("Failed to parse Date: "+inputDateStr, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get the current date without time with timezone.
     * 
     * @param inputDateStr
     *            input date string
     * @param format
     *            format
     * @return the date
     */
    public static Date getDateWithTimeZone(String inputDateStr, String format) throws ParseException {
        DateFormat formatter = new SimpleDateFormat(format, Locale.ENGLISH);
        formatter.setTimeZone(TimeZone.getTimeZone(ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE)));
        return formatter.parse(inputDateStr);
    }

    /**
     * Copy values from one bean to another by JSON conversion to avoid type
     * conversion issues. It uses default time zone for copying properties.
     * 
     * @param source
     *            Object to be copied from
     * @param target
     *            Class type of target object
     * @param <T>
     *            Type param
     * @return Target object with values populated
     */
    public static <T> T copyProperties(Object source, Class<T> target) {

        final ArrayList<Module> modules = new ArrayList<>();

        final SimpleModule module = new SimpleModule();
        // adding our custom serializer
        module.addSerializer(double.class, new DoubleRoundSerializer());
        module.addSerializer(Double.class, new DoubleRoundSerializer());

        modules.add(module);

        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().modules(modules).build()
                .setTimeZone(TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE))
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String sourceJson;
        try {
            sourceJson = mapper.writeValueAsString(source);
            T response = mapper.readValue(sourceJson, target);
            
            // Aurora uses dcc* attribute names and RBS uses fx* attribute names.
            // Due to this, there's no easy way to reverse map these attributes and
            // had to use the below ugly way to manually settings those attributes.
            if (response instanceof RoomReservation
                    && source instanceof com.mgm.services.booking.room.model.reservation.RoomReservation) {

                com.mgm.services.booking.room.model.reservation.RoomReservation orgResv = (com.mgm.services.booking.room.model.reservation.RoomReservation) source;
                RoomReservation auroraResv = (RoomReservation) response;

                if (null != orgResv.getPayments() && null != auroraResv.getPayments()) {
                    for (int i = 0; i < orgResv.getPayments()
                            .size(); i++) {
                        Payment payment = auroraResv.getPayments()[i];
                        com.mgm.services.booking.room.model.reservation.Payment orgPayment = orgResv.getPayments()
                                .get(i);
                        payment.setDccAcceptMessage(orgPayment.getDccAcceptMessage());
                        payment.setDccAmount(orgPayment.getDccAmount());
                        payment.setDccAuthApprovalCode(orgPayment.getDccAuthApprovalCode());
                        payment.setDccChecked(orgPayment.isDccChecked());
                        payment.setDccCurrencyCode(orgPayment.getDccCurrencyCode());
                        payment.setDccEligible(orgPayment.isDccEligible());
                        payment.setDccRate(orgPayment.getDccRate());
                        payment.setDccSettleAmount(orgPayment.getDccSettleAmount());
                        payment.setDccSettleReference(orgPayment.getDccSettleReference());
                        payment.setDccTransDate(orgPayment.getDccTransDate());
                        payment.setChargeCardExpiry(orgPayment.getChargeCardExpiry());
                    }
                }
            }
            try {
                if (response instanceof com.mgm.services.booking.room.model.reservation.RoomReservation
                        && source instanceof RoomReservation) {
                    RoomReservation auroraResv = (com.mgmresorts.aurora.common.RoomReservation) source;
                    com.mgm.services.booking.room.model.reservation.RoomReservation orgResv = (com.mgm.services.booking.room.model.reservation.RoomReservation) response;
                    if (CollectionUtils.isNotEmpty(orgResv.getPayments()) && null != auroraResv.getPayments()) {
                        for (int i = 0; i < orgResv.getPayments()
                                .size(); i++) {
                            Payment payment = auroraResv.getPayments()[i];
                            com.mgm.services.booking.room.model.reservation.Payment orgPayment = orgResv.getPayments()
                                    .get(i);
                            if (null != payment.getChargeCardExpiry()) {
                                orgPayment.setChargeCardExpiry(DateUtil.toDate(ReservationUtil.convertDateToLocalDateAtSystemDefault(payment.getChargeCardExpiry())));
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.error("Error occurred while making credit card expiry changes");
            }
            return response;
        } catch (IOException e) {
            log.error(ERROR_STR, e);
        }
        return null;
    }
    
    /**
     * Copy aurora reservation object to local room reservation object. Note this method
     * was specifically created to not use price rounding when copying properties, so that
     * calling method can manipulate prices as required before rounding is applied.
     * 
     * @param source
     *            Aurora room reservation Object
     * @return Local room reservation object with values populated
     */
    public static com.mgm.services.booking.room.model.reservation.RoomReservation copyPendingReservation(
            RoomReservation source) {

        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .build()
                .setTimeZone(TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE))
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String sourceJson;
        try {
            sourceJson = mapper.writeValueAsString(source);
            com.mgm.services.booking.room.model.reservation.RoomReservation response = mapper.readValue(sourceJson,
                    com.mgm.services.booking.room.model.reservation.RoomReservation.class);

            return response;
        } catch (IOException e) {
            log.error(ERROR_STR, e);
        }
        return null;
    }

    /**
     * Returns true if both the dates are in future and end date is same or
     * after start date.
     * 
     * @param startDate
     *            Start Date
     * @param endDate
     *            End Date
     * @return Returns true if both the dates are in future and end date is same
     *         or after start date.
     */
    public static boolean isCalendarDatesValid(LocalDate startDate, LocalDate endDate) {
        return DateUtil.isFuture(startDate) && DateUtil.isFuture(endDate)
                && (endDate.isAfter(startDate) || endDate.isEqual(startDate));
    }
    
    /**
     * UUID validation
     * 
     * @param uuid
     *            uuid to verify
     * @return boolean determines Uuid or not.
     */
    public static boolean isUuid(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException exception) {
        	return false;
        }
    }
    
    
    /**
     * Convert Object To Json String
     * 
     * @param source Object to be converted.
     * @return String
     */
    public static String convertObjectToJsonString(Object source) {
        if ( null != source ) {
            final ArrayList<Module> modules = new ArrayList<>();
            modules.add(new JavaTimeModule());

            ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().modules(modules).build()
                    .setTimeZone(TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE))
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .setDateFormat(new SimpleDateFormat(ServiceConstant.ISO_8601_DATE_FORMAT));

            try {
                return mapper.writeValueAsString(source);
            }
            catch (Exception e) {
                log.error(ERROR_STR, e);
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Returns an adjusted copy of this dateString.
     * 
     * @param dateString
     *            date as string
     * @param dateFormat
     *            date format as string
     * @param dayOfWeek
     *            DayOfWeek to which the dateString needs to be adjusted to
     * @return adjust date as string
     */
    public static String adjustDateInto(String dateString, String dateFormat, DayOfWeek dayOfWeek) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
        LocalDate adjustedDate = LocalDate.parse(dateString, dateFormatter).with(TemporalAdjusters.nextOrSame(dayOfWeek));
        return adjustedDate.format(dateFormatter);
    }

    /**
     * 
     * @param dateString
     *            date as string
     * @param dateFormat
     *            date format as string
     * @return DayOfWeek of the given date
     */
    public static DayOfWeek getDayOfWeek(String dateString, String dateFormat) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
        return LocalDate.parse(dateString, dateFormatter).getDayOfWeek();
    }

	/**
	 * 	Is channel in the list of pre-defined channels for exclude email
	 * functionality from properties
	 * 
	 * @param channel Channel from API input
	 * @param excludedChannels list of channel for email exclusion
	 * @return Returns true if channel is excluded for email
	 */
	public static boolean isChannelExcludedForEmail(String channel, List<String> excludedChannels) {
		if (CollectionUtils.isEmpty(excludedChannels)) {
			return false;
		} else {
			return StringUtils.isNotBlank(channel)
					&& excludedChannels.contains(channel.toLowerCase(Locale.ENGLISH));
		}
	}
    public static String getErrorCode(String message, String regEx, int position) {
        Pattern errorCodePattern = Pattern.compile(regEx);
        Matcher matcher = errorCodePattern.matcher(message);
        String errorCode = null; 
        if (matcher.find()) {
            errorCode = matcher.group(position);
        }
        return errorCode;
    }
    
    public static String round(double number) {
        double value = BaseCommonUtil.round(number, 2);
        DecimalFormat df = new DecimalFormat("#0.00");
        return df.format(value);
    }
    
    public static String roundDown(double number) {
        DecimalFormat df = new DecimalFormat("#0.00");
        return df.format(number);
    }
    
    public static boolean isDateRangInADateRange(Date newStart, Date newEnd, Date existingStart, Date existingEnd) {
        return !(newStart.before(existingStart) || newEnd.after(existingEnd));
    }

    /**
     * true if start<=date<end
     * @param date
     * @param start
     * @param end
     * @return
     */
    public static boolean isDateInADateRange(LocalDate date,  LocalDate start, LocalDate end) {
        return !date.isBefore(start) && date.isBefore(end);
    }
    public static boolean isProductBelongsToRoutingDateRange(LocalDate routingStart, LocalDate routingEnd, LocalDate productStart, LocalDate productEnd) {
        return !(productStart.isBefore(routingStart) || productEnd.isAfter(routingEnd));
    }

    public static boolean isProductIntersectsRouting(LocalDate routingStart, LocalDate routingEnd, LocalDate productStart, LocalDate productEnd) {
        return (routingStart.isBefore(productEnd) && !routingEnd.isBefore(productStart));
    }

    public static boolean isRoutingBelongsToProductDateRange(LocalDate routingStart, LocalDate routingEnd, LocalDate productStart, LocalDate productEnd, LocalDate checkOutDate) {
        return ((productStart.isBefore(routingStart) || productStart.isEqual(routingStart)) && (productEnd.isAfter(routingEnd) || productEnd.isEqual(checkOutDate)));
    }
    
    /**
     * URL encodes the string provided. Returns un-encoded string back if unable
     * to encode.
     * 
     * @param text
     *            String
     * @return Return URL encoded string
     */
    public static String urlEncode(String text) {

        if (StringUtils.isEmpty(text)) {
            return StringUtils.EMPTY;
        }
        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.warn("Unable to encode the string provided");
            return text;
        }
    }

    /**
     * Return the channel from the request header <code>x-mgm-channel</code>.
     * 
     * @return channel header value as String
     */
    public static String getChannelHeader() {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        return httpRequest.getHeader(ServiceConstant.X_MGM_CHANNEL);
    }
    
    /**
     * Return the source from the request header <code>x-mgm-source</code>.
     * 
     * @return source header value as String
     */
    public static String getSourceHeader() {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        return httpRequest.getHeader(ServiceConstant.X_MGM_SOURCE);
    }

    public static String getChannelHeaderWithFallback(HttpServletRequest servletRequest) {
        String channel = servletRequest.getHeader(ServiceConstant.HEADER_CHANNEL);
        return StringUtils.isNotBlank(channel) ? channel : servletRequest.getHeader(ServiceConstant.X_MGM_CHANNEL);
    }

    public static String getSourceHeaderWithFallback(HttpServletRequest servletRequest) {
        String channel = servletRequest.getHeader(ServiceConstant.HEADER_SOURCE);
        return StringUtils.isNotBlank(channel) ? channel : servletRequest.getHeader(ServiceConstant.X_MGM_SOURCE);
    }

    public static String getUserIdHeader(HttpServletRequest servletRequest) {
        String xUserId = servletRequest.getHeader(ServiceConstant.X_USER_ID);
        return StringUtils.isNotBlank(xUserId) ? xUserId : null;
    }
    
    /**
     * Remove non alpha characters from the string which includes spaces,
     * special characters, numbers etc.
     * 
     * @param text
     * @return Returns text with non-alpha chars stripped.
     */
    public static String removeNonAlphaChars(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        return text.replaceAll("[^a-zA-Z]", StringUtils.EMPTY);
    }

    public static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JodaModule jodaModule = new JodaModule();
        jodaModule.addDeserializer(org.joda.time.LocalDate.class, new LocalDateDeserializer(FormatConfig.DEFAULT_DATEONLY_FORMAT));
        jodaModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(FormatConfig.DEFAULT_DATETIME_PARSER));
        mapper.registerModule(jodaModule);
        return mapper;
    }

    public static RoomPrice copyRoomBooking(
            RoomPrice source) {
        RoomPrice booking = new RoomPrice();
        booking.setDate(source.getDate());
        booking.setProgramId(source.getProgramId());
        booking.setOverrideProgramId(source.getOverrideProgramId());
        booking.setOverridePrice(source.getOverridePrice());
        booking.setPrice(source.getPrice());
        booking.setComp(source.isComp());
        booking.setBasePrice(source.getBasePrice());
        return booking;
    }

    public static Date localDateToDate(LocalDate date, String timezone) {
        return Date.from(date.atStartOfDay(ZoneId.of(timezone)).toInstant());
    }
    
    
    /**
     * Method to set the perpetual flag based on perpetualEligiblePropertyIds which will be set from com.mgm.loyalty.perpetual_eligible_properties JWT Claim.
     * If Property Id is present in request, perpetual eligible flag will be set to true if the requested property id is present in the JWT claim.
     * If there is no property id, the perpetual flag will be set to true if the JWT claim has at least 1 perpetual eligible property.
     * @param perpetualEligiblePropertyIds
     * @param propertyId
     * @return
     */
    public static boolean isPerpetualPricingEligible(List<String> perpetualEligiblePropertyIds, String propertyId) {
    	if(CollectionUtils.isNotEmpty(perpetualEligiblePropertyIds)) {
    		if(StringUtils.isEmpty(propertyId) || (StringUtils.isNotEmpty(propertyId) && perpetualEligiblePropertyIds.contains(propertyId))) {
    			return true;
    		}
    		
    	}
    	return false;
    }

    public static String generateKey(String type,String idOrCode){
        if(StringUtils.isNotBlank(type)){
            return type.toUpperCase().concat(ServiceConstant.REDIS_KEY_DELIMITER).concat(idOrCode);
        }else{
            return idOrCode;
        }
    }

	public static HttpHeaders createPartnerAuthRequestHeader(String basicAuthUsername, String basicAuthPassword) {
		HttpHeaders headers = new HttpHeaders();
        headers.set(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_URLENCODED);
        headers.set("Authorization", getBasicAuthenticationHeader(basicAuthUsername,basicAuthPassword));
        return headers;
	}
	
	private static final String getBasicAuthenticationHeader(String username, String password) {
	    String valueToEncode = username + ":" + password;
	    return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
	}

	public static boolean isTempLogEnabled(String isTempLogEnabled){
        if (StringUtils.isNotBlank(isTempLogEnabled)) {
			return Boolean.valueOf(isTempLogEnabled);
		} else {
            return false;
        }
	}

    public static HttpHeaders setAdditionalPaymentHeaders(HttpHeaders headers){
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN))) {
            headers.set(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN, ThreadContext.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN));
        }
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.HEADER_USER_AGENT))) {
            headers.set(ServiceConstant.HEADER_USER_AGENT, ThreadContext.get(ServiceConstant.HEADER_USER_AGENT));
        }
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.IN_AUTH_TRANSACTION_ID))) {
            headers.set(ServiceConstant.IN_AUTH_TRANSACTION_ID, ThreadContext.get(ServiceConstant.IN_AUTH_TRANSACTION_ID));
        }
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.EXTERNAL_SESSION_ID))) {
            headers.set(ServiceConstant.EXTERNAL_SESSION_ID, ThreadContext.get(ServiceConstant.EXTERNAL_SESSION_ID));
        }
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.IP_ADDRESS))) {
            headers.set(ServiceConstant.IP_ADDRESS, ThreadContext.get(ServiceConstant.IP_ADDRESS));
        }
        if(org.apache.commons.lang3.StringUtils.isNotBlank(ThreadContext.get(RequestHeadersEnum.X_FRWD_FOR.getHeader()))) {
            headers.set(RequestHeadersEnum.X_FRWD_FOR.getHeader(), ThreadContext.get(RequestHeadersEnum.X_FRWD_FOR.getHeader()));
        }
        return headers;
    }

    public static HttpHeaders setPaymentSourceAndChannelHeaders(HttpHeaders headers) {
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.X_MGM_CHANNEL))) {
            headers.set(ServiceConstant.X_MGM_CHANNEL, ThreadContext.get(ServiceConstant.X_MGM_CHANNEL));
        }
        headers.set(ServiceConstant.X_MGM_SOURCE, ServiceConstant.RBS_CHANNEL_HEADER);
        return headers;
    }

    public static Properties getForterHeaders(){
        final Properties headers = new Properties();
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN))) {
            headers.put(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN, ThreadContext.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN));
        }
        if(StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.HEADER_USER_AGENT))) {
            headers.put(ServiceConstant.HEADER_USER_AGENT, ThreadContext.get(ServiceConstant.HEADER_USER_AGENT));
        }
        if(org.apache.commons.lang3.StringUtils.isNotBlank(ThreadContext.get(RequestHeadersEnum.X_FRWD_FOR.getHeader()))) {
            headers.put(RequestHeadersEnum.X_FRWD_FOR.getHeader(), ThreadContext.get(RequestHeadersEnum.X_FRWD_FOR.getHeader()));
        }
        return headers;
    }

    public static void logRequestHeaderAndBody(boolean logInfo, Logger log, String headerStr, String bodyStr, String paramStr, String headerObj, String bodyObj, Map<String, String> paramValues) {
        try {
            if (logInfo) {
                log.info(headerStr, headerObj);
                log.info(bodyStr, bodyObj);
                log.info(paramStr, paramValues);
            } else if (log.isDebugEnabled()) {
                log.debug(headerStr, headerObj);
                log.debug(bodyStr, bodyObj);
                log.debug(paramStr, paramValues);
            }
        } catch (Exception ex) {
            log.error("Error while logging :{}",ex.getMessage());
        }
    }

    public static Set<String> promoCodes(String promoCode){
        Set<String> promoCodes = new TreeSet<>();
        promoCodes.add(promoCode);

        // remove last chars for delano/nomad
        if (promoCode.startsWith("TMLIFE")) {
            promoCodes.add(addChar(promoCode, 'D', promoCode.length()));
            promoCodes.add(addChar(promoCode, 'N', promoCode.length()));
            promoCodes.add(removeChar(promoCode, 'D', promoCode.length()));
            promoCodes.add(removeChar(promoCode, 'N', promoCode.length()));
            promoCodes.add(replaceChar(promoCode, 'D', 'N', promoCode.length()));
            promoCodes.add(replaceChar(promoCode, 'N', 'D', promoCode.length()));
        } else {
            // Add or remove delano/nomad char codes
            promoCodes.add(addChar(promoCode, 'D', promoCode.length() - 1));
            promoCodes.add(addChar(promoCode, 'N', promoCode.length() - 1));
            promoCodes.add(removeChar(promoCode, 'D', promoCode.length() - 1));
            promoCodes.add(removeChar(promoCode, 'N', promoCode.length() - 1));
            promoCodes.add(replaceChar(promoCode, 'D', 'N', promoCode.length() - 1));
            promoCodes.add(replaceChar(promoCode, 'N', 'D', promoCode.length() - 1));
        }
        return promoCodes;
    }

    private static String addChar(String str, char ch, int position) {
        return str.substring(0, position) + ch + str.substring(position);
    }

    public static String removeChar(String str, char ch, int position) {

        if (str.charAt(position - 1) == ch) {
            return str.substring(0, position - 1) + str.substring(position);
        } else {
            return str;
        }

    }

    private static String replaceChar(String str, char ch, char ch1, int position) {

        if (str.charAt(position - 1) == ch) {
            return str.substring(0, position - 1) + ch1 + str.substring(position);
        } else {
            return str;
        }

    }

    public static String normalizePromoCode(String promoCode, String propertyId) {

        int characterToReplace;

        if (StringUtils.isEmpty(promoCode) || StringUtils.isEmpty(propertyId)) {
            return promoCode;
        }

        // If delano, remove D as second last char
        // If Nomad, remove N as second last char
        characterToReplace = promoCode.length() - 1;
        // TMLIFE requires special handling to remove last character
        if (promoCode.startsWith("TMLIFE")) {
            characterToReplace = promoCode.length();
        }

        // If delano, remove D as second last char
        // If Nomad, remove N as second last char
        if (propertyId.equals("8bf670c2-3e89-412b-9372-6c87a215e442")) {
            promoCode = CommonUtil.removeChar(promoCode, 'D', characterToReplace);
        } else if (propertyId.equals("2159252c-60d3-47db-bbae-b1db6bb15072")) {
            promoCode = CommonUtil.removeChar(promoCode, 'N', characterToReplace);
        }
        return promoCode;
    }

    public static boolean isProgramValid(RoomProgramBasic program) {
        Date currentDate = new Date();
        if(null != program.getTravelPeriodEnd()) {
            if (program.isActive() && program.isBookableOnline() 
                    && !program.getTravelPeriodEnd().before(currentDate)) {
                return true;
            }
        }
        return false;
    }

    public static HttpHeaders createBasicHeaders(String authToken, String correlationId){
        HttpHeaders headers = new HttpHeaders();
        headers.add(ServiceConstant.HEADER_AUTHORIZATION, ServiceConstant.HEADER_AUTH_BEARER + authToken);
        headers.add(ServiceConstant.X_MGM_CORRELATION_ID, correlationId);
        headers.set(ServiceConstant.HEADER_KEY_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_APPLICATION_JSON);
        return headers;
    }
}

