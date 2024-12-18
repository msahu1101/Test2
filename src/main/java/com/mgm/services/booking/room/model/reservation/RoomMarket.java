package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

public @Data class RoomMarket implements Serializable {

    private static final long serialVersionUID = 4469403194210481753L;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    private Date date;
    private String marketCode;
    private String sourceCode;
}
