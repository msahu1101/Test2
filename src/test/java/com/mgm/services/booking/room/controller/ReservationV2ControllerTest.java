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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.HibernateValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.CreatePartyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.CreateRoomReservationRequest;
import com.mgm.services.booking.room.model.reservation.AgentInfo;
import com.mgm.services.booking.room.model.response.CreatePartyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.CreateRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.service.IATAV2Service;
import com.mgm.services.booking.room.service.ReservationService;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the CreateRoomReservationRequest request object
 * with various params.
 * 
 * @author vararora
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ReservationV2ControllerTest extends BaseRoomBookingTest {

    @InjectMocks
    private ReservationV2Controller reservationV2Controller;

    @Mock
    private ReservationService reservationService;

    @Mock
    private IATAV2Service iataService;
    
    @Mock
    private TokenValidator tokenValidator;

    private LocalValidatorFactoryBean localValidatorFactory;
    
    @Mock
    Validator reservationRequestvalidator;

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
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setCustomerId(0);
        runTest(request, ErrorCode.INVALID_CUSTOMER_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingItineraryId() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setItineraryId(null);
        runTest(request, ErrorCode.INVALID_ITINERARY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyItineraryId() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setItineraryId("");
        runTest(request, ErrorCode.INVALID_ITINERARY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingPropertyId() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId(null);
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPropertyId() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId("");
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPropertyId() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId("111122223333");
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingRoomType() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId(null);
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyRoomType() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId("");
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidRoomType() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId("111122223333");
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingBookingDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setBookings(null);
        runTest(request, ErrorCode.INVALID_BOOKINGS);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingBillingDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setBilling(null);
        runTest(request, ErrorCode.INVALID_BILLING);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingTripDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setTripDetails(null);
        runTest(request, ErrorCode.INVALID_TRIP_DETAILS);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setProfile(null);
        runTest(request, ErrorCode.INVALID_PROFILE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidEmailInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setEmailAddress1("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidEmail2InProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setEmailAddress2("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingFirstNameInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setFirstName(null);
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidFirstNameInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setFirstName("First^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingLastNameInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setLastName(null);
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidLastNameInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setLastName("Last^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidAgeInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setDateOfBirth(new Date());
        runTest(request, ErrorCode.INVALID_AGE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingPhoneNumberInListInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber(null);
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPhoneNumberInListInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber("");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPhoneNumberInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber("abc");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1AddressInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet1("Street^1");
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1LengthInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet1(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet2AddressInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet2(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCityInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setCity("Invalid^City^Name");
        runTest(request, ErrorCode.INVALID_CITY);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStateInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setState("abc");
        runTest(request, ErrorCode.INVALID_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPostalCodeInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setPostalCode("");
        runTest(request, ErrorCode.INVALID_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPostalCodeInProfile() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setPostalCode("112233");
        runTest(request, ErrorCode.INVALID_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingAddressInBilling() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).setAddress(null);
        runTest(request, ErrorCode.INVALID_ADDRESS);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1AddressInBilling() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet1("Street^1");
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1LengthInBilling() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet1(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet2AddressInBilling() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet2(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCityInBilling() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setCity("Invalid^City^Name");
        runTest(request, ErrorCode.INVALID_BILLING_CITY);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStateInBilling() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setState("abc");
        runTest(request, ErrorCode.INVALID_BILLING_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPostalCodeInBilling() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setPostalCode("112233");
        runTest(request, ErrorCode.INVALID_BILLING_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCcTokenInBillingPayment() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken(null);
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyCcTokenInBillingPayment() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken("");
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCcTokenInBillingPayment() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken("abc");
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCardNumberInBillingPayment() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCardNumber("123");
        runTest(request, ErrorCode.INVALID_CARD);
    }


    @Test
    public void shouldReturnErrorMessageOnInvalidCardHolderInBillingPayment() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCardHolder("Invalid^Card^Holder");
        runTest(request, ErrorCode.INVALID_CARDHOLDER);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCvvInBillingPayment() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCvv("abc");
        runTest(request, ErrorCode.INVALID_CVV);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCheckinDateInTripDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setCheckInDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidDatesInTripDetails() throws ParseException {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails()
                .setCheckInDate(new SimpleDateFormat("yyyy-MM-dd").parse("2020-12-02"));
        request.getRoomReservation().getTripDetails()
                .setCheckInDate(new SimpleDateFormat("yyyy-MM-dd").parse("2020-12-01"));
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCheckoutDateInTripDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setCheckOutDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidNumAdultsInTripDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumAdults(0);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void shouldReturnErrorMessageOnNegativeNumAdultsInTripDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumAdults(-1);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }
    
    @Test
    public void shouldReturnErrorMessageOnInvalidNumRoomsInTripDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumRooms(0);
        runTest(request, ErrorCode.INVALID_NUM_ROOMS);
    }
    
    @Test
    public void shouldReturnErrorMessageOnInvalidNumRoomsMoreThanOneInTripDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumRooms(2);
        runTest(request, ErrorCode.INVALID_NUM_ROOMS);
    }

    @Test
    public void shouldReturnErrorMessageOnNegativeNumRoomsInTripDetails() {
        CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumRooms(-1);
        runTest(request, ErrorCode.INVALID_NUM_ROOMS);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCustomerIdForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setCustomerId(0);
        runTest(request, ErrorCode.INVALID_CUSTOMER_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingItineraryIdForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setItineraryId(null);
        runTest(request, ErrorCode.INVALID_ITINERARY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyItineraryIdForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setItineraryId("");
        runTest(request, ErrorCode.INVALID_ITINERARY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingPropertyIdForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId(null);
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPropertyIdForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId("");
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPropertyIdForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId("111122223333");
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingRoomTypeForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId(null);
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyRoomTypeForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId("");
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidRoomTypeForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId("111122223333");
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingBookingDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setBookings(null);
        runTest(request, ErrorCode.INVALID_BOOKINGS);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingBillingDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setBilling(null);
        runTest(request, ErrorCode.INVALID_BILLING);
    }
    
    @Test
    public void shouldReturnErrorMessageOnMultipleBillingDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().add(request.getRoomReservation().getBilling().get(0));
        runTest(request, ErrorCode.INVALID_BILLING_MULTI_CARDS);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingTripDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setTripDetails(null);
        runTest(request, ErrorCode.INVALID_TRIP_DETAILS);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidEmailInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setEmailAddress1("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidEmail2InProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setEmailAddress2("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidFirstNameInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setFirstName("First^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidLastNameInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setLastName("Last^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidAgeInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setDateOfBirth(new Date());
        runTest(request, ErrorCode.INVALID_AGE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingPhoneNumberInListInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber(null);
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPhoneNumberInListInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber("");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPhoneNumberInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber("abc");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1AddressInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet1("Street^1");
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1LengthInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet1(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet2AddressInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet2(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCityInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setCity("Invalid^City^Name");
        runTest(request, ErrorCode.INVALID_CITY);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStateInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setState("abc");
        runTest(request, ErrorCode.INVALID_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPostalCodeInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setPostalCode("");
        runTest(request, ErrorCode.INVALID_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPostalCodeInProfileForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setPostalCode("112233");
        runTest(request, ErrorCode.INVALID_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingAddressInBillingForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).setAddress(null);
        runTest(request, ErrorCode.INVALID_ADDRESS);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1AddressInBillingForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet1("Street^1");
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1LengthInBillingForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet1(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet2AddressInBillingForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet2(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCityInBillingForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setCity("Invalid^City^Name");
        runTest(request, ErrorCode.INVALID_BILLING_CITY);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStateInBillingForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setState("abc");
        runTest(request, ErrorCode.INVALID_BILLING_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPostalCodeInBillingForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setPostalCode("112233");
        runTest(request, ErrorCode.INVALID_BILLING_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCcTokenInBillingPaymentForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken(null);
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyCcTokenInBillingPaymentForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken("");
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCcTokenInBillingPaymentForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken("abc");
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCardNumberInBillingPaymentForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCardNumber("123");
        runTest(request, ErrorCode.INVALID_CARD);
    }


    @Test
    public void shouldReturnErrorMessageOnInvalidCardHolderInBillingPaymentForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCardHolder("Invalid^Card^Holder");
        runTest(request, ErrorCode.INVALID_CARDHOLDER);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCvvInBillingPaymentForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCvv("abc");
        runTest(request, ErrorCode.INVALID_CVV);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCheckinDateInTripDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setCheckInDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidDatesInTripDetailsForPartyReservation() throws ParseException {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails()
                .setCheckInDate(new SimpleDateFormat("yyyy-MM-dd").parse("2020-12-02"));
        request.getRoomReservation().getTripDetails()
                .setCheckInDate(new SimpleDateFormat("yyyy-MM-dd").parse("2020-12-01"));
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCheckoutDateInTripDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setCheckOutDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidNumAdultsInTripDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumAdults(0);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void shouldReturnErrorMessageOnNegativeNumAdultsInTripDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumAdults(-1);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidNumRoomsInTripDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumRooms(0);
        runTest(request, ErrorCode.INVALID_NUM_ROOMS);
    }

    @Test
    public void shouldReturnErrorMessageOnNegativeNumRoomsInTripDetailsForPartyReservation() {
        CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumRooms(-1);
        runTest(request, ErrorCode.INVALID_NUM_ROOMS);
    }

    private CreateRoomReservationRequest loadRequest(String scenario) {
        return convert(new File(getClass().getResource(String.format("/roomReservation-%s.json", scenario)).getPath()),
                CreateRoomReservationRequest.class);
    }

    private void runTest(CreateRoomReservationRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CreateRoomReservationRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            reservationV2Controller.reservation(TestConstant.ICE, "true", request, mockRequest);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }

    private CreatePartyRoomReservationRequest loadPartyReservationRequest(String scenario) {
        return convert(
                new File(getClass().getResource(String.format("/partyRoomReservation-%s.json", scenario)).getPath()),
                CreatePartyRoomReservationRequest.class);
    }

    private void runTest(CreatePartyRoomReservationRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CreatePartyRoomReservationRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            reservationV2Controller.partyReservation(TestConstant.ICE, TestConstant.TRUE_STRING, request, mockRequest);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void runPartyReservationTest() {
    	CreatePartyRoomReservationRequest request = loadPartyReservationRequest(VALID_REQUEST_SCENARIO);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CreatePartyRoomReservationRequest");
        localValidatorFactory.validate(request, result);
        CreatePartyRoomReservationResponse createPartyRoomReservationResponse=new CreatePartyRoomReservationResponse();
        List<RoomReservationV2Response> roomReservationV2ResponseList=new ArrayList<>();
        RoomReservationV2Response roomReservationV2Response=new RoomReservationV2Response();
        roomReservationV2Response.setAmountDue(10.10);
        roomReservationV2ResponseList.add(roomReservationV2Response);
        createPartyRoomReservationResponse.setRoomReservations(roomReservationV2ResponseList);
        Mockito.when(reservationService.makePartyRoomReservation(Mockito.any(), Mockito.any())).thenReturn(createPartyRoomReservationResponse);
        CreatePartyRoomReservationResponse response=reservationV2Controller
        		.partyReservation(TestConstant.ICE, TestConstant.TRUE_STRING, request, mockRequest);
        assertEquals(createPartyRoomReservationResponse,response);
    }
    
    @Test
    public void runTestPositive() {
    	CreateRoomReservationRequest request = loadRequest(VALID_REQUEST_SCENARIO);
    	request.getRoomReservation().setMyVegasPromoCode("123");
    	AgentInfo agentInfo=new AgentInfo();
    	agentInfo.setAgentType("IATA");
    	request.getRoomReservation().setAgentInfo(null);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "CreateRoomReservationRequest");
        localValidatorFactory.validate(request, result);
        CreateRoomReservationResponse createRoomReservationResponse=new CreateRoomReservationResponse();
        RoomReservationV2Response roomReservationV2Response=new RoomReservationV2Response();
        roomReservationV2Response.setAmountDue(10.10);
        createRoomReservationResponse.setRoomReservation(roomReservationV2Response);
        Mockito.when(reservationService.makeRoomReservationV2(Mockito.any(), Mockito.any())).thenReturn(createRoomReservationResponse);
        CreateRoomReservationResponse response=reservationV2Controller.reservation(TestConstant.ICE, "true", request, mockRequest);
        assertEquals(createRoomReservationResponse,response);
    }

}
