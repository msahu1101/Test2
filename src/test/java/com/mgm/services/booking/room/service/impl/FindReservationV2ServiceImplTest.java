package com.mgm.services.booking.room.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import java.io.File;

import com.mgm.services.booking.room.validator.RBSTokenScopes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.GetRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.helper.FindReservationServiceHelper;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Unit test class for service methods in FindReservationService.
 *
 */
@RunWith(MockitoJUnitRunner.class)

public class FindReservationV2ServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private FindReservationDAO findReservationDAO;

    @Mock
    private RoomReservationResponseMapper roomReservationResponseMapper;

    @InjectMocks
    FindReservationServiceImpl findReservationServiceImpl;

    @Mock
    private FindReservationServiceHelper findReservationServiceHelper;

    @Mock
    ApplicationProperties appProperties;

    @Before
    public void setup() {
        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");
    }

    private RoomReservation getRoomReservation(String fileName) {
        File file = new File(getClass().getResource(fileName).getPath());
        return convert(file, RoomReservation.class);

    }

    private RoomReservationV2Response getFindReservationResponse(String fileName) {
        File file = new File(getClass().getResource(fileName).getPath());
        RoomReservationV2Response data = convert(file, RoomReservationV2Response.class);
        return data;
    }

    /**
     * Test findRoomReservation service for success.
     */
    @Test
    public void findReservationSuccessTest() {

        when(findReservationDAO.findRoomReservation(Mockito.any(FindReservationV2Request.class)))
                .thenReturn(getRoomReservation("/findreservation-dao-response.json"));

        when(roomReservationResponseMapper.roomReservationModelToResponse(Mockito.any(RoomReservation.class)))
                .thenReturn(getFindReservationResponse("/findreservation-dao-response.json"));

        when(findReservationServiceHelper.validateTokenOrServiceBasedRole(RBSTokenScopes.GET_RESERVATION_ELEVATED.getValue())).thenReturn(true);

        FindReservationV2Request reservationV2Request = new FindReservationV2Request();
        reservationV2Request.setConfirmationNumber("M00AE44F1");

        GetRoomReservationResponse responseV2 = findReservationServiceImpl.findRoomReservationResponse(reservationV2Request);
        assertNotNull(responseV2);
        assertEquals("M00AE44F1", responseV2.getRoomReservation().getConfirmationNumber());
        assertEquals("Cancelled", responseV2.getRoomReservation().getState().toString());

    }

    /**
     * Test reservation not found scenario.
     */
    @Test
    public void findReservationInvalidReservationTest() {
        when(findReservationDAO.findRoomReservation(Mockito.any(FindReservationV2Request.class)))
                .thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        FindReservationV2Request reservationV2Request = new FindReservationV2Request();
        reservationV2Request.setConfirmationNumber("M00AE44F1");

        assertThatThrownBy(() -> findReservationServiceImpl.findRoomReservation(reservationV2Request, true))
                .isInstanceOf(BusinessException.class).hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));

    }

}
