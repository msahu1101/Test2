package com.mgm.services.booking.room.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.*;

import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.loyalty.UpdatedPromotion;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.properties.AcrsProperties;
import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;

public class RoomReservationTransformerTest extends BaseRoomBookingTest {

    private RoomReservation getRoomReservation() {
        File file = new File(getClass().getResource("/reservation-prereserve-rates.json").getPath());

        return convert(file, RoomReservation.class);
    }
    
    private RoomReservation getBorgataRoomReservation() {
        File file = new File(getClass().getResource("/reservation-prereserve-rates-borgata.json").getPath());

        return convert(file, RoomReservation.class);
    }

    @Test
    public void testRatesReturnedByCartApiResponse() {
        RoomReservation resv = getRoomReservation();

        ApplicationProperties appProps = new ApplicationProperties();
        Map<String, String> timezones = new HashMap<>();
        timezones.put(resv.getPropertyId(), "America/Los_Angeles");
        appProps.setTimezone(timezones);

        RoomReservationResponse response = RoomReservationTransformer.transform(resv, appProps);

        assertNotNull(response);
        RatesSummary rates = response.getRates();
        assertNotNull(rates);
        assertEquals(30239.97, rates.getRoomSubtotal(), 0.1);
        assertEquals(3000.00, rates.getProgramDiscount(), 0.1);
        assertEquals(40.00, rates.getRoomRequestsTotal(), 0.1);
        assertEquals(27279.97, rates.getAdjustedRoomSubtotal(), 0.1);
        assertEquals(111.00, rates.getResortFee(), 0.1);
        assertEquals(125.85, rates.getResortFeeAndTax(), 0.1);
        assertEquals(3650.06, rates.getRoomChargeTax(), 0.1);
        assertEquals(0.0, rates.getOccupancyFee(), 0.1);
        assertEquals(0.0, rates.getTourismFee(), 0.1);
        assertEquals(0.0, rates.getTourismFeeAndTax(), 0.1);
        assertEquals(0.0, rates.getCasinoSurcharge(), 0.1);
        assertEquals(0.0, rates.getCasinoSurchargeAndTax(), 0.1);
        assertEquals(31055.89, rates.getReservationTotal(), 0.1);
        assertEquals(10294.89, rates.getDepositDue(), 0.1);
        assertEquals(20760.99, rates.getBalanceUponCheckIn(), 0.1);
    }
    
    @Test
    public void testRatesReturnedByCartApiResponseForBorgata() {
        RoomReservation resv = getBorgataRoomReservation();

        ApplicationProperties appProps = new ApplicationProperties();
        Map<String, String> timezones = new HashMap<>();
        timezones.put(resv.getPropertyId(), "America/Los_Angeles");
        appProps.setTimezone(timezones);

        RoomReservationResponse response = RoomReservationTransformer.transform(resv, appProps);

        assertNotNull(response);
        RatesSummary rates = response.getRates();
        assertNotNull(rates);
        assertEquals(30239.97, rates.getRoomSubtotal(), 0.1);
        assertEquals(3000.00, rates.getProgramDiscount(), 0.1);
        assertEquals(40.00, rates.getRoomRequestsTotal(), 0.1);
        assertEquals(27279.97, rates.getAdjustedRoomSubtotal(), 0.1);
        assertEquals(111.00, rates.getResortFee(), 0.1);
        assertEquals(125.85, rates.getResortFeeAndTax(), 0.1);
        assertEquals(3650.06, rates.getRoomChargeTax(), 0.1);
        assertEquals(9.0, rates.getOccupancyFee(), 0.1);
        assertEquals(6.0, rates.getTourismFee(), 0.1);
        assertEquals(6.8175, rates.getTourismFeeAndTax(), 0.1);
        assertEquals(6.0, rates.getCasinoSurcharge(), 0.1);
        assertEquals(6.8175, rates.getCasinoSurchargeAndTax(), 0.1);
        assertEquals(31078.52, rates.getReservationTotal(), 0.1);
        assertEquals(10294.89, rates.getDepositDue(), 0.1);
        assertEquals(20783.63, rates.getBalanceUponCheckIn(), 0.1);
    }

    @Test
    public void testPromoConversion() {

        final ReservationRetrieveResReservation existingReservation = new ReservationRetrieveResReservation();
        final ReservationRetrieveResreservationData data = new ReservationRetrieveResreservationData();
        existingReservation.setData(data);
        final HotelReservationRetrieveResReservation hotelReservation = new HotelReservationRetrieveResReservation();
        data.setHotelReservation(hotelReservation);
        final List<HotelRes> hotels = new ArrayList<>();
        HotelRes hotel = new HotelRes();
        hotel.setPropertyCode("PropertyCode");
        hotels.add(hotel);
        hotelReservation.setHotels(hotels);
        final SegmentRes segmentRes = new SegmentRes();
        final SegmentResItem segment = new SegmentResItem();
        segment.setSegmentHolderId(1);
        final OfferRes offer = new OfferRes();
        offer.setPromoCode("PTRN123456");
        segment.setOffer(offer);
        segmentRes.add(segment);
        hotelReservation.setSegments(segmentRes);

        final GuestsRes userProfiles = new GuestsRes();
        final GuestsResItem profile = new GuestsResItem();
        final LoyaltyProgram loyaltyProgram = new LoyaltyProgram();
        String mLifeNo = "987877474";
        loyaltyProgram.setLoyaltyId(mLifeNo);
        profile.setId(1);
        profile.setLoyaltyProgram(loyaltyProgram);
        userProfiles.add(profile);
        hotelReservation.setUserProfiles(userProfiles);

        final String confirmationNumber = "9874578932";
        RoomReservation newReservation = new RoomReservation();
        ReservationProfile profile2 = new ReservationProfile();
        profile2.setMlifeNo(Integer.parseInt(mLifeNo));
        newReservation.setProfile(profile2);
        newReservation.setPromo("PTRN5678910");
        newReservation.setPropertyId("propertyguid");
        newReservation.setConfirmationNumber(confirmationNumber);

        // Modify
        Collection<UpdatedPromotion> promos = RoomReservationTransformer.getUpdatablePromos(existingReservation, newReservation, confirmationNumber, new AcrsProperties());
        assertEquals(2, promos.size());

        // Create
        promos = RoomReservationTransformer.getUpdatablePromos(null, newReservation, confirmationNumber, new AcrsProperties());
        assertEquals(1, promos.size());

        // Cancel
        promos = RoomReservationTransformer.getUpdatablePromos(existingReservation, null, confirmationNumber, new AcrsProperties());
        assertEquals(1, promos.size());

        // Modify with same promo
        newReservation.setPromo(offer.getPromoCode());
        promos = RoomReservationTransformer.getUpdatablePromos(existingReservation, newReservation, confirmationNumber, new AcrsProperties());
        assertEquals(0, promos.size());

    }
}
