package com.mgm.services.booking.room.model.request;

import lombok.Data;

public @Data class UpdateProfileRequest {

    private String operaConfirmationNumber;
    private String hotelCode;
    private String mgmId;
    private String mlifeNumber;
    private String firstName;
    private String lastName;
    private String programCode;
    private String partnerAccountNumber;
    private String membershipLevel;
}
