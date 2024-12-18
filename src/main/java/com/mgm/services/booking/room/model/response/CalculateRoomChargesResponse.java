package com.mgm.services.booking.room.model.response;

import java.util.List;

import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.RoomBookingComponent;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.DepositPolicy;
import com.mgm.services.booking.room.model.reservation.PkgComponent;
import com.mgm.services.booking.room.model.reservation.RoomChargesAndTaxes;
import com.mgm.services.booking.room.model.reservation.RoomMarket;
import com.mgm.services.booking.room.model.reservation.RoomPrice;

import lombok.Data;

/**
 * Pojo to hold room reservation charges response
 * @author swakulka
 *
 */
@Data
public class CalculateRoomChargesResponse {

    private String propertyId;
    private String roomTypeId;
    private String programId;
    private String customerId;
    private String guaranteeCode;
    private boolean isGroupCode;
    private boolean  perpetualPricing;
    private PricingModes pricingMode;
    private String promo;
    private String customerDominantPlay;
    private int customerRank;
    private String shoppedItineraryId;

    private UserProfileResponse profile;
    private TripDetailsResponse tripDetails;
    private RoomChargesAndTaxes chargesAndTaxes;
    private List<RoomBookingComponent> availableComponents;
    private List<RoomPrice> bookings;
    private RatesSummary ratesSummary;
    private Deposit depositDetails;
    private double amountDue;
    private DepositPolicy depositPolicy;

    private List<RoomMarket> markets;
    private String id;
    private String confirmationNumber;
    private boolean f1Package;
    private List<String> specialRequests;
    private List<PkgComponent> pkgComponents;
}
