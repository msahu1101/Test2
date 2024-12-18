package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseRoomPriceBookingTest;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulSingleAvailability;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.properties.*;
import com.mgm.services.booking.room.util.CommonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

public class SingleRoomPriceDAOStrategyImplTest extends BaseRoomPriceBookingTest {

	@Mock
	private static RestTemplate client;

	@InjectMocks
	private static DomainProperties domainProperties;

	@InjectMocks
	private static ApplicationProperties applicationProperties;

	@InjectMocks
	private static RestTemplateBuilder restTemplateBuilder;

	@InjectMocks
	private static URLProperties urlProperties;

	@InjectMocks
	private static AcrsProperties acrsProperties;

	@InjectMocks
	private static SecretsProperties secretsProperties;
	
	@InjectMocks
	private static RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;

	@InjectMocks
	private static ReferenceDataDAOHelper referenceDataDAOHelper;
	
	@InjectMocks
    private static ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;

	static Logger logger = LoggerFactory.getLogger(SingleRoomPriceDAOStrategyImplTest.class);

	/**
	 * Return Single Availability from JSON mock file.
	 */
	private HttpEntity<?> getCrsSingleRoomAvail(String filename) {
		File file = new File(getClass().getResource(filename).getPath());
		ResponseEntity<?> response = new ResponseEntity<SuccessfulSingleAvailability>(
				convertCrs(file, SuccessfulSingleAvailability.class), HttpStatus.OK);
		return response;

	}

