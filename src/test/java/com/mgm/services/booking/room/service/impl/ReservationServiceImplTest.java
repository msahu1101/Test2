package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.dao.ReservationDAO;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.model.request.CreatePartyRoomReservationRequest;
import com.mgm.services.booking.room.model.request.CreateRoomReservationRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CreatePartyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.EventPublisherService;
import com.mgm.services.booking.room.service.ItineraryService;
import com.mgm.services.booking.room.service.ReservationEmailV2Service;
import com.mgm.services.booking.room.service.helper.AccertifyInvokeHelper;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import com.mgmresorts.aurora.service.EAuroraException;

/**
 * Unit test class for service methods in CancelService.
 *
 */
@RunWith(MockitoJUnitRunner.class)

public class ReservationServiceImplTest extends BaseRoomBookingTest {

	@Mock
	private ReservationDAO reservationDao;

	@InjectMocks
	private ReservationServiceImpl reservationServiceImpl;

	@Mock
	private ItineraryService itineraryService;

	@Mock
	private ItineraryServiceImpl itineraryServiceImpl;

	@Mock
	private RoomReservationRequestMapper requestMapper;

	@Mock
	private RoomReservationResponseMapper responseMapper;

	@Mock
	private EventPublisherService<?> eventPublisherService;

	@Mock
	private AccertifyInvokeHelper accertifyInvokeHelper;

	@Mock
	private ReferenceDataDAOHelper referenceDataDAOHelper;

	@Mock
	HttpServletRequest request;

	@Mock
	private ApplicationProperties appProperties;

	@Mock
	private ReservationEmailV2Service emailService;

    @Mock
    private AcrsProperties acrsProperties;

    @Mock
    private SecretsProperties secretProperties;

    @Mock
	private ServiceConversionHelper serviceConversionHelper;

    @Mock
    private ReservationServiceHelper reservationServiceHelper;

	@Mock
	private RoomProgramDAO roomProgramDao;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	private RoomReservation makeRoomReservationResponse(String fileName) {
		File file = new File(getClass().getResource(fileName).getPath());
		return convert(file, RoomReservation.class);

	}

	/**
	 * Test make Reservation Success Test
	 */
	@Test
	public void makeReservationSuccessTest() {

		when(reservationDao.makeRoomReservation(Mockito.any()))
				.thenReturn(makeRoomReservationResponse("/reservation-makeRoomReservation-dao-response.json"));
		RoomReservation reservationRequest = new RoomReservation();

		RoomReservation response = reservationServiceImpl.makeRoomReservation(reservationRequest);

		assertEquals("Booked", response.getState().toString());
		Assert.assertNotNull(response.getConfirmationNumber());
		Assert.assertNotNull(response.getCheckInDate());
		Assert.assertNotNull(response.getCheckInDate());
		Assert.assertNotSame(0, response.getNumAdults());
		Assert.assertNotSame(0, response.getNumRooms());

	}

	/**
	 * Test makeRoomReservation for invalid credit card
	 */
	@Test
	public void makeReservationInvalidCCNumTest() {

		when(reservationDao.makeRoomReservation(Mockito.any())).thenThrow(
				new EAuroraException(new Throwable("<InvalidCreditCard>[Charge credit card number is invalid]")));
		RoomReservation reservationRequest = new RoomReservation();

		List<CreditCardCharge> paymentMethods = new ArrayList<>();
		CreditCardCharge cardCharge = new CreditCardCharge();

		cardCharge.setAmount(233.00);
		cardCharge.setCvv("345");
		cardCharge.setNumber("5555555555");

		paymentMethods.add(cardCharge);
		reservationRequest.setCreditCardCharges(paymentMethods);

		try {
			reservationServiceImpl.makeRoomReservation(reservationRequest);
		} catch (Exception businessException) {
			assertEquals("java.lang.Throwable: <InvalidCreditCard>[Charge credit card number is invalid]",
					businessException.getMessage());
		}
	}

	@Test
	public void shouldInvokeupdateCustomerItineraryWhenEnabled() {
		CreateRoomReservationRequest createReservationRequest = new CreateRoomReservationRequest();
		createReservationRequest.setRoomReservation(new RoomReservationRequest());
		ReflectionTestUtils.setField(itineraryServiceImpl, "itineraryServiceEnabled", true);
		RoomReservationV2Response roomReservationResponse = new RoomReservationV2Response();
		roomReservationResponse.setId("testId");
		when(responseMapper.roomReservationModelToResponse(Mockito.any())).thenReturn(roomReservationResponse);
		when(reservationDao.makeRoomReservationV2(Mockito.any()))
				.thenReturn(makeRoomReservationResponse("/reservation-makeRoomReservation-dao-response.json"));
		when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(new RoomProgramValidateResponse());
		String[] channels = { "ice" };
		when(appProperties.getExcludeEmailForChannels()).thenReturn(Arrays.asList(channels));
        reservationServiceImpl.makeRoomReservationV2(createReservationRequest, "true");
		verify(itineraryService, Mockito.times(1)).createOrUpdateCustomerItinerary(Mockito.any(),Mockito.any());
	}

