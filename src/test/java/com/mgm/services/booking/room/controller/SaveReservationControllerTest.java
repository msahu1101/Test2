/**
 * 
 */
package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.HibernateValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.SaveReservationRequest;
import com.mgm.services.booking.room.service.ReservationService;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the SaveReservationRequest request object
 * with various params.
 * 
 * @author vararora
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SaveReservationControllerTest extends BaseRoomBookingTest {

    @InjectMocks
    private SaveReservationController saveReservationV2Controller;

    @Mock
    private ReservationService reservationService;
    
    @Mock
    private TokenValidator tokenValidator;

    private LocalValidatorFactoryBean localValidatorFactory;

    private static final String VALID_REQUEST_SCENARIO = "validRequest";
    private static final String INVALID_STREET_ADDRESS = "aaaaabbbbbcccccdddddeeeeefffffgggggaaaaabbbbbcccccdddddeeeeeffffff";

    @Before
    public void setUp() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @After
    public void tearDown() {
        localValidatorFactory = null;
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCustomerId() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setCustomerId(0);
        runTest(request, ErrorCode.INVALID_CUSTOMER_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingItineraryId() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setItineraryId(null);
        runTest(request, ErrorCode.INVALID_ITINERARY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyItineraryId() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setItineraryId("");
        runTest(request, ErrorCode.INVALID_ITINERARY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingPropertyId() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId(null);
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPropertyId() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId("");
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPropertyId() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId("111122223333");
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingRoomType() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId(null);
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyRoomType() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId("");
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidRoomType() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId("111122223333");
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingBookingDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setBookings(null);
        runTest(request, ErrorCode.INVALID_BOOKINGS);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingBillingDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setBilling(null);
        runTest(request, ErrorCode.INVALID_BILLING);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingTripDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setTripDetails(null);
        runTest(request, ErrorCode.INVALID_TRIP_DETAILS);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidEmailInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setEmailAddress1("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidEmail2InProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setEmailAddress2("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidFirstNameInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setFirstName("First^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidLastNameInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setLastName("Last^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidAgeInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setDateOfBirth(new Date());
        runTest(request, ErrorCode.INVALID_AGE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingPhoneNumberInListInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber(null);
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPhoneNumberInListInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber("");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPhoneNumberInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber("abc");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1AddressInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet1("Street^1");
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1LengthInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet1(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet2AddressInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet2(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCityInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setCity("Invalid^City^Name");
        runTest(request, ErrorCode.INVALID_CITY);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStateInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setState("abc");
        runTest(request, ErrorCode.INVALID_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPostalCodeInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setPostalCode("");
        runTest(request, ErrorCode.INVALID_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPostalCodeInProfile() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setPostalCode("112233");
        runTest(request, ErrorCode.INVALID_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingAddressInBilling() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).setAddress(null);
        runTest(request, ErrorCode.INVALID_ADDRESS);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1AddressInBilling() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet1("Street^1");
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1LengthInBilling() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet1(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet2AddressInBilling() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet2(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCityInBilling() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setCity("Invalid^City^Name");
        runTest(request, ErrorCode.INVALID_BILLING_CITY);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStateInBilling() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setState("abc");
        runTest(request, ErrorCode.INVALID_BILLING_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPostalCodeInBilling() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setPostalCode("112233");
        runTest(request, ErrorCode.INVALID_BILLING_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCcTokenInBillingPayment() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken(null);
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyCcTokenInBillingPayment() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken("");
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCcTokenInBillingPayment() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken("abc");
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCardNumberInBillingPayment() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCardNumber("123");
        runTest(request, ErrorCode.INVALID_CARD);
    }


    @Test
    public void shouldReturnErrorMessageOnInvalidCardHolderInBillingPayment() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCardHolder("Invalid^Card^Holder");
        runTest(request, ErrorCode.INVALID_CARDHOLDER);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCvvInBillingPayment() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCvv("abc");
        runTest(request, ErrorCode.INVALID_CVV);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCheckinDateInTripDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setCheckInDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidDatesInTripDetails() throws ParseException {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails()
                .setCheckInDate(new SimpleDateFormat("yyyy-MM-dd").parse("2022-12-02"));
        request.getRoomReservation().getTripDetails()
                .setCheckInDate(new SimpleDateFormat("yyyy-MM-dd").parse("2022-12-01"));
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCheckoutDateInTripDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setCheckOutDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidNumAdultsInTripDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumAdults(0);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void shouldReturnErrorMessageOnNegativeNumAdultsInTripDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumAdults(-1);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }
    
    @Test
    public void shouldReturnErrorMessageOnInvalidNumRoomsInTripDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumRooms(0);
        runTest(request, ErrorCode.INVALID_NUM_ROOMS);
    }
    
    @Test
    public void shouldReturnErrorMessageOnInvalidNumRoomsMoreThanOneInTripDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumRooms(2);
        runTest(request, ErrorCode.INVALID_NUM_ROOMS);
    }

    @Test
    public void shouldReturnErrorMessageOnNegativeNumRoomsInTripDetails() {
        SaveReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumRooms(-1);
        runTest(request, ErrorCode.INVALID_NUM_ROOMS);
    }

    private SaveReservationRequest loadRequest(String scenario) {
        return convert(new File(getClass().getResource(String.format("/roomReservation-%s.json", scenario)).getPath()),
                SaveReservationRequest.class);
    }

    private void runTest(SaveReservationRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "SaveReservationRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            saveReservationV2Controller.saveReservation(TestConstant.ICE, request, mockRequest);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
}
