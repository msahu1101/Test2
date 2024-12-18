package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseAcrsRoomBookingTest;
import com.mgm.services.booking.room.BaseRoomPriceBookingTest;
import com.mgm.services.booking.room.exception.ACRSSearchOffersErrorData;
import com.mgm.services.booking.room.exception.ACRSSearchOffersErrorDetails;
import com.mgm.services.booking.room.exception.ACRSSearchOffersErrorRes;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.crs.calendarsearches.SuccessfulCalendarAvailability;
import com.mgm.services.booking.room.model.crs.searchoffers.Denial;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulMultiAvailability;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulSingleAvailability;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.AuroraPriceV3Request;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RoomPriceDAOStrategyACRSImplTest extends BaseAcrsRoomBookingTest {

	@Mock
	private static RestTemplate client;

	@InjectMocks
	private static DomainProperties domainProperties;

	@InjectMocks
	private static RestTemplateBuilder restTemplateBuilder;

	@InjectMocks
	private static AcrsProperties acrsProperties;
	
	@InjectMocks
	private static SecretsProperties secretsProperties;

	@Mock
	private static RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;

	@Mock
	private static GroupSearchDAOImpl groupSearchDAOImpl;

	static Logger logger = LoggerFactory.getLogger(RoomPriceDAOStrategyACRSImplTest.class);

	@BeforeEach
	public void init() {
		super.init();
		client = Mockito.mock(RestTemplate.class);
		groupSearchDAOImpl = Mockito.mock(GroupSearchDAOImpl.class);
		domainProperties = new DomainProperties();
		domainProperties.setCrs("ABC");
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		secretsProperties = Mockito.mock(SecretsProperties.class);
		acrsProperties = new AcrsProperties();
		acrsProperties.setModifyDateStartPath("modifyDateStartPath");
		acrsProperties.setModifyDateEndPath("modifyDateEndPath");
		acrsProperties.setModifySpecialRequestPath("modifySpecialRequestPath");
		acrsProperties.setDefaultBasePriceRatePlan("PREVL");
		acrsProperties.setMaxPropertiesForResortPricing(1);
		acrsProperties.setLiveCRS(true);
		urlProperties.setAcrsCalendarAvailabilitySearch(
				"/v2/availability/calendar?startDate=2020-08-11&endDate=2020-11-12&propertyId=dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad&numAdults=2&numRooms=2&isGroupCode=true");
		urlProperties.setAcrsAvailabilityReservation(
				"/hotel-platform/cit/mgm/v6/hotel/reservations/MGM?property_code={property_code}&start_date={start_date}&end_date={end_date}");
		urlProperties.setAcrsMultiAvailabilityReservation(
				"/hotel-platform/cit/mgm/v6/hotel/reservations/MGM?property_code={property_code}&start_date={start_date}&end_date={end_date}");
		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		
		when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(),
				acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeout(),
				applicationProperties.getReadTimeOut(),
				applicationProperties.getSocketTimeOut(),
				1,applicationProperties.getCrsRestTTL()))
				.thenReturn(client);
		roomPriceDAOStrategyACRSImpl = new RoomPriceDAOStrategyACRSImpl(urlProperties, domainProperties,
				applicationProperties, acrsProperties, restTemplateBuilder, groupSearchDAOImpl, referenceDataDAOHelper, acrsOAuthTokenDAOImpl, secretsProperties);
	}

	/**
	 * Return Single Availability from JSON mock file.
	 */
	private HttpEntity<?> getCrsSingleRoomAvail() {
		File file = new File(getClass().getResource("/single-room-avail_groupCode.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<>(
				convertCrs(file, SuccessfulSingleAvailability.class), HttpStatus.OK);
		return response;

	}

	private void setMockForSingleRoomAvail() {
		when(client.exchange(ArgumentMatchers.contains("reservations"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<SuccessfulSingleAvailability>>any(), Mockito.anyMap()))
						.thenReturn((ResponseEntity<SuccessfulSingleAvailability>) getCrsSingleRoomAvail());
	}

	private HttpEntity<?> getCrsSingleRoomAvailDenials() {
		File file =
				new File(getClass().getResource("/acrs/singleavailabilityv2/single-room-avail_denials.json").getPath());
		return new ResponseEntity<>(
				convertCrs(file, SuccessfulSingleAvailability.class), HttpStatus.OK);
	}

	private void setMockForSingleRoomAvailDenials() {
		when(client.exchange(ArgumentMatchers.contains("reservations"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<SuccessfulSingleAvailability>>any(), Mockito.anyMap()))
				.thenReturn((ResponseEntity<SuccessfulSingleAvailability>) getCrsSingleRoomAvailDenials());
	}
	
	private HttpEntity<?> getCrsGamingBucket() {
		File file =new File(getClass().getResource("/acrs/singleavailabilityv2/single-room-avail_denials.json").getPath());
		SuccessfulSingleAvailability singleAvail = convertCrs(file, SuccessfulSingleAvailability.class);
		singleAvail.getData().getRatePlans().get(0).setGamingBucket("COMP");
		return new ResponseEntity<>(
				singleAvail, HttpStatus.OK);
	}
	
	private void setMockForGamingBucket() {
		when(client.exchange(ArgumentMatchers.contains("reservations"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<SuccessfulSingleAvailability>>any(), Mockito.anyMap()))
				.thenReturn((ResponseEntity<SuccessfulSingleAvailability>) getCrsGamingBucket());
	}
	
	private void setMockReferenceDataDAOHelper() {        
	        when(referenceDataDAOHelper.getAcrsVendor(Mockito.any())).thenReturn("ICECC");
	   }
	/**
	 * Return CalendarSearch from JSON mock file.
	 */
	private HttpEntity<?> getCalendarSearch1() {
		File file = new File(getClass().getResource("/calendar_availability_1.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<>(
				convertCrs(file, SuccessfulCalendarAvailability.class), HttpStatus.OK);
		return response;

	}
	
	/**
	 * Return CalendarSearch from JSON mock file.
	 */
	private HttpEntity<?> getCalendarSearch2() {
		File file = new File(getClass().getResource("/calendar_availability_2.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<>(
				convertCrs(file, SuccessfulCalendarAvailability.class), HttpStatus.OK);
		return response;

	}
	
	private void setMockForCalendarSearch() {
		when(client.exchange(ArgumentMatchers.contains("calendar"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<SuccessfulCalendarAvailability>>any(),
				Mockito.anyMap())).then(a->{
					
					if (((Map<String, String>) a.getArgument(4)).get("start_date").equals("2020-08-10")) {
						return getCalendarSearch1();
					} else if (((Map<String, String>) a.getArgument(4)).get("start_date").equals("2020-10-12")) {
						return getCalendarSearch2();
					}
					return null;

				});
	}
	
	private HttpEntity<?> getCrsCalendarSearch_PO() {
		File file = new File(getClass().getResource("/calendar_availability_PO.json").getPath());
		ResponseEntity<?> response = new ResponseEntity<>(
				convertCrs(file, SuccessfulCalendarAvailability.class), HttpStatus.OK);
		return response;

	}
	
	private void setMockForCalendarSearch_PO() {
		when(client.exchange(ArgumentMatchers.contains("calendar"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<SuccessfulCalendarAvailability>>any(), Mockito.anyMap()))
						.thenReturn((ResponseEntity<SuccessfulCalendarAvailability>) getCrsCalendarSearch_PO());
	}
	
	 private HttpEntity<?> getCrsMultiRoomAvail() {
	        File file = new File(getClass().getResource("/multi-room-avail.json").getPath());
	        ResponseEntity<?> response = new ResponseEntity<SuccessfulMultiAvailability>(
	                convertCrs(file, SuccessfulMultiAvailability.class), HttpStatus.OK);
	        return response;

	 }
	 
	 private void setMockForMultiRoomAvail() {
	        when(client.exchange(ArgumentMatchers.contains("reservations"), ArgumentMatchers.any(HttpMethod.class),
	                ArgumentMatchers.any(), ArgumentMatchers.<Class<SuccessfulMultiAvailability>>any(), Mockito.anyMap()))
	                .thenReturn((ResponseEntity<SuccessfulMultiAvailability>) getCrsMultiRoomAvail());
	 }
	
	/**
	 * with > 62 days and isGroupCode = true
	 */
	@Test
	void testGetIterableCalendarPrices_success_groupCode_true_noOfDays_greater_62() {
		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse(
				"2020-08-10"),
				LocalDate.parse("2020-10-14"))
				.programId("GR1")
				.isGroupCode(true)
				.build();
		setMockForRoomPropertyCode();
		setMockAuthToken();

		List<GroupSearchV2Response> groupSearchResponseList = new ArrayList<>();
		GroupSearchV2Response groupSearchResponse = new GroupSearchV2Response();
		try {
			groupSearchResponse.setPeriodStartDate(fmt.parse("2020-10-04"));
			groupSearchResponse.setPeriodEndDate(fmt.parse("2020-10-05"));
		} catch (ParseException e) {
			fail("Unexpected ParseException caught while constructing request.");
		}
		groupSearchResponse.setOperaBlockCode("GR1");
		groupSearchResponseList.add(groupSearchResponse);

		when(groupSearchDAOImpl.searchGroup(ArgumentMatchers.any(GroupSearchV2Request.class)))
				.thenReturn(groupSearchResponseList);

		setMockForSingleRoomAvail();

		setMockForCalendarSearch();

		final List<AuroraPriceResponse> calendarPricesResponseList = roomPriceDAOStrategyACRSImpl.getIterableCalendarPrices(auroraPriceRequest);

		assertEquals(67, calendarPricesResponseList.size());
		assertEquals("2020-08-10", fmt.format(calendarPricesResponseList.get(0).getDate()));
		assertEquals(AvailabilityStatus.AVAILABLE, calendarPricesResponseList.get(1).getStatus());
		assertEquals(55.89, calendarPricesResponseList.get(2).getDiscountedPrice(), 0);
		assertEquals("TDAAA", calendarPricesResponseList.get(11).getProgramId());
		assertEquals(80.19, calendarPricesResponseList.get(38).getDiscountedPrice(), 0);

		// calendarPricesResponseList.get(55) is from single avail response
		assertEquals(AvailabilityStatus.AVAILABLE, calendarPricesResponseList.get(55).getStatus());
		assertEquals("2020-10-04", fmt.format(calendarPricesResponseList.get(55).getDate()));
		assertEquals(96.39, calendarPricesResponseList.get(55).getDiscountedPrice(), 0);
		assertEquals("TDAAA", calendarPricesResponseList.get(55).getProgramId());

		assertEquals("2020-10-10", fmt.format(calendarPricesResponseList.get(61).getDate()));
		assertEquals("2020-10-11", fmt.format(calendarPricesResponseList.get(62).getDate()));
	}

	@Test
	void testGetIterableCalendarPrices_fail_multipleGroupSearchResponse() {
		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2020-08-10"),
				LocalDate.parse("2020-10-12"))
				.programId("GRPCD-v-GR1-d-PROP-v-MV021")
				.isGroupCode(true)
				.build();
		setMockForRoomPropertyCode();
		setMockAuthToken();

		List<GroupSearchV2Response> groupSearchResponseList = new ArrayList<>();
		try {
			GroupSearchV2Response groupSearchResponse = new GroupSearchV2Response();
			groupSearchResponse.setPeriodStartDate(fmt.parse("2020-10-04"));
			groupSearchResponse.setPeriodEndDate(fmt.parse("2020-10-05"));
			groupSearchResponse.setOperaBlockCode("GR1");
			groupSearchResponseList.add(groupSearchResponse);

			GroupSearchV2Response groupSearchResponse1 = new GroupSearchV2Response();
			groupSearchResponse1.setPeriodStartDate(fmt.parse("2020-10-06"));
			groupSearchResponse1.setPeriodEndDate(fmt.parse("2020-10-07"));
			groupSearchResponse1.setOperaBlockCode("GR2");
			groupSearchResponseList.add(groupSearchResponse1);
		} catch (ParseException ex){
			fail("Unexpected Parse exception while creating request.");
		}
		when(groupSearchDAOImpl.searchGroup(ArgumentMatchers.any(GroupSearchV2Request.class)))
				.thenReturn(groupSearchResponseList);
		try {
			roomPriceDAOStrategyACRSImpl.getIterableCalendarPrices(auroraPriceRequest);
			fail("should fail");
		}catch(BusinessException e){
			assertEquals(ErrorCode.FAILURE_GROUP_SEARCH, e.getErrorCode());
		} catch (Exception ex){
			fail("We expected a BusinessException, failing test case for any other exception.");
		}
	}
	
	/**
	 * with > 62 days and isGroupCode = false
	 */    
	@Test
	void testGetIterableCalendarPrices_success_groupCode_false() {
		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2020-08-10"),
				LocalDate.parse("2020-10-14"))
				.programId("TDAAA")
				.isGroupCode(false)
				.build();
		setMockForRoomPropertyCode();
		setMockAuthToken();
		setMockForCalendarSearch();

		final List<AuroraPriceResponse> calendarPricesResponseList = roomPriceDAOStrategyACRSImpl.getIterableCalendarPrices(auroraPriceRequest);

		assertEquals(67, calendarPricesResponseList.size());
		assertEquals("2020-08-10", fmt.format(calendarPricesResponseList.get(0).getDate()));
		assertEquals(AvailabilityStatus.AVAILABLE, calendarPricesResponseList.get(1).getStatus());
		assertEquals(55.89, calendarPricesResponseList.get(2).getDiscountedPrice(), 0);
		assertEquals("TDAAA", calendarPricesResponseList.get(11).getProgramId());
		assertEquals(80.19, calendarPricesResponseList.get(38).getDiscountedPrice(), 0);

		// change as groupcode is false
		assertEquals(AvailabilityStatus.AVAILABLE, calendarPricesResponseList.get(55).getStatus());
		assertEquals("2020-10-04", fmt.format(calendarPricesResponseList.get(55).getDate()));
		assertEquals(96.39, calendarPricesResponseList.get(55).getDiscountedPrice(), 0);
		assertEquals("TDAAA", calendarPricesResponseList.get(55).getProgramId());

		assertEquals("2020-10-11", fmt.format(calendarPricesResponseList.get(62).getDate()));
	}
	private AuroraPriceV3Request getAuroraV3Request(AuroraPriceRequest auroraPriceRequest) {
		return AuroraPriceV3Request.builder().propertyId(auroraPriceRequest.getPropertyId())
				.roomTypeIds(auroraPriceRequest.getRoomTypeIds()).customerId(auroraPriceRequest.getCustomerId())
				.checkInDate(auroraPriceRequest.getCheckInDate()).checkOutDate(auroraPriceRequest.getCheckOutDate())
				.numGuests(auroraPriceRequest.getNumGuests()).programId(auroraPriceRequest.getProgramId())
				.source(auroraPriceRequest.getSource()).auroraItineraryIds(auroraPriceRequest.getAuroraItineraryIds())
				.programRate(auroraPriceRequest.isProgramRate()).numChildren(auroraPriceRequest.getNumChildren())
				.ignoreChannelMargins(auroraPriceRequest.isIgnoreChannelMargins())
				.numRooms(auroraPriceRequest.getNumRooms())
				.operaConfirmationNumber(auroraPriceRequest.getOperaConfirmationNumber())
				.customerDominantPlay(auroraPriceRequest.getCustomerDominantPlay())
				.customerRank(auroraPriceRequest.getCustomerRank()).isGroupCode(auroraPriceRequest.isGroupCode())
				.isPerpetualOffer(auroraPriceRequest.isPerpetualOffer()).mlifeNumber(auroraPriceRequest.getMlifeNumber())
				.tripLength(3).build();
	}

	@Test
	void testGetLOSBasedCalendarPrices_success_po_false() {
		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2020-08-10"),
				LocalDate.parse("2020-10-14"))
				.programId("TDAAA")
				.isPerpetualOffer(false)
				.build();

		AuroraPriceV3Request auroraPriceRequestV3 = getAuroraV3Request(auroraPriceRequest);

		setMockForRoomPropertyCode("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", "MV021", "DMDQ",
				"TDAAA", "ICECC");
		setMockAuthToken();

		setMockForCalendarSearch();

		List<AuroraPriceV3Response> auroraPriceV3ResponseList = roomPriceDAOStrategyACRSImpl
				.getLOSBasedCalendarPrices(auroraPriceRequestV3);

		assertEquals(67, auroraPriceV3ResponseList.size());
		assertEquals("2020-08-10", fmt.format(auroraPriceV3ResponseList.get(0).getDate()));
		assertNotNull(auroraPriceV3ResponseList.get(0).getTripDetails());
		assertEquals(auroraPriceRequestV3.getTripLength(), auroraPriceV3ResponseList.get(0).getTripDetails().size());
		assertEquals("2020-08-10", fmt.format(auroraPriceV3ResponseList.get(0).getTripDetails().get(0).getDate()));
		assertEquals("2020-08-11", fmt.format(auroraPriceV3ResponseList.get(1).getTripDetails().get(0).getDate()));
		assertEquals("2020-08-12", fmt.format(auroraPriceV3ResponseList.get(2).getTripDetails().get(0).getDate()));
		assertEquals("TDAAA", auroraPriceV3ResponseList.get(2).getTripDetails().get(0).getProgramId());
		assertEquals(55.89, auroraPriceV3ResponseList.get(0).getTotalNightlyTripPrice(), 0);
		assertFalse(auroraPriceV3ResponseList.get(0).isPOApplicable());
		assertEquals("DPRK", auroraPriceV3ResponseList.get(0).getRoomTypeId());
		assertEquals("2020-08-11", fmt.format(auroraPriceV3ResponseList.get(1).getDate()));
		assertNotNull(auroraPriceV3ResponseList.get(2).getTripDetails());
		assertFalse(auroraPriceV3ResponseList.get(45).getTripDetails().get(2).isComp());
		assertEquals(auroraPriceRequestV3.getTripLength(), auroraPriceV3ResponseList.get(3).getTripDetails().size());
		assertEquals("2020-08-15", fmt.format(auroraPriceV3ResponseList.get(5).getTripDetails().get(0).getDate()));
		assertEquals("2020-08-26", fmt.format(auroraPriceV3ResponseList.get(15).getTripDetails().get(1).getDate()));
		assertEquals("2020-09-07", fmt.format(auroraPriceV3ResponseList.get(26).getTripDetails().get(2).getDate()));
		assertEquals("TDAAA", auroraPriceV3ResponseList.get(32).getTripDetails().get(0).getProgramId());
		assertEquals(-1.0, auroraPriceV3ResponseList.get(50).getTotalNightlyTripPrice(), 0);
		assertNull(auroraPriceV3ResponseList.get(50).getTripDetails());
		assertEquals(AvailabilityStatus.SOLDOUT, auroraPriceV3ResponseList.get(50).getStatus());
		assertEquals(AvailabilityStatus.AVAILABLE, auroraPriceV3ResponseList.get(44).getStatus());
		assertFalse(auroraPriceV3ResponseList.get(60).isPOApplicable());
		assertEquals("DPRK", auroraPriceV3ResponseList.get(63).getRoomTypeId());
	}

	@Test
	void testGetLOSBasedCalendarPrices_success_po_true() {
		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2021-02-13"),
				LocalDate.parse("2021-02-18"))
				.programId("COMP10")
				.isPerpetualOffer(true)
				.build();

		AuroraPriceV3Request auroraPriceRequestV3 = getAuroraV3Request(auroraPriceRequest);

		setMockForRoomPropertyCode("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", "MV021", "DMDQ",
				"COMP10", "ICECC");
		setMockAuthToken();
		setMockForCalendarSearch_PO();

		List<AuroraPriceV3Response> auroraPriceV3ResponseList = roomPriceDAOStrategyACRSImpl
				.getLOSBasedCalendarPrices(auroraPriceRequestV3);

		assertEquals(6, auroraPriceV3ResponseList.size());
		assertEquals("2021-02-13", fmt.format(auroraPriceV3ResponseList.get(0).getDate()));
		assertNotNull(auroraPriceV3ResponseList.get(0).getTripDetails());
		assertEquals(auroraPriceRequestV3.getTripLength(), auroraPriceV3ResponseList.get(0).getTripDetails().size());
		assertEquals("2021-02-13", fmt.format(auroraPriceV3ResponseList.get(0).getTripDetails().get(0).getDate()));
		assertEquals("2021-02-14", fmt.format(auroraPriceV3ResponseList.get(1).getTripDetails().get(0).getDate()));
		assertEquals("2021-02-15", fmt.format(auroraPriceV3ResponseList.get(2).getTripDetails().get(0).getDate()));
		assertEquals("COMP10", auroraPriceV3ResponseList.get(2).getTripDetails().get(0).getProgramId());
		assertTrue(auroraPriceV3ResponseList.get(2).getTripDetails().get(0).isComp());
		assertEquals(99.00, auroraPriceV3ResponseList.get(0).getTotalNightlyTripPrice(), 0);
		assertTrue(auroraPriceV3ResponseList.get(0).isPOApplicable());
		assertEquals("DMDQ", auroraPriceV3ResponseList.get(0).getRoomTypeId());
	}

	@Test
	void testGetIterableCalendarPrices_success_PO() {
		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2021-02-13"),
				LocalDate.parse("2021-02-18"))
				.programId("COMP10")
				.isPerpetualOffer(true)
				.build();
		setMockForRoomPropertyCode("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", "MV021", "DMDQ",
				"COMP10", "ICECC");
		setMockAuthToken();
		setMockForCalendarSearch_PO();

		final List<AuroraPriceResponse> calendarPricesResponseList = roomPriceDAOStrategyACRSImpl.getIterableCalendarPrices(auroraPriceRequest);

		assertEquals(6, calendarPricesResponseList.size());
		assertEquals("2021-02-13", fmt.format(calendarPricesResponseList.get(0).getDate()));
		assertEquals(AvailabilityStatus.AVAILABLE, calendarPricesResponseList.get(1).getStatus());
		assertEquals(99.00, calendarPricesResponseList.get(2).getDiscountedPrice(), 0);
		assertEquals("COMP10", calendarPricesResponseList.get(3).getProgramId());
		assertEquals(99.00, calendarPricesResponseList.get(4).getDiscountedPrice(), 0);
		assertTrue(calendarPricesResponseList.get(2).isComp());
		assertTrue(calendarPricesResponseList.get(2).isPOApplicable());
	}

	@Test
	void testGetIterableCalendarPrices_promoCode() {
		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2021-02-13"),
				LocalDate.parse("2021-02-18"))
				.programId("PREVL")
				.promoCode("PROMO")
				.isPerpetualOffer(true)
				.build();
		setMockForRoomPropertyCode();
		setMockAuthToken();
		setMockForCalendarSearch_PO();

		final List<AuroraPriceResponse> calendarPricesResponseList = roomPriceDAOStrategyACRSImpl.getIterableCalendarPrices(auroraPriceRequest);

		assertEquals(6, calendarPricesResponseList.size());
		assertEquals("2021-02-13", fmt.format(calendarPricesResponseList.get(0).getDate()));
		assertEquals(AvailabilityStatus.AVAILABLE, calendarPricesResponseList.get(1).getStatus());
		assertEquals(99.00, calendarPricesResponseList.get(2).getDiscountedPrice(), 0);
		assertEquals("COMP10", calendarPricesResponseList.get(3).getProgramId());
		assertEquals(99.00, calendarPricesResponseList.get(4).getDiscountedPrice(), 0);
		assertTrue(calendarPricesResponseList.get(2).isComp());
		assertTrue(calendarPricesResponseList.get(2).isPOApplicable());
	}

	@Test
	void testGetLOSBasedCalendarPrices_success_po_with_basePrice() {
		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2021-02-13"),
				LocalDate.parse("2021-02-18"))
				.programId("COMP10")
				.isPerpetualOffer(true)
				.build();

		AuroraPriceV3Request auroraPriceRequestV3 = getAuroraV3Request(auroraPriceRequest);

		setMockForRoomPropertyCode("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", "MV021", "DMDQ",
				"COMP10", "ICECC");
		setMockAuthToken();
		setMockForCalendarSearch_PO();

		List<AuroraPriceV3Response> auroraPriceV3ResponseList = roomPriceDAOStrategyACRSImpl
				.getLOSBasedCalendarPrices(auroraPriceRequestV3);

		assertEquals(6, auroraPriceV3ResponseList.size());
		assertEquals("2021-02-13", fmt.format(auroraPriceV3ResponseList.get(0).getDate()));
		assertNotNull(auroraPriceV3ResponseList.get(0).getTripDetails());
		assertEquals(auroraPriceRequestV3.getTripLength(), auroraPriceV3ResponseList.get(0).getTripDetails().size());
		assertEquals("2021-02-13", fmt.format(auroraPriceV3ResponseList.get(0).getTripDetails().get(0).getDate()));
		assertEquals("2021-02-14", fmt.format(auroraPriceV3ResponseList.get(1).getTripDetails().get(0).getDate()));
		assertEquals("2021-02-15", fmt.format(auroraPriceV3ResponseList.get(2).getTripDetails().get(0).getDate()));
		assertEquals("COMP10", auroraPriceV3ResponseList.get(2).getTripDetails().get(0).getProgramId());
		assertTrue(auroraPriceV3ResponseList.get(2).getTripDetails().get(0).isComp());
		assertEquals(99.00, auroraPriceV3ResponseList.get(0).getTotalNightlyTripPrice(), 0);
		assertEquals(99.00, auroraPriceV3ResponseList.get(0).getTotalNightlyTripBasePrice(), 0);
		assertTrue(auroraPriceV3ResponseList.get(0).isPOApplicable());
		assertEquals("DMDQ", auroraPriceV3ResponseList.get(0).getRoomTypeId());
	}

	@Test()
	void singleAvailPromoCodeFailWithDenial() {
		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2023-12-13"),
				LocalDate.parse("2023-12-18"))
				.promoCode("testPromo")
				.build();

		setMockForRoomPropertyCode();
		setMockAuthToken();
		setMockForSingleRoomAvailDenials();
		try {
			List<AuroraPriceResponse> auroraPriceResponseList =
					roomPriceDAOStrategyACRSImpl.getRoomPrices(auroraPriceRequest);
			assertNull(auroraPriceResponseList); // Should not execute
		} catch(BusinessException be) {
			assertEquals(ErrorCode.UNABLE_TO_PRICE, be.getErrorCode());
		} catch (Exception ex) {
			fail("Caught unexpected Exception during singleAvailPromoCodeFailWithDenial Test.");
		}
	}
	
	@Test
    void testGetResortPrices() throws InterruptedException, ExecutionException {
		  try {
				setMockAuthToken();
				setMockForRoomPropertyCode();
				setMockAuthToken();
				setMockForSingleRoomAvailDenials();
				setMockForMultiRoomAvail();
			  setMockReferenceDataDAOHelper();

			  applicationProperties.setTempInfoLogEnabled("1234");
				when(secretsProperties.getSecretValue("1234")).thenReturn("true");
				LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2021-03-11"),
						LocalDate.parse("2021-03-12"))
						.propertyIds(Arrays.asList("MV021", "ARIA1"))
						.propertyId("ARIA1")
						.programId("CATST")
						.promo("ZNVLCLS")
						.mlifeNumber("12334")
						.checkInDate(checkInDate)
						.checkOutDate(checkInDate.plusDays(3))
						.build();
	            List<AuroraPriceResponse> response = roomPriceDAOStrategyACRSImpl.getResortPrices(auroraPriceRequest);

	            assertNotNull(response);
	            assertEquals(AvailabilityStatus.AVAILABLE, response.get(0).getStatus());
	            assertEquals("MV021", response.get(0).getPropertyId());
	            assertEquals(19.0, response.get(0).getBasePrice(), 0);

	        } catch (Exception e) {
	            e.printStackTrace();
	            fail("testGetResortPrices Failed");
	            logger.error(e.getMessage());
	            logger.error("Cause: " + e.getCause());
	        }
	    } 
	 
	@Test
	void calendarPricesV2Test() {
			AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2020-08-10"),
					LocalDate.parse("2020-10-14"))
					.programId("TDAAA")
					.isGroupCode(false)
					.build();
			setMockForRoomPropertyCode();
			setMockAuthToken();
			setMockForCalendarSearch();

			final List<AuroraPriceResponse> calendarPricesResponseList = roomPriceDAOStrategyACRSImpl.getCalendarPrices(auroraPriceRequest);

			assertEquals(67, calendarPricesResponseList.size());
			assertEquals("2020-08-10", fmt.format(calendarPricesResponseList.get(0).getDate()));
			assertEquals(AvailabilityStatus.AVAILABLE, calendarPricesResponseList.get(1).getStatus());
			assertEquals(55.89, calendarPricesResponseList.get(2).getDiscountedPrice(), 0);
			assertEquals("TDAAA", calendarPricesResponseList.get(11).getProgramId());
			assertEquals(80.19, calendarPricesResponseList.get(38).getDiscountedPrice(), 0);

			// change as groupcode is false
			assertEquals(AvailabilityStatus.AVAILABLE, calendarPricesResponseList.get(55).getStatus());
			assertEquals("2020-10-04", fmt.format(calendarPricesResponseList.get(55).getDate()));
			assertEquals(96.39, calendarPricesResponseList.get(55).getDiscountedPrice(), 0);
			assertEquals("TDAAA", calendarPricesResponseList.get(55).getProgramId());

			assertEquals("2020-10-11", fmt.format(calendarPricesResponseList.get(62).getDate()));
		}
	

	@Test()
	void getRoomPricesTest() {
		try {
		setMockForRoomPropertyCode();
		setMockAuthToken();
		setMockForGamingBucket();
		
		LocalDate checkInDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		AuroraPriceRequest auroraPriceRequest = BaseRoomPriceBookingTest.makeBaseAuroraPriceRequest(LocalDate.parse("2023-12-13"),
				LocalDate.parse("2023-12-18"))
				.isPerpetualOffer(true)
				.promoCode("testPromo")
				.propertyId("ARIA1")
				.programId(null)
				.checkInDate(checkInDate)
				.checkOutDate(checkInDate.plusDays(3))
				.source("abc")
				.promo("test")
				.roomTypeIds(Arrays.asList("MV21", "MV22"))
				.numGuests(3)
				.numChildren(2)
				.includeDefaultRatePlans(true)
				.includeSoldOutRooms(true)
				.build();
		
			assertThatThrownBy(() ->roomPriceDAOStrategyACRSImpl.getRoomPrices(auroraPriceRequest))
			.isInstanceOf(BusinessException.class);
		} catch(BusinessException be) {
			assertEquals(ErrorCode.UNABLE_TO_PRICE, be.getErrorCode());
		} catch (Exception ex) {
			fail("Caught unexpected Exception during getRoomPricesTest Test.");
		}
	}
	
	
	@Test
	void testHasError() throws IOException {
		RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);
		assertTrue(result);
	}
	
	@Test
	void testHandleErrorSystemExceptionSYSTEM_ERROR() throws IOException {
		RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSSearchOffersErrorRes acrsSearchOffersErrorRes = new ACRSSearchOffersErrorRes();
		ACRSSearchOffersErrorDetails error = new ACRSSearchOffersErrorDetails();
		error.setHttpStatus(501);
		acrsSearchOffersErrorRes.setError(error);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsSearchOffersErrorRes);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.CONTINUE);

		// Assertions
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorSystemExceptionUNABLE_TO_PRICE() throws IOException {
		RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSSearchOffersErrorRes acrsSearchOffersErrorRes = new ACRSSearchOffersErrorRes();
		ACRSSearchOffersErrorDetails error = new ACRSSearchOffersErrorDetails();
		error.setHttpStatus(400);
		error.setCode(50025);
		error.setMessage("no availability for number of products requested");
		acrsSearchOffersErrorRes.setError(error);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsSearchOffersErrorRes);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		Assertions.assertSame(ErrorCode.UNABLE_TO_PRICE, ex.getErrorCode());
	}
	
	@Test
	void testHandleErrorElse() throws IOException {
		RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSSearchOffersErrorRes acrsSearchOffersErrorRes = new ACRSSearchOffersErrorRes();
		acrsSearchOffersErrorRes.setError(null);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsSearchOffersErrorRes);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.CONTINUE);

		// Assertions
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		Assertions.assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
	
	
	@ParameterizedTest
	@CsvSource(value = {
			"50001, OFFER_NOT_AVAILABLE",
			"50002, OFFER_NOT_AVAILABLE",
			"50003, OFFER_NOT_AVAILABLE",
			"50004, OFFER_NOT_AVAILABLE",
			"50005, OFFER_NOT_AVAILABLE",
			"50009, OFFER_NOT_AVAILABLE",
			"50006, OFFER_NOT_AVAILABLE",
			
			"50037, INVALID_PROPERTY",
			"50010, INVALID_PROPERTY",
			"50014, INVALID_PROPERTY",
			
			"50018, INVALID_DATES",
			"50019, INVALID_DATES",
			"50027, INVALID_DATES",
			
			"50038, UNABLE_TO_PRICE",
			"50011, UNABLE_TO_PRICE",
			"50012, UNABLE_TO_PRICE",
			"50013, UNABLE_TO_PRICE",
			"50034, UNABLE_TO_PRICE",
			"50008, UNABLE_TO_PRICE",
			"50022, UNABLE_TO_PRICE",
			"50023, UNABLE_TO_PRICE",
			"50024, UNABLE_TO_PRICE",
			"50025, UNABLE_TO_PRICE",
			
			"50026, DATES_UNAVAILABLE",
			
			"50016, INVALID_NUM_ADULTS",
			"50030, INVALID_NUM_ADULTS",
			
			"50020, SYSTEM_ERROR",
			"50007, SYSTEM_ERROR",
			"50028, SYSTEM_ERROR",
			"60003, SYSTEM_ERROR"
	})
	void testHandleErrorBusinessException(int code, ErrorCode errorCode) throws IOException {
		RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSSearchOffersErrorRes acrsSearchOffersErrorRes = new ACRSSearchOffersErrorRes();
		ACRSSearchOffersErrorDetails error = new ACRSSearchOffersErrorDetails();
		error.setHttpStatus(404);
		error.setCode(code);
		acrsSearchOffersErrorRes.setError(error);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsSearchOffersErrorRes);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.CONTINUE);
		
		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(errorCode, ex.getErrorCode());
	}
	
	
	@Test
	void testHandleErrorBusinessExceptionMLIFE_NUMBER_NOT_FOUND() throws IOException {
		RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = new RoomPriceDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSSearchOffersErrorRes acrsSearchOffersErrorRes = new ACRSSearchOffersErrorRes();
		ACRSSearchOffersErrorDetails error = new ACRSSearchOffersErrorDetails();
		error.setHttpStatus(404);
		error.setCode(50021);
		acrsSearchOffersErrorRes.setError(error);
		ACRSSearchOffersErrorData data = new ACRSSearchOffersErrorData();
		List<Denial> denials = new ArrayList<>();
		Denial denial = new Denial();
		denial.setIdentifier(35);
		denials.add(denial);
		data.setDenials(denials);
		acrsSearchOffersErrorRes.setData(data);
		String acrsSearchOffersErrorResJson = CommonUtil.convertObjectToJsonString(acrsSearchOffersErrorRes);
		InputStream is = new ByteArrayInputStream(acrsSearchOffersErrorResJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.CONTINUE);
		
		// Assertions
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.MLIFE_NUMBER_NOT_FOUND, ex.getErrorCode());
	}
}
