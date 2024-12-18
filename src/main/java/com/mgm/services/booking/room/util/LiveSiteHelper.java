package com.mgm.services.booking.room.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;
import com.mgm.services.booking.room.config.LiveSiteConfig;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.common.azure.livesite.models.LiveSiteEvent;

import lombok.extern.log4j.Log4j2;

/**
 * Helper component provides methods to build the live site event object.
 * 
 * @author swakulka
 *
 */
@Component
@Log4j2
public class LiveSiteHelper {

	private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	private static final String REQUESTID = "REQUESTID";
	private static final String COMPUTERNAME = "COMPUTERNAME";
	private static final String WEBSITE_HOSTNAME = "WEBSITE_HOSTNAME";

	private static final List<String> SENSITIVE_HEADERS_LIST = Arrays.asList("authorization","cookie");

	@Autowired
	LiveSiteConfig liveSite;

	@Value("${application.livesite.domain}")
	private String domain;

	@Value("${application.livesite.env}")
	private String env;

	@Value("${application.version}")
	private String appVersion;

	/**
	 * Method to build the live site event object.
	 * 
	 * @param request  - HttpServletRequest object
	 * @param response - httpServletResponse object
	 * @param watch    - Stopwatch to measure total request time
	 * @return - LiveSite event object
	 */
	public LiveSiteEvent buildLiveSiteEvent(HttpServletRequest request, HttpServletResponse response, 
			int status, String errorCode, String errorMsg, Stopwatch watch) {

		TimeZone tz = TimeZone.getTimeZone("UTC");
		df.setTimeZone(tz);
		String ts = df.format(new Date());

		final String logLevel = status >= 500 ? "ERROR" : "INFO";
		StringBuilder requestPath = new StringBuilder().append(request.getMethod()).append(ServiceConstant.WHITESPACE_STRING)
				.append(request.getRequestURI());

		Object financialImpact = request.getAttribute("financialImpact");
		return LiveSiteEvent.builder().
				domain(domain).level(logLevel).
				logTimestamp(ts)
				.status(status)
				.source(CommonUtil.getChannelHeaderWithFallback(request))
				.duration((int) watch.elapsed(TimeUnit.MILLISECONDS))
				.correlationId(request.getHeader(ServiceConstant.X_MGM_CORRELATION_ID))
				.logId(ThreadContext.get(REQUESTID))
				.env(env)
				.path(requestPath.toString())
				.headers(getHeaders(request))
				.property(CommonUtil.getSourceHeaderWithFallback(request))
				.host(System.getenv(WEBSITE_HOSTNAME))
				.containerId(System.getenv(COMPUTERNAME))
				.requestData(getRequestData(request))
				.customer(getCustomerData(request))
				.responseData(getResponseData(response, status, errorCode, errorMsg))	
				.financialImpact(financialImpact!=null? financialImpact.toString():"NA")
				.version(appVersion).build();
	}



	/**
	 * Method to build the request data passed in live site event object
	 * 
	 * @param request - HttpServletRequest
	 * @return - Map populated with request data
	 */
	public Map<String, String> getRequestData(HttpServletRequest request) {
		Map<String, String> requestData = new HashMap<>();
		requestData.put("uri", request.getRequestURI());
		requestData.put("httpMethod", request.getMethod());
		requestData.put("query", request.getQueryString());
		requestData.put("channel", CommonUtil.getChannelHeaderWithFallback(request));
		requestData.put("source", CommonUtil.getSourceHeaderWithFallback(request));

		/*
		 * try {
		 * request.getReader().lines().collect(Collectors.joining(System.lineSeparator()
		 * )); } catch (IOException e) { log.error("Error reading request object"); }
		 */

		return requestData;
	}

	public Map<String, String> getCustomerData(HttpServletRequest request) {
		Map<String, String> customerData = new HashMap<>();
		Optional<String> mlifeNum = Optional.ofNullable(request.getParameter(ServiceConstant.MLIFE_NUM));
		Optional<String> customerId = Optional.ofNullable(request.getParameter(ServiceConstant.CUSTOMER_ID));

		if (customerId.isPresent())
			customerData.put("customerId", customerId.get());

		if (mlifeNum.isPresent())
			customerData.put("mLifeNum", mlifeNum.get());

		return customerData;
	}

	/**
	 * Method to populate the response data passed in the live site event object
	 * 
	 * @param response - HttpServletResponse
	 * @return - Map populated with response data
	 */
	public Map<String, String> getResponseData(HttpServletResponse response, int status, String errorCode,
			String errorMsg) {
		Map<String, String> responseData = new HashMap<>();
		responseData.put("status", String.valueOf(status));
		if (null != errorCode && null != errorMsg) {
			responseData.put("errorCode", errorCode);
			responseData.put("errorMessage", errorMsg);
		}

		return responseData;
	}

	/**
	 * Method to populate request headers in a map.
	 * 
	 * @param req - HttpServletRequest
	 * @return - Map consisting of header and corresponding values
	 */
	public Map<String, Object> getHeaders(HttpServletRequest req) {

		Map<String, Object> headers = new HashMap<>();
		try {
			Enumeration<String> headerNames = req.getHeaderNames();

			while (headerNames.hasMoreElements()) {
				String key = headerNames.nextElement();
				if (!SENSITIVE_HEADERS_LIST.contains(key))
					headers.put(key, req.getHeader(key));
			}

		} catch (Exception e) {
			log.error("Exception while fetching headers from HttpServletRequest", e);
		}

		return headers;
	}

	public void publishToLiveSite(HttpServletRequest request, HttpServletResponse response, Stopwatch watch) {

		int status = response.getStatus();
		liveSite.getEventHubClient().publish(buildLiveSiteEvent(request, response, status, null, null, watch));
	}

	public void publishErrorEventToLivesite(HttpServletRequest request, HttpServletResponse response,
			int status, String errorCode, String errorMsg,Stopwatch watch) {

		liveSite.getEventHubClient().publish(buildLiveSiteEvent(request, response, status, errorCode, errorMsg, watch));

	}
}
