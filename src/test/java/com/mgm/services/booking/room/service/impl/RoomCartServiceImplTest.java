package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.ReservationDAO;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;

@RunWith(MockitoJUnitRunner.class)
public class RoomCartServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private RoomProgramService roomProgramService;

    @Mock
    private ReservationDAO reservationDAO;

    @Mock
    private RoomProgramService programService;

    @Mock
    private RoomProgramCacheService programCacheService;

    @InjectMocks
    RoomCartServiceImpl preReserveServiceImpl;

    /**
     * Get Room Reservation object based on reservation state.
     * 
     * @param type
     *            Reservation state
     * @return Room reservation object
     */
    private RoomReservation getRoomReservation(String state) {
        File file;
        if ("PRERESERVE".equals(state)) {
            file = new File(getClass().getResource("/reservation-prereserve.json").getPath());
        } else {
            file = new File(getClass().getResource("/reservation-makeRoomReservation-dao-response.json").getPath());

        }

        return convert(file, RoomReservation.class);
    }

    /**
     * Test for pre-reserve state of reservation and verify reservation id.
     */
    @Test
    public void preReserveTest() {
        when(reservationDAO.prepareRoomCartItem(Mockito.any())).thenReturn(getRoomReservation("PRERESERVE"));

        RoomCartRequest request = new RoomCartRequest();

        RoomReservation response = preReserveServiceImpl.prepareRoomCartItem(request);
        assertEquals("5521b517-16c2-4d74-a481-bdcd1cc53522", response.getInSessionReservationId());
    }

    /**
     * Test for pre-reserve state of reservation for program flow and verify
     * reservation id.
     */
    @Test
    public void preReserveProgramTest() {
        when(reservationDAO.prepareRoomCartItem(Mockito.any())).thenReturn(getRoomReservation("PRERESERVE"));
        when(programService.isProgramApplicable(Mockito.any())).thenReturn(true);
        RoomProgram program = new RoomProgram();
        program.setId("89364848-c326-4319-a083-d5665df90349");
        program.setPropertyId("f8d6a944-7816-412f-a39a-9a63aad26833");
        when(programCacheService.getRoomProgram("89364848-c326-4319-a083-d5665df90349")).thenReturn(program);

        RoomCartRequest request = new RoomCartRequest();
        request.setProgramId("89364848-c326-4319-a083-d5665df90349");

        RoomReservation response = preReserveServiceImpl.prepareRoomCartItem(request);
        assertEquals("5521b517-16c2-4d74-a481-bdcd1cc53522", response.getInSessionReservationId());
    }

    /**
     * Test for pre-reserve state of reservation for promoCode flow and verify
     * reservation id.
     */
    @Test
    public void preReservePromoCodeTest() {
        when(reservationDAO.prepareRoomCartItem(Mockito.any())).thenReturn(getRoomReservation("PRERESERVE"));
        when(programService.isProgramApplicable(Mockito.any())).thenReturn(true);
        when(programService.getProgramByPromoCode("BOGO", "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
                .thenReturn("89364848-c326-4319-a083-d5665df90349");

        RoomCartRequest request = new RoomCartRequest();
        request.setPromoCode("BOGO");
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        RoomReservation response = preReserveServiceImpl.prepareRoomCartItem(request);
        assertEquals("5521b517-16c2-4d74-a481-bdcd1cc53522", response.getInSessionReservationId());
    }

    /**
     * Test for various expected failure scenarios.
     */
    @Test
    public void preReserveProgramFailuresTest() {

        when(programService.isProgramApplicable(Mockito.any())).thenReturn(false);

        RoomCartRequest request = new RoomCartRequest();
        request.setCustomerId(-1);
        request.setProgramId("89364848-c326-4319-a083-d5665df90349");

        try {
            preReserveServiceImpl.prepareRoomCartItem(request);
        } catch (Exception ex) {
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }

        when(programService.getProgramByPromoCode("BOGO", "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
                .thenReturn(StringUtils.EMPTY);
        request.setPromoCode("BOGO");
        request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

        try {
            preReserveServiceImpl.prepareRoomCartItem(request);
        } catch (Exception ex) {
            assertEquals("<_offer_not_available>[ Offer is not available or invalid ]", ex.getMessage());
        }

        when(programService.getProgramByPromoCode("BOGO", "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
                .thenReturn("1795e3c6-8e06-4baf-b1df-c09b20fbe1de");

        try {
            preReserveServiceImpl.prepareRoomCartItem(request);
        } catch (Exception ex) {
            assertEquals("<_offer_not_eligible>[ User is not eligible for the offer ]", ex.getMessage());
        }
    }

    /**
     * Test pre-reserve with add room requests scenarios.
     */
    @Test
    public void addRoomRequestSucessTest() {
        when(reservationDAO.updateRoomReservation(Mockito.any())).thenReturn(getRoomReservation("ADDROOM"));
        RoomReservation reservation = new RoomReservation();

        RoomReservation response = preReserveServiceImpl.addRoomRequests(reservation);

        assertNotNull(response.getCheckInDate());
        assertNotNull(response.getCheckOutDate());
        assertNotNull(response.getPropertyId());
        assertNotNull(response.getRoomTypeId());
    }

}
