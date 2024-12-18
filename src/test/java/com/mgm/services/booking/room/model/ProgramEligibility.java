package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class ProgramEligibility {
    private String programId;
    private String propertyId;
    private String mlifeNumber;
    private String promoCode;
    private String customerId;
    private boolean expectedResult;
    private ValidAvailabilityData availabilityData;

}
