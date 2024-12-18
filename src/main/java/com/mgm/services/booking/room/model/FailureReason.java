package com.mgm.services.booking.room.model;

import lombok.Data;

/**
 * Failure Reason 
 * @author nitpande0
 *
 */
public @Data class FailureReason {
    private String type;
    private String code;
    private String reason;

}
