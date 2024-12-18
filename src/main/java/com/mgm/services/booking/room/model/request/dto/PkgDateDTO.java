package com.mgm.services.booking.room.model.request.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import lombok.Data;


@Data
public class PkgDateDTO {

    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate start;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private LocalDate end;

    public PkgDateDTO() {}

    public PkgDateDTO(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

}
