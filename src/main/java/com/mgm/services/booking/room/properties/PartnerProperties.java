package com.mgm.services.booking.room.properties;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(
		prefix = "partner")
public @Data class PartnerProperties {

	private Map<String, String> membershipLevel;
	private String basicAuthPasswordKey;
	private boolean enabled;
	private int tokenRefreshRateInMS;
	private int connectionTimeOut;
	private int readTimeOut;
	private int socketTimeOut;
	private int clientMaxConn;
	private int clientMaxConnPerRoute;
	private int retryCount;
	private long ttl;


	@PostConstruct
	private void postConstruct() {
		final String partnerConnectionTimeOut = System.getenv("partnerConnectionTimeOut");
		final String partnerReadTimeOut = System.getenv("partnerReadTimeOut");
		final String partnerRetryCount = System.getenv("partnerRetryCount");
		if(StringUtils.isNotBlank(partnerConnectionTimeOut)){
			connectionTimeOut = Integer.parseInt(partnerConnectionTimeOut);
		}
		if(StringUtils.isNotBlank(partnerReadTimeOut)){
			readTimeOut = Integer.parseInt(partnerReadTimeOut);
		}
		if(StringUtils.isNotBlank(partnerRetryCount)){
			retryCount = Integer.parseInt(partnerRetryCount);
		}


	}

}
