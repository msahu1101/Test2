package com.mgm.services.booking.room.model;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.reservation.RoomRequest;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.StringUtils;
import lombok.Data;

public @Data class RoomComponent implements Serializable {

    private static final long serialVersionUID = 8874122362740828606L;

    private String id;
    private boolean nightlyCharge;
    private double price;
    private String description;
    private String pricingApplied;
    private Float taxRate;
    private String shortDescription;
    private String longDescription;
    private String ratePlanName;
    private String ratePlanCode;
    private double amtAftTax;

    public RoomComponent(){}

    protected RoomComponent(String id, boolean nightlyCharge, double price, String description, String pricingApplied,
                            Float taxRate, String shortDescription, String longDescription, String ratePlanName, String ratePlanCode, double amtAftTax) {
        this.id = id;
        this.nightlyCharge = nightlyCharge;
        this.price = price;
        this.description = description;
        this.pricingApplied = pricingApplied;
        this.taxRate = taxRate;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.ratePlanName = ratePlanName;
        this.ratePlanCode = ratePlanCode;
        this.amtAftTax = amtAftTax;
    }

    /**
     * Transform List of RoomRequest objects into List of RoomComponent objects
     * 
     * @param roomRequests
     *            list of RoomRequest objects
     * @return List of RoomComponent objects
     */
    public static List<RoomComponent> transformPckgComponents(List<RoomRequest> roomRequests, ApplicationProperties applicationProperties) {
                return roomRequests.stream()
                .map(rr -> new RoomComponent(rr.getId(), rr.isNightlyCharge(), rr.getPrice(), rr.getDescription(),
                        rr.getPricingApplied(), rr.getTaxRate(), rr.getShortDescription(), rr.getLongDescription(), rr.getRatePlanName(), rr.getRatePlanCode(), rr.getAmtAftTax()))
                .collect(Collectors.toList());

    }
    public static List<RoomComponent> transform(List<RoomRequest> roomRequests, ApplicationProperties applicationProperties) {
        roomRequests.removeIf(x-> (null != x.getCode() && (x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_F1) ||
                x.getCode().toUpperCase().startsWith(ServiceConstant.F1_COMPONENT_START_HDN) ||
                applicationProperties.getTcolvF1ComponentCodes().contains(x.getCode()))));
        return roomRequests.stream()
                .map(rr -> new RoomComponent(rr.getId(), rr.isNightlyCharge(), rr.getPrice(), rr.getDescription(),
                        rr.getPricingApplied(), rr.getTaxRate(), rr.getShortDescription(), rr.getLongDescription(), rr.getRatePlanName(), rr.getRatePlanCode(), rr.getAmtAftTax()))
                .collect(Collectors.toList());

    }
}
