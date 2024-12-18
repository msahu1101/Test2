package com.mgm.services.booking.room.model.response;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.OfferType;

import lombok.Data;

public @Data class RoomProgram {

    private String id;
    private OfferType type;
    @JsonFormat(pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date startDate;
    @JsonFormat(pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date endDate;
    @JsonFormat(pattern = ServiceConstant.DEFAULT_DATE_FORMAT)
    private Date bookByDate;
}
