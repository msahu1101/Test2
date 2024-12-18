package com.mgm.services.booking.room.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public @Data class Message {

    private String type;
    private String code;
    private String msg;

}
