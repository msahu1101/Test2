package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.common.model.RedemptionValidationResponse;

/**
 * DAO interface for validating and confirming the redemption code
 *
 */
public interface MyVegasDAO {

    /**
     * Service method to validate the redemption code
     * 
     * @param myVegasRequest
     *            myVegas request
     * @return Returns the response whether the redemption code is valid
     */
    RedemptionValidationResponse validateRedemptionCode(MyVegasRequest myVegasRequest);

    /**
     * Service method to confirm that the redemption code was successfully redeemed
     * 
     * @param myVegasRequest
     *            myVegas request
     * @return Void response or error
     */
    void confirmRedemptionCode(MyVegasRequest myVegasRequest);
    
}
