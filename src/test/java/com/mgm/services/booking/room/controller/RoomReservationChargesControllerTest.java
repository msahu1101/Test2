package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.RoomReservationChargesRequest;
import com.mgm.services.booking.room.model.response.RoomReservationChargesResponse;
import com.mgm.services.booking.room.service.RoomReservationChargesService;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the request object with various param.
 * 
 * @author jayveera
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RoomReservationChargesControllerTest extends BaseRoomBookingTest {

    @InjectMocks
    private RoomReservationChargesController roomReservationChargesController;
    
    @Mock
    private TokenValidator tokenValidator;
    
    @Mock
    RoomReservationChargesService roomRervationChargesService;

    @Test
    public void calculateRoomReservationCharges_WithoutPropertyId_validateErrorMessage() {
        RoomReservationChargesRequest request = loadRequest();
        request.setPropertyId(null);
        runTest(request, ErrorCode.INVALID_PROPERTY);
    }

    @Test
    public void calculateRoomReservationCharges_WithInvalidPropertyId_validateErrorMessage() {
        RoomReservationChargesRequest request = loadRequest();
        request.setPropertyId("ABC123");
        runTest(request, ErrorCode.INVALID_PROPERTY);
    }

    
    @Test
    public void calculateRoomReservationCharges_WithoutRoomTypeId_validateErrorMessage() {
        RoomReservationChargesRequest request = loadRequest();
        request.setRoomTypeId(null);
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void calculateRoomReservationCharges_WithInvalidRoomTypeId_validateErrorMessage() {
        RoomReservationChargesRequest request = loadRequest();
        request.setRoomTypeId("ABC123");
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void calculateRoomReservationCharges_WithoutNumAdults_validateErrorMessage() {
        RoomReservationChargesRequest request = loadRequest();
        request.getTripDetails().setNumAdults(0);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void calculateRoomReservationCharges_WithoutBooking_validateErrorMessage() {
        RoomReservationChargesRequest request = loadRequest();
        request.setBookings(null);
        runTest(request, ErrorCode.INVALID_BOOKINGS);
    }

    @Test
    public void calculateRoomReservationCharges_WithoutCheckInDateInTripDetails_validateErrorMessage() {
        RoomReservationChargesRequest request = loadRequest();
        request.getTripDetails().setCheckInDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }
    
    @Test
    public void calculateRoomReservationCharges_WithInvalidCheckOutDateInTripDetails_validateErrorMessage() {
        RoomReservationChargesRequest request = loadRequest();
        request.getTripDetails().setCheckOutDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void calculateRoomReservationCharges_WithInvalidTripDate_validateErrorMessage() {
        RoomReservationChargesRequest request = loadRequest();
        request.getTripDetails().setCheckInDate(getFutureDate(45));
        request.getTripDetails().setCheckOutDate(getFutureDate(43));
        runTest(request, ErrorCode.INVALID_DATES);
    }
    
    private RoomReservationChargesRequest loadRequest() {
        return convert(new File(getClass().getResource("/roomreservationchargesrequest-basic.json").getPath()),
                RoomReservationChargesRequest.class);
    }

    private void runTest(RoomReservationChargesRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        boolean validationExceptionOccured = false;

        try {
            roomReservationChargesController.calculateRoomReservationCharges(null, TestConstant.ICE, TestConstant.ICE,
                    request, mockRequest);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void calculateRoomReservationChargesTest() {
    	RoomReservationChargesRequest request = loadRequest();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        RoomReservationChargesResponse roomReservationChargesResponse=new RoomReservationChargesResponse();
        roomReservationChargesResponse.setAmountDue(10.10);
        Mockito.when(roomRervationChargesService.calculateRoomReservationCharges(request)).thenReturn(roomReservationChargesResponse);
        RoomReservationChargesResponse response=roomReservationChargesController.calculateRoomReservationCharges(null, TestConstant.ICE, TestConstant.ICE,
                    request, mockRequest);
        assertEquals(roomReservationChargesResponse,response);
    }
}
