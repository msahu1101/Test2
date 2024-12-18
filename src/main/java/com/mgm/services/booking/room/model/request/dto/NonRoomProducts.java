package com.mgm.services.booking.room.model.request.dto;

import lombok.Data;

import java.util.List;

@Data
public class NonRoomProducts {
    private String code;
    private Integer qty;
    private List<PkgDateDTO> dates;

}
