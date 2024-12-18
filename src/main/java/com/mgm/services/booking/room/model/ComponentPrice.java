package com.mgm.services.booking.room.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;

import lombok.Data;
@Data
public  class ComponentPrice {
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date date;
    private double amount;
    private double tax;

}
