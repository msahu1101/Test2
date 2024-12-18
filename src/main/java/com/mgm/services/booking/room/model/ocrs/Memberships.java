package com.mgm.services.booking.room.model.ocrs;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Memberships {
    private List<SelectedMembership> membership = new ArrayList<>();

}
