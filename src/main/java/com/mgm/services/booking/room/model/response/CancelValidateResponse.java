package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

public @Data class CancelValidateResponse {

    @JsonProperty("isCancelable")
    private boolean cancelable;
    @JsonProperty("isForfeit")
    private boolean forfeit;
    private double reservationTotal;
    private double depositAmount;
    private double forfeitAmount;
    private double refundAmount;
}
