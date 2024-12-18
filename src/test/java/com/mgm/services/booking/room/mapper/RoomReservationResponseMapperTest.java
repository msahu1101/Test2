package com.mgm.services.booking.room.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.reservation.ItemizedChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;

@RunWith(MockitoJUnitRunner.class)
public class RoomReservationResponseMapperTest extends BaseRoomBookingTest {

    @InjectMocks
    RoomReservationResponseMapper roomReservationResponseMapper = Mappers
            .getMapper(RoomReservationResponseMapper.class);
    
    @Mock
    ApplicationProperties appProps;

    @Mock
    ReservationServiceHelper reservationServiceHelper;

    @Test
    public void roomReservationModelToResponse_successTest() {
        RoomReservation roomReservation = convert(
                new File(getClass().getResource("/findreservation-dao-response.json").getPath()),
                RoomReservation.class);

        RoomReservationV2Response response = roomReservationResponseMapper
                .roomReservationModelToResponse(roomReservation);
        assertNotNull(response);
        assertTrue(response.getConfirmationNumber().equals("M00AE44F1"));
    }

    @Test
    public void roomReservationModelToResponse_GroupBlock_successTest() {
        RoomReservation roomReservation = convert(
                new File(getClass().getResource("/findreservation-dao-groupblock-response.json").getPath()),
                RoomReservation.class);
        when(reservationServiceHelper.isHDEPackageReservation(ArgumentMatchers.any())).thenReturn(true);
        RoomReservationV2Response response = roomReservationResponseMapper
                .roomReservationModelToResponse(roomReservation);
        assertNotNull(response);
        assertTrue(response.getConfirmationNumber().equals("M00AE44F1"));
        Assert.assertEquals(true,response.isHdePackage());
    }

    @Test
    public void roomReservationModelToResponse_Borgata_successTest() {
        RoomReservation roomReservation = convert(
                new File(getClass().getResource("/findreservation-dao-response-borgata.json").getPath()),
                RoomReservation.class);

        String[] specialRequests = { "78e56314-8aba-4107-b376-59a148f23b34", "3e205a63-84f7-41b6-a7e8-b83be9b72e80" };
        when(appProps.getBorgataSpecialRequests()).thenReturn(Arrays.asList(specialRequests));
        when(appProps.getBorgataPropertyId()).thenReturn("773000cc-468a-4d86-a38f-7ae78ecfa6aa");

        RoomReservationV2Response response = roomReservationResponseMapper
                .roomReservationModelToResponse(roomReservation);
        assertNotNull(response);

        response.getChargesAndTaxes().getCharges().forEach(charge -> {
            List<String> items = charge.getItemized().stream().map(ItemizedChargeItem::getItem)
                    .collect(Collectors.toList());
            assertTrue("Each charge must contain OCC ComponentCharges item",
                    items.contains(ServiceConstant.OCCUPANCY_FEE_ITEM));
            assertTrue("Each charge must contain TOR ComponentCharges item",
                    items.contains(ServiceConstant.TOURISM_FEE_ITEM));
        });
        response.getChargesAndTaxes().getTaxesAndFees().forEach(charge -> {
            List<String> items = charge.getItemized().stream().map(ItemizedChargeItem::getItem)
                    .collect(Collectors.toList());
            assertTrue("Each taxesAndFee must contain OCC ComponentCharges item",
                    items.contains(ServiceConstant.OCCUPANCY_FEE_ITEM));
            assertTrue("Each taxesAndFee must contain TOR ComponentCharges item",
                    items.contains(ServiceConstant.TOURISM_FEE_ITEM));
        });

        RatesSummary ratesSummary = response.getRatesSummary();
        assertNotNull(ratesSummary);
        assertEquals(-1.0, ratesSummary.getRoomSubtotal(), 0.1);
        assertEquals(-5514.74, ratesSummary.getProgramDiscount(), 0.1);
        assertEquals(0.00, ratesSummary.getRoomRequestsTotal(), 0.1);
        assertEquals(5513.74, ratesSummary.getAdjustedRoomSubtotal(), 0.1);
        assertEquals(15.00, ratesSummary.getResortFee(), 0.1);
        assertEquals(17.04, ratesSummary.getResortFeeAndTax(), 0.1);
        assertEquals(751.24, ratesSummary.getRoomChargeTax(), 0.1);
        assertEquals(3.0, ratesSummary.getOccupancyFee(), 0.1);
        assertEquals(2.0, ratesSummary.getTourismFee(), 0.1);
        assertEquals(2.2725, ratesSummary.getTourismFeeAndTax(), 0.1);
        assertEquals(6287.30, ratesSummary.getReservationTotal(), 0.1);
        assertEquals(6264.99, ratesSummary.getDepositDue(), 0.1);
        assertEquals(22.31, ratesSummary.getBalanceUponCheckIn(), 0.1);
    }
}
