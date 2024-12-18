package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;
import lombok.Data;

import java.util.List;

@Data
public class PaymentRoomReservationRequest extends BaseRequest{
    private String confirmationNumber;
    String firstName;
    String lastName;
    private List<RoomPaymentDetailsRequest> billing;
    boolean noChangesInReservation;
    String itineraryId;
    String id;
    boolean skipCustomerNotification;
}
