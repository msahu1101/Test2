package com.mgm.services.booking.room.exception;

import lombok.Data;

@Data
public class ACRSSearchOffersErrorDetails {
    private int httpStatus;
    private String message;
    private int code;
    private String origin;
    private String path;
    // Details object not mapped
}
