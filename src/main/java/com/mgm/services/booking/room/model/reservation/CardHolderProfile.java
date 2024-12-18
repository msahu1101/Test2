package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;

import com.mgm.services.common.model.ProfileAddress;

import lombok.Data;

public @Data class CardHolderProfile implements Serializable {

    private static final long serialVersionUID = -3596543184843162333L;
    private String firstName;
    private String lastName;
    private ProfileAddress address;
}
