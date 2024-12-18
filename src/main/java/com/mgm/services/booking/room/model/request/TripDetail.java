package com.mgm.services.booking.room.model.request;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;

public @Data class TripDetail {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date checkInDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date checkOutDate;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private int numAdults;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private int numChildren;
}
