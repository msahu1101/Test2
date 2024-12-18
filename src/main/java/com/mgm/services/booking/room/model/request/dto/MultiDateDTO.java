package com.mgm.services.booking.room.model.request.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import lombok.Data;

import java.time.LocalDate;
@Data
public class MultiDateDTO {
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate checkIn;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate checkOut;

    public MultiDateDTO() {}

    public MultiDateDTO(LocalDate checkIn, LocalDate checkOut) {
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }
}
