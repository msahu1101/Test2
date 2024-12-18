package com.mgm.services.booking.room.model.request.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RoomProgramDTO {

    private String programId;
    private String propertyId;
    @JsonIgnore
    private String ratePlanCode;
    @JsonIgnore
    private String promo;

}
