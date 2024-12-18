package com.mgm.services.booking.room.model.opera;

import com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OperaInfoResponse {
    private List<RoutingInstruction> routingInstructionList;
    private String operaProfileId;
	private String operaConfirmationNumber;
    private String resort;
    private String confirmationNumber;

    public List<RoutingInstruction> getRoutingInstructionList() {
        if (routingInstructionList == null) {
            routingInstructionList = new ArrayList<RoutingInstruction>();
        }
        return routingInstructionList;
    }
}
