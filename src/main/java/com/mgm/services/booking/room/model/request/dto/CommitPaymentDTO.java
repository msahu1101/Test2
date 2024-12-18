package com.mgm.services.booking.room.model.request.dto;

import com.mgm.services.booking.room.model.request.PaymentRoomReservationRequest;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.common.model.BaseRequest;
import lombok.Data;

import java.util.List;

@Data
public class CommitPaymentDTO extends BaseRequest {
    private String confirmationNumber;
    String firstName;
    String lastName;
    private List<CreditCardCharge> creditCardCharges;
    boolean noChangesInReservation;
    String itineraryId;
    String id;

}
