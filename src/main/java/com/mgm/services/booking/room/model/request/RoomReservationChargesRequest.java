package com.mgm.services.booking.room.model.request;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.RoomMarket;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.common.model.BaseRequest;
import com.mgm.services.common.util.ValidationUtil;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * POJO to hold room reservation charges request
 * @author swakulka
 *
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class RoomReservationChargesRequest extends BaseRequest {

    private int instance;
    @NotNull(message = "_invalid_property")
    private String propertyId;

    @NotNull(message = "_invalid_roomtype")
    private String roomTypeId;

    private String programId;
    private String guaranteeCode;
    private boolean isPriceOverride;
    private List<String> specialRequests;

    private UserProfileRequest profile;
    private String customerDominantPlay;
    private int customerRank;

    @Valid
    private TripDetailsRequest tripDetails;
    private List<RoomPaymentDetailsRequest> billing;

    @NotNull(message = "_invalid_bookings")
    private List<RoomPrice> bookings;
    private Deposit depositDetails;
    private List<RoomMarket> markets;

    private String confirmationNumber;
    private String promo;
    @AssertTrue(
            message = "_invalid_property")
    public boolean isPropertyValid() {
        return ValidationUtil.isUuid(propertyId);
    }
    
    @AssertTrue(
            message = "_invalid_roomtype")
    public boolean isRoomValid() {
        return ValidationUtil.isUuid(roomTypeId);
    }
}
