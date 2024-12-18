package com.mgm.services.booking.room.model.reservation;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ReservationRoutingInstruction {

	private String id;
    private String name;
    private String source;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date startDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date endDate;
    private String authorizerId;
    private String[] routingCodes;
    private String hostAuthorizerAppUserId;
    private String[] hostRoutingCodes;
    private int window;
    private String limitType;
    private String comments;
    private String memberShipNumber;
    private String limit;
    private boolean dailyYN;
    
    @JsonProperty
    private boolean isSystemRouting;
    private boolean applicableSunday;
    private boolean applicableMonday;
    private boolean applicableTuesday;
    private boolean applicableWednesday;
    private boolean applicableThursday;
    private boolean applicableFriday;
    private boolean applicableSaturday;
    
    public boolean getIsSystemRouting() {
        return isSystemRouting;
    }

    public void setIsSystemRouting(boolean isSystemRouting) {
        this.isSystemRouting = isSystemRouting;
    }
}
