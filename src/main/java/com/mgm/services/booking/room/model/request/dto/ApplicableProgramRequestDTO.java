package com.mgm.services.booking.room.model.request.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

public @SuperBuilder @Getter @AllArgsConstructor @NoArgsConstructor class ApplicableProgramRequestDTO {

    private String propertyId;
    private String roomTypeId;
    private Date bookDate;
    private Date travelDate;
    private boolean filterBookable;
    private boolean filterViewable;
    private Date checkInDate;
    private Date checkOutDate;
    private int numAdults;
    private int numChildren;
    private String source;
    private long customerId;
    private String mlifeNumber;
}
