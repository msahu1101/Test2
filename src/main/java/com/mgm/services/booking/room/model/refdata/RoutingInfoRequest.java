package com.mgm.services.booking.room.model.refdata;

import lombok.Data;

@Data
public class RoutingInfoRequest {

    private String propertyId;
    private String authorizerId;
    private String authorizerAppUserId;
    private String routingCodeArrangementId;
    private String routingCode;
}
