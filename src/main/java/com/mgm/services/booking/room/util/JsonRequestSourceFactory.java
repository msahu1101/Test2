package com.mgm.services.booking.room.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgm.services.booking.room.util.RequestSourceConfig.SourceDetails;

public class JsonRequestSourceFactory implements PropertySourceFactory{

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        final RequestSourceConfig readValues = new ObjectMapper().readValue(resource.getInputStream(),
                RequestSourceConfig.class);
        final List<SourceDetails> sourceValues = readValues.getRequestSources();
        final Map<String, SourceDetails> requestSourcesMap = new HashMap<String, SourceDetails>();
        final Map<String, Object> sourceMap = new HashMap<String, Object>();
        requestSourcesMap.putAll(sourceValues.stream().collect(Collectors.toMap(SourceDetails::getRbsSource, s -> s)));
        sourceMap.put("requestSourcesMap", requestSourcesMap);
        return new MapPropertySource("json-requestSource", sourceMap);
    }

}
