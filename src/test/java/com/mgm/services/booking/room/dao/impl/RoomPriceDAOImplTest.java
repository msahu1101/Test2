package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseRoomPriceBookingTest;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.AuroraPriceV3Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.util.ReservationUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

class RoomPriceDAOImplTest extends BaseRoomPriceBookingTest {

	private RoomPriceDAOStrategyACRSImpl acrsStrategy;
    private RoomPriceDAOStrategyGSEImpl gseStrategy;
    private ReferenceDataDAOHelper referenceDataDAOHelper;
	private SecretsProperties secretsProperties;
	private AcrsProperties acrsProperties;

	@InjectMocks
	private RoomPriceDAOImpl roomPriceDAOImpl;

	static Logger logger = LoggerFactory.getLogger(RoomPriceDAOImplTest.class);
	private String ACRS_PROPERTY_MOCK_KEY = "MockAcrsPropertyListSecretKey";

	@BeforeEach
	public void init() {
		BaseRoomPriceBookingTest.staticInit();
		this.acrsStrategy = Mockito.mock(RoomPriceDAOStrategyACRSImpl.class);
		this.gseStrategy = Mockito.mock(RoomPriceDAOStrategyGSEImpl.class);
		this.referenceDataDAOHelper = Mockito.mock(ReferenceDataDAOHelper.class);
		this.secretsProperties = Mockito.mock(SecretsProperties.class);
		this.acrsProperties = new AcrsProperties();
		this.acrsProperties.setAcrsPropertyListSecretKey(ACRS_PROPERTY_MOCK_KEY);
		this.roomPriceDAOImpl = new RoomPriceDAOImpl(acrsStrategy, gseStrategy, secretsProperties, acrsProperties,
				referenceDataDAOHelper);
	}

    private List<AuroraPriceResponse> getRoomAvailability() {
        File file = new File(getClass().getResource("/room-prices.json").getPath());

        List<AuroraPriceResponse> data = convert(file, mapper.getTypeFactory().constructCollectionType(List.class, AuroraPriceResponse.class));

        return data;
	}
    
    private List<AuroraPriceV3Response> getRoomAvailabilityV2() {
        File file = new File(getClass().getResource("/room-prices.json").getPath());

        List<AuroraPriceV3Response> data = convert(file, mapper.getTypeFactory().constructCollectionType(List.class, AuroraPriceV3Response.class));

        return data;
	}

	// parse MGMGLV availability from room-prices-v2-66964e2b-2550-4476-84c3-1a4c0c5c067f.json
	private AuroraPricesResponse getGSEAuroraPriceResponse() {
		String fileName = "/room-prices-v2-66964e2b-2550-4476-84c3-1a4c0c5c067f.json";
		return convert(fileName, AuroraPricesResponse.class);
	}

	// parse MV001 and MV021 from gse resorts prices mock response in room-prices-v2-gse-resorts.json
	private AuroraPricesResponse getGSEResortsAuroraPriceResponse() {
		String fileName = "/room-prices-v2-gse-resorts.json";
		return convert(fileName, AuroraPricesResponse.class);
	}

	// parse Partial ACRS resorts availability from room-prices-v2-acrs-resorts-MV021-MV276.json
	private List<AuroraPriceResponse> getACRSResortPriceResponse() {
		String fileName = "/room-prices-v2-acrs-resorts-MV021-MV276.json";
		return convertCrsList(fileName, AuroraPriceResponse.class);
	}

	// parse ACRS single avail response from room-prices-v2-acrs-MV276.json
	private AuroraPricesResponse getAcrsAuroraPriceResponseMV276() {
		String fileName = "/room-prices-v2-acrs-MV276.json";
		return convert(fileName, AuroraPricesResponse.class);
	}


