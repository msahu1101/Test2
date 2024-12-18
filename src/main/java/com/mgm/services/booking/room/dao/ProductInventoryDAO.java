package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.inventory.*;
import org.springframework.http.HttpEntity;

import java.util.List;

public interface ProductInventoryDAO {
    InventoryGetRes getInventory(String productCode, boolean cacheOnly);
    void holdInventory(HoldInventoryReq request);
    void releaseInventory(ReleaseInventoryReq request);
    String commitInventory(CommitInventoryReq request);
    void rollBackInventory(RollbackInventoryReq request);
    BookedItemList getInventoryStatus(String confirmationNumber, String holdId);
}
