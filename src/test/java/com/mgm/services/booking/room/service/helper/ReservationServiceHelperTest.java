package com.mgm.services.booking.room.service.helper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.util.TokenValidationUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.dao.ProgramContentDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.dao.impl.ComponentDAOImpl;
import com.mgm.services.booking.room.dao.impl.ComponentDAOStrategyACRSImpl;
import com.mgm.services.booking.room.dao.impl.ComponentDAOStrategyGSEImpl;
import com.mgm.services.booking.room.model.ComponentPrices;
import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.booking.room.model.content.Program;
import com.mgm.services.booking.room.model.inventory.BookedItemList;
import com.mgm.services.booking.room.model.inventory.BookedItems;
import com.mgm.services.booking.room.model.inventory.ItemStatus;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.CancelV3Request;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.ReservationAssociateRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.request.UpdateProfileInfoRequest;
import com.mgm.services.booking.room.model.request.UserProfileRequest;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.ItemizedChargeItem;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import com.mgm.services.booking.room.model.reservation.RoomChargesAndTaxes;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.MyVegasResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.properties.AuroraProperties.AuroraCredential;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.service.MyVegasService;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import io.jsonwebtoken.Jwts;

@RunWith(MockitoJUnitRunner.class)
public class ReservationServiceHelperTest extends BaseRoomBookingTest {
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @InjectMocks
    private ReservationServiceHelper resvServiceHelper;
    
    @Mock
    private MyVegasService myVegasService;
    
    @Mock
    private ApplicationProperties appProperties;
    
    @Mock
    private AuroraProperties auroraProperties;

    @Mock
    private ProgramContentDAO programContentDao;

    @Mock
    private SecretsProperties secretProperties;

    @Mock
    private RoomProgramCacheService roomProgramCacheService;
    
    @Mock
	private PreModifyV2Request preModifyV2Request;

	@Mock
	private RoomReservation reservation;

	@Mock
	private ReservationProfile profile;

	@Mock
	private CancelV3Request cancelV3Request;

	@Mock
	private UpdateProfileInfoRequest updateProfileInfoRequest;

	@Mock
	private RoomChargesAndTaxes chargesAndTaxesCalc;

	@Mock
	private ComponentDAOStrategyGSEImpl gseStrategy;

	@Mock
	private static ComponentDAOStrategyACRSImpl acrsStrategy;

	@Mock
	private ReferenceDataDAOHelper referenceDataDAOHelper;

	@Mock
	private ComponentDAOImpl componentDAOImpl;

	@Mock
	private ComponentDAO componentDAO;

	@Mock
	private ACRSConversionUtil acrsConversionUtil;

	@Mock
	private URLProperties urlProperties;

	@Mock
	private RoomProgramCacheService programCacheService;

	@Mock
    private TokenValidationUtil tokenValidationUtil;

    @Mock
    private MyVegasResponse myVegasResponse;

    @Mock
    private Date checkInDate;
    
    @Mock
    private HttpServletRequest httpServletRequest;
    
    @Mock
    private RoomReservationRequest roomReservationRequest;
    
    @Mock
    private Jwts jwts;

    @Test
    public void test_isReservationDatesModifiable_withNoDateChange_expectNoError() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        
        PreModifyV2Request request = convert("/preModifyV2Request-nodate-change.json", PreModifyV2Request.class);
        
