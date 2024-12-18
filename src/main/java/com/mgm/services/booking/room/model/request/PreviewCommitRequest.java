package com.mgm.services.booking.room.model.request;

import java.util.List;

import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = false)
public @Data class PreviewCommitRequest extends BaseRequest {

    private String confirmationNumber;
    private TripDetailsRequest tripDetails;
    private String propertyId;
    private List<String> roomRequests;
    private String firstName;
    private String lastName;
    private Double previewReservationTotal;
    private Double previewReservationDeposit;
    private String cvv;
    private String inAuthTransactionId;
    private double previewReservationChangeInDeposit;
    private String authId;
    private boolean skipPaymentProcess;
    private boolean skipCustomerNotification;
}
