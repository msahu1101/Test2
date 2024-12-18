package com.mgm.services.booking.room.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomProgramBasic {

    private String programId;
    private String propertyId;
    private String ratePlanCode;
    private boolean isActive;
    private boolean bookableOnline;
    private Date bookingStartDate;
    private Date bookingEndDate;
    private Date travelPeriodStart;
    private Date travelPeriodEnd;
    private String[] ratePlanTags;
    private String promoCode;
}
