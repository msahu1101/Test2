package com.mgm.services.booking.room.model.response;

import lombok.Data;

import java.util.List;

public @Data class RoomProgramValidateResponse {

    private boolean valid;
    private boolean eligible;
    private boolean expired;
    private String programId;
    private boolean segment;
    private String propertyId;
    private boolean myvegas;
    private boolean patronProgram;
    private String promo;
    private boolean hdePackage;
    private String ratePlanCode;
    private String groupCode;
    private List<String> ratePlanTags;
    private boolean f1Package;
    private boolean loyaltyNumberRequired;
}
