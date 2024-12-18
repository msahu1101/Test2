package com.mgm.services.booking.room.model.inventory;

import lombok.Data;

import java.util.Date;

@Data
public class InventoryDetails {
    private String dateTime;
    private InventoryObj inventory;
}
