package com.mgm.services.booking.room.util;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Component
@PropertySource(value = "classpath:${property.config.location}/PropertyConfig.json", factory = JsonPropertySourceFactory.class)
@ConfigurationProperties
public @Data class PropertyConfig  {

	private List<PropertyValue> propertyValues;

    private Map<String, PropertyValue> propertyValuesMap;

    @Data
    public static class PropertyValue {
        
        @JsonProperty("merchantId")
        private String gseMerchantID;

        @JsonProperty("patronSiteId")
        private Integer patronSiteId;

        @JsonProperty("operaCode")
        private String operaCode;

        @JsonProperty("gsePropertyIds")
        private List<String> gsePropertyIds;
        
        @JsonProperty("acrsPropertyIds")
        private List<String> acrsPropertyIds;

        @JsonProperty("masterPropertyId")
        private String masterPropertyId;

        @JsonProperty("masterPropertyCode")
        private String masterPropertyCode;
    }
}
