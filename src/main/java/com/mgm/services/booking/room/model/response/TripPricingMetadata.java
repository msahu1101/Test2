package com.mgm.services.booking.room.model.response;

import lombok.Data;

public @Data class TripPricingMetadata {

    private PricingModes pricingMode;
    private ShoppingFlow shoppingFlow;
    private boolean bookingLimitsApplied;
    
    public void setPricingModeIfEmpty(PricingModes pricingMode) {
        if (null == this.pricingMode) {
            this.pricingMode = pricingMode;
        }
    }
}