	private void setMockForSingleRoomAvail(String filename) {
		when(client.exchange(ArgumentMatchers.contains("reservations"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<SuccessfulSingleAvailability>>any(), Mockito.anyMap()))
						.thenReturn((ResponseEntity<SuccessfulSingleAvailability>) getCrsSingleRoomAvail(filename));
	}

	private void setMockForSingleRoomAvailPO(String filename) {
		when(client.exchange(ArgumentMatchers.contains("reservations"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<SuccessfulSingleAvailability>>any(), Mockito.anyMap()))
				.thenReturn((ResponseEntity<SuccessfulSingleAvailability>) getCrsSingleRoomAvail(filename));
	}



	private void setMockAuthToken() {
		Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = new HashMap<String, ACRSAuthTokenResponse>();
		ACRSAuthTokenResponse tokenRes = new ACRSAuthTokenResponse();
		tokenRes.setToken("token");
		acrsAuthTokenResponseMap.put("ICECC", tokenRes);
        when(acrsOAuthTokenDAOImpl.generateToken()).thenReturn(acrsAuthTokenResponseMap);
    }
	
	private void setMockReferenceDataDAOHelper() {
		when(referenceDataDAOHelper.retrieveAcrsPropertyID(Mockito.anyString())).thenReturn("ACRS");
		when(referenceDataDAOHelper.retrieveRatePlanDetail(Mockito.anyString(), Mockito.anyString()))
				.thenReturn("PREVL");
        when(referenceDataDAOHelper.getAcrsVendor(Mockito.any())).thenReturn("ICECC");
	}

	@BeforeAll
	public static void init() {
		BaseRoomPriceBookingTest.staticInit();
		client = Mockito.mock(RestTemplate.class);
		domainProperties = new DomainProperties();
		domainProperties.setCrs("");
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		applicationProperties = Mockito.mock(ApplicationProperties.class);
		secretsProperties = Mockito.mock(SecretsProperties.class);
		acrsProperties = new AcrsProperties();
		acrsProperties.setModifyDateStartPath("modifyDateStartPath");
		acrsProperties.setModifyDateEndPath("modifyDateEndPath");
		acrsProperties.setModifySpecialRequestPath("modifySpecialRequestPath");
		acrsProperties.setLiveCRS(true);
		acrsProperties.setDefaultBasePriceRatePlan("ANY");
		applicationProperties.setCrsRestTTL(5000);
		urlProperties = new URLProperties();
		urlProperties.setAcrsAvailabilityReservation(
				"/hotel-platform/cit/mgm/v6/hotel/reservations/MGM?property_code={property_code}&start_date={start_date}&end_date={end_date}");
		referenceDataDAOHelper = Mockito.mock(ReferenceDataDAOHelper.class);
		acrsOAuthTokenDAOImpl = Mockito.mock(ACRSOAuthTokenDAOImpl.class);
		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		when(commonUtil.getRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(),
				acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeout(),
				applicationProperties.getReadTimeOut(),
				applicationProperties.getSocketTimeOut(),
				applicationProperties.getCrsRestTTL())).thenReturn(client);
		roomPriceDAOStrategyACRSImpl = new RoomPriceDAOStrategyACRSImpl(urlProperties, domainProperties,
				applicationProperties, acrsProperties, restTemplateBuilder, null, referenceDataDAOHelper, acrsOAuthTokenDAOImpl, secretsProperties);
	}

	@Test
	void getSingleAvailability_Transient_NoProgramId_Test() {
		try {
			setMockReferenceDataDAOHelper();
			setMockAuthToken();
			setMockForSingleRoomAvail("/single-room-avail_transient_noProgramId.json");

			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2023-04-13"),
					LocalDate.parse("2023-04-14"))
					.customerRank(-1)
					.build();

			AuroraPricesResponse response = roomPriceDAOStrategyACRSImpl.getRoomPricesV2(auroraPriceRequest);
			// Asserting not null
			Assertions.assertNotNull(response);
			// Based on the values provided in the mock: 78 offers, 3 rateplans
			Assertions.assertEquals(78, response.getAuroraPrices().size());
			// 1st offer: status = available, rateplan = PREVL, discounterdPrice = 25, resort fee = 37
			AuroraPriceResponse price = response.getAuroraPrices().get(0);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, price.getStatus());
			Assertions.assertEquals("PREVL", price.getProgramId());
			Assertions.assertEquals(25.0, price.getDiscountedPrice(), 0);
			Assertions.assertEquals(37.0, price.getResortFee(), 0);
			// 78th offer: status = available, rateplan = MLIFE, discountedPrice = 52.99, resort fee = 37
			price = response.getAuroraPrices().get(77);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, price.getStatus());
			Assertions.assertEquals("MLIFE", price.getProgramId());
			Assertions.assertEquals(52.99, price.getDiscountedPrice(), 0);
			Assertions.assertEquals(37.0, price.getResortFee(), 0);
		} catch (Exception e) {
			Assertions.fail("getSingleAvailability_Transient_NoProgramId_Test Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	@Test
	void getSingleAvailability_Transient_WithProgramId_Test() {
		try {
			setMockReferenceDataDAOHelper();
			setMockAuthToken();
			setMockForSingleRoomAvail("/single-room-avail_transient_withProgramId_test.json");

			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2022-08-07"),
					LocalDate.parse("2022-08-29"))
					.programId("PREVL")
					.customerRank(-1)
					.build();

			AuroraPricesResponse response = roomPriceDAOStrategyACRSImpl.getRoomPricesV2(auroraPriceRequest);
			// Asserting not null
			Assertions.assertNotNull(response);
			// Based on the values provided in the mock: 7 offers
			Assertions.assertEquals(7, response.getAuroraPrices().size());
			// 1st offer: status = available, rateplan = PREVL, discounterdPrice = 60, resort fee = 39
			AuroraPriceResponse price = response.getAuroraPrices().get(0);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, price.getStatus());
			Assertions.assertEquals("PREVL", price.getProgramId());
			Assertions.assertEquals(60.0, price.getDiscountedPrice(), 0);
			Assertions.assertEquals(39.0, price.getResortFee(), 0);
			// 7th offer: status = available, rateplan = PREVL, discountedPrice = 200, resort fee = 39
			price = response.getAuroraPrices().get(6);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, price.getStatus());
			Assertions.assertEquals("PREVL", price.getProgramId());
			Assertions.assertEquals(200.0, price.getDiscountedPrice(), 0);
			Assertions.assertEquals(39.0, price.getResortFee(), 0);
		} catch (Exception e) {
			Assertions.fail("getSingleAvailability_Transient_withProgramId_Test Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	@Test
	void getSingleAvailability_Promo_Test() {
		try {
			setMockReferenceDataDAOHelper();
			setMockAuthToken();
			setMockForSingleRoomAvail("/single-room-avail_promo.json");

			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2022-07-28"),
					LocalDate.parse("2022-09-04"))
					.customerRank(2)
					.mlifeNumber("100195867")
					.promo("TDEVPROMO")
					.build();

			AuroraPricesResponse response = roomPriceDAOStrategyACRSImpl.getRoomPricesV2(auroraPriceRequest);
			// Asserting not null
			Assertions.assertNotNull(response);
			// Based on the values provided in the mock: 28 offers, 2 rateplans
			Assertions.assertEquals(444, response.getAuroraPrices().size());
			// 1st offer: status = available, rateplan = WEBMLIFE, discounterdPrice = 100.0, resort fee = 39.0
			AuroraPriceResponse price = response.getAuroraPrices().get(0);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, price.getStatus());
			Assertions.assertEquals("WEBMLIFE", price.getProgramId());
			Assertions.assertEquals("TDEVPROMOMB", price.getPromo());
			Assertions.assertEquals(100.0, price.getDiscountedPrice(), 0);
			Assertions.assertEquals(39.0, price.getResortFee(), 0);
		} catch (Exception e) {
			Assertions.fail("getSingleAvailability_promo Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	@Test
	void getSingleAvailability_PO_Test() {
		try {
			setMockReferenceDataDAOHelper();
			setMockAuthToken();
			setMockForSingleRoomAvail("/single-room-avail_PO.json");

			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2022-07-28"),
					LocalDate.parse("2022-07-29"))
					.propertyId("MV180")
					.mlifeNumber("79053999")
					.customerRank(2)
					.build();

			AuroraPricesResponse response = roomPriceDAOStrategyACRSImpl.getRoomPricesV2(auroraPriceRequest);
			// Asserting not null
			Assertions.assertNotNull(response);
			// Based on the values provided in the mock: 28 offers, 2 rateplans
			Assertions.assertEquals(28, response.getAuroraPrices().size());
			// 1st offer: status = available, rateplan = CASHT002, discounterdPrice = 159.0, resort fee = 0
			AuroraPriceResponse price = response.getAuroraPrices().get(0);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, price.getStatus());
			Assertions.assertEquals("CASHT002", price.getProgramId());
			Assertions.assertEquals(159.0, price.getDiscountedPrice(), 0);
			Assertions.assertEquals(15.0, price.getResortFee(), 0);
			// 27th offer: status = available, rateplan = COMPT002, discountedPrice = 0, resort fee = 0
			price = response.getAuroraPrices().get(26);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, price.getStatus());
			Assertions.assertEquals("COMPT002", price.getProgramId());
			Assertions.assertEquals(0, price.getDiscountedPrice(), 0);
			Assertions.assertEquals(0, price.getResortFee(), 0);
		} catch (Exception e) {
			Assertions.fail("getSingleAvailability_PO Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	/**
	 * Check Single Availability if available for given dates.
	 */
	@Test
	void getACRSSingleRoomAvailabilitySuccessTest() {
		setMockReferenceDataDAOHelper();
		setMockAuthToken();
		try {
			List<String> roomTypeIds = Arrays.asList(new String[] { "91dc7d4e-d531-4a73-8b1b-a92ed0d73b14" });
			List<String> itineraryIds = Arrays.asList(new String[] { "3836376577" });

			setMockForSingleRoomAvail("/single-room-avail.json");
			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2021-03-06"),
					LocalDate.parse("2021-03-07"))
					.programId("CATST")
					.roomTypeIds(roomTypeIds)
					.auroraItineraryIds(itineraryIds)
					.customerDominantPlay("SLOT")
					.customerRank(2)
					.build();

			List<AuroraPriceResponse> response = roomPriceDAOStrategyACRSImpl.getRoomPrices(auroraPriceRequest);
			Assertions.assertNotNull(response);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, response.get(0).getStatus());
			Assertions.assertEquals(0.0, response.get(0).getBasePrice(), 0);
			Assertions.assertEquals(150.0, response.get(0).getDiscountedPrice(), 0);
		} catch (Exception e) {
			Assertions.fail("ACRSSingleRoomAvailabilitySuccessTest Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	@Test
	void getACRSSingleRoomAvailabilitySuccessTest_promoCode() {
		setMockReferenceDataDAOHelper();
		setMockAuthToken();
		try {
			List<String> roomTypeIds = Arrays.asList(new String[] { "91dc7d4e-d531-4a73-8b1b-a92ed0d73b14" });
			List<String> itineraryIds = Arrays.asList(new String[] { "3836376577" });

			setMockForSingleRoomAvail("/single-room-avail.json");
			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2021-03-06"),
					LocalDate.parse("2021-03-07"))
					.programId("CATST")
					.roomTypeIds(roomTypeIds)
					.auroraItineraryIds(itineraryIds)
					.customerDominantPlay("SLOT")
					.customerRank(2)
					.promoCode("PROMO")
					.build();

			List<AuroraPriceResponse> response = roomPriceDAOStrategyACRSImpl.getRoomPrices(auroraPriceRequest);
			Assertions.assertNotNull(response);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, response.get(0).getStatus());
			Assertions.assertEquals(0.0, response.get(0).getBasePrice(), 0);
			Assertions.assertEquals(150.0, response.get(0).getDiscountedPrice(), 0);
		} catch (Exception e) {
			Assertions.fail("ACRSSingleRoomAvailabilitySuccessTest Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	/**
	 * Check Single Availability if sold out for given dates.
	 */
	@Test
	void getACRSSingleRoomSoldOutSuccessTest() {
		setMockReferenceDataDAOHelper();
		setMockAuthToken();
		try {
			List<String> roomTypeIds = Arrays.asList(new String[] { "91dc7d4e-d531-4a73-8b1b-a92ed0d73b14" });
			List<String> itineraryIds = Arrays.asList(new String[] { "3836376577" });

			setMockForSingleRoomAvail("/single-room-avail.json");
			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2021-03-11"),
					LocalDate.parse("2021-03-12"))
					.propertyId("ARIA1")
					.programId("CANR9")
					.roomTypeIds(roomTypeIds)
					.auroraItineraryIds(itineraryIds)
					.customerDominantPlay("SLOT")
					.customerRank(2)
					.build();

			List<AuroraPriceResponse> response = roomPriceDAOStrategyACRSImpl.getRoomPrices(auroraPriceRequest);
			Assertions.assertNotNull(response);
			Assertions.assertEquals(AvailabilityStatus.SOLDOUT, response.get(2).getStatus());
			// assertEquals("CANR9", response.get(2).getProgramId());
		} catch (Exception e) {
			Assertions.fail("ACRSSingleRoomSoldOutSuccessTest Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}
	
	/**
	 * Check Single Availability resort fees.
	 */
	@Test
	void getACRSSingleRoomResortFeeTest() {
		setMockReferenceDataDAOHelper();
		setMockAuthToken();
		try {
			List<String> roomTypeIds = Arrays.asList(new String[] { "91dc7d4e-d531-4a73-8b1b-a92ed0d73b14" });
			List<String> itineraryIds = Arrays.asList(new String[] { "3836376577" });

			setMockForSingleRoomAvail("/single-room-avail.json");
			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2021-12-10"),
					LocalDate.parse("2021-12-12"))
					.propertyId("ARIA1")
					.programId("CANR9")
					.roomTypeIds(roomTypeIds)
					.auroraItineraryIds(itineraryIds)
					.customerDominantPlay("SLOT")
					.customerRank(2)
					.build();

			List<AuroraPriceResponse> response = roomPriceDAOStrategyACRSImpl.getRoomPrices(auroraPriceRequest);
			Assertions.assertNotNull(response);
			Assertions.assertEquals(45.0, response.get(0).getResortFee(), 0);
		} catch (Exception e) {
			Assertions.fail("ACRSSingleRoomResortFeeTest Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}
}
