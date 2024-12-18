package com.mgm.services.booking.room.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@PropertySource(value = "classpath:${partner.config.location}/PartnerConfig.json", factory = JsonPartnerSourceFactory.class)
@ConfigurationProperties
public @Data class PartnerProgramConfig {

    @JsonProperty("partnerProgramValues")
    private List<PartnerProgramValue> partnerProgramValues;

    @Data
    public static class PartnerProgramValue {

        @JsonProperty("programName")
        private String programName;

        @JsonProperty("programCode")
        private String programCode;
    }

}
