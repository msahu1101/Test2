package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.ModifyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.PaymentRoomReservationRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.PreviewCommitRequest;
import com.mgm.services.booking.room.model.request.ReservationAssociateRequest;
import com.mgm.services.booking.room.model.request.UpdateProfileInfoRequest;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.response.ModifyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.model.response.UpdateProfileInfoResponse;
import com.mgm.services.booking.room.service.ModifyReservationService;
import com.mgm.services.booking.room.validator.PreModifyRequestV2Validator;
import com.mgm.services.booking.room.validator.PreviewCommitRequestValidator;
import com.mgm.services.booking.room.validator.ReservationAssociateRequestValidator;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.booking.room.validator.helper.ValidationHelper;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the request object with various param.
 * 
 * @author jayveera
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ModifyV2ControllerTest extends BaseRoomBookingTest {

    @InjectMocks
    private ModifyV2Controller modifyV2Controller;
    
    @Mock
    private TokenValidator tokenValidator;

    @Mock
    private ValidationHelper helper;

    @InjectMocks
    private PreModifyRequestV2Validator preModifyRequestV2Validator;

    @InjectMocks
    private PreviewCommitRequestValidator previewCommitRequestValidator;

    @InjectMocks
    private ReservationAssociateRequestValidator associationRequestValidator;
    
    @Mock
    ModifyReservationService modifyReservationService;

    private LocalValidatorFactoryBean localValidatorFactory;

    private static final String VALID_REQUEST_SCENARIO = "request";

    private static final String INVALID_STREET_ADDRESS = "aaaaabbbbbcccccdddddeeeeefffffgggggaaaaabbbbbcccccdddddeeeeeffffff";

    @Before
    public void setUp() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ReflectionTestUtils.setField(modifyV2Controller, "preModifyRequestValidator", preModifyRequestV2Validator);
        ReflectionTestUtils.setField(modifyV2Controller, "commitValidator", previewCommitRequestValidator);
        ReflectionTestUtils.setField(modifyV2Controller, "associationRequestValidator", associationRequestValidator);
    }
    private <T> T getObject(String fileName, Class<T> target) {
		File file = new File(getClass().getResource(fileName).getPath());
		return convert(file, target);
	}
	private PaymentRoomReservationRequest getCommitRequestV2_ForRefundDeposit() {
		return getObject("/paymentwidgetv4/commit/refund_deposit/commit-v4-refunddeposit-request.json", PaymentRoomReservationRequest.class);
	}
    @After
    public void tearDown() {
        localValidatorFactory = null;
    }

    @Test
    public void modifyReservationUpdateProfileInfo_WithInvalidItineraryId_validateErrorMessage() {
        UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.setItineraryId(null);
        runTest(request, ErrorCode.INVALID_ITINERARY_ID);

    }

    @Test
    public void modifyReservationUpdateProfileInfo_MissingEmailAddress1_validateErrorMessage() {
        UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getUserProfile().setEmailAddress1("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void modifyReservationUpdateProfileInfo_MissingEmailAddress2_validateErrorMessage() {
        UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getUserProfile().setEmailAddress2("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void modifyReservationUpdateProfileInfo_WithInvalidFirstName_validateErrorMessage() {
        UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getUserProfile().setFirstName("First^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void modifyReservationUpdateProfileInfo_WithInvalidLastName_validateErrorMessage() {
        UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getUserProfile().setLastName("Last^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void modifyReservationUpdateProfileInfo_WithInvalidAge_validateErrorMessage() {
        UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getUserProfile().setDateOfBirth(new Date());
        runTest(request, ErrorCode.INVALID_AGE);
    }

    @Test
    public void modifyReservationUpdateProfileInfo_WithoutPhoneNumber_validateErrorMessage() {
        UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getUserProfile().getPhoneNumbers().get(0).setNumber(null);
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void modifyReservationUpdateProfileInfo_WithBlankPhoneNumber_validateErrorMessage() {
        UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getUserProfile().getPhoneNumbers().get(0).setNumber("");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void modifyReservationUpdateProfileInfo_WithInvalidPhoneNumber_validateErrorMessage() {
        UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        request.getUserProfile().getPhoneNumbers().get(0).setNumber("abc");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCustomerIdForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setCustomerId(0);
        runTest(request, ErrorCode.INVALID_CUSTOMER_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingItineraryIdForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setItineraryId(null);
        runTest(request, ErrorCode.INVALID_ITINERARY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyItineraryIdForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setItineraryId("");
        runTest(request, ErrorCode.INVALID_ITINERARY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingPropertyIdForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId(null);
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPropertyIdForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId("");
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPropertyIdForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setPropertyId("111122223333");
        runTest(request, ErrorCode.INVALID_PROPERTY_ID);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingConfirmationNumberForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setConfirmationNumber(null);
        runTest(request, ErrorCode.NO_CONFIRMATION_NUMBER);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyConfirmationNumberForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setConfirmationNumber("");
        runTest(request, ErrorCode.NO_CONFIRMATION_NUMBER);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingStateForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setState(null);
        runTest(request, ErrorCode.INVALID_BOOKING_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStateForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setState(ReservationState.Saved);
        runTest(request, ErrorCode.INVALID_BOOKING_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingRoomTypeForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId(null);
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyRoomTypeForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId("");
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidRoomTypeForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setRoomTypeId("111122223333");
        runTest(request, ErrorCode.INVALID_ROOMTYPE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingBookingDetailsForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setBookings(null);
        runTest(request, ErrorCode.INVALID_BOOKINGS);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingBillingDetailsForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setBilling(null);
        runTest(request, ErrorCode.INVALID_BILLING);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingTripDetailsForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setTripDetails(null);
        runTest(request, ErrorCode.INVALID_TRIP_DETAILS);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().setProfile(null);
        runTest(request, ErrorCode.INVALID_PROFILE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidEmailInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setEmailAddress1("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidEmail2InProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setEmailAddress2("test@test");
        runTest(request, ErrorCode.INVALID_EMAIL);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingFirstNameInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setFirstName(null);
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidFirstNameInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setFirstName("First^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingLastNameInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setLastName(null);
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidLastNameInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setLastName("Last^Name");
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidAgeInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().setDateOfBirth(new Date());
        runTest(request, ErrorCode.INVALID_AGE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingPhoneNumberInListInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber(null);
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPhoneNumberInListInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber("");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPhoneNumberInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getPhoneNumbers().get(0).setNumber("abc");
        runTest(request, ErrorCode.INVALID_PHONE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1AddressInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet1("Street^1");
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1LengthInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet1(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet2AddressInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setStreet2(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCityInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setCity("Invalid^City^Name");
        runTest(request, ErrorCode.INVALID_CITY);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStateInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setState("abc");
        runTest(request, ErrorCode.INVALID_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyPostalCodeInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setPostalCode("");
        runTest(request, ErrorCode.INVALID_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPostalCodeInProfileForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getProfile().getAddresses().get(0).setPostalCode("112233");
        runTest(request, ErrorCode.INVALID_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingAddressInBillingForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).setAddress(null);
        runTest(request, ErrorCode.INVALID_ADDRESS);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1AddressInBillingForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet1("Street^1");
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet1LengthInBillingForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet1(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStreet2AddressInBillingForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setStreet2(INVALID_STREET_ADDRESS);
        runTest(request, ErrorCode.INVALID_BILLING_STREET);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCityInBillingForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setCity("Invalid^City^Name");
        runTest(request, ErrorCode.INVALID_BILLING_CITY);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidStateInBillingForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setState("abc");
        runTest(request, ErrorCode.INVALID_BILLING_STATE);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidPostalCodeInBillingForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getAddress().setPostalCode("112233");
        runTest(request, ErrorCode.INVALID_BILLING_POSTALCODE);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCcTokenInBillingPaymentForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken(null);
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnEmptyCcTokenInBillingPaymentForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken("");
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCcTokenInBillingPaymentForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCcToken("abc");
        runTest(request, ErrorCode.INVALID_CARD);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCardNumberInBillingPaymentForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCardNumber("123");
        runTest(request, ErrorCode.INVALID_CARD);
    }



    @Test
    public void shouldReturnErrorMessageOnInvalidCardHolderInBillingPaymentForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCardHolder("Invalid^Card^Holder");
        runTest(request, ErrorCode.INVALID_CARDHOLDER);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidCvvInBillingPaymentForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getBilling().get(0).getPayment().setCvv("abc");
        runTest(request, ErrorCode.INVALID_CVV);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCheckinDateInTripDetailsForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setCheckInDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidDatesInTripDetailsForModifyReservation() throws ParseException {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails()
                .setCheckInDate(new SimpleDateFormat("yyyy-MM-dd").parse("2020-12-02"));
        request.getRoomReservation().getTripDetails()
                .setCheckInDate(new SimpleDateFormat("yyyy-MM-dd").parse("2020-12-01"));
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnMissingCheckoutDateInTripDetailsForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setCheckOutDate(null);
        runTest(request, ErrorCode.INVALID_DATES);
    }

    @Test
    public void shouldReturnErrorMessageOnInvalidNumAdultsInTripDetailsForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumAdults(0);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    @Test
    public void shouldReturnErrorMessageOnNegativeNumAdultsInTripDetailsForModifyReservation() {
        ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        request.getRoomReservation().getTripDetails().setNumAdults(-1);
        runTest(request, ErrorCode.INVALID_NUM_ADULTS);
    }

    private UpdateProfileInfoRequest loadRequest(String scenario) {
        return convert(new File(
                getClass().getResource(String.format("/modifyprofileroomreservation-%s.json", scenario)).getPath()),
                UpdateProfileInfoRequest.class);
    }

    private void runTest(UpdateProfileInfoRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "UpdateProfileInfoRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            modifyV2Controller.updateProfileInfo(TestConstant.ICE, request, result, mockRequest, null);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void updateProfileInfoTest() {
    	UpdateProfileInfoRequest request = loadRequest(VALID_REQUEST_SCENARIO);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "UpdateProfileInfoRequest");
        localValidatorFactory.validate(request, result);
        UpdateProfileInfoResponse updateProfileInfoResponse=new UpdateProfileInfoResponse();
        RoomReservationV2Response roomReservation=new RoomReservationV2Response();
        roomReservation.setAmountDue(10.10);
        updateProfileInfoResponse.setRoomReservation(roomReservation);
        Mockito.when(modifyReservationService.updateProfileInfo(Mockito.any(), Mockito.any())).thenReturn(updateProfileInfoResponse);
        UpdateProfileInfoResponse response=modifyV2Controller.updateProfileInfo(TestConstant.ICE, request, result, mockRequest, null);
        assertEquals(updateProfileInfoResponse,response);
    }

    private ModifyRoomReservationRequest loadModifyReservationRequest(String scenario) {
        return convert(
                new File(getClass().getResource(String.format("/modifyRoomReservation-%s.json", scenario)).getPath()),
                ModifyRoomReservationRequest.class);
    }
    
    @Test
    public void modifyReservationTest() {
    	 ModifyRoomReservationRequest request = loadModifyReservationRequest(VALID_REQUEST_SCENARIO);
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "ModifyRoomReservationRequest");
        localValidatorFactory.validate(request, result);
        Mockito.when(modifyReservationService.modifyRoomReservationV2(request)).thenReturn(getModifyRoomReservationResponse());
        ModifyRoomReservationResponse response= modifyV2Controller.modifyReservation(TestConstant.ICE, request, mockRequest);
        assertEquals(getModifyRoomReservationResponse(),response);
    }

    private void runTest(ModifyRoomReservationRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "ModifyRoomReservationRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            modifyV2Controller.modifyReservation(TestConstant.ICE, request, mockRequest);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }

    // preview test cases

    @Test
    public void preModifyRoom_WithNoFirstName_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreModifyV2Request request = loadPreModifyV2Request();
        request.setFirstName(null);
        runTest(request, ErrorCode.INVALID_NAME);

    }

    @Test
    public void preModifyRoom_WithNoLastName_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreModifyV2Request request = loadPreModifyV2Request();
        request.setLastName(null);
        runTest(request, ErrorCode.INVALID_NAME);

    }

    private PreModifyV2Request loadPreModifyV2Request() {
        return convert(
                new File(getClass().getResource("/preModifyV2Request.json").getPath()),
                PreModifyV2Request.class);
    }

    private void runTest(PreModifyV2Request request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "PreModifyV2Request");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            modifyV2Controller.preModifyRoom(TestConstant.MGM_RESORTS, request, mockRequest);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void preModifyRoomTest() {
    	PreModifyV2Request request = loadPreModifyV2Request();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "PreModifyV2Request");
        localValidatorFactory.validate(request, result);
        Mockito.when(modifyReservationService.preModifyReservation(request,null)).thenReturn(getModifyRoomReservationResponse());
        ModifyRoomReservationResponse response= modifyV2Controller.preModifyRoom(TestConstant.MGM_RESORTS, request, mockRequest);
        assertEquals(getModifyRoomReservationResponse(),response);
    }

	private ModifyRoomReservationResponse getModifyRoomReservationResponse() {
		ModifyRoomReservationResponse modifyRoomReservationResponse=new ModifyRoomReservationResponse();
        RoomReservationV2Response roomReservation=new RoomReservationV2Response();
        roomReservation.setAmountDue(10.10);
        modifyRoomReservationResponse.setRoomReservation(roomReservation);
		return modifyRoomReservationResponse;
	}

    // preview commit test cases

    @Test
    public void previewCommit_WithNoConfirmationNumber_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setConfirmationNumber(null);
        runTest(request, ErrorCode.NO_CONFIRMATION_NUMBER);
    }

    @Test
    public void previewCommit_WithNoPreviewReservationTotal_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setPreviewReservationTotal(null);
        runTest(request, ErrorCode.MODIFY_VIOLATION_NO_TOTALS);
    }

    @Test
    public void previewCommit_WithNoPreviewReservationDeposit_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setPreviewReservationDeposit(null);
        runTest(request, ErrorCode.MODIFY_VIOLATION_NO_TOTALS);
    }

    @Test
    public void previewCommit_WithNoFirstName_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setFirstName(null);
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void previewCommit_WithNoLastName_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setLastName(null);
        runTest(request, ErrorCode.INVALID_NAME);
    }

    @Test
    public void previewCommit_WithInvalidCvv_validateErrorMessage() {
        when(helper.hasServiceRoleAccess()).thenReturn(false);
        PreviewCommitRequest request = loadPreviewCommitRequest();
        request.setCvv("abc");
        runTest(request, ErrorCode.INVALID_CVV);
    }

    private PreviewCommitRequest loadPreviewCommitRequest() {
        return convert(new File(getClass().getResource("/previewCommitRequest.json").getPath()),
                PreviewCommitRequest.class);
    }

    private void runTest(PreviewCommitRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        BindingResult result = new DirectFieldBindingResult(request, "PreviewCommitRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            modifyV2Controller.previewCommit(TestConstant.MGM_RESORTS, request, mockRequest, mockResponse);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void previewCommitTest() {
    	PreviewCommitRequest request = loadPreviewCommitRequest();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        BindingResult result = new DirectFieldBindingResult(request, "PreviewCommitRequest");
        localValidatorFactory.validate(request, result);
        Mockito.when(modifyReservationService.commitReservation(request, null)).thenReturn(getModifyRoomReservationResponse());
        ModifyRoomReservationResponse response=modifyV2Controller.previewCommit(TestConstant.MGM_RESORTS, request, mockRequest, mockResponse);
        assertEquals(getModifyRoomReservationResponse(),response);
    }

    // associate reservation test cases

    @Test
    public void associateReservation_WithNoConfirmationNumber_validateErrorMessage() {
        when(helper.isTokenAGuestToken()).thenReturn(true);
        ReservationAssociateRequest request = loadReservationAssociateRequest();
        request.setConfirmationNumber(null);
        runTest(request, ErrorCode.ASSOCIATION_VIOLATION_NO_CONFIRMATION_NUMBER);
    }

    @Test
    public void associateReservation_WithServiceToken_validateErrorMessage() {
        when(helper.isTokenAGuestToken()).thenReturn(false);
        ReservationAssociateRequest request = loadReservationAssociateRequest();
        runTest(request, ErrorCode.SERVICE_TOKEN_NOT_SUPPORTED);
    }

    private ReservationAssociateRequest loadReservationAssociateRequest() {
        return convert(new File(getClass().getResource("/associationRequest.json").getPath()),
                ReservationAssociateRequest.class);
    }

    private void runTest(ReservationAssociateRequest request, ErrorCode errorCode) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "ReservationAssociateRequest");
        boolean validationExceptionOccured = false;

        try {
            localValidatorFactory.validate(request, result);
            modifyV2Controller.associateReservation(TestConstant.MGM_RESORTS, request, result, mockRequest);
        } catch (ValidationException e) {
            validationExceptionOccured = true;
            assertEquals("Error Code should match: ", errorCode.getErrorCode(), e.getErrorCodes().get(0));
        } finally {
            assertTrue("Controller should throw ValidationException", validationExceptionOccured);
        }
    }
    
    @Test
    public void associateReservationTest() {
    	when(helper.isTokenAGuestToken()).thenReturn(true);
    	 ReservationAssociateRequest request = loadReservationAssociateRequest();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(request, "PreviewCommitRequest");
        localValidatorFactory.validate(request, result);
        UpdateProfileInfoResponse updateProfileInfoResponse=new UpdateProfileInfoResponse();
        RoomReservationV2Response roomReservation=new RoomReservationV2Response();
        roomReservation.setAmountDue(10.10);
        updateProfileInfoResponse.setRoomReservation(roomReservation);
        Mockito.when(modifyReservationService.associateReservation(request, null)).thenReturn(updateProfileInfoResponse);
        UpdateProfileInfoResponse response=modifyV2Controller.associateReservation(TestConstant.MGM_RESORTS, request, result, mockRequest);
        assertEquals(updateProfileInfoResponse,response);
    }
    @Test
    public void commitRefundTest() {
    	PaymentRoomReservationRequest request = getCommitRequestV2_ForRefundDeposit();
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		HttpServletResponse mockResponse = mock(HttpServletResponse.class);
		BindingResult result = new DirectFieldBindingResult(request, "PaymentRoomReservationRequest");
		localValidatorFactory.validate(request, result);
		Mockito.when(modifyReservationService.commitPaymentReservation(Mockito.any())).thenReturn(getModifyRoomReservationResponse());
		ModifyRoomReservationResponse response= modifyV2Controller.commitRefund(TestConstant.ICE, request, mockRequest, mockResponse);
		assertEquals(getModifyRoomReservationResponse(),response);
		assertNotNull(response);
	}
    
}
