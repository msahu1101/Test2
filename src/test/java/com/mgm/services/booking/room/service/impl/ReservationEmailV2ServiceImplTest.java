package com.mgm.services.booking.room.service.impl;

import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.mgm.services.booking.room.dao.EmailDAO;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.ProgramContentDAO;
import com.mgm.services.booking.room.dao.RoomContentDAO;
import com.mgm.services.booking.room.dao.impl.EmailDAONSImpl;
import com.mgm.services.booking.room.model.Email;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.content.Program;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.content.Room;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.DepositPolicy;
import com.mgm.services.booking.room.model.reservation.Payment;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.model.response.UserProfileResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretPropertiesAzure;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.service.cache.EmailCacheService;
import com.mgm.services.booking.room.service.cache.PropertyContentCacheService;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;

/**
 * Unit test class to validate the publishing of event
 * 
 * @author priyanka
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ReservationEmailV2ServiceImplTest {

	@InjectMocks
	ReservationEmailV2ServiceImpl reservationEmailV2ServiceImpl;

	@Mock
	DomainProperties domainProperties;

	@Mock
	ApplicationProperties appProperties;

	@Mock
	PropertyContentCacheService propertyCacheService;

	@Mock
	URLProperties urlProperties;

	@Mock
	EmailCacheService emailCacheService;

	@Mock
	RoomContentDAO roomContentDao;

	@Mock
	ProgramContentDAO programContentDao;

	@Mock
	Map<String, EmailDAO> emailDAOMap;

	@Mock
	IDMSTokenDAO idmsTokenDAO;

	@Mock
	EmailDAONSImpl emailDAONSImpl;

	@Before
	public void setup() {
		ReflectionTestUtils.setField(reservationEmailV2ServiceImpl, "isNSForEmailEnabled", true);
		MockitoAnnotations.initMocks(this);
		when(domainProperties.getAem()).thenReturn("https://preprod.devtest.vegas");
		SecretsProperties secretsProperties=new SecretPropertiesAzure();
		ReflectionTestUtils.setField(secretsProperties, "azureProfile", "azureProfile");
		TokenResponse tokenResponse=new TokenResponse();
		tokenResponse.setAccessToken("authToken");
		idmsTokenDAO=Mockito.mock(IDMSTokenDAO.class);
		emailDAONSImpl=Mockito.mock(EmailDAONSImpl.class);
		Mockito.doNothing().when(emailDAONSImpl).sendEmail(Mockito.any());
		when(emailDAOMap.get(Mockito.any())).thenReturn(emailDAONSImpl);
	}

	@Test
	public void sendConfirmationEmailTest() throws ParseException
	{
		reservationEmailV2ServiceImpl.sendConfirmationEmail(getRoomReservation(),getRoomReservationV2Response(),true);
	}

	@Test
	public void sendConfirmationEmailInternalTest() throws ParseException
	{
		TokenResponse tokenResponse=new TokenResponse();
		tokenResponse.setAccessToken("AuthToken");
		when(urlProperties.getItineraryDeepLink()).thenReturn("/redirect/orders/room/{confNumber}/{firstName}/{lastName}/{encryptedString}");
		List<String> specialRequests=new ArrayList<>();
		specialRequests.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
		when(appProperties.getBorgataSpecialRequests()).thenReturn(specialRequests);

		Property property=new Property();
		property.setName("priyanka");
		property.setGeneralPhoneNumber("123456");
		property.setReservationPhoneNumber("123456");
		when(propertyCacheService.getProperty(Mockito.anyString())).thenReturn(property);
		when(emailCacheService.getConfirmationEmailTemplate(Mockito.anyString())).thenReturn(getEmailTemplate());

		Room room=new Room();
		room.setName("Double Room");
		when(roomContentDao.getRoomContent(Mockito.anyString())).thenReturn(room);
		Program program=new Program();
		when(programContentDao.getProgramContent(Mockito.anyString(),Mockito.anyString())).thenReturn(program);
		when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/New_York");
		reservationEmailV2ServiceImpl.sendConfirmationEmailInternal(getRoomReservation(),getRoomReservationV2Response(),true);
	}

	@Test
	public void sendCancellationEmailTest_Exception() throws ParseException
	{

		when(urlProperties.getItineraryDeepLink()).thenReturn("/redirect/orders/room/{confNumber}/{firstName}/{lastName}/{encryptedString}");
		List<String> specialRequests=new ArrayList<>();
		specialRequests.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
		when(appProperties.getBorgataSpecialRequests()).thenReturn(specialRequests);
		when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/New_York");

		Property property=new Property();
		property.setName("priyanka");
		property.setGeneralPhoneNumber("123456");
		property.setReservationPhoneNumber("123456");
		when(propertyCacheService.getProperty(Mockito.anyString())).thenReturn(property);
		when(emailCacheService.getCancellationEmailTemplate(Mockito.anyString())).thenReturn(getEmailTemplate());
		reservationEmailV2ServiceImpl.sendCancellationEmail(getRoomReservation(),getRoomReservationV2Response());
	}

	@Test
	public void sendConfirmationEmailInternalTest_Exception() throws ParseException
	{

		when(urlProperties.getItineraryDeepLink()).thenReturn("/redirect/orders/room/{confNumber}/{firstName}/{lastName}/{encryptedString}");
		List<String> specialRequests=new ArrayList<>();
		specialRequests.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
		when(appProperties.getBorgataSpecialRequests()).thenReturn(specialRequests);
		when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/New_York");

		Property property=new Property();
		property.setName("priyanka");
		property.setGeneralPhoneNumber("123456");
		property.setReservationPhoneNumber("123456");
		when(propertyCacheService.getProperty(Mockito.anyString())).thenReturn(property);
		reservationEmailV2ServiceImpl.sendConfirmationEmailInternal(getRoomReservation(),getRoomReservationV2Response(),true);
	}

	@Test
	public void sendCancellationEmailTest() throws ParseException
	{

		when(urlProperties.getItineraryDeepLink()).thenReturn("/redirect/orders/room/{confNumber}/{firstName}/{lastName}/{encryptedString}");
		List<String> specialRequests=new ArrayList<>();
		specialRequests.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
		when(appProperties.getBorgataSpecialRequests()).thenReturn(specialRequests);
		when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/New_York");

		Property property=new Property();
		property.setName("priyanka");
		property.setGeneralPhoneNumber("123456");
		property.setReservationPhoneNumber("123456");
		when(propertyCacheService.getProperty(Mockito.anyString())).thenReturn(property);

		Program program=new Program();
		program.setName("offerName");
		program.setShortDescription("offerDescription");
		program.setPromoCode("offerCode");
		program.setPrepromotionalCopy("prepromotionalCopy");

		when(programContentDao.getProgramContent(Mockito.anyString(),
					Mockito.anyString())).thenReturn(program);
		List<String> roomAdaAttributes=new ArrayList<>();
		roomAdaAttributes.add("abc");
		Room room=new Room();
		room.setName("Double Room");
		room.setAdaAttributes(roomAdaAttributes);
		when(roomContentDao.getRoomContent(Mockito.anyString())).thenReturn(room);

		when(emailCacheService.getCancellationEmailTemplate(Mockito.anyString())).thenReturn(getEmailTemplate());
		reservationEmailV2ServiceImpl.sendCancellationEmail(getRoomReservation(),getRoomReservationV2Response());
	}

	@Test
	public void sendCancellationEmailTest_with_program_id() throws ParseException
	{

		when(urlProperties.getItineraryDeepLink()).thenReturn("/redirect/orders/room/{confNumber}/{firstName}/{lastName}/{encryptedString}");
		List<String> specialRequests=new ArrayList<>();
		specialRequests.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
		when(appProperties.getBorgataSpecialRequests()).thenReturn(specialRequests);
		when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/New_York");

		Property property=new Property();
		property.setName("priyanka");
		property.setGeneralPhoneNumber("123456");
		property.setReservationPhoneNumber("123456");
		when(propertyCacheService.getProperty(Mockito.anyString())).thenReturn(property);

		List<String> roomAdaAttributes=new ArrayList<>();
		roomAdaAttributes.add("abc");
		Room room=new Room();
		room.setName("Double Room");
		room.setAdaAttributes(roomAdaAttributes);
		when(roomContentDao.getRoomContent(Mockito.anyString())).thenReturn(room);

		when(emailCacheService.getCancellationEmailTemplate(Mockito.anyString())).thenReturn(getEmailTemplate());
		reservationEmailV2ServiceImpl.sendCancellationEmail(getRoomReservation(),getRoomReservationV2Response_with_no_program_id());
	}

	private Email getEmailTemplate() {
		Email emailTemplate=new Email();
		emailTemplate.setFrom("12345@mgmresorts.com");
		emailTemplate.setBody("Hello");
		emailTemplate.setReplyTo("123@mgmresorts.com");
		emailTemplate.setSubject("Testing mail");
		return emailTemplate;
	}

	private RoomReservation getRoomReservation() throws ParseException {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		double chargeAmount = 100.00;
        RoomReservation request = new RoomReservation();
        request.setConfirmationNumber("ABC123");
        request.setSource("mgmresorts");
        request.setConfirmationNumber("1271274619");
        request.setPropertyId("MV021");
        request.setRoomTypeId("DMLQ");
        request.setProgramId("CATST");
        List<RoomPrice> bookings = new ArrayList<>();
        RoomPrice booking = new RoomPrice();
        booking.setProgramId("CATST");
        bookings.add(booking);
        request.setBookings(bookings);

        ReservationProfile profile = new ReservationProfile();
        profile.setId(1000);
        profile.setFirstName("Test");
        profile.setLastName("Test");
        profile.setEmailAddress1("123@mgmresorts.com");

        ProfileAddress profileAddress = new ProfileAddress();
        profileAddress.setStreet1("street");
        profileAddress.setStreet2("street2");
        profileAddress.setCity("city");
        profile.setAddresses(Collections.singletonList(profileAddress));

        ProfilePhone profilePhone = new ProfilePhone();
        profilePhone.setNumber("787263547334");
        profilePhone.setType("Home");
        profile.setPhoneNumbers(Collections.singletonList(profilePhone));

        request.setProfile(profile);        

        request.setCheckInDate(fmt.parse("2021-02-23"));
        request.setCheckOutDate(fmt.parse("2021-02-24"));
        // setPayments
        // setCreditcard with exp date
        CreditCardCharge creditCardCharge = new CreditCardCharge();
        creditCardCharge.setExpiry(fmt.parse("2026-02-23"));
        creditCardCharge.setAmount(chargeAmount);
        creditCardCharge.setNumber("55555544444");
        creditCardCharge.setType("Mastercard");
        List<CreditCardCharge> cards = new ArrayList<CreditCardCharge>();
        cards.add(creditCardCharge);
        request.setCreditCardCharges(cards);
        List<String> splReqs = new ArrayList<>();
        splReqs.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
        request.setSpecialRequests(splReqs);

        List<Payment> paymentList=new ArrayList<>();
        Payment payment=new Payment();
        payment.setChargeAmount(chargeAmount);
        payment.setChargeCardExpiry(fmt.parse("2026-02-23"));
        payment.setChargeCardType("Mastercard");
        payment.setChargeCardNumber("55555544444");
        paymentList.add(payment);
        request.setPayments(paymentList);

        Deposit depositCalc = new Deposit();
        depositCalc.setAmount(100.0);    
        depositCalc.setForfeitDate(new Date());
        request.setDepositCalc(depositCalc);

        DepositPolicy depositPolicy = new DepositPolicy();
        depositPolicy.setDepositRequired(true);
        depositPolicy.setCreditCardRequired(true);
        request.setDepositPolicyCalc(depositPolicy);

        return request;

	}

	private RoomReservationV2Response getRoomReservationV2Response() {
		RoomReservationV2Response roomReservationV2Response=new RoomReservationV2Response();
		roomReservationV2Response.setProgramId("7b36149a-0ab8-442b-8651-cfa5f800349e");
		roomReservationV2Response.setRoomTypeId("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd");
		roomReservationV2Response.setPropertyId("7b36149a-0ab8-442b-8651-cfa5f800349e");

		//bookings info
		List<RoomPrice> roomPriceList=new ArrayList<>();
		RoomPrice roomPrice=new RoomPrice();
		roomPrice.setBasePrice(58140.111);
		roomPrice.setDiscounted(true);
		roomPriceList.add(roomPrice);
		roomReservationV2Response.setBookings(roomPriceList);

		//rate summary
		RatesSummary rateSummary=new RatesSummary();
		rateSummary.setDiscountedAveragePrice(143.11);
		rateSummary.setRoomSubtotal(60199.1234);
		rateSummary.setProgramDiscount(12.11);
		rateSummary.setRoomChargeTax(123.11);
		rateSummary.setResortFeeAndTax(123.11);
		rateSummary.setResortFee(12345.111);
		rateSummary.setOccupancyFee(12.11);
		rateSummary.setTourismFeeAndTax(13.11);
		rateSummary.setCasinoSurchargeAndTax(12.11);
		rateSummary.setRoomRequestsTotal(12.11);
		roomReservationV2Response.setRatesSummary(rateSummary);

		//special requests
		List<String> specialRequests=new ArrayList<>();
		specialRequests.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
		roomReservationV2Response.setSpecialRequests(specialRequests);
		roomReservationV2Response.setDepositForfeit(true);

		roomReservationV2Response.setConfirmationNumber("ABC123");

		UserProfileResponse  userProfileResponse =new UserProfileResponse ();
		userProfileResponse.setFirstName("Michael");
		userProfileResponse.setLastName("Fu");
		userProfileResponse.setEmailAddress1("123@mgmresorts.com");
		roomReservationV2Response.setProfile(userProfileResponse);

		return roomReservationV2Response;

	}

	private RoomReservationV2Response getRoomReservationV2Response_with_no_program_id() {
		RoomReservationV2Response roomReservationV2Response=new RoomReservationV2Response();
		roomReservationV2Response.setRoomTypeId("b46361e9-e3dc-4fbf-8a66-d3dbd9fa74cd");
		roomReservationV2Response.setPropertyId("7b36149a-0ab8-442b-8651-cfa5f800349e");

		//bookings info
		List<RoomPrice> roomPriceList=new ArrayList<>();
		RoomPrice roomPrice=new RoomPrice();
		roomPrice.setBasePrice(58140.111);
		roomPrice.setDiscounted(true);
		roomPriceList.add(roomPrice);
		roomReservationV2Response.setBookings(roomPriceList);

		//rate summary
		RatesSummary rateSummary=new RatesSummary();
		rateSummary.setDiscountedAveragePrice(143.11);
		rateSummary.setRoomSubtotal(60199.1234);
		rateSummary.setProgramDiscount(12.11);
		rateSummary.setRoomChargeTax(123.11);
		rateSummary.setResortFeeAndTax(123.11);
		rateSummary.setResortFee(12345.111);
		rateSummary.setOccupancyFee(12.11);
		rateSummary.setTourismFeeAndTax(13.11);
		rateSummary.setCasinoSurchargeAndTax(12.11);
		rateSummary.setRoomRequestsTotal(12.11);
		roomReservationV2Response.setRatesSummary(rateSummary);

		//special requests
		List<String> specialRequests=new ArrayList<>();
		specialRequests.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
		roomReservationV2Response.setSpecialRequests(specialRequests);
		roomReservationV2Response.setDepositForfeit(true);

		roomReservationV2Response.setConfirmationNumber("ABC123");

		UserProfileResponse  userProfileResponse =new UserProfileResponse ();
		userProfileResponse.setFirstName("Michael");
		userProfileResponse.setLastName("Fu");
		userProfileResponse.setEmailAddress1("123@mgmresorts.com");
		roomReservationV2Response.setProfile(userProfileResponse);

		return roomReservationV2Response;

	}

}
