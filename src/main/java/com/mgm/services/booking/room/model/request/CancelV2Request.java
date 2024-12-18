package com.mgm.services.booking.room.model.request;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import org.apache.commons.lang3.StringUtils;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Data Object for cancellation flow
 *
 * @author jayveera
 *
 */

@EqualsAndHashCode(callSuper = false)
public @Data class CancelV2Request extends BaseRequest {

    @NotNull(message = "_invalid_itinerary_id")
    private String itineraryId;

    private boolean overrideDepositForfeit;
    private String reservationId;
    private String confirmationNumber;
    private String cancellationReason;
    private String propertyId;
    private boolean f1Package;
    private RoomReservation existingReservation;
    private String inAuthTransactionId;
    private boolean skipPaymentProcess;
    private boolean skipCustomerNotification;
    private boolean cancelPending;
    private List<CreditCardCharge> creditCardCharges;
    @AssertTrue(message = "_invalid_reservation_id_confirmation_number")
    public boolean isValidRequest() {
        return StringUtils.isNotEmpty(reservationId) || StringUtils.isNotEmpty(confirmationNumber);
    }
}
