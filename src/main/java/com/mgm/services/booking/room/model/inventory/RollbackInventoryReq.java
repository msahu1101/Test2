package com.mgm.services.booking.room.model.inventory;

import lombok.Data;

@Data
public class RollbackInventoryReq {
    private String holdId;
    private String orderId;
    private String orderLineItemId;
    private String confirmationNumber;
}
