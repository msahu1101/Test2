package com.mgm.services.booking.room.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.RoomReservationBasicInfoRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ReservationsBasicInfoResponse;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

/**
 * Unit test class for service methods in FindReservationService.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class FindReservationServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private FindReservationDAO findReservationDAO;

    @InjectMocks
    FindReservationServiceImpl findReservationServiceImpl;

    @SuppressWarnings("hiding")
    private <T> T getObject(String fileName, Class<T> target) {
        File file = new File(getClass().getResource(fileName).getPath());

        return convert(file, target);

    }

    /**
     * Test findRoomReservation service for success.
     */
    @Test
    public void findReservationSuccessTest() {

        when(findReservationDAO.findRoomReservation(Mockito.any(FindReservationRequest.class)))
                .thenReturn(getObject("/findreservation-dao-response.json", RoomReservation.class));

        FindReservationRequest reservationRequest = new FindReservationRequest();
        reservationRequest.setConfirmationNumber("M00AE44F1");
        reservationRequest.setFirstName("Test");
        reservationRequest.setLastName("Test");

        RoomReservation response = findReservationServiceImpl.findRoomReservation(reservationRequest);

        assertEquals("M00AE44F1", response.getConfirmationNumber());
        assertEquals("Cancelled", response.getState().toString());

    }

    /**
     * Test Cancel Validate as it uses same findRoomReservation method of
     * FindReservationServiceImpl
     */
    @Test
    public void cancelValidateSuccessTest() {

        when(findReservationDAO.findRoomReservation(Mockito.any(FindReservationRequest.class)))
                .thenReturn(getObject("/findreservation-dao-response.json", RoomReservation.class));

        FindReservationRequest reservationRequest = new FindReservationRequest();
        reservationRequest.setConfirmationNumber("M00AE44F1");
        reservationRequest.setFirstName("Test");
        reservationRequest.setLastName("Test");
        ApplicationProperties appProperties = new ApplicationProperties();
        Map<String, String> timezone = new HashMap<>();
        timezone.put("default", "America/Los_Angeles");
        appProperties.setTimezone(timezone);

        RoomReservation response = findReservationServiceImpl.findRoomReservation(reservationRequest);

        RoomReservationResponse roomReservation = RoomReservationTransformer.transform(response, appProperties);

        Assert.assertNotNull(response.getDepositCalc().getForfeitDate().toInstant());
        Assert.assertNotNull(roomReservation.getRates().getReservationTotal());
        Assert.assertNotNull(response.getDepositCalc().getAmount());

    }

    /**
     * Test reservation not found scenario.
     */
    @Test
    public void findReservationInvalidReservationTest() {
        when(findReservationDAO.findRoomReservation(Mockito.any(FindReservationRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        FindReservationRequest reservationRequest = new FindReservationRequest();
        reservationRequest.setConfirmationNumber("M00AE44F1");
        reservationRequest.setFirstName("Test");
        reservationRequest.setLastName("Test");

        assertThatThrownBy(() -> findReservationServiceImpl.findRoomReservation(reservationRequest))
                .isInstanceOf(BusinessException.class).hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));

    }

    @Test
    public void getReservationBasicInfoList_SuccessfulValidation() {
        when(findReservationDAO.getRoomReservationsBasicInfoList(Mockito.any())).thenReturn(getObject(
                "/reservationsBasicInfo-success.json", ReservationsBasicInfoResponse.class));
        RoomReservationBasicInfoRequest request = new RoomReservationBasicInfoRequest();
        request.setConfirmationNumber("797447927");
        ReservationsBasicInfoResponse response = findReservationServiceImpl.getReservationBasicInfoList(request);
        assertFalse(response.getReservationAdditionalInfo().isEmpty());
        assertTrue(response.getReservationAdditionalInfo().get(0).getPrimarySharerConfNo().equals("797447927"));
    }

    @Test
    public void getReservationBasicInfoList_ValidateReservationNotFound() {
        when(findReservationDAO.getRoomReservationsBasicInfoList(Mockito.any()))
                .thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        RoomReservationBasicInfoRequest request = new RoomReservationBasicInfoRequest();
        request.setConfirmationNumber("797447927");
        assertThatThrownBy(() -> findReservationServiceImpl.getReservationBasicInfoList(request))
                .isInstanceOf(BusinessException.class).hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }
}
