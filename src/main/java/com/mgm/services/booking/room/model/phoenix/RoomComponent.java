package com.mgm.services.booking.room.model.phoenix;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(
        callSuper = true)
@ToString(
        callSuper = true)
public @Data class RoomComponent extends BasePhoenixEntity {

    private Float price;
    private Float taxRate;
    private String componentType;
    private String pricingApplied;
    private String description;
    private String learnMoreDescription;
    private String externalCode;
    private String propertyId;
}
