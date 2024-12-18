package com.mgm.services.booking.room.model.reservation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.model.HoldReservationBasicInfo;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.RoomBookingComponent;
import com.mgm.services.booking.room.model.crs.reservation.Warning;
import com.mgm.services.booking.room.model.response.PricingModes;

import lombok.Data;

public @Data class RoomReservation implements Serializable {

    private static final long serialVersionUID = -8325088074053237799L;

    private int instance;
    private long customerId;
    private String inSessionReservationId;
    private int numRooms;
    private Date bookDate;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    private Date checkInDate;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd")
    private Date checkOutDate;
    private int numAdults;
    private String billingType;
    private ReservationState state;
    private String postingFlow;
    private String postingState;
    private long postingStartedAt;
    private long postingEndedAt;
    private double amountDue;
    private long createdAt;
    private long updatedAt;
    private long syncFromOperaAt;
    private boolean nrgStatus;
    private String id;
    private String itineraryId;
    private String propertyId;
    private String programId;
    private String roomTypeId;
    private String guaranteeCode;
    private ReservationProfile profile;
    private List<RoomPrice> bookings;
    private List<RoomMarket> markets;
    private RoomChargesAndTaxes chargesAndTaxesCalc;
    private DepositPolicy depositPolicyCalc;
    private Deposit depositCalc;
    private String channelId;
    private String origin;
    private String source;
    private List<String> specialRequests = new ArrayList<>();
    private List<CreditCardCharge> creditCardCharges;
    private String confirmationNumber;
    private String requestConfirmationNumber;
    private List<Payment> payments;
    private String comments;
    private List<String> additionalComments;
    private List<String> shoppedItineraryIds;

    // Attributes added for V2 APIs
    private int customerRank;
    private int customerSegment;
    private String customerDominantPlay;
    private AgentInfo agentInfo;
    private String[] shareWiths;
    private ShareWithType shareWithType;
    private String shareId;
    private String upgradeRoomTypeId;
    private String operaState;
    private String roomNumber;
    private String partyConfirmationNumber;
    private String extConfirmationNumber;
    private String otaConfirmationNumber;
    private String primarySharerConfirmationNumber;
    private String operaConfirmationNumber;
    private String rrUpSell;
    private int numChildren;
    private String cancellationReason;
    private String cancellationPolicyInfo;
    private String cancellationNumber;
    private List<RoomReservationAlert> alerts;
    private List<ReservationRoutingInstruction> routingInstructions = new ArrayList<>();
    private List<RoomReservationTrace> traces;
    private List<ReservationProfile> shareWithCustomers;
    private List<String> sharedConfirmationNumbers;
    private List<Warning> crsWarnings;
    // Attributes added for CRS
    @JsonProperty
    private boolean isGroupCode;
    @JsonProperty
    private boolean isPaymentWidgetFlow;
    
    private boolean perpetualPricing;
    private PricingModes pricingMode;
 // Attributes added for hold api
    private List<RoomBookingComponent> availableComponents;
    private List<PurchasedComponent> purchasedComponents;
    private String promo;
    private String myVegasPromoCode;
    private boolean ignoreChannelMargins;
    private boolean excludeNonOffer;
    private boolean isPendingReservation;
    private boolean thirdParty;
    //This is added to handle special request ID to code and reverse.
    private List<ReservationSplRequest> specialRequestObjList;
    List<AddOnDeposit> addOnsDeposits;
    private String operaRoomCode;
    private boolean f1Package;
    private List<String> ratePlanTags;
    public boolean getIsGroupCode() {
        return isGroupCode;
    }

    public void setIsGroupCode(boolean isGroupCode) {
        this.isGroupCode = isGroupCode;
    }
    private List<RoomReservation> shareWithReservations;
    private boolean skipFraudCheck;
    private boolean skipPaymentProcess;
    private List<PkgComponent> pkgComponents;
    private List<HoldReservationBasicInfo> itemsInCart;

}
