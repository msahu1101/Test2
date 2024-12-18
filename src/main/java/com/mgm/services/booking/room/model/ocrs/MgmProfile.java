package com.mgm.services.booking.room.model.ocrs;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MgmProfile {

    private String mgmId;
    private String mlifeNumber;
    private List<AdditionalGuest> additionalGuests = new ArrayList<>();
}
