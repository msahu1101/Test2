package com.mgm.services.booking.room.model.request.dto;

import com.mgm.services.booking.room.model.request.RoomPaymentDetailsRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import lombok.Data;

import java.util.List;

@Data
public class RefundCommitRequestDTO {
    private String confirmationNumber;
    RoomReservation existingReservation;
    private List<RoomPaymentDetailsRequest> billing;
}
