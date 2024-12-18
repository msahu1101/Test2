package com.mgm.services.booking.room.dao.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgmresorts.aurora.messages.GetCustomerItineraryByRoomConfirmationNumberResponse;
import com.mgmresorts.aurora.service.Client;

public class FindReservationDAOStrategyGSEImplTest extends BaseRoomBookingTest {

    private static Client auroraClient;

    private static FindReservationDAOStrategyGSEImpl findDaoImpl;
    

    /**
     * Return itinerary with a room reservation from JSON mock file.
     */
    private GetCustomerItineraryByRoomConfirmationNumberResponse getItinerary() {
        File file = new File(getClass().getResource("/itinerary.json").getPath());

        return convert(file, GetCustomerItineraryByRoomConfirmationNumberResponse.class);

    }

    @BeforeClass
    public static void runOnceBeforeCancel() {
        auroraClient = Mockito.mock(Client.class);

        findDaoImpl = Mockito.spy(FindReservationDAOStrategyGSEImpl.class);

        Mockito.doReturn(auroraClient).when(findDaoImpl).getAuroraClient(Mockito.anyString());

    }

    /**
     * Test find room reservation when confirmation number is incorrect.
     */
    @Test
    public void findReservationNotFoundTest() {

        when(auroraClient.getCustomerItineraryByRoomConfirmationNumber(Mockito.any())).thenReturn(getItinerary());

        FindReservationRequest request = new FindReservationRequest();
        request.setSource("mgmresorts");
        request.setConfirmationNumber("M00AE5151AB");
        request.setFirstName("Test");
        request.setLastName("Test");

        assertThatThrownBy(()-> findDaoImpl.findRoomReservation(request))
    	.isInstanceOf(BusinessException.class)
    	.hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }

    /**
     * Test find room reservation when first name doesn't match
     */
    @Test
    public void findReservationFirstNameNotMatchTest() {

        when(auroraClient.getCustomerItineraryByRoomConfirmationNumber(Mockito.any())).thenReturn(getItinerary());

        FindReservationRequest request = new FindReservationRequest();
        request.setSource("mgmresorts");
        request.setConfirmationNumber("M00AE5151");
        request.setFirstName("Test");
        request.setLastName("Ganji");
        
        assertThatThrownBy(()-> findDaoImpl.findRoomReservation(request))
    	.isInstanceOf(BusinessException.class)
    	.hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }

    /**
     * Test find room reservation when last name doesn't match
     */
    @Test
    public void findReservationLastNameNotMatchTest() {

        when(auroraClient.getCustomerItineraryByRoomConfirmationNumber(Mockito.any())).thenReturn(getItinerary());

        FindReservationRequest request = new FindReservationRequest();
        request.setSource("mgmresorts");
        request.setConfirmationNumber("M00AE5151");
        request.setFirstName("Ravi Kiran");
        request.setLastName("Test");

        assertThatThrownBy(()-> findDaoImpl.findRoomReservation(request))
    	.isInstanceOf(BusinessException.class)
    	.hasMessage(getErrorMessage(ErrorCode.RESERVATION_NOT_FOUND));
    }

    /**
     * Test successful reservation retrieval and assert the state and
     * confirmation number
     */
    @Test
    public void cancelReservationSuccessTest() {

        when(auroraClient.getCustomerItineraryByRoomConfirmationNumber(Mockito.any())).thenReturn(getItinerary());

        FindReservationRequest request = new FindReservationRequest();
        request.setSource("mgmresorts");
        request.setConfirmationNumber("M00AE5151");
        request.setFirstName("Ravi Kiran");
        request.setLastName("Ganji");

        RoomReservation response = findDaoImpl.findRoomReservation(request);
        assertEquals("Status should be booked", "Booked", response.getState().toString());
        assertEquals("Confirmation number should match", "M00AE5151", response.getConfirmationNumber());

    }
}