        resvServiceHelper.isReservationDatesModifiable(resv, request);
    }
    
    @Test
    public void test_isReservationDatesModifiable_withDateChangeAndCompReservation_expectError() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getBookings().get(0).setComp(true);
        
        PreModifyV2Request request = convert("/preModifyV2Request-nodate-change.json", PreModifyV2Request.class);
        request.getTripDetails().setCheckInDate(new Date());
        
        exceptionRule.expect(BusinessException.class);
        exceptionRule.expectMessage("Modification not allowed for reservation with 1 or more comp nights");
        resvServiceHelper.isReservationDatesModifiable(resv, request);
    }
    
    @Test
    public void test_isReservationDatesModifiable_withDateChangeAndMultiplePrograms_expectError() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getBookings().get(0).setProgramId("new-program-id");
        
        PreModifyV2Request request = convert("/preModifyV2Request-nodate-change.json", PreModifyV2Request.class);
        request.getTripDetails().setCheckInDate(new Date());
        
        exceptionRule.expect(BusinessException.class);
        exceptionRule.expectMessage("Modification not allowed for reservation with multiple program pricing");
        resvServiceHelper.isReservationDatesModifiable(resv, request);
    }
    
    @Test
    public void test_isReservationDatesModifiable_withDateChangeAndPerpetualPricing_expectError() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.setPerpetualPricing(true);
        
        PreModifyV2Request request = convert("/preModifyV2Request-nodate-change.json", PreModifyV2Request.class);
        request.getTripDetails().setCheckInDate(new Date());
        
        exceptionRule.expect(BusinessException.class);
        exceptionRule.expectMessage("Modification not allowed for reservation with perpetual pricing");
        resvServiceHelper.isReservationDatesModifiable(resv, request);
    }
    
    @Test
    public void test_isReservationDatesModifiable_withDateChangeAndNoCcOnFile_expectError() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getCreditCardCharges().get(0).setMaskedNumber(null);
        
        PreModifyV2Request request = convert("/preModifyV2Request-nodate-change.json", PreModifyV2Request.class);
        request.getTripDetails().setCheckInDate(new Date());
        
        exceptionRule.expect(BusinessException.class);
        exceptionRule.expectMessage("Modification not allowed for reservation with no credit card on file");
        resvServiceHelper.isReservationDatesModifiable(resv, request);
    }
    
    @Test
    public void test_isReservationDatesModifiable_withDateChangeAndInForfeitWindow_expectError() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        
        PreModifyV2Request request = convert("/preModifyV2Request-nodate-change.json", PreModifyV2Request.class);
        request.getTripDetails().setCheckInDate(new Date());
        
        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");
        
        exceptionRule.expect(BusinessException.class);
        exceptionRule.expectMessage("Modification not allowed for reservation within forfeit window");
        resvServiceHelper.isReservationDatesModifiable(resv, request);
    }

    @Test
    public void test_isMlifeAddedOrChanged_withOnlyOriginalReservationProfileNull_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.setProfile(null);

        UpdateProfileInfoRequest req = convert("/modifyprofileroomreservation-request.json",
                UpdateProfileInfoRequest.class);

        assertFalse(resvServiceHelper.isMlifeAddedOrChanged(resv, req));
    }

    @Test
    public void test_isMlifeAddedOrChanged_withOnlyUpdateRequestProfileNull_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);

        UpdateProfileInfoRequest req = convert("/modifyprofileroomreservation-request.json",
                UpdateProfileInfoRequest.class);
        req.setUserProfile(null);

        assertFalse(resvServiceHelper.isMlifeAddedOrChanged(resv, req));
    }

    @Test
    public void test_isMlifeAddedOrChanged_withOriginalReservationProfileAndUpdateRequestProfileNull_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.setProfile(null);

        UpdateProfileInfoRequest req = convert("/modifyprofileroomreservation-request.json",
                UpdateProfileInfoRequest.class);
        req.setUserProfile(null);

        assertFalse(resvServiceHelper.isMlifeAddedOrChanged(resv, req));
    }

    @Test
    public void test_isMlifeAddedOrChanged_withNoChangeInMlifeNo_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getProfile().setMlifeNo(12345);

        UpdateProfileInfoRequest req = convert("/modifyprofileroomreservation-request.json",
                UpdateProfileInfoRequest.class);
        req.getUserProfile().setMlifeNo(12345);

        assertFalse(resvServiceHelper.isMlifeAddedOrChanged(resv, req));
    }

    @Test
    public void test_isMlifeAddedOrChanged_withChangeInMlifeNo_expectedTrue() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getProfile().setMlifeNo(12345);

        UpdateProfileInfoRequest req = convert("/modifyprofileroomreservation-request.json",
                UpdateProfileInfoRequest.class);
        req.getUserProfile().setMlifeNo(123456);

        assertTrue(resvServiceHelper.isMlifeAddedOrChanged(resv, req));
    }
    
    @Test
    public void test_delete() {
        RoomReservation resv = new RoomReservation();
        ReservationProfile pf = new ReservationProfile();
        pf.setMlifeNo(0);
        resv.setProfile(pf);
        
        UpdateProfileInfoRequest req = new UpdateProfileInfoRequest();
        UserProfileRequest upr = new UserProfileRequest();
        upr.setMlifeNo(80881035);
        req.setUserProfile(upr);
        
        assertTrue(resvServiceHelper.isMlifeAddedOrChanged(resv, req));    

    }

    @Test
    public void test_isStayDateModifiable_withCompNight_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.getBookings().get(0).setComp(true);

        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }
    
    @Test
    public void test_isStayDateModifiable_withNoProgram_expectedFalse() {
        RoomReservation resv = convert("/reservation-no-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));

        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isStayDateModifiable_withDifferentPrograms_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.getBookings().get(0).setProgramId("5467d989-24f5-4188-a177-daa715763c59");

        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isStayDateModifiable_withPerpetualPricingTrue_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.setPerpetualPricing(true);

        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isStayDateModifiable_withoutCreditCardCharges_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.setCreditCardCharges(null);

        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isStayDateModifiable_withoutCreditCardNumber_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.getCreditCardCharges().get(0).setMaskedNumber(null);

        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isStayDateModifiable_withForfeitDate_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);

        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");
        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isStayDateModifiable_withStatusCancelled_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.setState(ReservationState.Cancelled);

        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");
        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isStayDateModifiable_withThirdPartyChannel_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.setOrigin("TIBCOBUS");

        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");
        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isStayDateModifiable_withHDEProgram_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.setProgramId("7cbd1975-8c9d-4c1f-9f40-355e1f02179b");

        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");

        Program program = convert("/program-content.json", Program.class);
        program.setHdePackage("true");
        when(programContentDao.getProgramContent(Mockito.anyString(), Mockito.anyString())).thenReturn(program);
        assertFalse(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isStayDateModifiable_withNonHDEProgram_expectedTrue() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.setProgramId("7cbd1975-8c9d-4c1f-9f40-355e1f02179b");

        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");

        Program program = convert("/program-content.json", Program.class);
        when(programContentDao.getProgramContent(Mockito.anyString(), Mockito.anyString())).thenReturn(program);
        when(roomProgramCacheService.getRoomProgram(Mockito.anyString())).thenReturn(null);
        assertTrue(resvServiceHelper.isStayDateModifiable(resv, getChannel(resv)));
    }

    @Test
    public void test_isReservationHasHDEProgram_withHDEProgram_expectedTrue() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.setProgramId("7cbd1975-8c9d-4c1f-9f40-355e1f02179b");

        Program program = convert("/program-content.json", Program.class);
        program.setHdePackage("true");
        when(programContentDao.getProgramContent(Mockito.anyString(), Mockito.anyString())).thenReturn(program);
        assertTrue(resvServiceHelper.isReservationHasHDEProgram(resv));
    }

    @Test
    public void test_isReservationHasHDEProgram_withNonHDEProgram_expectedFalse() {
        RoomReservation resv = convert("/reservation-same-program.json", RoomReservation.class);
        resv.getDepositCalc().setForfeitDate(getFutureDate(10));
        resv.setProgramId("7cbd1975-8c9d-4c1f-9f40-355e1f02179b");

        Program program = convert("/program-content.json", Program.class);
        when(programContentDao.getProgramContent(Mockito.anyString(), Mockito.anyString())).thenReturn(program);
        when(roomProgramCacheService.getRoomProgram(Mockito.anyString())).thenReturn(null);
        assertFalse(resvServiceHelper.isReservationHasHDEProgram(resv));
    }

    @Test
    public void test_isNotifyCustomerViaRTC_withChannelExcludedFromSendEmail_expectedFalse() {
        assertFalse(resvServiceHelper.isNotifyCustomerViaRTC("66964e2b-2550-4476-84c3-1a4c0c5c067f", true, false));
    }

    @Test
    public void test_isNotifyCustomerViaRTC_withChannelNotExcludedFromSendEmail_withPropertyNotConfiguredInRTC_expectedFalse() {
        when(appProperties.getRtcEmailsOnboardedListSecretKey()).thenReturn("RTCEmailsOnboardedList-rooms-%s");
        when(appProperties.getRbsEnv()).thenReturn("d");
        when(secretProperties.getSecretValue("RTCEmailsOnboardedList-rooms-d"))
                .thenReturn("6c5cff3f-f01a-4f9b-87ab-8395ae8108db, 1f3ed672-3f8f-44d8-9215-81da3c845d83");
        assertFalse(resvServiceHelper.isNotifyCustomerViaRTC("66964e2b-2550-4476-84c3-1a4c0c5c067f", false, false));
    }

    @Test
    public void test_isNotifyCustomerViaRTC_withChannelNotExcludedFromSendEmail_withPropertyConfiguredInRTC_expectedTrue() {
        when(appProperties.getRtcEmailsOnboardedListSecretKey()).thenReturn("RTCEmailsOnboardedList-rooms-%s");
        when(appProperties.getRbsEnv()).thenReturn("d");
        when(secretProperties.getSecretValue("RTCEmailsOnboardedList-rooms-d"))
                .thenReturn("6c5cff3f-f01a-4f9b-87ab-8395ae8108db, 1f3ed672-3f8f-44d8-9215-81da3c845d83");
        assertTrue(resvServiceHelper.isNotifyCustomerViaRTC("6c5cff3f-f01a-4f9b-87ab-8395ae8108db", false, false));
    }

    private String getChannel(RoomReservation resv) {
        List<AuroraCredential> auroraCredentials = convert(
                new File(getClass().getResource("/auroraChannelCredentials.json").getPath()),
                mapper.getTypeFactory().constructCollectionType(List.class, AuroraCredential.class));
        when(auroraProperties.getChannelCredentials()).thenReturn(auroraCredentials);
        String bookingSource = resvServiceHelper.getBookingSource(resv.getOrigin());
        String bookgingChannel = resvServiceHelper.getBookingChannel(bookingSource);
        return bookgingChannel;
    }
    
	@Test
	public void testIsRequestAllowedToFindReservation() {
		// Arrange
		PreModifyV2Request preModifyV2Request = new PreModifyV2Request();
		preModifyV2Request.setFirstName("John");
		preModifyV2Request.setLastName("Doe");
		when(reservation.getProfile()).thenReturn(profile);
		when(profile.getFirstName()).thenReturn("John");
		when(profile.getLastName()).thenReturn("Doe");

		// Act
		boolean result = resvServiceHelper.isRequestAllowedToFindReservation(preModifyV2Request, reservation);

		// Assert
		assertTrue(result);

	}

	@Test
	public void testIsRequestNotAllowedToFindReservation() {
		// Arrange
		PreModifyV2Request preModifyV2Request = new PreModifyV2Request();
		preModifyV2Request.setFirstName("John");
		preModifyV2Request.setLastName("Doe");
		when(reservation.getProfile()).thenReturn(profile);
		when(profile.getFirstName()).thenReturn("Jane");
		when(profile.getLastName()).thenReturn("Smith");

		// Act
		boolean result = resvServiceHelper.isRequestAllowedToFindReservation(preModifyV2Request, reservation);

		// Assert
		assertFalse(result);
	}

	@Test
	public void testCreateCancelV2Request() {
		// Arrange
		when(cancelV3Request.getConfirmationNumber()).thenReturn("123456");
		when(cancelV3Request.isOverrideDepositForfeit()).thenReturn(true);
		when(cancelV3Request.getCancellationReason()).thenReturn("Reason");
		when(cancelV3Request.getPropertyId()).thenReturn("Property123");
		when(cancelV3Request.getSource()).thenReturn("Source123");
		when(reservation.getPropertyId()).thenReturn("Property123");
		when(reservation.getItineraryId()).thenReturn("Itinerary123");
		when(reservation.getId()).thenReturn("Reservation123");
		when(reservation.getCustomerId()).thenReturn((long) 23458789);
		when(reservation.isF1Package()).thenReturn(true);
		when(cancelV3Request.getInAuthTransactionId()).thenReturn("Transaction123");

		// Act
		CancelV2Request result = resvServiceHelper.createCancelV2Request(cancelV3Request, reservation);

		// Assert
		assertNotNull(result);
		assertEquals("123456", result.getConfirmationNumber());
		assertTrue(result.isOverrideDepositForfeit());
		assertEquals("Reason", result.getCancellationReason());
		assertEquals("Property123", result.getPropertyId());
		assertEquals("Source123", result.getSource());
		assertEquals("Itinerary123", result.getItineraryId());
		assertEquals("Reservation123", result.getReservationId());
		assertEquals((long) 23458789, result.getCustomerId());
		assertTrue(result.isF1Package());
		assertEquals(reservation, result.getExistingReservation());
		assertEquals("Transaction123", result.getInAuthTransactionId());
	}

	@Test
	public void testGetBookingSourceMlife() {
		// Arrange
		String origin = "mlife";

		// Act
		String result = resvServiceHelper.getBookingSource(origin);

		// Assert
		assertEquals("mgmri", result);
	}

	@Test
	public void testGetBookingSourceOtherOrigin() {
		// Arrange
		String origin = "otherOrigin";
		List<AuroraCredential> credentials = new ArrayList<>();
		AuroraCredential credential = new AuroraCredential();
		credential.setName("otherOrigin");
		credential.setKey("otherKey");
		credentials.add(credential);

		when(auroraProperties.getChannelCredentials()).thenReturn(credentials);

		// Act
		String result = resvServiceHelper.getBookingSource(origin);

		// Assert
		assertEquals("otherKey", result);
	}

	@Test
	public void testGetBookingSourceEmptyOrigin() {
		// Arrange
		String origin = "";

		// Act
		String result = resvServiceHelper.getBookingSource(origin);

		// Assert
		assertNull(result);
	}

	@Test
	public void testGetBookingSourceNullOrigin() {
		// Arrange
		String origin = null;

		// Act
		String result = resvServiceHelper.getBookingSource(origin);

		// Assert
		assertNull(result);
	}

	@Test
	public void testIsReservationCancellable() {
		// Arrange
		RoomReservation reservation = new RoomReservation();
		reservation.setPropertyId("property123");
		reservation.setCheckInDate(Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
		String channel = "test";

		// Mocking the behavior of appProperties.getTimezone()
		when(appProperties.getTimezone("property123")).thenReturn("America/New_York");

		// Act
		boolean result = resvServiceHelper.isReservationCancellable(reservation, channel);

		// Assert
		assertFalse(result);
	}

	@Test
	public void testIsReservationNotCancellableDueToCheckInDate() {
		// Arrange
		RoomReservation reservation = new RoomReservation();
		reservation.setPropertyId("property123");
		reservation
				.setCheckInDate(Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant())); // Past
		// check-in
		// date
		String channel = "web";

		// Mocking the behavior of appProperties.getTimezone()
		when(appProperties.getTimezone("property123")).thenReturn("America/New_York");

		// Act
		boolean result = resvServiceHelper.isReservationCancellable(reservation, channel);

		// Assert
		assertFalse(result);
	}

	@Test
	public void testIsReservationNotCancellableDueToChannel() {
		// Arrange
		RoomReservation reservation = new RoomReservation();
		reservation.setPropertyId("property123");
		reservation.setCheckInDate(new Date()); // Future check-in date
		String channel = "app";

		// Mocking the behavior of appProperties.getTimezone()
		when(appProperties.getTimezone("property123")).thenReturn("America/New_York");

		// Act
		boolean result = resvServiceHelper.isReservationCancellable(reservation, channel);

		// Assert
		assertFalse(result);
	}

	@Test
	public void testCreateFindReservationV2Request() {
		// Arrange
		UpdateProfileInfoRequest updateProfileInfoRequest = new UpdateProfileInfoRequest();
		updateProfileInfoRequest.setConfirmationNumber("123456");
		updateProfileInfoRequest.setSource("web");

		// Act
		FindReservationV2Request result = resvServiceHelper.createFindReservationV2Request(updateProfileInfoRequest);

		// Assert
		assertNotNull(result);
		assertEquals("123456", result.getConfirmationNumber());
		assertEquals("web", result.getSource());
		assertTrue(result.isCacheOnly());
	}

	@Test
	public void testIsPartnerAccountNoAddedOrChanged_NewPartnerAccountAdded() {
		// Arrange
		List<PartnerAccounts> newPartnerAccounts = new ArrayList<>();
		newPartnerAccounts.add(new PartnerAccounts("partner123", null, null, null, null, null));
		UserProfileRequest userProfileRequest = new UserProfileRequest();
		userProfileRequest.setPartnerAccounts(newPartnerAccounts);
		when(updateProfileInfoRequest.getUserProfile()).thenReturn(userProfileRequest);

		List<PartnerAccounts> originalPartnerAccounts = new ArrayList<>();
		originalPartnerAccounts.add(new PartnerAccounts("originalPartner123", null, null, null, null, null));
		ReservationProfile profile = new ReservationProfile();
		profile.setPartnerAccounts(originalPartnerAccounts);
		when(reservation.getProfile()).thenReturn(profile);

		// Act
		boolean result = resvServiceHelper.isPartnerAccountNoAddedOrChanged(reservation, updateProfileInfoRequest);

		// Assert
		assertTrue(result);
	}

	@Test
	public void testIsPartnerAccountNoAddedOrChanged_PartnerAccountNoChanged() {
		// Arrange
		List<PartnerAccounts> newPartnerAccounts = new ArrayList<>();
		newPartnerAccounts.add(new PartnerAccounts("partner123", null, null, null, null, null));
		UserProfileRequest userProfileRequest = new UserProfileRequest();
		userProfileRequest.setPartnerAccounts(newPartnerAccounts);
		when(updateProfileInfoRequest.getUserProfile()).thenReturn(userProfileRequest);

		List<PartnerAccounts> originalPartnerAccounts = new ArrayList<>();
		originalPartnerAccounts.add(new PartnerAccounts("originalPartner456", null, null, null, null, null));
		ReservationProfile profile = new ReservationProfile();
		profile.setPartnerAccounts(originalPartnerAccounts);
		when(reservation.getProfile()).thenReturn(profile);

		// Act
		boolean result = resvServiceHelper.isPartnerAccountNoAddedOrChanged(reservation, updateProfileInfoRequest);

		// Assert
		assertTrue(result);
	}

	@Test
	public void testIsPartnerAccountNoAddedOrChanged_PartnerAccountNoNotChanged() {
		// Arrange
		List<PartnerAccounts> newPartnerAccounts = new ArrayList<>();
		newPartnerAccounts.add(new PartnerAccounts("partner123", null, null, null, null, null));
		UserProfileRequest userProfileRequest = new UserProfileRequest();
		userProfileRequest.setPartnerAccounts(newPartnerAccounts);
		when(updateProfileInfoRequest.getUserProfile()).thenReturn(userProfileRequest);

		List<PartnerAccounts> originalPartnerAccounts = new ArrayList<>();
		originalPartnerAccounts.add(new PartnerAccounts("partner123", null, null, null, null, null));
		ReservationProfile profile = new ReservationProfile();
		profile.setPartnerAccounts(originalPartnerAccounts);
		when(reservation.getProfile()).thenReturn(profile);

		// Act
		boolean result = resvServiceHelper.isPartnerAccountNoAddedOrChanged(reservation, updateProfileInfoRequest);

		// Assert
		assertFalse(result);
	}

	@Test
	public void testGetComponentPrices() {
		// Arrange
		RoomChargeItem roomChargeItem1 = new RoomChargeItem();
		RoomChargeItemType roomChargeItemType = RoomChargeItemType.RoomCharge;

		ItemizedChargeItem itemizedChargeItem = new ItemizedChargeItem();
		itemizedChargeItem.setId("id1");
		itemizedChargeItem.setItem("item1");
		itemizedChargeItem.setShortDescription("Short Desc");
		itemizedChargeItem.setActive(true);
		itemizedChargeItem.setPricingApplied("pricingApplied");
		itemizedChargeItem.setItemType(roomChargeItemType);
		itemizedChargeItem.setAmount(10.0);

		List<ItemizedChargeItem> itemized = new ArrayList<ItemizedChargeItem>();
		itemized.add(itemizedChargeItem);

		roomChargeItem1.setDate(new Date());
		roomChargeItem1.setAmount(20.0);
		roomChargeItem1.setItemized(itemized);

		RoomChargeItem roomChargeItem2 = new RoomChargeItem();
		RoomChargeItemType roomChargeItemType2 = RoomChargeItemType.RoomChargeTax;

		ItemizedChargeItem itemizedChargeItem2 = new ItemizedChargeItem();
		itemizedChargeItem2.setId("id2");
		itemizedChargeItem2.setItem("item1");
		itemizedChargeItem2.setShortDescription("Short Desc");
		itemizedChargeItem2.setActive(true);
		itemizedChargeItem2.setPricingApplied("pricingApplied");
		itemizedChargeItem2.setItemType(roomChargeItemType2);
		itemizedChargeItem2.setAmount(10.0);

		List<ItemizedChargeItem> itemized1 = new ArrayList<ItemizedChargeItem>();
		itemized1.add(itemizedChargeItem2);

		roomChargeItem2.setDate(new Date());
		roomChargeItem2.setAmount(20.0);
		roomChargeItem2.setItemized(itemized1);

		List<RoomChargeItem> charges = Arrays.asList(roomChargeItem1, roomChargeItem2);
		when(chargesAndTaxesCalc.getCharges()).thenReturn(charges);

		RoomChargeItem taxChargeItem1 = new RoomChargeItem();
		taxChargeItem1.setDate(new Date());
		taxChargeItem1.setAmount(20.0);
		RoomChargeItemType roomChargeItemType3 = RoomChargeItemType.ExtraGuestCharge;
		ItemizedChargeItem itemizedChargeItem3 = new ItemizedChargeItem();
		itemizedChargeItem3.setId("id3");
		itemizedChargeItem3.setItem("item1");
		itemizedChargeItem3.setShortDescription("Short Desc");
		itemizedChargeItem3.setActive(true);
		itemizedChargeItem3.setPricingApplied("pricingApplied");
		itemizedChargeItem3.setItemType(roomChargeItemType3);
		itemizedChargeItem3.setAmount(10.0);
		List<ItemizedChargeItem> itemized2 = new ArrayList<ItemizedChargeItem>();
		itemized2.add(itemizedChargeItem3);
		taxChargeItem1.setItemized(itemized2);

		RoomChargeItem taxChargeItem2 = new RoomChargeItem();
		taxChargeItem2.setDate(new Date());
		taxChargeItem2.setAmount(20.0);
		RoomChargeItemType roomChargeItemType4 = RoomChargeItemType.ExtraGuestChargeTax;
		ItemizedChargeItem itemizedChargeItem4 = new ItemizedChargeItem();
		itemizedChargeItem4.setId("id4");
		itemizedChargeItem4.setItem("item1");
		itemizedChargeItem4.setShortDescription("Short Desc");
		itemizedChargeItem4.setActive(true);
		itemizedChargeItem4.setPricingApplied("pricingApplied");
		itemizedChargeItem4.setItemType(roomChargeItemType4);
		itemizedChargeItem4.setAmount(10.0);
		List<ItemizedChargeItem> itemized3 = new ArrayList<ItemizedChargeItem>();
		itemized3.add(itemizedChargeItem3);
		taxChargeItem2.setItemized(itemized3);

		List<RoomChargeItem> taxesAndFees = Arrays.asList(taxChargeItem1, taxChargeItem2);
		when(chargesAndTaxesCalc.getTaxesAndFees()).thenReturn(taxesAndFees);

		// Act
		ComponentPrices result = resvServiceHelper.getComponentPrices(chargesAndTaxesCalc, "item1");

		// Assert
		assertEquals(2, result.size());
	}

	@Test
	public void testIsDepositForfeit() {
		// Mocks
		when(appProperties.getTimezone(anyString())).thenReturn("UTC");

		Deposit depositCalc = new Deposit();
		depositCalc.setForfeitDate(Date.from(LocalDateTime.now().minusDays(1).atZone(ZoneId.systemDefault()).toInstant()));
		
		RoomReservation reservation = new RoomReservation();
		reservation.setPropertyId("123");
		reservation.setDepositCalc(depositCalc);
	
		// Call the method under test
		boolean result = resvServiceHelper.isDepositForfeit(reservation);

		// Assertions
		assertTrue(result);
	}

	@Test
	public void testValidateTicketCount_WithF1Components() { // Mocks

		RoomReservation roomReservation = mock(RoomReservation.class);
		List<String> specialRequests = new ArrayList<>();
		specialRequests.add("someRequestId");
		when(roomReservation.getSpecialRequests()).thenReturn(specialRequests);
		when(roomReservation.getPropertyId()).thenReturn("somePropertyId");

		// Verify that the BusinessException is thrown when F1 components are present
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			resvServiceHelper.validateTicketCount(roomReservation, new ArrayList<>());
		});
		assertEquals(ErrorCode.F1_NON_EDITABLE_COMPONENTS_NOT_AVAILABLE, exception.getErrorCode());
	}

	@Test
	public void testValidateF1InventoryStatus_F1InventoryHeld() {
		// Mocks
		BookedItemList bookedItemList = mock(BookedItemList.class);
		List<BookedItems> itemsList = new ArrayList<>();
		BookedItems bookedItems = new BookedItems();
		bookedItems.setStatus(ItemStatus.HELD.toString());
		itemsList.add(bookedItems);

		when(bookedItemList.get(0)).thenReturn(bookedItems);

		// Call the method under test
		assertDoesNotThrow(() -> resvServiceHelper.validateF1InventoryStatus(bookedItemList));
	}

	@Test
	public void testValidateF1InventoryStatus_F1InventoryNotHeld() {
		// Mocks
		BookedItemList bookedItemList = mock(BookedItemList.class);
		List<BookedItems> itemsList = new ArrayList<>();
		BookedItems bookedItems = new BookedItems();
		bookedItems.setStatus(ItemStatus.CANCELED.toString()); // Setting status other than 'HELD'
		itemsList.add(bookedItems);
		when(bookedItemList.get(0)).thenReturn(bookedItems);

		// Verify that the BusinessException is thrown when F1 inventory is not held
		BusinessException exception = assertThrows(BusinessException.class, () -> {
			resvServiceHelper.validateF1InventoryStatus(bookedItemList);
		});
		assertEquals(ErrorCode.F1_INVENTORY_NOT_HELD, exception.getErrorCode());
	}

	@Test
	public void testAddF1CasinoDefaultComponentPrices() {

		String source = "ICE";
		Date checkInDate = new GregorianCalendar(2024, Calendar.MARCH, 9).getTime();
		Date checkOutDate = new GregorianCalendar(2024, Calendar.MARCH, 11).getTime();

		RoomReservation roomReservation = mock(RoomReservation.class);
		RoomReservationV2Response response = mock(RoomReservationV2Response.class);
		List<String> ratePlanTags = new ArrayList<>();
		ratePlanTags.add(ServiceConstant.F1_COMPONENT_CASINO_START_TAG.concat("12023BG"));

		List<RoomPrice> bookings = new ArrayList<RoomPrice>();
		RoomPrice roomPrice = new RoomPrice();
		roomPrice.setProgramId("1234");
		roomPrice.setComp(true);
		roomPrice.setCustomerPrice(100.00);
		roomPrice.setBasePrice(90.00);
		roomPrice.setDiscounted(true);
		roomPrice.setPrice(100.00);
		bookings.add(roomPrice);

		// when(roomReservation.getBookings()).thenReturn(bookings);

		// Behavior setup for roomReservation
		when(roomReservation.getPropertyId()).thenReturn("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		when(roomReservation.getCheckInDate()).thenReturn(checkInDate);
		when(roomReservation.getCheckOutDate()).thenReturn(checkOutDate);
		when(roomReservation.getRoomTypeId()).thenReturn("someRoomTypeId");
		when(roomReservation.getProgramId()).thenReturn("someProgramId");
		ReservationProfile profile = new ReservationProfile();
		profile.setMlifeNo(123456);
		when(roomReservation.getProfile()).thenReturn(profile);

		RoomComponent roomComponent = new RoomComponent();
		roomComponent.setPrice(Float.valueOf(99999));

		assertDoesNotThrow(() -> resvServiceHelper.addF1CasinoDefaultComponentPrices(roomReservation, response,
				ratePlanTags, source));

		// Verify that appropriate methods are called
		verify(roomReservation, atLeastOnce()).getPropertyId();
		verify(roomReservation, atLeastOnce()).getCheckInDate();
		verify(roomReservation, atLeastOnce()).getCheckOutDate();
		verify(roomReservation, atLeastOnce()).getProgramId();
		verify(roomReservation, atLeastOnce()).getProfile();
		verify(componentDAO, atLeastOnce()).getRoomComponentByCode(anyString(), anyString(), anyString(), anyString(),
				any(Date.class), any(Date.class), anyString(), anyString());
	}

	@Test
	public void testIsHDEPackageReservation() {
		RoomReservation roomReservation = mock(RoomReservation.class);
		roomReservation.setProgramId("test123");
		roomReservation.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");

		List<RoomPrice> bookings = new ArrayList<RoomPrice>();
		RoomPrice roomPrice = new RoomPrice();
		roomPrice.setProgramId("1234");
		roomPrice.setComp(true);
		roomPrice.setCustomerPrice(100.00);
		roomPrice.setBasePrice(90.00);
		roomPrice.setDiscounted(true);
		roomPrice.setPrice(100.00);
		bookings.add(roomPrice);
		when(roomReservation.getProgramId()).thenReturn("5c629ee4-ec38-4a07-a8d8-b435e0ef4069");
		when(roomReservation.getPropertyId()).thenReturn("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");

		Program program = convert("/program-content.json", Program.class);
		program.setHdePackage("true");
		when(programContentDao.getProgramContent(Mockito.anyString(), Mockito.anyString())).thenReturn(program);
		program.setActive(true);

		when(programContentDao.getProgramContent(Mockito.anyString(), Mockito.anyString())).thenReturn(program);

		boolean result = resvServiceHelper.isHDEPackageReservation(roomReservation);

		assertTrue(result);
	}

	@Test
	public void testCreateFindReservationV2Request_ReservationAssociateRequest() {
		// Arrange
		ReservationAssociateRequest request = mock(ReservationAssociateRequest.class);
		when(request.getConfirmationNumber()).thenReturn("123456");
		when(request.getSource()).thenReturn("source");

		// Act
		FindReservationV2Request result = resvServiceHelper.createFindReservationV2Request(request);

		// Assert
		assertEquals("123456", result.getConfirmationNumber());
		assertEquals("source", result.getSource());
		assertFalse(result.isCacheOnly());
	}

	@Test
	public void testCreateFindReservationV2Request_CancelV3Request() {
		// Arrange
		CancelV3Request request = mock(CancelV3Request.class);
		when(request.getConfirmationNumber()).thenReturn("123456");
		when(request.getSource()).thenReturn("source");

		// Act
		FindReservationV2Request result = resvServiceHelper.createFindReservationV2Request(request);

		// Assert
		assertEquals("123456", result.getConfirmationNumber());
		assertEquals("source", result.getSource());
		assertTrue(result.isCacheOnly());
	}

	@Test
	public void testCreateFindReservationV2Request_PreModifyV2Request() {

		// Prepare data for mocking
		String confirmationNumber = "ABC123";
		String source = "Web";

		// Mocking the behavior of preModifyV2Request
		when(preModifyV2Request.getConfirmationNumber()).thenReturn(confirmationNumber);
		when(preModifyV2Request.getSource()).thenReturn(source);

		// Call the method under test
		FindReservationV2Request result = resvServiceHelper.createFindReservationV2Request(preModifyV2Request);

		// Assertions
		assertEquals(confirmationNumber, result.getConfirmationNumber());
		assertEquals(source, result.getSource());
		assertTrue(result.isCacheOnly());
	}

	@Test
	public void testValidateRedemptionCode_ValidPromoCode_ReturnsResponse() {
		// Prepare test data
		String promoCode = "VALID_PROMO_CODE";
		Date checkInDate = new Date();
		String propertyId = "PROPERTY_ID";

		// Mock authorization header extraction
		when(httpServletRequest.getHeader("Authorization")).thenReturn("VALID_JWT_TOKEN");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));

		// Mock response from MyVegasService
		MyVegasResponse expectedResponse = new MyVegasResponse();
		expectedResponse.setStatus("SUCCESS");
		when(myVegasService.validateRedemptionCodeV2(any(MyVegasRequest.class), eq("VALID_JWT_TOKEN")))
				.thenReturn(expectedResponse);

		// Call the method under test
		MyVegasResponse actualResponse = resvServiceHelper.validateRedemptionCode(promoCode, checkInDate, propertyId);

		// Verify that the service method was called with the correct arguments
		verify(myVegasService).validateRedemptionCodeV2(any(MyVegasRequest.class), eq("VALID_JWT_TOKEN"));

		// Verify that the response is as expected
		assertNotNull(actualResponse);
		assertEquals("SUCCESS", actualResponse.getStatus());
	}

	@Test
	public void confirmRedemptionCode() {
		// Mock input data
		RoomReservationRequest reservationRequest = new RoomReservationRequest();
		MyVegasResponse redemptionResponse = new MyVegasResponse();
		String confirmationNumber = "12345";
		Date reservationDate = new Date();
		String propertyId = "property123";

		UserProfileRequest upr = new UserProfileRequest();
		upr.setMlifeNo(80881035);
		upr.setFirstName("Ross");
		upr.setLastName("Gellar");

		reservationRequest.setProfile(upr);
		;
		// Mock behavior of appProperties
		when(appProperties.getTimezone(propertyId)).thenReturn("America/New_York");

		// Call the method under test
		resvServiceHelper.confirmRedemptionCode(reservationRequest, redemptionResponse, confirmationNumber,
				reservationDate, propertyId);

		// Verify that the MyVegasService.confirmRedemptionCodeV2() method was called
		// with the correct argument
		verify(myVegasService).confirmRedemptionCodeV2(any());
	}

	@Test
	public void testUpdateProgramId_WithValidResponse() {
		// Mocking the redemption response
		String programId = "CATST";
		when(myVegasResponse.getProgramId()).thenReturn(programId);

		// Creating bookings for room reservation
		List<RoomPrice> bookings = new ArrayList<>();
		reservation.setBookings(bookings);

		// Calling the method to be tested
		resvServiceHelper.updateProgramId(reservation, myVegasResponse);

		// Verifying that programId is set for each booking
		bookings.forEach(b -> assertEquals(programId, b.getProgramId()));
	}
}
