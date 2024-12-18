package com.mgm.services.booking.room.model.request.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

public @Data class RoomProgramsResponseDTO {

    private List<RoomProgramDTO> patronPrograms = new ArrayList<>();
    private List<RoomProgramDTO> poPrograms = new ArrayList<>();
    private List<RoomProgramDTO> iceChannelPrograms = new ArrayList<>();
    private String userCvsValues;
}
