package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class Email {

    private String replyTo;
    private String from;
    private String bccTo;
    private String bccLanguage;
    private String subject;
    private String body;
    private String to;

}
