package com.mgm.services.booking.room.service.helper;

import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.model.response.MyVegasResponse;
import com.mgm.services.common.model.RedemptionValidationResponse;

/**
 * Helper class for RoomProgram v2 Services.
 * 
 */
@Component
public class MyVegasServiceHelper {
    
    private static final String TOKEN_PATTERN = "_";
    private static final int TOKEN_SIZE = 2;
    
    /**
     * Convert the Aurora Redemption Response to MyVegas Response
     * @param response - the Redemption Validation Response
     * @return My Vegas Response
     */
    public MyVegasResponse convertFromAuroraRedemptionResponse(RedemptionValidationResponse response) {

        MyVegasResponse myVegasResponse = new MyVegasResponse();
        myVegasResponse.setRedemptionCode(response.getRedemptionCode());
        myVegasResponse.setStatus(response.getStatus());
        if (response.getCouponCode() != null && response.getCouponCode().split(TOKEN_PATTERN).length == TOKEN_SIZE) {
            String[] tokens = response.getCouponCode().split(TOKEN_PATTERN);
            myVegasResponse.setProgramId(tokens[1]);
            myVegasResponse.setPropertyId(tokens[0]);
            myVegasResponse.setCouponCode(response.getCouponCode());
        }
        myVegasResponse.setRewardType(response.getRewardType());

        return myVegasResponse;
    }
    
}
