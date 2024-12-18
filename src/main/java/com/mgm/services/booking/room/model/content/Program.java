package com.mgm.services.booking.room.model.content;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

public @Data class Program {

    private String id;
    private String crsId;
    private String name;
    private String propertyId;
    private Boolean active;

    private String longDescription;
    private String shortDescription;
    private String termsConditions;
    private String promoCode;
    private String prepromotionalCopy;

    private Integer minNights;
    private Integer maxNights;

    private String viewableOnline;
    private String bookableOnline;
    private String viewableByProperty;
    private String bookableByProperty;

    private Date bookStartDate;
    private Date bookEndDate;
    private Date travelPeriodStartDate;
    private Date travelPeriodEndDate;

    private String hdePackage;

    @JsonProperty("descriptions")
    private void unpackDescFromNestedObject(Map<String, String> descriptions) {
        longDescription = descriptions.get("long");
        shortDescription = descriptions.get("short");
    }
}
