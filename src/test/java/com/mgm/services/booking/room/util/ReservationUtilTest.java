package com.mgm.services.booking.room.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.PaymentBasic;
import com.mgm.services.booking.room.model.PriceItemized;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.TripDetail;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;

/**
 * Unit test class to test ReservationUtil.
 *
 */
public class ReservationUtilTest extends BaseRoomBookingTest {

    private RoomReservation getRoomReservation(String filename) {
        File file = new File(getClass().getResource(filename).getPath());

        return convert(file, RoomReservation.class);

    }

    /**
     * Test transformation for room reservation object which is in pre-reserve
     * state.
     * 
     */
    @Test
    public void transformPrereserveTest() {

        RoomReservation reservation = getRoomReservation("/reservation-prereserve.json");
        ApplicationProperties appProperties = new ApplicationProperties();
        Map<String, String> timezone = new HashMap<>();
        timezone.put("default", "America/Los_Angeles");
        appProperties.setTimezone(timezone);

        RoomReservationResponse response = RoomReservationTransformer.transform(reservation, appProperties);

        assertEquals("f44d10c3-aeaa-4aa1-8d94-1d0df9017437", response.getRoomTypeId());
        assertEquals("66964e2b-2550-4476-84c3-1a4c0c5c067f", response.getPropertyId());
        assertNull(response.getProgramId());
        assertNotNull(response.getItemId());
        assertNull(response.getConfirmationNumber());
        assertNull(response.getState());

        TripDetail tripDetail = response.getTripDetails();
        assertEquals(reservation.getCheckInDate(), tripDetail.getCheckInDate());
        assertEquals(reservation.getCheckOutDate(), tripDetail.getCheckOutDate());
        assertEquals(2, tripDetail.getNumGuests());
        assertEquals(2, tripDetail.getNights());

        RatesSummary rates = response.getRates();
        assertEquals(20049.98, rates.getRoomSubtotal(), 0.1);
        assertEquals(10024.99, rates.getProgramDiscount(), 0.1);
        assertEquals(0.0, rates.getRoomRequestsTotal(), 0.1);
        assertEquals(10024.99, rates.getAdjustedRoomSubtotal(), 0.1);
        assertEquals(72.1, rates.getResortFeeAndTax(), 0.1);
        assertEquals(200.49, rates.getRoomChargeTax(), 0.1);
        assertEquals(10297.58, rates.getReservationTotal(), 0.1);
        assertEquals(5112.74, rates.getDepositDue(), 0.1);
        assertEquals(5184.84, rates.getBalanceUponCheckIn(), 0.1);

        // Itemized
        PriceItemized itemized = rates.getItemized().get(0);
        assertEquals(10024.99, itemized.getBasePrice(), 0.1);
        assertEquals(5012.495, itemized.getDiscountedPrice(), 0.1);
        assertEquals("2e44c1cf-097c-4b0b-a86f-7993d239b055", itemized.getProgramId());

        itemized = rates.getItemized().get(1);
        assertEquals(10024.99, itemized.getBasePrice(), 0.1);
        assertEquals(5012.495, itemized.getDiscountedPrice(), 0.1);
        assertEquals("2e44c1cf-097c-4b0b-a86f-7993d239b055", itemized.getProgramId());

        assertNull(response.getPayment());

    }

    /**
     * Test transformation of room reservation object which is in booked state
     */
    @Test
    public void transformReservationTest() {

        RoomReservation reservation = getRoomReservation("/reservation-booked.json");
        ApplicationProperties appProperties = new ApplicationProperties();
        Map<String, String> timezone = new HashMap<>();
        timezone.put("default", "America/Los_Angeles");
        appProperties.setTimezone(timezone);
        RoomReservationResponse response = RoomReservationTransformer.transform(reservation, appProperties);

        assertEquals("f44d10c3-aeaa-4aa1-8d94-1d0df9017437", response.getRoomTypeId());
        assertEquals("66964e2b-2550-4476-84c3-1a4c0c5c067f", response.getPropertyId());
        assertNull(response.getProgramId());
        assertNotNull(response.getItemId());
        assertEquals("M00AE5151", response.getConfirmationNumber());
        assertEquals(ReservationState.Booked, response.getState());

        TripDetail tripDetail = response.getTripDetails();
        assertEquals(reservation.getCheckInDate(), tripDetail.getCheckInDate());
        assertEquals(reservation.getCheckOutDate(), tripDetail.getCheckOutDate());
        assertEquals(2, tripDetail.getNumGuests());
        assertEquals(2, tripDetail.getNights());

        RatesSummary rates = response.getRates();
        assertEquals(20049.98, rates.getRoomSubtotal(), 0.1);
        assertEquals(9022.49, rates.getProgramDiscount(), 0.1);
        assertEquals(0.0, rates.getRoomRequestsTotal(), 0.1);
        assertEquals(11027.489, rates.getAdjustedRoomSubtotal(), 0.1);
        assertEquals(72.1, rates.getResortFeeAndTax(), 0.1);
        assertEquals(220.54, rates.getRoomChargeTax(), 0.1);
        assertEquals(11320.13, rates.getReservationTotal(), 0.1);
        assertEquals(5624.01, rates.getDepositDue(), 0.1);
        assertEquals(5696.12, rates.getBalanceUponCheckIn(), 0.1);

        // Itemized
        PriceItemized itemized = rates.getItemized().get(0);
        assertEquals(10024.99, itemized.getBasePrice(), 0.1);
        assertEquals(5513.7445, itemized.getDiscountedPrice(), 0.1);
        assertEquals("89364848-c326-4319-a083-d5665df90349", itemized.getProgramId());

        itemized = rates.getItemized().get(1);
        assertEquals(10024.99, itemized.getBasePrice(), 0.1);
        assertEquals(5513.7445, itemized.getDiscountedPrice(), 0.1);
        assertEquals("89364848-c326-4319-a083-d5665df90349", itemized.getProgramId());

        assertNotNull(response.getPayment());
        PaymentBasic payment = response.getPayment();

        assertEquals(5624.02, payment.getChargeAmount(), 0.1);
        assertEquals("Mastercard", payment.getCardType());
        assertEquals("XXXXXXXXXXXX4444", payment.getCardMaskedNumber());

    }

    /**
     * Calculate the forfeit amount in various scenarios.
     */
    @Test
    public void getForfeitAmountTest() {
        RoomReservation reservation = getRoomReservation("/reservation-booked.json");
        ApplicationProperties appProperties = new ApplicationProperties();
        Map<String, String> timezone = new HashMap<>();
        timezone.put("default", "America/Los_Angeles");
        appProperties.setTimezone(timezone);

        // forfeit date is passed, expecting total amount to be forfeit'ed
        double forfeitAmount = ReservationUtil.getForfeitAmount(reservation, appProperties);
        assertEquals(5624.01, forfeitAmount, 0.1);

        // forfeit date is null, expecting 0.0 to be forfeit'ed
        reservation.getDepositCalc().setForfeitDate(null);
        forfeitAmount = ReservationUtil.getForfeitAmount(reservation, appProperties);
        assertEquals(0.0, forfeitAmount, 0.1);

        // forfeit date is in future, expecting 0.0 to be forfeit'ed
        reservation.getDepositCalc().setForfeitDate(getFutureDate(5));
        forfeitAmount = ReservationUtil.getForfeitAmount(reservation, appProperties);
        assertEquals(0.0, forfeitAmount, 0.1);

    }

}
