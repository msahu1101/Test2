package com.mgm.services.booking.room.model.request;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgm.services.booking.room.model.BillingInfo;
import com.mgm.services.booking.room.model.profile.Profile;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservationAlert;
import com.mgm.services.booking.room.model.reservation.RoomReservationTrace;
import com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction;
import com.mgm.services.common.model.BaseRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(
        callSuper = false)
public @Data class ModificationChangesRequest extends BaseRequest {

    private String confirmationNumber;
    private String partyConfirmationNumber;
    private String firstName;
    private String lastName;
    private TripDetail tripDetails;
    private List<RoomRequest> roomRequests = new ArrayList<>();
    private List<String> comments;
    private String propertyId;
    private String roomTypeId;
    private String programId;
    private String guaranteeCode;
    private List<BillingInfo> billingInfo;
    private Profile profile;
    private int rankOrSegment;
    private String dominantPlay;
    private List<RoomReservationAlert> alerts;
    private List<ReservationRoutingInstruction> routingInstructions;
    private List<RoomReservationTrace> traces;
    private boolean isPerpetualOffer;
    private List<RoomPrice> bookings;
    private boolean groupCode;
    private String promo;
    private String promoCode;
}
