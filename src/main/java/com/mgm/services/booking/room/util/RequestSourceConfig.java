package com.mgm.services.booking.room.util;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Component
@PropertySource(value = "classpath:data/${source.config.name}.json", factory = JsonRequestSourceFactory.class)
@ConfigurationProperties
public @Data class RequestSourceConfig {
    
    private List<SourceDetails> requestSources;
    private Map<String, SourceDetails> requestSourcesMap;
    public @Data static class SourceDetails {
        @JsonProperty("rbsSource")
        private String rbsSource;
        @JsonProperty("rbsChannel")
        private String rbsChannel;
        @JsonProperty("acrsChannelClass")
        private String acrsChannelClass;
        @JsonProperty("acrsChannelType")
        private String  acrsChannelType;
        @JsonProperty("acrsChannel")
        private String acrsChannel;
        @JsonProperty("acrsSubChannel")
        private String  acrsSubChannel;
        @JsonProperty("acrsVendor")
        private String acrsVendor;
        @JsonProperty("channelName")
        private String channelName;
    }
}
