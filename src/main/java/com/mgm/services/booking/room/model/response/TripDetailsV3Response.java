package com.mgm.services.booking.room.model.response;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class TripDetailsV3Response {

    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date date;
    private String programId;
    @JsonProperty("isComp")
    private boolean comp;
}
