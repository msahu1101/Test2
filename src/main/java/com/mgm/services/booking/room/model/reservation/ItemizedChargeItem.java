package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class ItemizedChargeItem implements Serializable {

    private static final long serialVersionUID = -1825373049205972352L;

    private String id;
    private String shortDescription;
    private Boolean active;
    private String pricingApplied;
    private RoomChargeItemType itemType;
    private double amount;
    private String item;

}
