package com.mgm.services.booking.room;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.LoyaltyDao;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.dao.impl.ACRSOAuthTokenDAOImpl;
import com.mgm.services.booking.room.model.acrschargeandtax.ChargeDetails;
import com.mgm.services.booking.room.model.acrschargeandtax.TaxDetails;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRetrieveResReservation;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.service.helper.AccertifyInvokeHelper;
import com.mgm.services.booking.room.util.RequestSourceConfig;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mockito.Mockito.when;

public abstract class BaseAcrsRoomBookingTest extends BaseRoomBookingTest {

	@InjectMocks
	protected static ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;

	@Mock
	protected static AccertifyInvokeHelper accertifyInvokeHelper;

	@InjectMocks
	protected ReferenceDataDAOHelper referenceDataDAOHelper;

	@InjectMocks
	protected LoyaltyDao loyaltyDao;

	protected ApplicationProperties applicationProperties;
	protected static URLProperties urlProperties;

	protected final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");

	protected void init() {
		acrsOAuthTokenDAOImpl = Mockito.mock(ACRSOAuthTokenDAOImpl.class);
		accertifyInvokeHelper = Mockito.mock(AccertifyInvokeHelper.class);
		referenceDataDAOHelper = Mockito.mock(ReferenceDataDAOHelper.class);
		applicationProperties = new ApplicationProperties();
		applicationProperties.setAcrsConnectionPerRouteDaoImpl(2);
		applicationProperties.setAcrsMaxConnectionPerDaoImpl(5);
		applicationProperties.setConnectionTimeout(60000);
		applicationProperties.setReadTimeOut(60000);
		applicationProperties.setSocketTimeOut(60000);
		loyaltyDao = Mockito.mock(LoyaltyDao.class);
		urlProperties = new URLProperties();
		fmt.setTimeZone(TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE));

