package com.mgm.services.booking.room.exception;

import lombok.Data;

@Data
public class ACRSErrorDetails {
    private String title;
    private int code;
    private String status;
}
