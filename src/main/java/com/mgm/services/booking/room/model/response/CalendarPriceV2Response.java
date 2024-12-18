package com.mgm.services.booking.room.model.response;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.AvailabilityStatus;

import lombok.Data;

/**
 * Calendar Price Response class
 * @author nitpande0
 *
 */
@JsonInclude(Include.NON_NULL)
public @Data class CalendarPriceV2Response {

    @JsonProperty("isComp")
    private boolean comp;
    private AvailabilityStatus status;
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date date;
    private String roomTypeId;
    private String programId;
    private double price;
    @JsonProperty("isPOApplicable")
    private boolean isPOApplicable;

}
