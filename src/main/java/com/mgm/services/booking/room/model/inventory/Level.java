package com.mgm.services.booking.room.model.inventory;

import lombok.Data;

@Data
public class Level {
    private String levelIdentifier;
    private String levelType;
    private String totalUnits;
    private String totalAvailableUnits;
    private Integer units;
}
