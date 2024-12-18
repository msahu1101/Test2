package com.mgm.services.booking.room.model.response;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

public @Data class MyVegasResponse {
    
    /** The redemption code. */
    private String redemptionCode;

    /** The reward type. */
    private String rewardType;

    /** The property id. */
    private String propertyId;

    /** The program id. */
    private String programId;

    /** The status. */
    private String status;
    
    /** The coupon code. */
    private String couponCode;
    
    /** The list of dates when redemption is unavailable. */
    private List<DatesRedemptionIsUnAvailable> datesRedemptionIsUnAvailable;
    
    /**
     * Dates Redemption is unavailable
     * @author vararora
     *
     */
    public static @Data class DatesRedemptionIsUnAvailable implements Serializable {
        private static final long serialVersionUID = 4866203939190150444L;
        
        private String beginDate;
        
        private String endDate;

    }

}
