package com.mgm.services.booking.room.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;

public @Data class PriceItemized {

    @JsonFormat(
            pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date date;
    @JsonProperty("isComp")
    private boolean comp;
    private double basePrice;
    private double discountedPrice;
    private String programId;
}