	/**
     * Check Single Availability if available for given dates without mrd applicable.
     */
    @Test
	void getACRSSingleRoomAvailabilityWithOutMRDTest() {

        try {
			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2021-03-06"),
					LocalDate.parse("2021-03-07"))
					.programId("CATST")
					.promo("ZNVLCLS")
					.build();

            when(acrsStrategy.getRoomPrices(ArgumentMatchers.any())).thenReturn(getRoomAvailability());
            when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
            List<AuroraPriceResponse> response = roomPriceDAOImpl.getRoomPrices(auroraPriceRequest);

			Assertions.assertNotNull(response);
			Assertions.assertEquals(AvailabilityStatus.SOLDOUT, response.get(0).getStatus());
			Assertions.assertEquals(-1, response.get(0).getBasePrice(), 0.01);
			Assertions.assertEquals(-1, response.get(0).getDiscountedPrice(), 0.01);
			Assertions.assertEquals(0.0, response.get(0).getBaseMemberPrice(), 0.01);
			Assertions.assertEquals(0.0, response.get(0).getDiscountedMemberPrice(), 0.01);

			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, response.get(6).getStatus());
			Assertions.assertEquals(10019.99, response.get(6).getBasePrice(), 0.01);
			Assertions.assertEquals(5510.9945, response.get(6).getDiscountedPrice(), 0.01);
			Assertions.assertEquals(10019.99, response.get(6).getBaseMemberPrice(), 0.01);
			Assertions.assertEquals(5009.995, response.get(6).getDiscountedMemberPrice(), 0.01);
        } catch (Exception e) {
			Assertions.fail("ACRSSingleRoomAvailabilityWithOutMRDTest Failed");
			logger.error("ACRSSingleRoomAvailabilityWithOutMRDTest Failed with exception:", e);
        }
    }

    @Test
	void getRoomPricesV2Test() {
		// Create Request
		AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2024-03-06"),
				LocalDate.parse("2024-12-13"))
				.programId("CATST")
				.propertyId("8bf670c2-3e89-412b-9372-6c87a215e442")
				.enableMrd(true)
				.promo("ZNVLCLS")
				.build();

		// Setup Mocks for single ACRS property with no call to GSE
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(
				ArgumentMatchers.matches("MV(021|276)|(dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad)|(8bf670c2-3e89-412b-9372-6c87a215e442)")))
				.thenReturn(true);
		// note: This is needed for aCrsPropertyList() implementation
		when(secretsProperties.getSecretValue(ACRS_PROPERTY_MOCK_KEY)).thenReturn("MV021,MV276");

		when(acrsStrategy.getRoomPricesV2(ArgumentMatchers.any()))
				.thenReturn(getAcrsAuroraPriceResponseMV276());
		when(gseStrategy.getRoomPricesV2(ArgumentMatchers.any()))
				.thenThrow(new RuntimeException("GSE should not be called in this test case"));

		// Execute Method
		AuroraPricesResponse response = null;
		try {
			response = roomPriceDAOImpl.getRoomPricesV2(auroraPriceRequest);
		} catch (Exception e) {
			Assertions.fail("getRoomPricesV2Test Failed");
			logger.error("getRoomPricesV2Test Failed with exception:", e);
		}
		// Assert Response values
		Assertions.assertNotNull(response.getAuroraPrices());
		Assertions.assertTrue(response.isMrdPricing());

		Assertions.assertEquals(6, response.getAuroraPrices().size());
		Assertions.assertFalse(response.getAuroraPrices().stream()
				.anyMatch(auroraPriceResponse -> !"8bf670c2-3e89-412b-9372-6c87a215e442".equalsIgnoreCase(auroraPriceResponse.getPropertyId())));

		// validate available room type
		List<AuroraPriceResponse> availablePrices = response.getAuroraPrices().stream()
				.filter(auroraPriceResponse -> "ROOMCD-v-SELS-d-PROP-v-MV276".equalsIgnoreCase(auroraPriceResponse.getRoomTypeId()))
				.collect(Collectors.toList());
		Assertions.assertEquals(2, availablePrices.size());
		availablePrices.forEach(auroraPriceResponse -> {
			Assertions.assertFalse(auroraPriceResponse.isComp());
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, auroraPriceResponse.getStatus());
			Assertions.assertEquals("RPCD-v-BAR-d-PROP-v-MV276", auroraPriceResponse.getProgramId());
			Assertions.assertEquals(299.00, auroraPriceResponse.getBasePrice(), 0.01);
			Assertions.assertEquals(299.00, auroraPriceResponse.getDiscountedPrice(), 0.01);
			Assertions.assertEquals(39.00, auroraPriceResponse.getResortFee(), 0.01);
			Assertions.assertEquals(383.23, auroraPriceResponse.getAmtAftTax(), 0.01);
		});

		// validate unavailable room type
		List<AuroraPriceResponse> unavailablePrices = response.getAuroraPrices().stream()
				.filter(auroraPriceResponse -> "ROOMCD-v-SEWQ-d-PROP-v-MV276".equalsIgnoreCase(auroraPriceResponse.getRoomTypeId()))
				.collect(Collectors.toList());
		Assertions.assertEquals(2, unavailablePrices.size());
		unavailablePrices.forEach(auroraPriceResponse -> {
			Assertions.assertFalse(auroraPriceResponse.isComp());
			Assertions.assertEquals(AvailabilityStatus.SOLDOUT, auroraPriceResponse.getStatus());
			Assertions.assertEquals(-1.00, auroraPriceResponse.getDiscountedPrice(), 0.01);
			Assertions.assertEquals(0.00, auroraPriceResponse.getResortFee(), 0.01);
		});
	}

	@Test
	void getRoomPricesV2GSETest() {
		// Create Request
		AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2024-03-06"),
				LocalDate.parse("2024-12-13"))
				.programId("CATST")
				.propertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f")
				.enableMrd(true)
				.promo("ZNVLCLS")
				.build();

		// Setup Mocks for single GSE property with no call to ACRS
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(
				ArgumentMatchers.matches("MV(021|276)|(dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad)|(8bf670c2-3e89-412b-9372-6c87a215e442)")))
				.thenReturn(true);
		// note: This is needed for aCrsPropertyList() implementation
		when(secretsProperties.getSecretValue(ACRS_PROPERTY_MOCK_KEY)).thenReturn("MV021,MV276");

		when(gseStrategy.getRoomPricesV2(ArgumentMatchers.any()))
				.thenReturn(getGSEAuroraPriceResponse());
		when(acrsStrategy.getRoomPricesV2(ArgumentMatchers.any()))
				.thenThrow(new RuntimeException("ACRS should not be called in this test case"));

		// Execute Method
		AuroraPricesResponse response = null;
		try {
			response = roomPriceDAOImpl.getRoomPricesV2(auroraPriceRequest);
		} catch (Exception e) {
			Assertions.fail("getRoomPricesV2GSETest Failed");
			logger.error("getRoomPricesV2GSETest Failed with exception:", e);
		}

		//Assert response values
		List<AuroraPriceResponse> responsePrices = response.getAuroraPrices();
		Assertions.assertNotNull(responsePrices);
		Assertions.assertTrue(response.isMrdPricing());
		Assertions.assertEquals(12, responsePrices.size());
		Assertions.assertFalse(responsePrices.stream()
				.anyMatch(auroraPriceResponse -> !"66964e2b-2550-4476-84c3-1a4c0c5c067f".equalsIgnoreCase(auroraPriceResponse.getPropertyId())));

		// Assert values on a fully available room type
		List<AuroraPriceResponse> availablePrices = responsePrices.stream()
				.filter(auroraPriceResponse -> "23f5bef8-63ea-4ba9-a290-13b5a3056595".equalsIgnoreCase(auroraPriceResponse.getRoomTypeId()))
				.collect(Collectors.toList());
		Assertions.assertEquals(2, availablePrices.size());
		availablePrices.forEach(auroraPriceResponse -> {
			Assertions.assertFalse(auroraPriceResponse.isComp());
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, auroraPriceResponse.getStatus());
			Assertions.assertEquals(150.0, auroraPriceResponse.getDiscountedPrice(), 0.01);
			Assertions.assertEquals("89364848-c326-4319-a083-d5665df90349", auroraPriceResponse.getProgramId());
			Assertions.assertEquals("2e44c1cf-097c-4b0b-a86f-7993d239b055", auroraPriceResponse.getMemberProgramId());
		});

		// Assert values on a fully sold out room type
		List<AuroraPriceResponse> unavailablePrices = responsePrices.stream()
				.filter(auroraPriceResponse -> "c959d0b9-6392-42ec-a1e1-11463ba200dd".equalsIgnoreCase(auroraPriceResponse.getRoomTypeId()))
				.collect(Collectors.toList());
		Assertions.assertEquals(2, unavailablePrices.size());
		unavailablePrices.forEach(auroraPriceResponse -> {
			Assertions.assertFalse(auroraPriceResponse.isComp());
			Assertions.assertEquals(AvailabilityStatus.SOLDOUT, auroraPriceResponse.getStatus());
			Assertions.assertEquals(-1.0, auroraPriceResponse.getDiscountedPrice(), 0.01);
			Assertions.assertEquals(0.00, auroraPriceResponse.getResortFee(), 0.01);
		});

		// Assert values on a partially sold out room type
		List<AuroraPriceResponse> partialAvailablePrices = responsePrices.stream()
				.filter(auroraPriceResponse -> "8b6f42b4-e8a4-4921-80cc-35fe87be8acd".equalsIgnoreCase(auroraPriceResponse.getRoomTypeId()))
				.collect(Collectors.toList());
		Assertions.assertEquals(2, partialAvailablePrices.size());
		LocalDate checkInDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse("2024-12-13"));
		LocalDate secondNightDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse("2024-12-14"));

		Optional<AuroraPriceResponse> partialPricesFirstNightOptional = partialAvailablePrices.stream()
				.filter(auroraPriceResponse -> checkInDate.equals(ReservationUtil.convertDateToLocalDate(auroraPriceResponse.getDate())))
				// Use reduce to validate only 1 AuroraPriceResponse returns with these combined filters
				.reduce((a, b) -> {
					throw new IllegalStateException("Multiple elements: " + a + ", " + b);
				});
		partialPricesFirstNightOptional.orElseThrow(() -> new RuntimeException("getRoomPricesV2GSETest failed; Couldn't find expected price."));
		AuroraPriceResponse partialFirstNight = partialPricesFirstNightOptional.get();
		Assertions.assertEquals(AvailabilityStatus.SOLDOUT, partialFirstNight.getStatus());
		Assertions.assertEquals(-1.0, partialFirstNight.getDiscountedPrice(), 0.01);

		Optional<AuroraPriceResponse> partialPricesSecondNightOptional = partialAvailablePrices.stream()
				.filter(auroraPriceResponse -> secondNightDate.equals(ReservationUtil.convertDateToLocalDate(auroraPriceResponse.getDate())))
				// Use reduce to validate only 1 AuroraPriceResponse returns with these combined filters
				.reduce((a, b) -> {
					throw new IllegalStateException("Multiple elements: " + a + ", " + b);
				});
		partialPricesSecondNightOptional.orElseThrow(() -> new RuntimeException("getRoomPricesV2GSETest failed; Couldn't find expected price."));
		AuroraPriceResponse partialSecondNight = partialPricesSecondNightOptional.get();
		Assertions.assertFalse(partialSecondNight.isComp());
		Assertions.assertEquals(AvailabilityStatus.AVAILABLE, partialSecondNight.getStatus());
		Assertions.assertEquals(170.00, partialSecondNight.getDiscountedPrice(), 0.01);
		Assertions.assertEquals("89364848-c326-4319-a083-d5665df90349", partialSecondNight.getProgramId());
	}

	@Test
	void getCalendarPricesTest() {

        try {
			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2024-03-06"),
					LocalDate.parse("2024-03-07"))
					.programId("CATST")
					.promo("ZNVLCLS")
					.build();
		
            when(acrsStrategy.getCalendarPrices(ArgumentMatchers.any())).thenReturn(getRoomAvailability());
            when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
            List<AuroraPriceResponse> response = roomPriceDAOImpl.getCalendarPrices(auroraPriceRequest);

			Assertions.assertNotNull(response);
            
        } catch (Exception e) {
			Assertions.fail("getCalendarPricesTest Failed");
			logger.error("getCalendarPricesTest Failed with exception:", e);
        }
    }
    
    @Test
	void getCalendarPricesV2Test() {

        try {
			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2024-03-06"),
					LocalDate.parse("2024-03-07"))
					.programId("CATST")
					.promo("ZNVLCLS")
					.build();
		
            when(acrsStrategy.getCalendarPricesV2(ArgumentMatchers.any())).thenReturn(getRoomAvailability());
            when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
            List<AuroraPriceResponse> response = roomPriceDAOImpl.getCalendarPricesV2(auroraPriceRequest);

			Assertions.assertNotNull(response);
            
        } catch (Exception e) {
			Assertions.fail("getCalendarPricesV2Test Failed");
			logger.error("getCalendarPricesV2Test Failed with exception:", e);
        }
    }
    
    @Test
	void getIterableCalendarPricesTest() {

        try {
			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2024-03-06"),
					LocalDate.parse("2024-03-07"))
					.programId("CATST")
					.promo("ZNVLCLS")
					.build();
		
            when(acrsStrategy.getIterableCalendarPrices(ArgumentMatchers.any())).thenReturn(getRoomAvailability());
            when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
            List<AuroraPriceResponse> response = roomPriceDAOImpl.getIterableCalendarPrices(auroraPriceRequest);

			Assertions.assertNotNull(response);
            
        } catch (Exception e) {
			Assertions.fail("getIterableCalendarPricesTest Failed");
			logger.error("getIterableCalendarPricesTest Failed with exception:", e);
        }
    }

    @Test
	void getIterableCalendarPricesV2Test() {

        try {
			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2024-03-06"),
					LocalDate.parse("2024-03-07"))
					.programId("CATST")
					.promo("ZNVLCLS")
					.build();
		
            when(acrsStrategy.getIterableCalendarPricesV2(ArgumentMatchers.any())).thenReturn(getRoomAvailability());
            when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
            List<AuroraPriceResponse> response = roomPriceDAOImpl.getIterableCalendarPricesV2(auroraPriceRequest);

			Assertions.assertNotNull(response);
            
        } catch (Exception e) {
			Assertions.fail("getIterableCalendarPricesV2Test Failed");
			logger.error("getIterableCalendarPricesV2Test Failed with exception:", e);
        }
    }

    @Test
	void getLOSBasedCalendarPricesTest() {

		try {
			AuroraPriceV3Request auroraPriceV3Request = new AuroraPriceV3Request();
            when(acrsStrategy.getLOSBasedCalendarPrices(ArgumentMatchers.any())).thenReturn(getRoomAvailabilityV2());
            when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
            List<AuroraPriceV3Response> response = roomPriceDAOImpl.getLOSBasedCalendarPrices(auroraPriceV3Request);

			Assertions.assertNotNull(response);
            
        } catch (Exception e) {
			Assertions.fail("getLOSBasedCalendarPricesTest Failed");
			logger.error("getLOSBasedCalendarPricesTest Failed with exception:", e);
        }
    }
    
    @Test
	void getRoomPricesV2WithoutPropertyIdsTest() {
		// Create Mock request
		AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequestWithoutPropertyIds(LocalDate.parse("2024-03-06"),
				LocalDate.parse("2024-12-13"))
				.programId("CATST")
				.promo("ZNVLCLS")
				.build();

		// Setup the Mocks
		// note: below gse mock returns MV001(GSE Managed) and 1 entry of MV021 (ACRS managed)
		when(gseStrategy.getRoomPricesV2(ArgumentMatchers.any())).thenReturn(getGSEResortsAuroraPriceResponse());
		// note: below acrs mock returns limited availability for MV021 & MV276
		when(acrsStrategy.getResortPrices(ArgumentMatchers.any())).thenReturn(getACRSResortPriceResponse());
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(
				ArgumentMatchers.matches("MV(021|276)|(dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad)|(8bf670c2-3e89-412b-9372-6c87a215e442)")))
				.thenReturn(true);
		// note: This is needed for aCrsPropertyList() implementation
		when(secretsProperties.getSecretValue(ACRS_PROPERTY_MOCK_KEY)).thenReturn("MV021,MV276");
		Set<String> validPropertyIds = Stream.of("66964e2b-2550-4476-84c3-1a4c0c5c067f", "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", "8bf670c2-3e89-412b-9372-6c87a215e442")
				.collect(Collectors.toCollection(HashSet::new));

		// Execute the Method
		AuroraPricesResponse response = null;
		try {
			response = roomPriceDAOImpl.getRoomPricesV2(auroraPriceRequest);
		} catch (Exception e) {
			Assertions.fail("getRoomPricesV2WithoutPropertyIdsTest Failed");
			logger.error("getRoomPricesV2WithoutPropertyIdsTest Failed with exception:", e);
		}

		// Assert response
		LocalDate checkInDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse("2024-12-13"));
		LocalDate secondNightDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse("2024-12-14"));
		Assertions.assertNotNull(response.getAuroraPrices());
		Assertions.assertFalse(response.getAuroraPrices().stream()
				.anyMatch(auroraPriceResponse -> !validPropertyIds.contains(auroraPriceResponse.getPropertyId())));
		// Validate values from dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad (MV021) match what is in ACRS Json
		Optional<AuroraPriceResponse> optionalMV021Day1 = response.getAuroraPrices().stream()
				.filter(auroraPriceResponse -> "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad".equalsIgnoreCase(auroraPriceResponse.getPropertyId()))
				.filter(auroraPriceResponse -> checkInDate.equals(ReservationUtil.convertDateToLocalDate(auroraPriceResponse.getDate())))
				// Use reduce to validate only 1 AuroraPriceResponse returns with these combined filters
				.reduce((a, b) -> {
					throw new IllegalStateException("Multiple elements: " + a + ", " + b);
				});
		optionalMV021Day1.orElseThrow(() -> new RuntimeException("getRoomPricesV2WithoutPropertyIdsTest failed; Couldn't find expected price."));
		AuroraPriceResponse priceResponseMV021Day1 = optionalMV021Day1.get();
		Assertions.assertEquals("ROOMCD-v-DMAD-d-PROP-v-MV021", priceResponseMV021Day1.getRoomTypeId());
		Assertions.assertEquals(75.85, priceResponseMV021Day1.getDiscountedPrice(), 0.01);
		Assertions.assertEquals("RPCD-v-CASINO-d-PROP-v-MV021", priceResponseMV021Day1.getProgramId());
		Optional<AuroraPriceResponse> optionalMV021Day2 = response.getAuroraPrices().stream()
				.filter(auroraPriceResponse -> "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad".equalsIgnoreCase(auroraPriceResponse.getPropertyId()))
				.filter(auroraPriceResponse -> secondNightDate.equals(ReservationUtil.convertDateToLocalDate(auroraPriceResponse.getDate())))
				// Use reduce to validate only 1 AuroraPriceResponse returns with these combined filters
				.reduce((a, b) -> {
					throw new IllegalStateException("Multiple elements: " + a + ", " + b);
				});
		optionalMV021Day2.orElseThrow(() -> new RuntimeException("getRoomPricesV2WithoutPropertyIdsTest failed; Couldn't find expected price."));
		AuroraPriceResponse priceResponseMV021Day2 = optionalMV021Day2.get();
		Assertions.assertEquals("ROOMCD-v-DMAD-d-PROP-v-MV021", priceResponseMV021Day2.getRoomTypeId());
		Assertions.assertEquals(75.85, priceResponseMV021Day2.getDiscountedPrice(), 0.01);
		Assertions.assertEquals("RPCD-v-CASINO-d-PROP-v-MV021", priceResponseMV021Day2.getProgramId());

		// Validate values from 8bf670c2-3e89-412b-9372-6c87a215e442 (MV276) match what is in ACRS Json
		Optional<AuroraPriceResponse> optionalMV276Day1 = response.getAuroraPrices().stream()
				.filter(auroraPriceResponse -> "8bf670c2-3e89-412b-9372-6c87a215e442".equalsIgnoreCase(auroraPriceResponse.getPropertyId()))
				.filter(auroraPriceResponse -> checkInDate.equals(ReservationUtil.convertDateToLocalDate(auroraPriceResponse.getDate())))
				// Use reduce to validate only 1 AuroraPriceResponse returns with these combined filters
				.reduce((a, b) -> {
					throw new IllegalStateException("Multiple elements: " + a + ", " + b);
				});
		optionalMV276Day1.orElseThrow(() -> new RuntimeException("getRoomPricesV2WithoutPropertyIdsTest failed; Couldn't find expected price."));
		AuroraPriceResponse priceResponseMV276Day1 = optionalMV276Day1.get();
		Assertions.assertEquals("ROOMCD-v-SKST-d-PROP-v-MV276", priceResponseMV276Day1.getRoomTypeId());
		Assertions.assertEquals(185.00, priceResponseMV276Day1.getDiscountedPrice(), 0.01);
		Assertions.assertEquals("RPCD-v-BAR-d-PROP-v-MV276", priceResponseMV276Day1.getProgramId());
		Optional<AuroraPriceResponse> optionalMV276Day2 = response.getAuroraPrices().stream()
				.filter(auroraPriceResponse -> "8bf670c2-3e89-412b-9372-6c87a215e442".equalsIgnoreCase(auroraPriceResponse.getPropertyId()))
				.filter(auroraPriceResponse -> secondNightDate.equals(ReservationUtil.convertDateToLocalDate(auroraPriceResponse.getDate())))
				// Use reduce to validate only 1 AuroraPriceResponse returns with these combined filters
				.reduce((a, b) -> {
					throw new IllegalStateException("Multiple elements: " + a + ", " + b);
				});
		optionalMV276Day2.orElseThrow(() -> new RuntimeException("getRoomPricesV2WithoutPropertyIdsTest failed; Couldn't find expected price."));
		AuroraPriceResponse priceResponseMV276Day2 = optionalMV276Day2.get();
		Assertions.assertEquals("ROOMCD-v-SKST-d-PROP-v-MV276", priceResponseMV276Day2.getRoomTypeId());
		Assertions.assertEquals(185.00, priceResponseMV276Day2.getDiscountedPrice(), 0.01);
		Assertions.assertEquals("RPCD-v-BAR-d-PROP-v-MV276", priceResponseMV276Day2.getProgramId());

		// Validate values from 66964e2b-2550-4476-84c3-1a4c0c5c067f (MV001) match what is in GSE Json
		List<AuroraPriceResponse> priceResponsesMv001 = response.getAuroraPrices().stream()
				.filter(auroraPriceResponse -> "66964e2b-2550-4476-84c3-1a4c0c5c067f".equalsIgnoreCase(auroraPriceResponse.getPropertyId()))
				.collect(Collectors.toList());
		Assertions.assertEquals(12, priceResponsesMv001.size());
		//Assert Price and member price for specific room type in response
		priceResponsesMv001.stream()
				.filter(auroraPriceResponse -> "23f5bef8-63ea-4ba9-a290-13b5a3056595".equalsIgnoreCase(auroraPriceResponse.getRoomTypeId()))
				.forEach(auroraPriceResponse -> {
					Assertions.assertFalse(auroraPriceResponse.isComp());
					Assertions.assertEquals("89364848-c326-4319-a083-d5665df90349", auroraPriceResponse.getProgramId());
					Assertions.assertEquals("2e44c1cf-097c-4b0b-a86f-7993d239b055", auroraPriceResponse.getMemberProgramId());
					Assertions.assertEquals(160.00, auroraPriceResponse.getBasePrice(), 0.01);
					Assertions.assertEquals(AvailabilityStatus.AVAILABLE, auroraPriceResponse.getStatus());
				});
		//Assert Sold out by specific room type
		priceResponsesMv001.stream()
				.filter(auroraPriceResponse -> "c959d0b9-6392-42ec-a1e1-11463ba200dd".equalsIgnoreCase(auroraPriceResponse.getRoomTypeId()))
				.forEach(auroraPriceResponse -> {
					Assertions.assertEquals(AvailabilityStatus.SOLDOUT, auroraPriceResponse.getStatus());
					Assertions.assertEquals(-1.0, auroraPriceResponse.getDiscountedPrice(), 0.01);
				});

		// Validate value from ACRS managed property returned in GSE mock is not in response.
		Assertions.assertFalse(response.getAuroraPrices().stream()
				.filter(auroraPriceResponse -> "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad".equalsIgnoreCase(auroraPriceResponse.getPropertyId()))
				.anyMatch(auroraPriceResponse -> "managed-by-acrs-property-roomtype123".equalsIgnoreCase(auroraPriceResponse.getRoomTypeId())));
	}
}