	@Test
	public void shouldNotInvokeupdateCustomerItineraryWhenDisabled() {
		CreateRoomReservationRequest createReservationRequest = new CreateRoomReservationRequest();
		createReservationRequest.setRoomReservation(new RoomReservationRequest());
		ReflectionTestUtils.setField(itineraryServiceImpl, "itineraryServiceEnabled", false);
		RoomReservationV2Response roomReservationResponse = new RoomReservationV2Response();
		roomReservationResponse.setId("testId");
		when(responseMapper.roomReservationModelToResponse(Mockito.any())).thenReturn(roomReservationResponse);
		when(reservationDao.makeRoomReservationV2(Mockito.any()))
				.thenReturn(makeRoomReservationResponse("/reservation-makeRoomReservation-dao-response.json"));
		when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(new RoomProgramValidateResponse());
		String[] channels = { "ice" };
		when(appProperties.getExcludeEmailForChannels()).thenReturn(Arrays.asList(channels));
        reservationServiceImpl.makeRoomReservationV2(createReservationRequest, "true");
		verify(itineraryService, Mockito.times(0)).createOrUpdateCustomerItinerary(Mockito.any(),Mockito.any());
	}

	@Test
	public void shouldInvokeupdateCustomerItineraryWhenEnabledForParty() {
		CreatePartyRoomReservationRequest createReservationRequest = new CreatePartyRoomReservationRequest();
		createReservationRequest.setRoomReservation(new RoomReservationRequest());
		ReflectionTestUtils.setField(itineraryServiceImpl, "itineraryServiceEnabled", true);
		CreatePartyRoomReservationResponse response = new CreatePartyRoomReservationResponse();
		List<RoomReservationV2Response> roomReservations = new ArrayList<>();
		RoomReservationV2Response resp1 = new RoomReservationV2Response();
		roomReservations.add(resp1);
		RoomReservationV2Response resp2 = new RoomReservationV2Response();
		roomReservations.add(resp2);
		response.setRoomReservations(roomReservations);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(Mockito.any())).thenReturn(true);
		when(responseMapper.partyRoomReservationsModelToResponse(Mockito.any())).thenReturn(response);
		reservationServiceImpl.makePartyRoomReservation(createReservationRequest, TestConstant.TRUE_STRING);
		verify(itineraryService, Mockito.times(2)).updateCustomerItinerary(Mockito.any());
	}

	@Test
	public void shouldNotInvokeupdateCustomerItineraryWhenDisabledForParty() {
		CreatePartyRoomReservationRequest createReservationRequest = new CreatePartyRoomReservationRequest();
		createReservationRequest.setRoomReservation(new RoomReservationRequest());
		ReflectionTestUtils.setField(itineraryServiceImpl, "itineraryServiceEnabled", false);
		CreatePartyRoomReservationResponse response = new CreatePartyRoomReservationResponse();
		List<RoomReservationV2Response> roomReservations = new ArrayList<>();
		RoomReservationV2Response resp1 = new RoomReservationV2Response();
		roomReservations.add(resp1);
		response.setRoomReservations(roomReservations);
		when(responseMapper.partyRoomReservationsModelToResponse(Mockito.any())).thenReturn(response);
		reservationServiceImpl.makePartyRoomReservation(createReservationRequest, TestConstant.TRUE_STRING);
		verify(itineraryService, Mockito.times(0)).updateCustomerItinerary(Mockito.any());
	}

    @Test
    public void test_makeRoomReservationV2_withIsNotifyCustomerViaRtcAsTrue_shouldNotInvokeSendConfirmationEmail() {
        CreateRoomReservationRequest createReservationRequest = new CreateRoomReservationRequest();
        createReservationRequest.setRoomReservation(new RoomReservationRequest());
        RoomReservationV2Response roomReservationResponse = new RoomReservationV2Response();
        roomReservationResponse.setId("testId");
        when(responseMapper.roomReservationModelToResponse(Mockito.any())).thenReturn(roomReservationResponse);
        when(reservationDao.makeRoomReservationV2(Mockito.any()))
                .thenReturn(makeRoomReservationResponse("/reservation-makeRoomReservation-dao-response.json"));
        when(reservationServiceHelper.isNotifyCustomerViaRTC(null, false, false)).thenReturn(true);
		when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(new RoomProgramValidateResponse());
		reservationServiceImpl.makeRoomReservationV2(createReservationRequest, "true");
        verify(emailService, Mockito.times(0)).sendConfirmationEmail(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void test_makeRoomReservationV2_withIsNotifyCustomerViaRtcAsFalse_shouldInvokeSendConfirmationEmail() {
        CreateRoomReservationRequest createReservationRequest = new CreateRoomReservationRequest();
        createReservationRequest.setRoomReservation(new RoomReservationRequest());
        RoomReservationV2Response roomReservationResponse = new RoomReservationV2Response();
        roomReservationResponse.setId("testId");
        when(responseMapper.roomReservationModelToResponse(Mockito.any())).thenReturn(roomReservationResponse);
        when(reservationDao.makeRoomReservationV2(Mockito.any()))
                .thenReturn(makeRoomReservationResponse("/reservation-makeRoomReservation-dao-response.json"));
        String[] channels = { "web" };
        when(appProperties.getExcludeEmailForChannels()).thenReturn(Arrays.asList(channels));
        when(reservationServiceHelper.isNotifyCustomerViaRTC(null, false, false)).thenReturn(false);
		when(roomProgramDao.validateProgramV2(Mockito.any())).thenReturn(new RoomProgramValidateResponse());
		reservationServiceImpl.makeRoomReservationV2(createReservationRequest, "true");
        verify(emailService, Mockito.times(1)).sendConfirmationEmail(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }
}
