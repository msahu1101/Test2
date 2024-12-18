package com.mgm.services.booking.room.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgm.services.booking.room.util.PropertyConfig.PropertyValue;

public class JsonPropertySourceFactory implements PropertySourceFactory {

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
		final PropertyConfig readValues = new ObjectMapper().readValue(resource.getInputStream(), PropertyConfig.class);
		final Map<String, Object> propertyMap = new HashMap<>();
		final List<com.mgm.services.booking.room.util.PropertyConfig.PropertyValue> propertyValues = readValues
				.getPropertyValues();
		propertyMap.put("propertyValues", propertyValues);
		final Map<String, PropertyValue> propertyCodeIdMap = new HashMap<>();

		for (final com.mgm.services.booking.room.util.PropertyConfig.PropertyValue propertyValue : propertyValues) {
			propertyCodeIdMap.putAll(propertyValue.getGsePropertyIds().stream()
					.collect(Collectors.toMap(id -> id, id -> propertyValue)));
			propertyCodeIdMap.putAll(propertyValue.getAcrsPropertyIds().stream()
					.collect(Collectors.toMap(id -> id, id -> propertyValue)));
			if (StringUtils.isNotEmpty(propertyValue.getMasterPropertyId())) {
				propertyCodeIdMap.put("MASTER-"+propertyValue.getMasterPropertyId(), propertyValue);
			}
		}

		propertyMap.put("propertyValuesMap", propertyCodeIdMap);
		return new MapPropertySource("json-property", propertyMap);
	}
}