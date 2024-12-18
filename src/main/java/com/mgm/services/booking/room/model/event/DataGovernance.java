package com.mgm.services.booking.room.model.event;

import lombok.Data;

public @Data class DataGovernance {
    private String eventExpiryTime;
    private String catalogId;
    private boolean containsPCI;
    private boolean containsPII;
    private String[] tags;

}
