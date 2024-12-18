package com.mgm.services.booking.room.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class reads in the json file and initializes a list of partnerProgram objects during application startup.
 */
public class JsonPartnerSourceFactory  implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String s, EncodedResource resource)  throws IOException {
        final PartnerProgramConfig readValues = new ObjectMapper().readValue(resource.getInputStream(), PartnerProgramConfig.class);
        final Map<String, Object> partnerMap = new HashMap<>();
        final List<PartnerProgramConfig.PartnerProgramValue> partnerProgramValueList = readValues.getPartnerProgramValues();
        partnerMap.put("partnerProgramValues", partnerProgramValueList);
        return new MapPropertySource("json-partnerProgram", partnerMap);

    }
}
