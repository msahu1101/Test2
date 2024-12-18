package com.mgm.services.booking.room.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.wrapper.XSSRequestWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XSSFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        XSSRequestWrapper wrappedRequest = new XSSRequestWrapper((HttpServletRequest) request);
        String body = IOUtils.toString(wrappedRequest.getReader());
        if (StringUtils.isNotBlank(body)) {
            body = XSSRequestWrapper.stripXSS(body);
            wrappedRequest.resetInputStream(body.getBytes());
        }
        chain.doFilter(wrappedRequest, response);
    }

}
