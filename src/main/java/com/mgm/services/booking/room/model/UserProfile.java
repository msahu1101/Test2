package com.mgm.services.booking.room.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

public @Data class UserProfile {

    private String mlifeNumber;
    private long customerId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String password;
    @DateTimeFormat(
            pattern = "M/d/yyyy")
    @JsonFormat(
            pattern = "M/d/yyyy")
    private LocalDate dateOfBirth;
    private String securityQuestionId;
    private String securityAnswer;
    @JsonProperty("canadian-user")
    private String canadianUser;
}
