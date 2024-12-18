package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mgm.services.booking.room.constant.ServiceConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

/**
 * Response class for segment
 * @author nitpande0
 *
 */
@Data
@SuperBuilder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RoomOfferDetails {

    private String id;
    private String propertyId;
    private String name;
    private boolean active;

    private String category;
    private String description;
    private String shortDescription;  // Public name of Program
    private String termsAndConditions;
    private String learnMoreDescription;
    private String agentText;

    private Integer maxNights;
    private Integer minNights;

    private boolean barProgram;         // publicOfferFlag flag
    private boolean publicProgram;      // Public Program flag

    // Group Program related fields
    private String groupCode;
    private String operaBlockCode;
    private String operaBlockName;
    private String reservationMethod;   // Only applicable for Group Programs

    private String patronPromoId;       // Patron Promo Id
    private String promoCode;           // Opera Promo Code
    private String promo;               // ACRS Promo Code
    private String operaGuaranteeCode;  // ReservationType

    // Program Dates
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date periodStartDate;
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date periodEndDate;
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date travelPeriodStart;
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date travelPeriodEnd;
    @JsonFormat(pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date cutOffDate;

    // Customer value attributes
    private Integer customerRank;
    private Integer segmentFrom;
    private Integer segmentTo;
    private Integer multiRateSequenceNo;

    //Channel Flags
    private boolean bookableOnline;
    private boolean viewOnline;
    private boolean bookableByProperty;
    private boolean viewableByProperty;
    private boolean availableInIce;

    // Add class objects from IMD
    private List<String> tags;
    private Object playerTiers;         //Not available in ACRS
    private List<String> roomIds;

}
