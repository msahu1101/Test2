package com.mgm.services.booking.room.filter;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.WebUtils;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;
import com.mgm.services.common.filter.AbstractRequestFilter;

import lombok.extern.log4j.Log4j2;

/**
 * Request filter to intercept all requests and set session id and request id in
 * log4j thread context.
 * 
 */
@Component
@Log4j2
public class RequestFilter extends AbstractRequestFilter {

    @Autowired
    private ApplicationProperties appProperties;

    private static final String SESSIONID = "SESSIONID";
    private static final String REQUESTID = "REQUESTID";
    private static final String BLANK_STR = "";
    private static final int LIMIT = 8;

    protected void validate(HttpServletRequest request) {


        String uri = request.getRequestURI();
        String method = request.getMethod();
        String channel = CommonUtil.getChannelHeaderWithFallback(request);
        String source = CommonUtil.getSourceHeaderWithFallback(request);
        String xUserId = CommonUtil.getUserIdHeader(request);
        
        // OPTIONS is used for cors which doesn't need these checks
        // Actuator endpoints should be allowed directly
        if (!"OPTIONS".equalsIgnoreCase(method) && !uri.contains("/actuator") && !uri.contains("/health-check")) {

            // channel should be mandatory for all APIs
            if (!isChannelValid(channel, uri)) {
                throw new ValidationException(
                        Collections.singletonList(ErrorCode.INVALID_CHANNEL_HEADER.getErrorCode()));
            }

            boolean isTokenEmpty = isTokenEmpty(request);

            boolean isWhitelistedUrl = (isUrlWhitelisted(uri));

            boolean isWhitelistedChannel = (isChannelWhitelisted(channel));

            // x-state-token should be mandatory for all APIs except white lists
            if (isTokenEmpty && !isWhitelistedUrl && !isWhitelistedChannel) {
                log.info("API call made without the token. Expires Value: {}", getTokenExpires(request));
                throwValidationError(ErrorCode.NO_TOKEN_IN_HEADER.getErrorCode());
            }

            // source should be mandatory for all APIs
            if (appProperties.isRoom() && StringUtils.isBlank(source) && !isWhitelistedUrl) {
                throwValidationError(ErrorCode.RUNTIME_MISSING_SOURCE_HEADER.getErrorCode());
            }
            
            // transaction id is mandatory for v2 APIs in azure
            if (!uri.contains("/v1") && !isWhitelistedUrl
                    && StringUtils.isBlank(request.getHeader(ServiceConstant.HEADER_TRANSACTION_ID))) {
                throwValidationError(ErrorCode.INVALID_TRANSACTIONID_HEADER.getErrorCode());
            }
            
            HttpSession session = request.getSession(false);

            // x-state-token should be valid for all APIs except Token API and
            // V2 services
            if (!isWhitelistedUrl && !isWhitelistedChannel && null == session) {
                log.info("API call made with invalid/expired token. Expires Value: {}", getTokenExpires(request));
                throwValidationError(ErrorCode.INVALID_TOKEN.getErrorCode());
            }

            if (null != session) {
                ThreadContext.put(SESSIONID, session.getId().substring(0, LIMIT));
                log.info(session.getId());
            }
            ThreadContext.put(REQUESTID, UUID.randomUUID().toString().substring(0, LIMIT));
            // Remove if condition for resetting value in context. else it will be there for next request.
            ThreadContext.put(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN, request.getHeader(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN));
            ThreadContext.put(ServiceConstant.HEADER_USER_AGENT, request.getHeader(ServiceConstant.HEADER_USER_AGENT));
            ThreadContext.put(ServiceConstant.IN_AUTH_TRANSACTION_ID, request.getHeader(ServiceConstant.IN_AUTH_TRANSACTION_ID));
            ThreadContext.put(ServiceConstant.EXTERNAL_SESSION_ID, request.getHeader(ServiceConstant.EXTERNAL_SESSION_ID));
            ThreadContext.put(ServiceConstant.IP_ADDRESS, request.getHeader(ServiceConstant.IP_ADDRESS));
            ThreadContext.put(RequestHeadersEnum.X_FRWD_FOR.getHeader(), request.getHeader(RequestHeadersEnum.X_FRWD_FOR.getHeader()));
            ThreadContext.put(ServiceConstant.X_USER_ID, null);
            if(null != xUserId) {
                ThreadContext.put(ServiceConstant.X_USER_ID, xUserId);
            }
            ThreadContext.put(ServiceConstant.X_MGM_CHANNEL, request.getHeader(ServiceConstant.X_MGM_CHANNEL));
        }
    }

