package com.mgm.services.booking.room.model.response;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.Room;
import com.mgm.services.common.model.BaseRequest;

import lombok.*;

@EqualsAndHashCode(
        callSuper = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupSearchV2Response {

    private boolean activeFlag;
    private String id;
    private String name;
    private Boolean sunday;
    private Boolean monday;
    private Boolean tuesday;
    private Boolean wednesday;
    private Boolean thursday;
    private Boolean friday;
    private Boolean saturday;
    private String description;
    private String learnMoreDescription;
    private String shortDescription;
    private String termsAndConditions;
    private String agentText;
    private String category;

    private String operaBlockCode;
    private String operaBlockName;
    private String operaGuaranteeCode;
    private String propertyId;

    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date travelPeriodEnd;
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date travelPeriodStart;
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date periodEndDate;
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date periodStartDate;

    // Add class objects from IMD
    private List<Room> rooms;

    private String reservationMethod;
    private String publicName;
    private String groupCode;
    private Boolean isElastic;
    private String saleStatus;
    private String industryType;
    private String groupCnfNumber;

}
