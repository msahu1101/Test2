package com.mgm.services.booking.room.model.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

public @Data class ApplicableProgramsResponse {

    private List<String> programIds = new ArrayList<>();
    private List<ApplicableRoomProgram> programs = new ArrayList<>();

}
