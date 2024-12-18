package com.mgm.services.booking.room.model.refdata;

import lombok.Data;

@Data
public class RefDataEntity {
    String id;
    String Code;
    String phoenixId;
    private String pricingApplied;
    private String shortDescription;
    private String description;

}
