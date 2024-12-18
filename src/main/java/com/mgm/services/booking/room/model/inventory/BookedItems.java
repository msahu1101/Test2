package com.mgm.services.booking.room.model.inventory;

import lombok.Data;

@Data
public class BookedItems {

    private String id;
    private String holdId;
    private String productType;
    private String productCode;
    private String datetime;
    private String unitName;
    private String unitType;
    private int units;
    private String status;
    private String orderId;
    private String orderLineItemId;
    private String confirmationNumber;
}
