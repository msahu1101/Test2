package com.mgm.services.booking.room.model.refdata;

import lombok.*;

@Data
public class RoutingCodesDataEntity {
    private String id;
    private String phoenixId;
    private String arrangementId;
    private String authorizer;
    private String code;
    private boolean dailyYesNo;
    private String description;
    private String name;
    private String operaPropertyCode;
    private String propertyId;
    private String lastModifiedByTime;
    private String lastModifiedByUser;

}

