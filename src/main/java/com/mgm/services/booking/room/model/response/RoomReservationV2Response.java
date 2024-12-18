package com.mgm.services.booking.room.model.response;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.FailureReason;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.reservation.AgentInfo;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.DepositPolicy;
import com.mgm.services.booking.room.model.reservation.Payment;
import com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomChargesAndTaxes;
import com.mgm.services.booking.room.model.reservation.RoomMarket;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservationAlert;
import com.mgm.services.booking.room.model.reservation.RoomReservationTrace;
import com.mgm.services.booking.room.model.reservation.ShareWithType;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(Include.NON_NULL)
@Data
public class RoomReservationV2Response {

    private String itemId;
    private String id;
    private int instance;
    private long customerId;
    private String propertyId;
    private String itineraryId;
    private String roomTypeId;
    private String programId;
    private String upgradeRoomTypeId;
    private String operaState;
    private ReservationState state;
    private boolean nrgStatus;
    private List<String> specialRequests;
    private boolean thirdParty;


    private String roomNumber;
    private String confirmationNumber;
    private String requestConfirmationNumber;
    private String partyConfirmationNumber;
    private String otaConfirmationNumber;
    private String primarySharerConfirmationNumber;
    private String operaConfirmationNumber;
    private String operaHotelCode;
    private String postingState;

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
    private List<Payment> payments;
    private List<PurchasedComponent> purchasedComponents;

    private double amountDue;
    private String guaranteeCode;
    private int customerRank;
    private int customerSegment;
    private String customerDominantPlay;

    private String[] shareWiths;
    private ShareWithType shareWithType;
    private String rrUpSell;
    private List<RoomReservationAlert> alerts;
    private List<ReservationRoutingInstruction> routingInstructions;
    private List<RoomReservationTrace> traces;
    private List<UserProfileResponse> shareWithCustomers;
    private String shareId;
    private String comments;
    private String[] additionalComments;
    private AgentInfo agentInfo;
    private String promo;
    @Getter(onMethod = @__(@JsonIgnore))
    @Setter
    private FailureReason failureReason;
    private boolean perpetualPricing;

    private String bookingSource;
    private String bookingChannel;
    @JsonProperty
    private boolean isGroupCode;
    private String operaRoomCode;
    private boolean f1Package;
    private String holdId;
    private String orderId;
    private String orderLineItemId;

    public boolean getIsGroupCode() {
        return isGroupCode;
    }

    public void setIsGroupCode(boolean isGroupCode) {
        this.isGroupCode = isGroupCode;
    }

    @JsonProperty
    private boolean isCreditCardExpired;
    @JsonProperty
    private boolean isStayDateModifiable;

    @JsonIgnore
    private boolean notifyCustomer;
    @JsonIgnore
    private String ratesFormat;
    @JsonProperty
    private boolean hdePackage;
    
    @JsonProperty("isCancellable")
    private boolean isCancellable;
    @JsonProperty
    private boolean depositForfeit;

    public boolean getIsCreditCardExpired() {
        return isCreditCardExpired;
    }

    public void setIsCreditCardExpired(boolean isCreditCardExpired) {
        this.isCreditCardExpired = isCreditCardExpired;
    }

    public boolean getIsStayDateModifiable() {
        return isStayDateModifiable;
    }

    public void setIsStayDateModifiable(boolean isStayDateModifiable) {
        this.isStayDateModifiable = isStayDateModifiable;
    }
}
