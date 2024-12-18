package com.mgm.services.booking.room.model.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

public @Data class Room {

    private String id;
    private String name;
    private String image;
    private List<String> adaAttributes;
    private String propertyId;

    @SuppressWarnings("unchecked")
    @JsonProperty("images")
    private void unpackImageFromNestedObject(Map<String, Object> images) {
        image = ((Map<String,String>)images.get("tile")).get("url");
    }

    @JsonProperty("accessibilityAmenities")
    private void unpackAdaFromNestedObject(List<Map<String, String>> accessibilityAmenities) {

        adaAttributes = new ArrayList<>();
        for (Map<String, String> amenity : accessibilityAmenities) {
            adaAttributes.add(amenity.get("name"));
        }
    }
}
