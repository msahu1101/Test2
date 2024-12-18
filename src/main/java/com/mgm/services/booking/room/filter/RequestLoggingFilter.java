package com.mgm.services.booking.room.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import com.mgm.services.booking.room.util.LogMask;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class RequestLoggingFilter extends OncePerRequestFilter {

    /**
     * The default value prepended to the log message written <i>before</i> a
     * request is processed.
     */
    public static final String BEFORE_MESSAGE_PREFIX = "Before request [";

    /**
     * The default value appended to the log message written <i>before</i> a
     * request is processed.
     */
    public static final String MESSAGE_SUFFIX = "]";

    /**
     * The default value prepended to the log message written <i>after</i> a
     * request is processed.
     */
    public static final String AFTER_MESSAGE_PREFIX = "After request [";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean isFirstRequest = !isAsyncDispatch(request);
        HttpServletRequest requestToUse = request;

        if (isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
            requestToUse = new ContentCachingRequestWrapper(request, 100000);
        }

        if (isFirstRequest) {
            log.info(createMessage(requestToUse, BEFORE_MESSAGE_PREFIX, MESSAGE_SUFFIX));
        }
        try {
            filterChain.doFilter(requestToUse, response);
        } finally {
            if (!isAsyncStarted(requestToUse)) {
                log.info(createMessage(requestToUse, AFTER_MESSAGE_PREFIX, MESSAGE_SUFFIX));
            }
        }

    }

    /**
     * Create a log message for the given request, prefix and suffix.
     * <p>
     * The inner part of the log message will take the form
     * {@code request_uri?query_string}; otherwise the message will simply be of
     * the form {@code request_uri}.
     * <p>
     * The final message is composed of the inner part as described and the
     * supplied prefix and suffix.
     */
    protected String createMessage(HttpServletRequest request, String prefix, String suffix) {
        StringBuilder msg = new StringBuilder();
        msg.append(prefix);
        msg.append(request.getMethod()).append(" ");
        msg.append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(LogMask.maskQueryString(queryString));
        }

        String payload = getMessagePayload(request);
        if (payload != null) {
            msg.append(", payload=").append(payload);
        }

        msg.append(suffix);
        return msg.toString();
    }

    /**
     * Extracts the message payload portion of the message created by
     * {@link #createMessage(HttpServletRequest, String, String)} when
     * {@link #isIncludePayload()} returns true.
     * 
     * @since 5.0.3
     */
    @Nullable
    protected String getMessagePayload(HttpServletRequest request) {
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper != null) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                int length = Math.min(buf.length, 100000);
                try {
                    return LogMask.mask(new String(buf, 0, length, wrapper.getCharacterEncoding()));
                } catch (UnsupportedEncodingException ex) {
                    return "[unknown]";
                }
            }
        }
        return null;
    }

}