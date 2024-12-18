package com.mgm.services.booking.room.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.common.azure.livesite.clients.EventHubUtil;
import com.mgm.services.common.azure.livesite.clients.impl.EventHubUtilImpl;
import com.mgm.services.common.azure.livesite.models.LiveSiteEvent;

@Component
public class LiveSiteConfig {

	@Autowired
	SecretsProperties secrets;
	
	private EventHubUtil<LiveSiteEvent> eventHubClient;
	
	@PostConstruct
	public void setup() {
		eventHubClient = new EventHubUtilImpl<>(secrets.getSecretValue("livesite-eh-conn-string"));
	}

	public EventHubUtil<LiveSiteEvent> getEventHubClient() {
		return eventHubClient;
	}
	
	
	
}
