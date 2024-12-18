package com.mgm.services.booking.room.dao;

import java.util.Collection;
import java.util.List;

import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.loyalty.UpdatedPromotion;

/**
 * Interface exposing services for loyalty related functionality
 *
 */
public interface LoyaltyDao {

    /**
     * Get patron promotions for a mlife number
     * 
     * @param mlifeNumber
     *            Mlife number for which patron promos should be returned
     * @return Returns patron promotions
     */
    List<CustomerPromotion> getPlayerPromos(String mlifeNumber);

    /**
     * This method updates Patron promo status
     * @param promos
     */
    void updatePlayerPromo(Collection<UpdatedPromotion> promos);
}
