package com.mgm.services.booking.room.model.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

public @Data class User {

    @JsonInclude(Include.NON_NULL)
    private String id;
    @JsonInclude(Include.NON_NULL)
    private String status;
    @JsonInclude(Include.NON_NULL)
    private String created;
    @JsonInclude(Include.NON_NULL)
    private String activated;
    @JsonInclude(Include.NON_NULL)
    private String statusChanged;
    @JsonInclude(Include.NON_NULL)
    private String lastLogin;
    @JsonInclude(Include.NON_NULL)
    private String lastUpdated;
    @JsonInclude(Include.NON_NULL)
    private String passwordChanged;
    @JsonInclude(Include.NON_NULL)
    private String transitioningToStatus;

    private Profile profile;
    private LoginCredentials credentials;
}
