package com.mgm.services.booking.room.model;

import lombok.Data;

@Data
public class PIMPkgComponent {
    String id;
    String code;
    String categoryCode;
    String description;
    boolean active;
    String nonRoomInventoryType;
    String acrsPropertyCode;
}
