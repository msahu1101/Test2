package com.mgm.services.booking.room.model.event;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.RoomPaymentDetailsResponse;
import com.mgm.services.booking.room.model.response.TripDetailsResponse;
import com.mgm.services.booking.room.model.response.UserProfileResponse;

import lombok.Data;

@JsonInclude(Include.NON_NULL)
@Data
public class RoomReservationEventRequest {

    private String id;
    private long customerId;
    private String propertyId;
    private String itineraryId;
    private String roomTypeId;
    private String programId;
    private String operaState;
    private ReservationState state;
    private boolean nrgStatus;
    private List<String> specialRequests;
    private List<ReservationRoutingInstruction> routingInstructions;


    private String roomNumber;
    private String confirmationNumber;
    private String partyConfirmationNumber;
    private String otaConfirmationNumber;
    private String primarySharerConfirmationNumber;
    private String operaConfirmationNumber;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = ServiceConstant.ISO_8601_DATE_FORMAT)
    private Date bookDate;

    private UserProfileResponse profile;

    private List<RoomPaymentDetailsResponse> billing;
    private TripDetailsResponse tripDetails;

    private String cancellationReason;
    private String cancellationNumber;
    private String cancellationPolicyInfo;

    private List<RoomMarket> markets;
    private List<RoomPrice> bookings;

    private RoomChargesAndTaxes chargesAndTaxes;
    
    private RatesSummary ratesSummary;
    private Deposit depositDetails;
    private DepositPolicy depositPolicy;

    private double amountDue;
    private String guaranteeCode;
    private int customerRank;
    private int customerSegment;
    private String customerDominantPlay;

    private String[] shareWiths;
    private ShareWithType shareWithType;
    private List<PurchasedComponent> purchasedComponents;

    private boolean notifyCustomer;
    private String ratesFormat;
    private boolean hdePackage;
    private boolean depositForfeit;
    private boolean f1Package;
    private String holdId;
    private String orderId;
    private String orderLineItemId;
}
