package com.mgm.services.booking.room.model.response;
import lombok.Data;


public @Data
class EcidByEmrResponse {
    private String emr;
    private String ecid;
    private String perpetualEligiblePropertyCodes;
}




