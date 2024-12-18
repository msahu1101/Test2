package com.mgm.services.booking.room.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.wrapper.HeaderMapRequestWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestWrappedFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String channel = request.getHeader(ServiceConstant.HEADER_CHANNEL);
        String source = request.getHeader(ServiceConstant.HEADER_SOURCE);
        
        HeaderMapRequestWrapper wrappedRequest = new HeaderMapRequestWrapper((HttpServletRequest) request);
        
        if(StringUtils.isBlank(channel)) {
            wrappedRequest.addHeader("channel", request.getHeader(ServiceConstant.X_MGM_CHANNEL));
        }
        if(StringUtils.isBlank(source)) {
            wrappedRequest.addHeader("source", request.getHeader(ServiceConstant.X_MGM_SOURCE));
        }

        filterChain.doFilter(wrappedRequest, response);
    }
}