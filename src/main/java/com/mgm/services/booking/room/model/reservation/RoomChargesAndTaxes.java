package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

public @Data class RoomChargesAndTaxes implements Serializable {

    private static final long serialVersionUID = 5443816053950719630L;

    private List<RoomChargeItem> charges;
    private List<RoomChargeItem> taxesAndFees;
}
