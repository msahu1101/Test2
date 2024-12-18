package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;

import lombok.Data;

public @Data class RoomRequest implements Serializable {

    private static final long serialVersionUID = -7945645158056528894L;

    private String id;
    private String code;
    private boolean nightlyCharge;
    private boolean selected;
    private boolean active;
    private double price;
    private String description;
    private String shortDescription;
    private String longDescription;
    private String pricingApplied;
    private Float taxRate;
    private double depositAmount;
    private String ratePlanName;
    private String ratePlanCode;
    private double amtAftTax;
}
