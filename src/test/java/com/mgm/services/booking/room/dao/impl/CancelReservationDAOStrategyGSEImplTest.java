package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.request.CancelRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgmresorts.aurora.messages.CancelRoomReservationResponse;
import com.mgmresorts.aurora.messages.GetCustomerItineraryByRoomConfirmationNumberResponse;
import com.mgmresorts.aurora.service.Client;

/**
 * Unit test class for CancelReservationDAOImpl to validation and cancel
 * functionality.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CancelReservationDAOStrategyGSEImplTest extends BaseRoomBookingTest {

    private static Client auroraClient;

    private static CancelReservationDAOStrategyGSEImpl cancelDaoImpl;

    /**
     * Return itinerary with a room reservation from JSON mock file.
     */
    private GetCustomerItineraryByRoomConfirmationNumberResponse getItinerary() {
        File file = new File(getClass().getResource("/itinerary.json").getPath());

        return convert(file, GetCustomerItineraryByRoomConfirmationNumberResponse.class);

    }

    /**
     * Return itinerary with a cancelled room reservation from JSON mock file.
     */
    private CancelRoomReservationResponse getCancelledItinerary() {
        File file = new File(getClass().getResource("/itinerary-cancelled.json").getPath());

        return convert(file, CancelRoomReservationResponse.class);

    }

    @BeforeClass
    public static void runOnceBeforeCancel() {
        auroraClient = Mockito.mock(Client.class);

        cancelDaoImpl = Mockito.spy(CancelReservationDAOStrategyGSEImpl.class);

        Mockito.doReturn(auroraClient).when(cancelDaoImpl).getAuroraClient(Mockito.anyString());

    }

    /**
     * Test cancel room reservation when confirmation number is incorrect.
     */
    @Test
    public void cancelReservationNotFoundTest() {

        when(auroraClient.getCustomerItineraryByRoomConfirmationNumber(Mockito.any())).thenReturn(getItinerary());

        CancelRequest cancelRequest = new CancelRequest();
        cancelRequest.setSource("mgmresorts");
        cancelRequest.setConfirmationNumber("M00AE5151AB");
        cancelRequest.setFirstName("Test");
        cancelRequest.setLastName("Test");

        assertThatThrownBy(()-> cancelDaoImpl.cancelReservation(cancelRequest, "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
    	.isInstanceOf(BusinessException.class)
    	.hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }

    /**
     * Test cancel room reservation when first name doesn't match
     */
    @Test
    public void cancelReservationFirstNameNotMatchTest() {

        when(auroraClient.getCustomerItineraryByRoomConfirmationNumber(Mockito.any())).thenReturn(getItinerary());

        CancelRequest cancelRequest = new CancelRequest();
        cancelRequest.setSource("mgmresorts");
        cancelRequest.setConfirmationNumber("M00AE5151");
        cancelRequest.setFirstName("Test");
        cancelRequest.setLastName("Ganji");

        assertThatThrownBy(()-> cancelDaoImpl.cancelReservation(cancelRequest, "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
    	.isInstanceOf(BusinessException.class)
    	.hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }

    /**
     * Test cancel room reservation when last name doesn't match
     */
    @Test
    public void cancelReservationLastNameNotMatchTest() {

        when(auroraClient.getCustomerItineraryByRoomConfirmationNumber(Mockito.any())).thenReturn(getItinerary());

        CancelRequest cancelRequest = new CancelRequest();
        cancelRequest.setSource("mgmresorts");
        cancelRequest.setConfirmationNumber("M00AE5151");
        cancelRequest.setFirstName("Ravi Kiran");
        cancelRequest.setLastName("Test");

        assertThatThrownBy(()-> cancelDaoImpl.cancelReservation(cancelRequest, "66964e2b-2550-4476-84c3-1a4c0c5c067f"))
    	.isInstanceOf(BusinessException.class)
    	.hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
        
    }

    /**
     * Test successful reservation cancellation and assert the state and
     * confirmation number
     */
    @Test
    public void cancelReservationSuccessTest() {

        when(auroraClient.getCustomerItineraryByRoomConfirmationNumber(Mockito.any())).thenReturn(getItinerary());
        when(auroraClient.cancelRoomReservation(Mockito.any())).thenReturn(getCancelledItinerary());

        CancelRequest cancelRequest = new CancelRequest();
        cancelRequest.setSource("mgmresorts");
        cancelRequest.setConfirmationNumber("M00AE5151");
        cancelRequest.setFirstName("Ravi Kiran");
        cancelRequest.setLastName("Ganji");

        RoomReservation response = cancelDaoImpl.cancelReservation(cancelRequest, "66964e2b-2550-4476-84c3-1a4c0c5c067f");
        assertEquals("Status should be cancelled", "Cancelled", response.getState().toString());
        assertEquals("Confirmation number should match", "M00AE5151", response.getConfirmationNumber());

    }

}
