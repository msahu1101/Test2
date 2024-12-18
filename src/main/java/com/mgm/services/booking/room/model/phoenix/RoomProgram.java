package com.mgm.services.booking.room.model.phoenix;

import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(
        callSuper = true)
@ToString(
        callSuper = true)
public @Data class RoomProgram extends BasePhoenixEntity {

    private boolean isActive;
    private boolean bookableOnline;
    private Date bookingStartDate;
    private Date bookingEndDate;
    private Date travelPeriodStart;
    private Date travelPeriodEnd;
    private Date bookBy;
    private String segmentId;
    private String[] tags;
    private String[] rooms;
    private int minNights;
    private int maxNights;
    private String patronPromoId;
    private String promoCode;
    private int segmentFrom;
    private int segmentTo;
    private String agentText;

    private int customerRank;
    private int multiRateSequenceNo;
    private Boolean publicOfferFlag;
    private Boolean publicProgram;

    private Date periodStartDate;
    private Date periodEndDate;
    private String operaBlockCode;
    private String operaBlockName;
    private String reservationMethod;
    private String operaGuaranteeCode;

    private String category;
    private String description;
    private String learnMoreDescription;
    private String termsAndConditions;
    private boolean availableInIce;

}
