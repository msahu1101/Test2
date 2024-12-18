package com.mgm.services.booking.room.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAwsCloudEnvironment;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;
import com.mgm.services.booking.room.util.LiveSiteHelper;

/**
 * This filter is tasked to pass live event object to the eventhub. The filter
 * will only be initialized when the environment it runs on is NOT on AWS.
 * 
 * @author swakulka
 *
 */
@Component
@ConditionalOnMissingAwsCloudEnvironment
public class LiveSiteLoggingFilter implements Filter {


	@Autowired
	LiveSiteHelper livesiteHelper;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		Stopwatch watch = Stopwatch.createStarted();
		chain.doFilter(request, response);

		watch.stop();
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		if(res.getStatus() < 400) {
			livesiteHelper.publishToLiveSite(req, res, watch);
		}
	}

}
