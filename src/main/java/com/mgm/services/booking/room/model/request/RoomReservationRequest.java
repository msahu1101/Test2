package com.mgm.services.booking.room.model.request;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=false)
@Data
public class RoomReservationRequest extends BaseRequest{

    private String channelId;//add this into BaseRequest
    private String inAuthTransactionId;
    private String id;
    private int instance;
    private String propertyId;
    private String itineraryId;
    private String roomTypeId;
    private String programId;
    private String upgradeRoomTypeId;
    private String holdId;
    private String orderLineItemId;
    private String orderId;
    private String operaState;
    private ReservationState state;
    private boolean nrgStatus;
    private boolean skipCustomerNotification;
    private List<String> specialRequests;

    private String roomNumber;
    private String confirmationNumber;
    private String partyConfirmationNumber;
    private String otaConfirmationNumber;
    private String primarySharerConfirmationNumber;
    private String operaConfirmationNumber;
    private boolean skipFraudCheck;
    private boolean skipPaymentProcess;
    private List<PkgComponent> pkgComponents;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date bookDate;

    private UserProfileRequest profile;

    private List<RoomPaymentDetailsRequest> billing;
    private TripDetailsRequest tripDetails;

    private List<RoomPrice> bookings;
    private List<RoomMarket> markets;

    private RoomChargesAndTaxes chargesAndTaxes;

    private Deposit depositDetails;
    private DepositPolicy depositPolicy;
    private List<Payment> payments;

    private double amountDue;
    private String guaranteeCode;
    private int customerRank;
    private int customerSegment;
    private String customerDominantPlay;

    private String rrUpSell;
    private List<RoomReservationAlert> alerts;
    private List<ReservationRoutingInstruction> routingInstructions;
    private List<RoomReservationTrace> traces;
    private List<UserProfileRequest> shareWithCustomers;
    private List<String> sharedConfirmationNumbers;
    private String comments;
    private String[] additionalComments;
    private AgentInfo agentInfo;
    private String myVegasPromoCode;
    private String promo;
    @JsonProperty
    private boolean isGroupCode;

    public boolean getIsGroupCode() {
        return isGroupCode;
    }

    public void setIsGroupCode(boolean isGroupCode) {
        this.isGroupCode = isGroupCode;
    }

}
