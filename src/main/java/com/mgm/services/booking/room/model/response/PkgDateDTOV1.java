package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PkgDateDTOV1 {
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate checkIn;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate checkOut;

    public PkgDateDTOV1() {}

    public PkgDateDTOV1(LocalDate checkIn, LocalDate checkOut) {
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }
}
