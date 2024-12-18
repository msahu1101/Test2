package com.mgm.services.booking.room.model.request;

import java.util.Date;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class TripDetailsRequest {

    @NotNull(message = "_invalid_dates")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date checkInDate;

    @NotNull(message = "_invalid_dates")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date checkOutDate;
      
    private int numAdults;
    private int numChildren;
    private int numRooms;
    
    @AssertTrue(
            message = "_invalid_num_adults")
    public boolean isNumAdultsValid() {
        return numAdults > 0;
    }
    
}
