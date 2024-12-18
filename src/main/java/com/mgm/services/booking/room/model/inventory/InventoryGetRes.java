package com.mgm.services.booking.room.model.inventory;

import lombok.Data;

import java.util.List;
@Data
public class InventoryGetRes {
    private String productType;
    private String productCode;
    private String productAssignment;
    private String venueId;
    private List<InventoryDetails> inventories;
}
