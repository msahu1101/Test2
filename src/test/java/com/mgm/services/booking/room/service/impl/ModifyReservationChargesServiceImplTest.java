package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.ModifyReservationDAO;
import com.mgm.services.booking.room.dao.RoomReservationChargesDAO;
import com.mgm.services.booking.room.mapper.RoomReservationChargesRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationChargesResponseMapper;
import com.mgm.services.booking.room.model.request.RoomReservationChargesRequest;
import com.mgm.services.booking.room.model.request.TripDetailsRequest;
import com.mgm.services.booking.room.model.response.RoomReservationChargesResponse;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;

/**
 * Unit test class for service methods in ModifyReservationService.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ModifyReservationChargesServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private ModifyReservationDAO modifyReservationDAO;

    @InjectMocks
    private RoomReservationChargesServiceImpl roomReservationChargesServiceImpl;

    @Mock
    private RoomReservationChargesDAO roomReservationChargesDAO;

    @Mock
    private RoomReservationChargesRequestMapper reservationChargesRequestMapper;

    @Mock
    private RoomReservationChargesResponseMapper reservationChargesResponseMapper;

    @Mock
    private CommonServiceImpl commonService;

    private RoomReservationChargesResponse getRoomReservationChargesResponse(String fileName) {
        File file = new File(getClass().getResource(fileName).getPath());

        return convert(file, RoomReservationChargesResponse.class);
    }
    /**
     * Test calculateRoomReservationCharges method in the service class for success
     */
    @Test
    public void preModifyReservationChargesSuccessTest() {

        when(reservationChargesResponseMapper.reservationModelToRoomReservationChargesResponse(Mockito.any()))
                .thenReturn(getRoomReservationChargesResponse("/modifyreservationdao-premodifycharges-response.json"));

        RoomReservationChargesRequest roomReservationChargesRequest = new RoomReservationChargesRequest();
        roomReservationChargesRequest.setConfirmationNumber("9153131255");

        RoomReservationChargesResponse response = roomReservationChargesServiceImpl.calculateRoomReservationCharges(roomReservationChargesRequest);

        assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.getPropertyId());
        assertEquals("9153131255", response.getConfirmationNumber());
        assertEquals(89.91, response.getChargesAndTaxes().getCharges().get(0).getAmount(),0);
        assertEquals(103.07, response.getChargesAndTaxes().getTaxesAndFees().get(0).getAmount(),0);
        assertEquals(90.91, response.getChargesAndTaxes().getCharges().get(1).getAmount(),0);
        assertEquals(37, response.getChargesAndTaxes().getTaxesAndFees().get(1).getAmount(),0);
    }

    /**
     * Test calculateRoomReservationCharges method in the service class for failure
     */
    @Test
    public void preModifyReservationChargesFailureTest() {

        when(reservationChargesResponseMapper.reservationModelToRoomReservationChargesResponse(Mockito.any()))
                .thenReturn(getRoomReservationChargesResponse("/modifyreservationdao-premodifycharges-response.json"));

        RoomReservationChargesRequest roomReservationChargesRequest = new RoomReservationChargesRequest();
        roomReservationChargesRequest.setProgramId("3cd8e260-8f0b-4598-85cc-cce2221fca8b");
        roomReservationChargesRequest.setRoomTypeId("6a82104c-153a-4caf-9cda-91ccea9739c7");

        RoomReservationChargesResponse response = roomReservationChargesServiceImpl.calculateRoomReservationCharges(roomReservationChargesRequest);

        assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", response.getPropertyId());
        assertNotEquals("ProgramId not matching", "3cd8e260-8f0b-4598-85cc-cce2221fca8b", response.getProgramId());
        assertNotEquals("Room Type Id not matching", "6a82104c-153a-4caf-9cda-91ccea9739c7", response.getRoomTypeId());
        assertNotEquals("Not matching", 99.91, response.getChargesAndTaxes().getCharges().get(0).getAmount(),0);
        assertNotEquals("Not matching",203.07, response.getChargesAndTaxes().getTaxesAndFees().get(0).getAmount(),0);
    }

    /**
     * Test calculateRoomReservationCharges method in the service class for invalid dates
     * scenario.
     */
    @Test
    public void preModifyReservationChargesInvalidDatesTest() {

        when(reservationChargesResponseMapper.reservationModelToRoomReservationChargesResponse(Mockito.any()))
                .thenThrow(new BusinessException(ErrorCode.DATES_UNAVAILABLE));
        RoomReservationChargesRequest roomReservationChargesRequest = new RoomReservationChargesRequest();
        roomReservationChargesRequest.setConfirmationNumber("9153131255");

        TripDetailsRequest tripDetail = new TripDetailsRequest();
        // past dates
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -4);
        tripDetail.setCheckInDate(calendar.getTime());

        // past dates
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.DATE, -2);
        tripDetail.setCheckOutDate(calendar1.getTime());
        roomReservationChargesRequest.setTripDetails(tripDetail);

        try {
            roomReservationChargesServiceImpl.calculateRoomReservationCharges(roomReservationChargesRequest);
        } catch (BusinessException businessException) {
            assertEquals("<_dates_not_available>[ One of more dates are not available ]",
                    businessException.getMessage());
        }
    }

}
