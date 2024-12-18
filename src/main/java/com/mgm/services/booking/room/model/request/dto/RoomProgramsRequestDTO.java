package com.mgm.services.booking.room.model.request.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

public @SuperBuilder @Getter @AllArgsConstructor class RoomProgramsRequestDTO {

    private String source;
    private String channel;
    private String propertyId;
    private long customerId;
    private String mlifeNumber;
    private boolean perpetualPricing;
    private boolean resortPricing;

}