		if (null == crsMapper || null == mapper) {
			runOnceBeforeClass();
		}
	}

	protected void setMockAuthToken() {
		setMockAuthToken("token", "ICECC");
	}

	protected void setMockAuthToken(String token, String channel) {
		Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = new HashMap<>();
		ACRSAuthTokenResponse tokenRes = new ACRSAuthTokenResponse();
		tokenRes.setToken(token);
		acrsAuthTokenResponseMap.put(channel, tokenRes);
		when(acrsOAuthTokenDAOImpl.generateToken()).thenReturn(acrsAuthTokenResponseMap);
	}

	protected void setMockForRoomPropertyCode() {
		setMockForRoomPropertyCode("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", "ARIA1", "KNGN",
				"TDAAA", "ICECC");
	}

	protected void setMockForRoomPropertyCode(String propertyId, String propertyCode, String roomType,
											  String ratePlan, String acrsVendor) {
		when(referenceDataDAOHelper.retrieveAcrsPropertyID(ArgumentMatchers.anyString())).thenReturn(propertyCode);
		when(referenceDataDAOHelper.retrieveRoomTypeDetail(ArgumentMatchers.anyString(),
				ArgumentMatchers.anyString())).thenReturn(roomType);
		when(referenceDataDAOHelper.retrieveRatePlanDetail(ArgumentMatchers.anyString(),
				ArgumentMatchers.anyString())).thenReturn(ratePlan);
		RequestSourceConfig.SourceDetails source = new RequestSourceConfig.SourceDetails();
		when(referenceDataDAOHelper.getRequestSource((ArgumentMatchers.anyString()))).thenReturn(source);
		when(referenceDataDAOHelper.getAcrsVendor(Mockito.anyString())).thenReturn(acrsVendor);
		when(referenceDataDAOHelper.retrieveGsePropertyID(Mockito.anyString())).thenReturn(propertyId);
	}

	protected ResponseEntity<ReservationRetrieveResReservation> getCrsRetrieveResv() {
		return getCrsRetrieveResv("crs_retrieve_resv");
	}

	protected ResponseEntity<ReservationRetrieveResReservation> getCrsRetrieveResv(String jsonName) {
		File file = new File(getClass().getResource("/acrs/retrievereservation/" + jsonName + ".json").getPath());
		return new ResponseEntity<>(
				convertCrs(file, ReservationRetrieveResReservation.class), HttpStatus.OK);
	}

	protected RoomReservation createRoomReservation(Date checkInDate, Date checkOutDate) throws ParseException {
		List<String> splReqs = new ArrayList<>();
		splReqs.add("COMPONENTCD-v-LPH-d-TYP-v-COMPONENT-d-PROP-v-dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad-d-NRPCD-v-NONE");
		return createRoomReservation(checkInDate, checkOutDate, "ARIA1", "KNGN", "programId", splReqs);
	}
	protected RoomReservation createRoomReservation(Date checkInDate, Date checkOutDate, String propertyId,
													String roomTypeId, String programId,
													List<String> specialRequests) throws ParseException {
		RoomReservation request = new RoomReservation();
		new RoomReservation();
		request.setSource("mgmresorts");
		request.setPartyConfirmationNumber("PRTY_2965148425");
		request.setCustomerId(23458789);
		request.setItineraryId("623458789");
		request.setProfile(createUserProfile("Test", "Test"));
		request.setPropertyId(propertyId);
		request.setRoomTypeId(roomTypeId);
		request.setProgramId(programId);

		// create bookings
		List<RoomPrice> bookings = new ArrayList<>();
		Calendar cal = Calendar.getInstance();
		cal.setTime(checkInDate);
		while(cal.getTime().before(checkOutDate)) {
			RoomPrice booking = new RoomPrice();
			booking.setProgramId(programId);
			booking.setPrice(100.00);
			booking.setDate(cal.getTime());
			bookings.add(booking);
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		request.setBookings(bookings);

		DepositPolicy depositPolicy = new DepositPolicy();
		depositPolicy.setCreditCardRequired(true);
		depositPolicy.setDepositRequired(true);
		request.setDepositPolicyCalc(depositPolicy);

		request.setAmountDue(100.00);
		request.setCheckInDate(checkInDate);
		request.setCheckOutDate(checkOutDate);
		request.setNumRooms(1);
		// setPayments
		// setCreditcard with exp date
		CreditCardCharge creditCardCharge = new CreditCardCharge();
		creditCardCharge.setExpiry(fmt.parse("2026-02-23"));
		creditCardCharge.setNumber("5555555555554444");
		creditCardCharge.setAmount(100);
		creditCardCharge.setType("Mastercard");
		List<CreditCardCharge> cards = new ArrayList<CreditCardCharge>();
		cards.add(creditCardCharge);
		request.setCreditCardCharges(cards);
		request.setSpecialRequests(specialRequests);

		Deposit deposit = new Deposit();
		deposit.setAmount(100.00);
		deposit.setOverrideAmount(-1);
		request.setDepositCalc(deposit);

		return request;
	}

	protected ReservationProfile createUserProfile(String fname, String lname) {
		ReservationProfile profile = new ReservationProfile();
		profile.setId(1000);
		profile.setFirstName(fname);
		profile.setLastName(lname);
		profile.setDateOfBirth(new Date());
		ProfileAddress profileAddress = new ProfileAddress();
		profileAddress.setStreet1("street");
		profileAddress.setStreet2("street2");
		profileAddress.setCity("city");
		profile.setAddresses(Collections.singletonList(profileAddress));

		ProfilePhone profilePhone = new ProfilePhone();
		profilePhone.setNumber("787263547334");
		profilePhone.setType("Home");
		profile.setPhoneNumbers(Collections.singletonList(profilePhone));
		return profile;
	}

	public void setPropertyTaxAndChargesConfig(AcrsProperties acrsProperties ){
		if(null != acrsProperties){
			//mock charge config
			Map<String, List<ChargeDetails>> acrsPropertyChargeCodeMap = new HashMap<String,List<ChargeDetails>>();
			List<ChargeDetails> chargeCodes = new ArrayList<>();
			ChargeDetails chargeDetails1 = new ChargeDetails();
			chargeDetails1.setChargeType(RoomChargeItemType.ResortFee);
			chargeDetails1.setChargeCodes("RSFEE");
			chargeCodes.add(chargeDetails1);
			acrsPropertyChargeCodeMap.put("MV021",chargeCodes);
			acrsProperties.setAcrsPropertyChargeCodeMap(acrsPropertyChargeCodeMap);
			//mock tax config
			Map<String,List<TaxDetails>> acrsPropertyTaxCodeMap = new HashMap<String,List<TaxDetails>>() ;
			List<TaxDetails> taxCodes = new ArrayList<>();
			TaxDetails taxDetails1 = new TaxDetails();
			taxDetails1.setTaxType(RoomChargeItemType.RoomChargeTax);
			taxDetails1.setTaxCodes("OCTAX,RFOTX,OCFEE");
			taxCodes.add(taxDetails1);

			TaxDetails taxDetails2 = new TaxDetails();
			taxDetails2.setTaxType(RoomChargeItemType.ResortFeeTax);
			taxDetails2.setTaxCodes("RSFTX,CTTAX");
			taxCodes.add(taxDetails2);

			acrsPropertyTaxCodeMap.put("MV021",taxCodes);
			acrsProperties.setAcrsPropertyTaxCodeMap(acrsPropertyTaxCodeMap);
			Map<String, List<String>> acrsPropertyTaxCodeExceptionMap = new HashMap<String, List<String>>();
			acrsProperties.setAcrsPropertyTaxCodeExceptionMap(acrsPropertyTaxCodeExceptionMap);
		}
	}

	protected List<CreditCardCharge> createMockRefundCreditCardChargesWithExpiredCard() {
		CreditCardCharge expiredCreditCardCharge = new CreditCardCharge();
		Date expiredDate = null;
		try {
			expiredDate = fmt.parse("2023-12-30");
		} catch (ParseException e) {
			Assertions.fail("Failed to create mock credit card charge. Test case must be fixed.");
		}

		expiredCreditCardCharge.setExpiry(expiredDate);
		expiredCreditCardCharge.setAmount(-100.00);
		expiredCreditCardCharge.setNumber("5115555555554444"); // fake card number
		expiredCreditCardCharge.setHolder("Mock Name");
		expiredCreditCardCharge.setType("Mastercard");
		expiredCreditCardCharge.setCcToken("MockToken55554444");
		// holder profile can be null at this time

		return Collections.singletonList(expiredCreditCardCharge);
	}
}
