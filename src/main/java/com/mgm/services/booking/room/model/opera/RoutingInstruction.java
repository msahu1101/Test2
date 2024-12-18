package com.mgm.services.booking.room.model.opera;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RoutingInstruction {

	private String id;
    private String name;
    private String source;
    private String startDate;
    private String endDate;
    private String authorizer;
    private String[] routingCodes;
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
