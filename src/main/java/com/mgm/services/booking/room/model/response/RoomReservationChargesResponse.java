package com.mgm.services.booking.room.model.response;

import java.util.List;

import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.RoomBookingComponent;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.DepositPolicy;
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
public class RoomReservationChargesResponse {

    private int instance;
    private String propertyId;
    private String roomTypeId;
    private String programId;
    private String customerId;
    private String guaranteeCode;
    private List<String> specialRequests;

    private UserProfileResponse profile;
    private TripDetailsResponse tripDetails;
    private RoomChargesAndTaxes chargesAndTaxes;

    private List<RoomPrice> bookings;
    private RatesSummary ratesSummary;
    private Deposit depositDetails;
    private double amountDue;
    private DepositPolicy depositPolicy;
    private boolean perpetualPricing;
    

    private List<RoomMarket> markets;

    private String confirmationNumber;
    private List<RoomBookingComponent> availableComponents;
    private String promo;
}
