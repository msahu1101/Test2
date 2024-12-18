package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data 
@Builder(toBuilder=true)
@NoArgsConstructor 
@AllArgsConstructor
public class ENRRatePlanSearchResponse {

    @JsonProperty("ratePlanId")
    private String ratePlanId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("isActive")
    private Boolean isActive;

    @JsonProperty("isPublic")
    private Boolean isPublic;

    @JsonProperty("bookableOnline")
    private Boolean bookableOnline;

    @JsonProperty("rateCode")
    private String rateCode;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("longDescription")
    private String longDescription;

    @JsonProperty("bookingMessage")
    private String bookingMessage;

    @JsonProperty("loyaltyNumberRequired")
    private boolean loyaltyNumberRequired;

    @JsonProperty("propertyCode")
    private String propertyCode;

    @JsonProperty("propertyId")
    private String propertyId;

    @JsonProperty("travelStartDate")
    private String travelStartDate;

    @JsonProperty("travelEndDate")
    private String travelEndDate;

    @JsonProperty("bookingStartDate")
    private String bookingStartDate;

    @JsonProperty("bookingEndDate")
    private String bookingEndDate;

    @JsonProperty("sequenceNo")
    private Integer sequenceNo;

    @JsonProperty("promo")
    private String promo;

    @JsonProperty("roomTypeCodes")
    private List<String> roomTypeCodes;

    @JsonProperty("ratePlanTags")
    private List<String> ratePlanTags;
    
    @JsonProperty("minLos")
    private String minLos;
    
    @JsonProperty("maxLos")
    private String maxLos;
}
