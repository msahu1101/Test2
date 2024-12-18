package com.mgm.services.booking.room.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.CancelReservationDAO;
import com.mgm.services.booking.room.model.request.CancelRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Unit test class for service methods in CancelService.
 *
 */
@RunWith(MockitoJUnitRunner.class)

public class CancelServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private CancelReservationDAO cancelDao;

    @InjectMocks
    CancelServiceImpl cancelServiceImpl;

    private RoomReservation getCancelResponse(String fileName) {
        File file = new File(getClass().getResource(fileName).getPath());

        return convert(file, RoomReservation.class);

    }

    /**
     * Test cancelReservation for success.
     */
    @Test
    public void cancelReservationSuccessTest() {

        when(cancelDao.cancelReservation(Mockito.any(CancelRequest.class)))
                .thenReturn(getCancelResponse("/cancelled-reservation.json"));
        CancelRequest cancelRequest = new CancelRequest();
        cancelRequest.setConfirmationNumber("M00AE5151");
        cancelRequest.setFirstName("Ravi Kiran");
        cancelRequest.setLastName("Ganji");

        RoomReservation response = cancelServiceImpl.cancelReservation(cancelRequest);

        assertEquals("M00AE5151", response.getConfirmationNumber());
        assertEquals("Cancelled", response.getState().toString());

    }

    /**
     * Test cancelReservation for reservation not found scenario.
     */
    @Test
    public void cancelReservationFailedTest() {

        when(cancelDao.cancelReservation(Mockito.any(CancelRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        CancelRequest cancelRequest = new CancelRequest();
        cancelRequest.setConfirmationNumber("Foobar");
        cancelRequest.setFirstName("Ravi Kiran");
        cancelRequest.setLastName("Ganji");

        assertThatThrownBy(() -> cancelServiceImpl.cancelReservation(cancelRequest))
                .isInstanceOf(BusinessException.class).hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }

}
