package com.mgm.services.booking.room.model;

import lombok.Data;
@Data
public  class RoomBookingComponent {
    private String id;  
    private String code;
    private String shortDescription;
    private String longDescription;
    private boolean active;
    private boolean nonEditable;
    private String pricingApplied;
    private double tripPrice;
    private double tripTax;
    private double price;
    private double depositAmount;
    private Boolean isDespsitRequired;
    private ComponentPrices prices;
}
