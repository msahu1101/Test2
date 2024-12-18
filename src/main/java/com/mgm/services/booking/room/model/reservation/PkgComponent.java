package com.mgm.services.booking.room.model.reservation;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import lombok.Data;

import java.util.Date;

@Data
public class PkgComponent {
    private String id;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date start;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date end;
    private String type;
}
