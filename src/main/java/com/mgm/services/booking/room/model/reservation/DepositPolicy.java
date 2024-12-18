package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;

import lombok.Data;

public @Data class DepositPolicy implements Serializable {

    private static final long serialVersionUID = -2531887272985527737L;

    private boolean depositRequired;
    private boolean creditCardRequired;
}
