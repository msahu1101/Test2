package com.mgm.services.booking.room.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class PurchasedComponent {

    private String id;  
    private String code;
    private String shortDescription;
    private String longDescription;
    private boolean active;
    private String pricingApplied;
    private double tripPrice;
    private double tripTax;
    private double price;
    private ComponentPrices prices;
    private double depositAmount;
    private Boolean isDepositRequired;
    private boolean nonEditable;
    private String ratePlanName;
    private String ratePlanCode;
    @JsonProperty
    private boolean isPkgComponent;
    public boolean getIsPkgComponent() {
        return isPkgComponent;
    }

    public void setIsPkgComponent(boolean isPkgComponent) {
        this.isPkgComponent = isPkgComponent;
    }
}