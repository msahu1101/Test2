package com.mgm.services.booking.room.model.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ActualBookingProgram {
    @JsonProperty("p")
    private String program;
    @JsonProperty("d")
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy-MM-dd")
    private LocalDate date;
    @JsonProperty("r")
    private Double price;
}
