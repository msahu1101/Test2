package com.mgm.services.booking.room.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class PriceV2Itemized {

    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date date;
    @JsonProperty("isComp")
    private Boolean comp;
    private Double basePrice;
    private Double discountedPrice;
    private Double discount;
    @JsonProperty("isDiscounted")
    private Boolean discounted;
    private String programId;
    private String pricingRuleId;
    private Boolean programIdIsRateTable;
    private String unavailabilityReason;
    private double f1RoomAndTicketPrice;
    private double amtAftTax;
    private double baseAmtAftTax;

}
