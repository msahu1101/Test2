package com.mgm.services.booking.room.model.reservation;

import lombok.Data;

@Data
public class ReservationSplRequest {
    private String id;
    private String code;
    private String type;
    private String pricingApplied;
    private String shortDescription;
    private String description;
    private String exactType;
    private String ratePlanName;
    private String ratePlanCode;
}
