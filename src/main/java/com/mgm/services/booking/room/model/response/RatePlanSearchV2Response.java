package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.Room;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@EqualsAndHashCode(
        callSuper = false)
public @Data class RatePlanSearchV2Response {

    private String id;
    private String activeFlag;
    private boolean bookableOnline;
    private boolean viewOnline;
    private boolean bookableByProperty;
    private boolean viewableByProperty;
    private boolean availableInIce;
    private String description;
    private String name;
    private String learnMoreDescription;
    private String ratePlanCode;
    private String category;
    private String operaGuaranteeCode;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date periodEndDate;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date periodStartDate;
    private String propertyId;
    private boolean publicOfferFlag;
    private boolean publicProgram;
    private String shortDescription;
    private String termsAndConditions;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date travelPeriodEnd;
    @JsonFormat(
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date travelPeriodStart;
    private String agentText;
    private Object tags;

}

