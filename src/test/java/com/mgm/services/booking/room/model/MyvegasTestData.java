package com.mgm.services.booking.room.model;

import lombok.Data;

public @Data class MyvegasTestData {

    private String guestIdentityUrl;
    private String basicAuthToken;
    private String redeemedToken;
    private String nonMatchingRedemptionCode;
    private String redemptionCodeWithUnavailableDates;
    private String myvegasGuestIdentityUsername;
    private String myvegasGuestIdentityPassword;
    private String grantTypePassword;
    
}
