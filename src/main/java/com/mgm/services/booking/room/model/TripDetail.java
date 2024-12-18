package com.mgm.services.booking.room.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
public @Data class TripDetail {

    @JsonFormat(
            pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date checkInDate;
    @JsonFormat(
            pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date checkOutDate;
    @JsonFormat(
            pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date bookDate;
    private int numGuests;
    private int nights;
}
