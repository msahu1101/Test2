package com.mgm.services.booking.room.model.request.dto;

import lombok.Data;

import java.util.List;

@Data
public class PkgDataDTO {
    private String programId;
    private String propertyId;
    private String roomTypeId;
    private List<NonRoomProducts> nonRoomProducts;
}
