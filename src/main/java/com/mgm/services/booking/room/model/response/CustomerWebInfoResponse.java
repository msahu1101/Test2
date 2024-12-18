package com.mgm.services.booking.room.model.response;

import lombok.Data;

/**
 * The Class CustomerWebInfoResponse.
 */
public @Data class CustomerWebInfoResponse {
    private String customerEmail;
    private int secretQuestionId;
    private String emailAddress;
    private String emailPreference;
    private boolean active;
    private int mlifeNo;
}