    private boolean isTokenEmpty(HttpServletRequest request) {

        Cookie cookie = WebUtils.getCookie(request, ServiceConstant.X_STATE_TOKEN);

        return StringUtils.isBlank(request.getHeader(ServiceConstant.X_STATE_TOKEN))
                && (null == cookie || StringUtils.isBlank(cookie.getValue()));

    }

    private String getTokenExpires(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, ServiceConstant.X_STATE_TOKEN_EXPIRES);

        return null == cookie ? request.getHeader(ServiceConstant.X_STATE_TOKEN_EXPIRES) : cookie.getValue();

    }

    private void throwValidationError(String error) {
        ValidationException ex = new ValidationException(Collections.singletonList(error));
        log.error("Request resulted in an error: ", ex);
        throw ex;
    }

    /**
     * Is channel in the list of pre-defined channels from properties
     * 
     * @param channel
     *            Channel from API input
     * @param uri
     *            Request path
     * @return Returns true if channel is valid
     */
    private boolean isChannelValid(String channel, String uri) {

        if (CollectionUtils.isEmpty(appProperties.getValidChannels()) || uri.contains("/v1/token")) {
            return true;
        } else {
            return StringUtils.isNotBlank(channel)
                    && appProperties.getValidChannels().contains(channel.toLowerCase(Locale.ENGLISH));
        }
    }

    /**
     * Add known headers into thread context for logging purposes
     * 
     * @param exchange
     *            Web Exchange
     */
    protected void addRequestHeaders(HttpServletRequest request) {

        for (RequestHeadersEnum headerVals : RequestHeadersEnum.values()) {
            ThreadContext.put(headerVals.getLogVal(), request.getHeader(headerVals.getHeader()) == null ? BLANK_STR
                    : request.getHeader(headerVals.getHeader()));
        }
        
        // Generate correlation id if not passed
        if (null == request.getHeader(RequestHeadersEnum.X_MGM_CORRELATION_ID.getHeader())) {
            ThreadContext.put(RequestHeadersEnum.X_MGM_CORRELATION_ID.getLogVal(), UUID.randomUUID().toString());
        }

    }

    /**
     * Checks if the requested URI is part of whitelist
     * 
     * @param incomingUri
     *            URI of incoming request
     * @return
     */
    private boolean isUrlWhitelisted(String incomingUri) {

        Optional<String> result = appProperties.getWhitelisturl().stream().filter(incomingUri::contains).findAny();

        return result.isPresent();
    }

    /**
     * Checks if the channel sent the request is part of whitelist
     *
     * @param channel
     *            Channel from API input
     * @return
     */
    private boolean isChannelWhitelisted(String channel) {

        return StringUtils.isNotEmpty(channel)
                && appProperties.getWhitelistChannels().stream().anyMatch(channel::equalsIgnoreCase);

    }

    /**
     * Set security response headers
     * 
     * @param response
     *            http servlet response
     */
    protected void setAdditionalSecurityResponseHeaders(HttpServletResponse response) {
        // No additional headers to be added.
    }

    protected void setSessionExpiry(HttpServletRequest request, HttpServletResponse response) {

        String uri = request.getRequestURI();
        String channel = CommonUtil.getChannelHeaderWithFallback(request);

        if (isUrlWhitelisted(uri) || isChannelWhitelisted(channel)) {
            // don;t set session, if status is not Ok
            if (HttpStatus.valueOf(response.getStatus()).isError()) {
                request.getSession().invalidate();
            }
            return;
        }

        HttpSession session = request.getSession();

        // If session id is not empty, add expiry time
        if (StringUtils.isNotEmpty(session.getId()) && !session.isNew()) {
            String expiry = String.valueOf(
                    session.getLastAccessedTime() + (session.getMaxInactiveInterval() * ServiceConstant.NUM_1000));

            // Set expiry time as cookie for channels with cookie support
            if (StringUtils.isNotEmpty(channel) && appProperties.getCookieChannels().contains(channel)) {
                // AWS API Gateway doesn't support multiple cookies from
                // upstream systems.
                // So, using unique header which gets converted as cookie in GW
                // integration
                response.setHeader(ServiceConstant.EXPIRES_COOKIE, "x-state-token-expires=" + expiry
                        + "; Path=/; Secure; SameSite=None; Domain=" + appProperties.getSharedCookieDomain());

                log.info(response.getHeader(ServiceConstant.EXPIRES_COOKIE));
            } else {
                // set header
                response.setHeader(ServiceConstant.X_STATE_TOKEN_EXPIRES, expiry);
            }

        }

    }

}