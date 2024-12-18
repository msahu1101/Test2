package com.mgm.services.booking.room.model.response;

import lombok.Data;

/**
 * Authorization response class
 * @author nitpande0
 *
 */
public @Data class AuthorizationTransactionResponse {

    private boolean authorized;
    private String transactionId;
    private String reference;
    private String authorizationRemarks;
}
