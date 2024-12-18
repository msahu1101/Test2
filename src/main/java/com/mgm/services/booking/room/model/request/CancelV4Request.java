package com.mgm.services.booking.room.model.request;

import com.mgm.services.common.model.BaseRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Data Object for cancellation flow
 *
 * @author laknaray
 *
 */

@EqualsAndHashCode(callSuper = false)
public @Data class CancelV4Request extends BaseRequest {
    private boolean overrideDepositForfeit;
    private String confirmationNumber;
    private String cancellationReason;
    private String propertyId;
    private String firstName;
    private String lastName;
    private String inAuthTransactionId;
    private List<RoomPaymentDetailsRequest> billing;
    private boolean skipCustomerNotification;

}
