package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

public @Data class RoomChargeItem implements Serializable {

    private static final long serialVersionUID = -2308358414601546566L;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    private Date date;
    private double amount;
    private List<ItemizedChargeItem> itemized;

}
