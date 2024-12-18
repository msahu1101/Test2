package com.mgm.services.booking.room.model;

import org.springframework.util.MultiValueMap;

import lombok.AllArgsConstructor;
import lombok.Data;

public @Data @AllArgsConstructor class ApiDetails {

    public enum Method {
        GET, POST, PUT
    };
    private Method httpMethod;
    private String baseServiceUrl;
    private MultiValueMap<String, String> defaultQueryParams;
    private Object defaultRequest;
}
