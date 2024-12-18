package com.mgm.services.booking.room.model;

import lombok.Data;
import lombok.ToString;

@ToString
public @Data class ValidAvailabilityData {
    
    private String checkInDate;
    private String checkOutDate;
    private String roomTypeId;
    private String propertyId;
    private String secondRoomTypeId;
    private String programId;
    private String pricingRuleId;
    private boolean programIdIsRateTable;
    private String customerId;

}
