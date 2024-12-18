package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.booking.room.model.response.MyVegasResponse;
import com.mgm.services.common.model.RedemptionValidationResponse;

/**
 * Service interface that exposes service for validating and confirming the redemption code
 * 
 */
public interface MyVegasService {

    /**
     * Service method to validate whether the my vegas redemption code is valid
     * 
     * @param myVegasRequest
     *            the request object
     * @return Returns redemption response
     */
    RedemptionValidationResponse validateRedemptionCode(MyVegasRequest myVegasRequest);

    /**
     * Service method to confirm that the redemption code has been successfully redeeemed
     * 
     * @param myVegasRequest
     *            the request object
     * @return Void response or error
     */
    void confirmRedemptionCode(MyVegasRequest myVegasRequest);
    
    /**
     * Service method to validate whether the my vegas redemption code is valid
     * 
     * @param myVegasRequest
     *            the request object
     * @param token
     *            the token
     * @return Returns MyVegas Response
     */
    MyVegasResponse validateRedemptionCodeV2(MyVegasRequest myVegasRequest, String token);
    
    /**
     * Service method to confirm that the redemption code has been successfully redeemed
     * 
     * @param myVegasRequest
     *            the request object
     * @return Void response or error
     */
    void confirmRedemptionCodeV2(MyVegasRequest myVegasRequest);


}
