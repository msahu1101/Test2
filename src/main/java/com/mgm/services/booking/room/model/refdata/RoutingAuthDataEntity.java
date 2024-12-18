package com.mgm.services.booking.room.model.refdata;

import lombok.Data;

@Data
public class RoutingAuthDataEntity {
    private String id;
    private String phoenixId;
    private String addressId;
    private String appUserId;
    private String authorizer;
    private String externalId;
    private String fullName;
    private String nameId;
    private String resort;
    private String propertyId;
    private String lastModifiedByUser;
    private String lastModifiedByTime;
}
