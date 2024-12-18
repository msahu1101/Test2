package com.mgm.services.booking.room.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mgm.services.booking.room.constant.ServiceConstant;

/**
 * Filter to be executed only once. Intercepts the request and sets the request
 * headers back in the response headers
 * 
 * @author swakulka
 *
 */
@Component
public class ResponseHeadersFilter extends OncePerRequestFilter {

    /**
     * Overriden method to set the request headers back in the response headers.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        response.setHeader(ServiceConstant.X_MGM_CORRELATION_ID,
                request.getHeader(ServiceConstant.X_MGM_CORRELATION_ID));
        response.setHeader(ServiceConstant.X_MGM_TRANSACTION_ID,
                request.getHeader(ServiceConstant.X_MGM_TRANSACTION_ID));
        response.setHeader(ServiceConstant.X_MGM_SOURCE, request.getHeader(ServiceConstant.X_MGM_SOURCE));
        response.setHeader(ServiceConstant.X_MGM_CHANNEL, request.getHeader(ServiceConstant.X_MGM_CHANNEL));

        filterChain.doFilter(request, response);

    }

}