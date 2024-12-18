package com.mgm.services.booking.room.model.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.model.crs.reservation.CustomData;

import lombok.Data;
@Data
public class ReservationCustomData extends CustomData{
    @JsonProperty("pp")
    public String parentProgram;
    @JsonProperty("b")
    public List<ActualBookingProgram> actualBookingPrograms;

}
