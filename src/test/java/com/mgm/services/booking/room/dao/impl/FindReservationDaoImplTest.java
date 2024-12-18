package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.*;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.content.Room;
import com.mgm.services.booking.room.model.ocrs.OcrsReservation;
import com.mgm.services.booking.room.model.ocrs.OcrsReservationList;
import com.mgm.services.booking.room.model.ocrs.SelectedMembership;
import com.mgm.services.booking.room.model.opera.OperaInfoResponse;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.request.FindReservationV2Request;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.ReservationRoutingInstruction;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.helper.FindReservationServiceHelper;
import com.mgm.services.booking.room.util.PartnerProgramConfig;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindReservationDaoImplTest extends BaseRoomBookingTest{
	private ReferenceDataDAOHelper referenceDataDAOHelper;
	private FindReservationDAOStrategyGSEImpl gseStrategy;
	private FindReservationDAOStrategyACRSImpl acrsStrategy;
	private OCRSDAO ocrsDao;
	private OperaInfoDAO operaInfoDAO;
	private PropertyConfig propertyConfig;
	private ApplicationProperties applicationProperties;
	private AcrsProperties acrsProperties;
	private FindReservationServiceHelper findReservationServiceHelper;
	private PartnerProgramConfig partnerProgramConfig;
	private RefDataDAO refDataDAO;
	private RoomContentDAO roomContentDAO;
	private RoomProgramDAO roomProgramDAO;
	private ServiceConversionHelper serviceConversionHelper;

	// Test Class
	private FindReservationDAOImpl findReservationDAOImpl;

	private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
	static Logger logger = LoggerFactory.getLogger(FindReservationDaoImplTest.class);

	@BeforeAll
	public static void runOnce() {
		runOnceBeforeClass();
	}

	@BeforeEach
	void init() {
		// mock dependencies
		fmt.setTimeZone(TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE));
		referenceDataDAOHelper = mock(ReferenceDataDAOHelper.class);
		gseStrategy = mock(FindReservationDAOStrategyGSEImpl.class);
		acrsStrategy = mock(FindReservationDAOStrategyACRSImpl.class);
		ocrsDao = mock(OCRSDAO.class);
		operaInfoDAO = mock(OperaInfoDAO.class);
		propertyConfig = mock(PropertyConfig.class);
		findReservationServiceHelper = new FindReservationServiceHelper();
		partnerProgramConfig = mock(PartnerProgramConfig.class);
		refDataDAO = mock(RefDataDAO.class);
		roomContentDAO = mock(RoomContentDAO.class);
		roomProgramDAO = mock(RoomProgramDAO.class);
		serviceConversionHelper = mock(ServiceConversionHelper.class);
		// create dependencies
		applicationProperties = new ApplicationProperties();
		applicationProperties.setTcolvHotelCode("195");
		applicationProperties.setF1PackageTag("F1PACKAGE");
		applicationProperties.setValidF1ProductCodes(Collections.singletonList("F12024PDC5D"));
		applicationProperties.setTransferredPropertyIds(Collections.singletonList("a689885f-cba2-48e8-b8e0-1dff096b8835"));
		applicationProperties.setHotelCode(Collections.singletonMap("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", "021"));
		try {
			applicationProperties.setHandOverDate(Collections.singletonMap("a689885f-cba2-48e8-b8e0-1dff096b8835", fmt.parse("2022-11-15")));
		} catch (ParseException parseException) {
			Assertions.fail("FindReservatinoDaoImplTest's init() method has failed with exception: ", parseException);
		}
		applicationProperties.setTimezone(Collections.singletonMap("default", "America/Los_Angeles"));
		applicationProperties.setHandOverErrorCode(Collections.singletonMap("a689885f-cba2-48e8-b8e0-1dff096b8835", "_transferred_mirage_property"));
		acrsProperties = new AcrsProperties();
		acrsProperties.setOperaReservationReferenceTypes(Collections.singletonList("AMADEUSCRS"));
		acrsProperties.setActiveCustomerOperaId(true);
		acrsProperties.setSuppresWebComponentPatterns(Collections.singletonList("ICE"));
		// Create Mock class
		findReservationDAOImpl = new FindReservationDAOImpl(acrsStrategy, applicationProperties, applicationProperties,
				findReservationServiceHelper, gseStrategy, ocrsDao, operaInfoDAO, partnerProgramConfig, propertyConfig,
				refDataDAO, roomContentDAO, roomProgramDAO, serviceConversionHelper, acrsProperties, referenceDataDAOHelper);
	}

	private OcrsReservation getOcrsReservation() {
		File file = new File(getClass().getResource("/findReservation_ocrsJsonResponse.json").getPath());
		OcrsReservationList ocrsReservation = convertCrs(file, OcrsReservationList.class);
		Assertions.assertTrue(CollectionUtils.isNotEmpty(ocrsReservation));
		return ocrsReservation.get(0);

	}
	
	private Map<String, PropertyConfig.PropertyValue> createPropertyValueMap() {
		Map<String, PropertyConfig.PropertyValue> returnMap = new HashMap<>();
		PropertyConfig.PropertyValue propertyValue = new PropertyConfig.PropertyValue();
		propertyValue.setMasterPropertyCode("MV021");
		propertyValue.setGsePropertyIds(Arrays.asList("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad"));
		returnMap.put("MV021", propertyValue);
		return returnMap;
	}
	
	private OperaInfoResponse getOperaInfo() {
		File file = new File(getClass().getResource("/OperaInfo.json").getPath());
		return convertCrs(file, OperaInfoResponse.class);

	}

	private ReservationProfile setACRSReservationProfile() {
		ReservationProfile profile = new ReservationProfile();
		profile.setMlifeNo(0);
		profile.setPartnerAccounts(setACRSPartnerAccounts());
		return profile;
	}

	private List<PartnerAccounts> setACRSPartnerAccounts() {
		List<PartnerAccounts> partnerAccounts = new ArrayList<>();
		PartnerAccounts partnerAccount1 = new PartnerAccounts();
		partnerAccount1.setPartnerAccountNo("mockAccountId");
		partnerAccount1.setProgramCode("MI");
		partnerAccount1.setMembershipLevel("mockLevelCode");
		partnerAccounts.add(partnerAccount1);

		PartnerAccounts partnerAccount2 = new PartnerAccounts();
		partnerAccount2.setPartnerAccountNo("mockFilterAccountId");
		partnerAccount2.setProgramCode("FILTER");
		partnerAccount2.setMembershipLevel("mockFilterLevelCode");
		partnerAccounts.add(partnerAccount2);
		return partnerAccounts;
	}

	private List<ReservationRoutingInstruction> setRoutingInstruction() {
		ReservationRoutingInstruction routingInstruction =  new ReservationRoutingInstruction();
		routingInstruction.setId("id1");
		routingInstruction.setName("RountingName");
		String [] routCode = {"49078"};
		routingInstruction.setRoutingCodes(routCode);
		routingInstruction.setAuthorizerId("5a46ebe5-58fd-49ca-af47-19722fe3c3a4");
		routingInstruction.setComments("add new perform");
		routingInstruction.setDailyYN(false);
		routingInstruction.setMemberShipNumber("1010");
		try {
			Date startDate = fmt.parse("2024-03-02");
			Date endDate = fmt.parse("2024-03-03");
			routingInstruction.setStartDate(startDate);
			routingInstruction.setEndDate(endDate);
		} catch (ParseException parseException) {
			String errorMsg = "Failed to setup mock date when parsing String to date. ";
			logger.error(errorMsg, parseException);
			Assertions.fail(errorMsg);
		}
		routingInstruction.setLimit("20");
		routingInstruction.setIsSystemRouting(true);
		routingInstruction.setSource("ICECC");
		List<ReservationRoutingInstruction> routingList = new ArrayList<>();
		routingList.add(routingInstruction);
		return routingList;
	}

	private static List<PurchasedComponent> getPurchasedACRSComponents() {
		PurchasedComponent purchasedICEOnlyComponent = new PurchasedComponent();
		purchasedICEOnlyComponent.setId("COMPONENTCD-v-ICEFEE-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-ICEFEE");
		List<PurchasedComponent> componentList = new ArrayList<>();
		PurchasedComponent purchasedValidComponent = new PurchasedComponent();
		purchasedValidComponent.setId("COMPONENTCD-v-EARLYCI-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-EARLYCI");
		PurchasedComponent purchasedValidComponent2 = new PurchasedComponent();
		purchasedValidComponent2.setId("COMPONENTCD-v-VGKCENTERICE-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-VGKCENTERICE");
		componentList.add(purchasedICEOnlyComponent);
		componentList.add(purchasedValidComponent);
		componentList.add(purchasedValidComponent2);
		return componentList;
	}

	private List<PartnerProgramConfig.PartnerProgramValue> mockPartnerProgramValues() {
		List<PartnerProgramConfig.PartnerProgramValue> partnerProgramValues = new ArrayList<>();
		PartnerProgramConfig.PartnerProgramValue programValue = new PartnerProgramConfig.PartnerProgramValue();
		programValue.setProgramCode("MI");
		programValue.setProgramName("Marriott Bonvoy");
		partnerProgramValues.add(programValue);
		return partnerProgramValues;
	}

	private RoomProgramValidateResponse mockRoomProgramValidateResponseF1Package() {
		RoomProgramValidateResponse roomProgramValidateResponse = new RoomProgramValidateResponse();
		List<String> ratePlanTagsList = new ArrayList<>();
		ratePlanTagsList.add("F1PACKAGE");
		ratePlanTagsList.add("F12024PDC5D");
		roomProgramValidateResponse.setRatePlanTags(ratePlanTagsList);
		return roomProgramValidateResponse;
	}

	private RoomProgramValidateResponse mockRoomProgramValidateResponseNotF1Package() {
		RoomProgramValidateResponse roomProgramValidateResponse = new RoomProgramValidateResponse();
		List<String> ratePlanTagsList = new ArrayList<>();
		ratePlanTagsList.add("F2PACKAGE");
		ratePlanTagsList.add("F22023Drugovich");
		roomProgramValidateResponse.setRatePlanTags(ratePlanTagsList);
		return roomProgramValidateResponse;
	}

	private List<SelectedMembership> getMockSelectedMemberships() {
		List<SelectedMembership> selectedMemberships = new ArrayList<>();
		SelectedMembership selectedMembership = new SelectedMembership();
		selectedMembership.setAccountID("mockAccountId");
		selectedMembership.setProgramCode("MI");
		selectedMembership.setLevelCode("mockLevelCode");
		selectedMemberships.add(selectedMembership);

		SelectedMembership selectedMembershipToFilter = new SelectedMembership();
		selectedMembershipToFilter.setAccountID("mockFilterAccountId");
		selectedMembershipToFilter.setProgramCode("FILTER");
		selectedMembershipToFilter.setLevelCode("mockFilterLevelCode");
		selectedMemberships.add(selectedMembershipToFilter);
		return selectedMemberships;
	}

	/*
		FindReservationV1 test cases
	 */

	@Test
	void findRoomReservation_GSETest() {
		try {
			FindReservationRequest findReservationReq = new FindReservationRequest();
			findReservationReq.setSource("ICE");
			RoomReservation roomReservation = new RoomReservation();
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
			when(gseStrategy.findRoomReservation(findReservationReq)).thenReturn(roomReservation);
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);

			Assertions.assertNotNull(roomReservation);
			Assertions.assertEquals(actualResponse, roomReservation);
		} catch (Exception e) {
			logger.error("findRoomReservation_GSETest failed with exception: ", e);
			Assertions.fail("findRoomReservation_GSETest Failed due to unexpected exception");
		}

	}

	@Test
	void findRoomReservation_ACRSTest() {
		try {
			FindReservationRequest findReservationReq = new FindReservationRequest();
			findReservationReq.setSource("ICE");
			RoomReservation roomReservation = new RoomReservation();
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			when(acrsStrategy.findRoomReservation(findReservationReq)).thenReturn(roomReservation);
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);

			Assertions.assertNotNull(roomReservation);
			Assertions.assertEquals(actualResponse, roomReservation);
		} catch (Exception e) {
			logger.error("findRoomReservation_ACRSTest failed with exception: ", e);
			Assertions.fail("findRoomReservation_ACRSTest Failed due to unexpected exception");
		}

	}

	@Test
	void findRoomReservation_ExceptionTest() {
		try {
			FindReservationRequest findReservationReq = new FindReservationRequest();
			findReservationReq.setSource("ICE");
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
			when(gseStrategy.findRoomReservation(findReservationReq)).thenThrow(new BusinessException(ErrorCode.INVALID_RESERVATION_ID_CONFIRMATION_NUMBER, "Invalid reservation"));

			assertThatThrownBy(() -> findReservationDAOImpl.findRoomReservation(findReservationReq))
					.isInstanceOf(BusinessException.class);
		} catch (Exception e) {
			logger.error("findRoomReservation_ExceptionTest failed with exception: ", e);
			Assertions.fail("findRoomReservation_ExceptionTest Failed due to unexpected exception");
		}

	}

	/*
		FindReservationV2 tests
	 */

	@Test
	void findReservationV2ACRSTest() {
		// ACRS test case
		// Arrange Mock Request
		FindReservationV2Request findReservationReq = new FindReservationV2Request();
		findReservationReq.setSource("ICE");
		findReservationReq.setConfirmationNumber("1WPK23GNZR");

		// Arrange Mock ACRS roomReservation response
		RoomReservation roomReservation = new RoomReservation();
		roomReservation.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		roomReservation.setConfirmationNumber("1WPK23GNZR");
		ReservationProfile profile = setACRSReservationProfile();
		roomReservation.setProfile(profile);
		List<PurchasedComponent> componentList = getPurchasedACRSComponents();
		roomReservation.setPurchasedComponents(componentList);
		roomReservation.setRoutingInstructions(setRoutingInstruction());
		roomReservation.setProgramId("TestNonF1ProgramId");

		// Arrange Mock Ocrs Reservation response
		OcrsReservation ocrsReservation = getOcrsReservation();
		ocrsReservation.getMgmProfile().setMgmId("mockMGMId");

		// Mock Partner Config
		List<PartnerProgramConfig.PartnerProgramValue> partnerProgramValues = mockPartnerProgramValues();

		// Arrange Mock validateProgramV2 response for f1 package == false
		RoomProgramValidateResponse roomProgramValidateResponse = mockRoomProgramValidateResponseNotF1Package();

		// Arrange Mocks
		when(roomProgramDAO.validateProgramV2(ArgumentMatchers.any())).thenReturn(roomProgramValidateResponse);
		when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(ocrsReservation);
		when(partnerProgramConfig.getPartnerProgramValues()).thenReturn(partnerProgramValues);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(operaInfoDAO.getOperaInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(getOperaInfo());
		when(acrsStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class))).thenReturn(roomReservation);
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
		when(propertyConfig.getPropertyValuesMap()).thenReturn(createPropertyValueMap());
		when(refDataDAO.getRoutingAuthPhoenixId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn("5a46ebe5-58fd-49ca-af47-19722fe3c3a4");

		try {
			// Act
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);

			// Assert Response
			Assertions.assertNotNull(actualResponse);
			Assertions.assertEquals("1WPK23GNZR", actualResponse.getRequestConfirmationNumber());
			Assertions.assertFalse(actualResponse.isF1Package());

			ReservationProfile responseProfile = actualResponse.getProfile();
			Assertions.assertNotNull(responseProfile);
			Assertions.assertEquals("mockMGMId", responseProfile.getMgmId());
			Assertions.assertEquals("907322386", responseProfile.getOperaId());

			List<PartnerAccounts> partnerAccounts = actualResponse.getProfile().getPartnerAccounts();
			Assertions.assertTrue(CollectionUtils.isNotEmpty(partnerAccounts));
			Assertions.assertEquals("MI", partnerAccounts.get(0).getProgramCode());

			List<ReservationRoutingInstruction> responseRoutingInstructions = actualResponse.getRoutingInstructions();
			Assertions.assertNotNull(responseRoutingInstructions);
			Assertions.assertEquals(1, responseRoutingInstructions.size());

			ReservationRoutingInstruction responseRoutingInstruction = responseRoutingInstructions.get(0);
			Assertions.assertEquals("id1", responseRoutingInstruction.getId());
			Assertions.assertEquals("5a46ebe5-58fd-49ca-af47-19722fe3c3a4", responseRoutingInstruction.getAuthorizerId());
			Assertions.assertEquals("1010", responseRoutingInstruction.getMemberShipNumber());

			List<PurchasedComponent> purchasedComponents = actualResponse.getPurchasedComponents();
			Assertions.assertNotNull(purchasedComponents);
			Assertions.assertEquals(3, purchasedComponents.size());

			Optional<PurchasedComponent> iceComponentOptional = purchasedComponents.stream()
					.filter(component -> "COMPONENTCD-v-ICEFEE-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-ICEFEE".equalsIgnoreCase(component.getId()))
					.findAny();
			Assertions.assertTrue(iceComponentOptional.isPresent());

			PurchasedComponent iceComponent = iceComponentOptional.get();
			Assertions.assertTrue(iceComponent.isNonEditable());

			boolean isNonIceComponentsEditable = purchasedComponents.stream()
					.filter(component -> !"COMPONENTCD-v-ICEFEE-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-ICEFEE".equalsIgnoreCase(component.getId()))
					.noneMatch(PurchasedComponent::isNonEditable);
			Assertions.assertTrue(isNonIceComponentsEditable);
		} catch (Exception e) {
			logger.error("findReservationV2Test failed with exception: ", e);
			Assertions.fail("findReservationV2Test Failed due to unexpected exception");
		}
	}

	@Test
	void findReservationV2_ExceptionTest() {
		try {
			FindReservationV2Request findReservationReq =new  FindReservationV2Request();
			findReservationReq.setSource("ICE");
			findReservationReq.setConfirmationNumber("1WPK23GNZR");
			
			when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(null);
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			when(operaInfoDAO.getOperaInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(getOperaInfo());
			
			when(acrsStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class))).thenThrow(new BusinessException(ErrorCode.RESERVATION_BLACKLISTED,"unable to make reservation"));
			
			when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
			
			when(propertyConfig.getPropertyValuesMap()).thenReturn(createPropertyValueMap());
			applicationProperties.setTcolvHotelCode("195");
		
			assertThatThrownBy(() ->  findReservationDAOImpl.findRoomReservation(findReservationReq))
			.isInstanceOf(BusinessException.class);
		} catch (Exception e) {
			logger.error("findReservationV2_ExceptionTest failed with exception: ", e);
			Assertions.fail("findReservationV2_ExceptionTest Failed due to unexpected exception");
		}
		
	}
	
	@Test
	void findRoomReservationV2_GSETest() {
		// Find Reservation V2 GSE Test
		// Arrange Mock Request
		FindReservationV2Request findReservationReq = new FindReservationV2Request();
		findReservationReq.setSource("ICE");
		findReservationReq.setConfirmationNumber("1WPK23GNZR");

		// Arrange Mock GSE Room Reservation Response
		RoomReservation roomReservation = new RoomReservation();
		ReservationProfile profile = new ReservationProfile();
		profile.setMlifeNo(0);
		roomReservation.setProfile(profile);
		roomReservation.setConfirmationNumber("1WPK23GNZR");
		roomReservation.setProgramId("NonF1Program");

		// Arrange Mock Partner Config
		List<PartnerProgramConfig.PartnerProgramValue> partnerProgramValues = mockPartnerProgramValues();

		// Arrange mock OCRS response
		OcrsReservation ocrsReservation = getOcrsReservation();
		ocrsReservation.getResProfiles()
				.getResProfile().get(0)
				.getProfile()
				.getMemberships()
				.getMembership()
				.addAll(getMockSelectedMemberships());
		ocrsReservation.getMgmProfile().setMgmId("mockMGMId");

		// Arrange isF1Package false mock
		RoomProgramValidateResponse roomProgramValidateResponse = mockRoomProgramValidateResponseNotF1Package();

		// Arrange Mocks
		when(roomProgramDAO.validateProgramV2(ArgumentMatchers.any())).thenReturn(roomProgramValidateResponse);
		when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(ocrsReservation);
		when(partnerProgramConfig.getPartnerProgramValues()).thenReturn(partnerProgramValues);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
		when(gseStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class)))
				.thenReturn(roomReservation);
		try {
			// Act
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);

			// Assert Response
			Assertions.assertNotNull(actualResponse);
			Assertions.assertNotNull(actualResponse.getProfile());

			List<PartnerAccounts> partnerAccounts = actualResponse.getProfile().getPartnerAccounts();
			Assertions.assertTrue(CollectionUtils.isNotEmpty(partnerAccounts));
			Assertions.assertEquals("MI", partnerAccounts.get(0).getProgramCode());
			Assertions.assertEquals("1WPK23GNZR", actualResponse.getRequestConfirmationNumber());
			Assertions.assertEquals("mockMGMId", actualResponse.getProfile().getMgmId());
			Assertions.assertFalse(actualResponse.isF1Package());
		} catch (Exception e) {
			logger.error("findRoomReservationV2_GSETest failed with exception: ", e);
			Assertions.fail("findRoomReservationV2_GSETest Failed due to unexpected exception");
		}
	}

	@Test
	void findRoomReservationV2_GSERetryTest() {
		// Find Reservation V2 GSE Retry with cacheOnly Test
		// Arrange Mock Request
		FindReservationV2Request findReservationReq = new FindReservationV2Request();
		findReservationReq.setSource("ICE");
		findReservationReq.setConfirmationNumber("M0001313");

		// Arrange Mock GSE Room Reservation Response
		RoomReservation roomReservation = new RoomReservation();
		ReservationProfile profile = new ReservationProfile();
		profile.setMlifeNo(0);
		roomReservation.setProfile(profile);
		roomReservation.setConfirmationNumber("M0001313");
		roomReservation.setProgramId("NonF1Program");

		// Arrange Mock Partner Config
		List<PartnerProgramConfig.PartnerProgramValue> partnerProgramValues = mockPartnerProgramValues();

		// Arrange mock OCRS response
		OcrsReservation ocrsReservation = getOcrsReservation();
		ocrsReservation.getResProfiles()
				.getResProfile().get(0)
				.getProfile()
				.getMemberships()
				.getMembership()
				.addAll(getMockSelectedMemberships());

		// Arrange isF1Package false mock
		RoomProgramValidateResponse roomProgramValidateResponse = mockRoomProgramValidateResponseNotF1Package();

		// Arrange Mocks
		when(roomProgramDAO.validateProgramV2(ArgumentMatchers.any())).thenReturn(roomProgramValidateResponse);
		when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(ocrsReservation);
		when(partnerProgramConfig.getPartnerProgramValues()).thenReturn(partnerProgramValues);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
		when(gseStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class)))
				.thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND))
				.thenReturn(roomReservation);
		try {
			// Act
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);

			// Assert Response
			Assertions.assertNotNull(actualResponse);
			Assertions.assertNotNull(actualResponse.getProfile());

			List<PartnerAccounts> partnerAccounts = actualResponse.getProfile().getPartnerAccounts();
			Assertions.assertTrue(CollectionUtils.isNotEmpty(partnerAccounts));
			Assertions.assertEquals("MI", partnerAccounts.get(0).getProgramCode());
			Assertions.assertEquals("M0001313", actualResponse.getRequestConfirmationNumber());
			Assertions.assertFalse(actualResponse.isF1Package());
		} catch (Exception e) {
			logger.error("findRoomReservationV2_GSERetryTest failed with exception: ", e);
			Assertions.fail("findRoomReservationV2_GSERetryTest Failed due to unexpected exception");
		}
	}

	@Test
	void findRoomReservationV2_GSEExceptionTest() {
		try {
			FindReservationV2Request findReservationReq =new  FindReservationV2Request();
			findReservationReq.setSource("ICE");
			findReservationReq.setConfirmationNumber("1WPK23GNZR");
			
			RoomReservation roomReservation = new RoomReservation();
			ReservationProfile profile = new ReservationProfile();
			profile.setMlifeNo(0);
			roomReservation.setProfile(new ReservationProfile());
			
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
			when(gseStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class))).thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_SUCCESSFUL,"Test error message"));
			when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
			
			assertThatThrownBy(() ->  findReservationDAOImpl.findRoomReservation(findReservationReq))
			.isInstanceOf(BusinessException.class);
		} catch (Exception e) {
			logger.error("findRoomReservationV2_GSETest failed with exception: ", e);
			Assertions.fail("findRoomReservationV2_GSETest Failed due to unexpected exception");
		}
		
	}

	@Test
	void findReservationV2BlacklistedReservationGSETest() {
		// GSE blacklisted reservation test case
		// Arrange Mock Request
		FindReservationV2Request findReservationReq = new FindReservationV2Request();
		findReservationReq.setSource("ICE");
		findReservationReq.setConfirmationNumber("1WPK23GNZR");

		// Arrange Mock Ocrs Reservation response that blacklist will be built from
		OcrsReservation ocrsReservation = getOcrsReservation();
		ocrsReservation.getMgmProfile().setMgmId("mockMGMId");

		// Arrange RoomContent mock response
		Room roomContent = new Room();
		roomContent.setId("L1PL"); // RoomTypeId
		roomContent.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");

		// Arrange Mocks
		when(roomContentDAO.getRoomContent(anyString(), anyString(), anyBoolean())).thenReturn(roomContent);
		when(gseStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class)))
				.thenThrow(new BusinessException(ErrorCode.RESERVATION_BLACKLISTED));
		when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(ocrsReservation);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
		when(operaInfoDAO.getOperaInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(getOperaInfo());
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
		when(propertyConfig.getPropertyValuesMap()).thenReturn(createPropertyValueMap());

		try {
			// Act
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);

			// Assert Response
			Assertions.assertNotNull(actualResponse);
			Assertions.assertEquals("1WPK23GNZR", actualResponse.getRequestConfirmationNumber());
			Assertions.assertEquals(ReservationState.Booked, actualResponse.getState());
			Assertions.assertTrue(actualResponse.isThirdParty());
			Assertions.assertEquals("914088576", actualResponse.getOperaConfirmationNumber());
			Assertions.assertEquals(1, actualResponse.getNumRooms());
			Assertions.assertNotNull(actualResponse.getProfile());
			Assertions.assertEquals("907322386", actualResponse.getProfile().getOperaId());
			Assertions.assertEquals("Ghorai", actualResponse.getProfile().getLastName());
		} catch (Exception e) {
			logger.error("findReservationV2BlacklistedReservationGSETest failed with exception: ", e);
			Assertions.fail("findReservationV2BlacklistedReservationGSETest Failed due to unexpected exception");
		}
	}

	@Test
	void findReservationV2BlackListedReservationACRSTest() {
		// ACRS blacklisted reservation test case
		// Arrange Mock Request
		FindReservationV2Request findReservationReq = new FindReservationV2Request();
		findReservationReq.setSource("ICE");
		findReservationReq.setConfirmationNumber("1WPK23GNZR");

		// Arrange Mock Ocrs Reservation response that blacklist will be built from
		OcrsReservation ocrsReservation = getOcrsReservation();
		ocrsReservation.getMgmProfile().setMgmId("mockMGMId");

		// Arrange RoomContent mock response
		Room roomContent = new Room();
		roomContent.setId("L1PL"); // RoomTypeId
		roomContent.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");

		// Arrange Mocks
		when(roomContentDAO.getRoomContent(anyString(), anyString(), anyBoolean())).thenReturn(roomContent);
		when(acrsStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class)))
				.thenThrow(new BusinessException(ErrorCode.RESERVATION_BLACKLISTED));
		when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(ocrsReservation);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(operaInfoDAO.getOperaInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(getOperaInfo());
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
		when(propertyConfig.getPropertyValuesMap()).thenReturn(createPropertyValueMap());

		try {
			// Act
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);

			// Assert Response
			Assertions.assertNotNull(actualResponse);
			Assertions.assertEquals("1WPK23GNZR", actualResponse.getRequestConfirmationNumber());
			Assertions.assertEquals(ReservationState.Booked, actualResponse.getState());
			Assertions.assertTrue(actualResponse.isThirdParty());
			Assertions.assertEquals("914088576", actualResponse.getOperaConfirmationNumber());
			Assertions.assertEquals(1, actualResponse.getNumRooms());
			Assertions.assertNotNull(actualResponse.getProfile());
			Assertions.assertEquals("907322386", actualResponse.getProfile().getOperaId());
			Assertions.assertEquals("Ghorai", actualResponse.getProfile().getLastName());
		} catch (Exception e) {
			logger.error("findReservationV2BlackListedReservationACRSTest failed with exception: ", e);
			Assertions.fail("findReservationV2BlackListedReservationACRSTest Failed due to unexpected exception");
		}
	}

	@Test
	void findReservationV2AcrsNoLongerMGMMangedExceptionTest() {
		// ACRS Property no longer mgm managed exception case
		// Arrange Mock Request
		FindReservationV2Request findReservationReq = new FindReservationV2Request();
		findReservationReq.setSource("ICE");
		findReservationReq.setConfirmationNumber("1WPK23GNZR");

		// Arrange Mock ACRS roomReservation response
		RoomReservation roomReservation = new RoomReservation();
		roomReservation.setPropertyId("a689885f-cba2-48e8-b8e0-1dff096b8835");
		roomReservation.setConfirmationNumber("1WPK23GNZR");
		ReservationProfile profile = setACRSReservationProfile();
		roomReservation.setProfile(profile);
		List<PurchasedComponent> componentList = getPurchasedACRSComponents();
		roomReservation.setPurchasedComponents(componentList);
		roomReservation.setRoutingInstructions(setRoutingInstruction());
		roomReservation.setProgramId("TestF1ProgramId");
		try {
			roomReservation.setCheckInDate(fmt.parse("2023-01-01")); // Needs to be after 2022-11-15
		} catch (ParseException e) {
			Assertions.fail("Failed to Arrange findReservationV2AcrsNoLongerMGMMangedExceptionTest with exception: ", e);
		}

		// Arrange Mock Ocrs Reservation response
		OcrsReservation ocrsReservation = getOcrsReservation();
		ocrsReservation.getMgmProfile().setMgmId("mockMGMId");

		// Mock Partner Config
		List<PartnerProgramConfig.PartnerProgramValue> partnerProgramValues = mockPartnerProgramValues();

		// Arrange Mock validateProgramV2 response for f1 package == false
		RoomProgramValidateResponse roomProgramValidateResponse = mockRoomProgramValidateResponseF1Package();

		// Arrange Mocks
		when(roomProgramDAO.validateProgramV2(ArgumentMatchers.any())).thenReturn(roomProgramValidateResponse);
		when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(ocrsReservation);
		when(partnerProgramConfig.getPartnerProgramValues()).thenReturn(partnerProgramValues);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(operaInfoDAO.getOperaInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(getOperaInfo());
		when(acrsStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class))).thenReturn(roomReservation);
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
		when(propertyConfig.getPropertyValuesMap()).thenReturn(createPropertyValueMap());
		when(refDataDAO.getRoutingAuthPhoenixId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn("5a46ebe5-58fd-49ca-af47-19722fe3c3a4");

		try {
			// Act
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);
			Assertions.assertNull(actualResponse);
			Assertions.fail("This should never execute as we are expecting an exception.");

		} catch (BusinessException businessException) {
			// Assert Correct Exception type and ErrorCode thrown
			Assertions.assertNotNull(businessException);
			Assertions.assertEquals(ErrorCode.TRANSFERRED_MIRAGE_PROPERTY, businessException.getErrorCode());
		} catch (Exception e) {
			logger.error("findReservationV2AcrsNoLongerMGMMangedExceptionTest failed with exception: ", e);
			Assertions.fail("findReservationV2AcrsNoLongerMGMMangedExceptionTest Failed due to unexpected exception");
		}
	}

	@Test
	void findReservationV2AcrsF1PackageTest() {
		// ACRS test case with isF1Package should be true
		// Arrange Mock Request
		FindReservationV2Request findReservationReq = new FindReservationV2Request();
		findReservationReq.setSource("ICE");
		findReservationReq.setConfirmationNumber("1WPK23GNZR");

		// Arrange Mock ACRS roomReservation response
		RoomReservation roomReservation = new RoomReservation();
		roomReservation.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		roomReservation.setConfirmationNumber("1WPK23GNZR");
		ReservationProfile profile = setACRSReservationProfile();
		roomReservation.setProfile(profile);
		List<PurchasedComponent> componentList = getPurchasedACRSComponents();
		roomReservation.setPurchasedComponents(componentList);
		roomReservation.setRoutingInstructions(setRoutingInstruction());
		roomReservation.setProgramId("TestF1ProgramId");

		// Arrange Mock Ocrs Reservation response
		OcrsReservation ocrsReservation = getOcrsReservation();
		ocrsReservation.getMgmProfile().setMgmId("mockMGMId");

		// Arrange Mock Partner Config
		List<PartnerProgramConfig.PartnerProgramValue> partnerProgramValues = mockPartnerProgramValues();

		// Arrange Mock validateProgramV2 response for f1 package == false
		RoomProgramValidateResponse roomProgramValidateResponse = mockRoomProgramValidateResponseF1Package();

		// Arrange Mocks
		when(roomProgramDAO.validateProgramV2(ArgumentMatchers.any())).thenReturn(roomProgramValidateResponse);
		when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(ocrsReservation);
		when(partnerProgramConfig.getPartnerProgramValues()).thenReturn(partnerProgramValues);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(operaInfoDAO.getOperaInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(getOperaInfo());
		when(acrsStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class))).thenReturn(roomReservation);
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
		when(propertyConfig.getPropertyValuesMap()).thenReturn(createPropertyValueMap());
		when(refDataDAO.getRoutingAuthPhoenixId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn("5a46ebe5-58fd-49ca-af47-19722fe3c3a4");

		try {
			// Act
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);

			// Assert Response
			Assertions.assertNotNull(actualResponse);
			Assertions.assertEquals("1WPK23GNZR", actualResponse.getRequestConfirmationNumber());
			Assertions.assertTrue(actualResponse.isF1Package());
			List<String> ratePlanTags = actualResponse.getRatePlanTags();
			Assertions.assertEquals(2, ratePlanTags.size());
			Assertions.assertTrue(ratePlanTags.contains("F1PACKAGE"));
			Assertions.assertTrue(ratePlanTags.contains("F12024PDC5D"));
			// Note: No Need to assert all fields as those are tested in other Test cases
		} catch (Exception e) {
			logger.error("findReservationV2AcrsF1PackageTest failed with exception: ", e);
			Assertions.fail("findReservationV2AcrsF1PackageTest Failed due to unexpected exception");
		}
	}

	@Test
	void findRoomReservationV2_ACRSSearchConfNoTest() {
		// Test case for ACRS with search reservation to get ACRS confirmation number

		// Arrange Mock Request
		FindReservationV2Request findReservationReq = new FindReservationV2Request();
		findReservationReq.setSource("ICE");
		findReservationReq.setConfirmationNumber("914088576");

		// Arrange Mock ACRS Reservation response
		RoomReservation roomReservation = new RoomReservation();
		roomReservation.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		roomReservation.setConfirmationNumber("1WPK23GNZR");
		roomReservation.setOperaConfirmationNumber("914088576");
		roomReservation.setProfile(setACRSReservationProfile());
		List<PurchasedComponent> componentList = getPurchasedACRSComponents();
		roomReservation.setPurchasedComponents(componentList);
		roomReservation.setRoutingInstructions(setRoutingInstruction());

		// Arrange Mock OCRS Response
		OcrsReservation ocrsReservation = getOcrsReservation();

		// Arrange Mocks
		when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(ocrsReservation);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(operaInfoDAO.getOperaInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(getOperaInfo());
		when(acrsStrategy.findRoomReservation(ArgumentMatchers.any(FindReservationV2Request.class))).thenReturn(roomReservation);
		when(acrsStrategy.searchRoomReservationByExternalConfirmationNo(ArgumentMatchers.any())).thenReturn("1WPK23GNZR");
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
		when(propertyConfig.getPropertyValuesMap()).thenReturn(createPropertyValueMap());
		when(refDataDAO.getRoutingAuthPhoenixId(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn("5a46ebe5-58fd-49ca-af47-19722fe3c3a4");
		try {
			// Act
			RoomReservation actualResponse = findReservationDAOImpl.findRoomReservation(findReservationReq);

			// Assert
			Assertions.assertNotNull(actualResponse);
			Assertions.assertEquals("1WPK23GNZR", actualResponse.getConfirmationNumber());
			Assertions.assertEquals("914088576", actualResponse.getRequestConfirmationNumber());
		} catch (Exception e) {
			logger.error("findRoomReservationV2_ACRSSearchConfNoTest failed with exception: ", e);
			Assertions.fail("findRoomReservationV2_ACRSSearchConfNoTest Failed due to unexpected exception");
		}
	}

	@Test
	void findRoomReservationV2_ACRSConfNoExceptionTest() {
		try {
			FindReservationV2Request findReservationReq =new  FindReservationV2Request();
			findReservationReq.setSource("ICE");
			findReservationReq.setConfirmationNumber("914088576");

			RoomReservation roomReservation = new RoomReservation();
			roomReservation.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
			roomReservation.setConfirmationNumber("914088576");

			roomReservation.setProfile(setACRSReservationProfile());
			List<PurchasedComponent> componentList = getPurchasedACRSComponents();
			roomReservation.setPurchasedComponents(componentList);

			roomReservation.setRoutingInstructions(setRoutingInstruction());

			OcrsReservation ocrsReservation = getOcrsReservation();
			when(ocrsDao.getOCRSReservation(ArgumentMatchers.anyString())).thenReturn(ocrsReservation);
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			when(operaInfoDAO.getOperaInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(getOperaInfo());

			when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(false);
			when(acrsStrategy.searchRoomReservationByExternalConfirmationNo(ArgumentMatchers.any(FindReservationV2Request.class))).thenThrow(new BusinessException(ErrorCode.RESERVATION_NOT_SUCCESSFUL,"Test error message"));

			when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);

			when(propertyConfig.getPropertyValuesMap()).thenReturn(createPropertyValueMap());
			applicationProperties.setTcolvHotelCode("195");

			PartnerProgramConfig.PartnerProgramValue programValue = new PartnerProgramConfig.PartnerProgramValue();
			programValue.setProgramCode("MI");
			programValue.setProgramName("Marriott Bonvoy");

			List<PartnerProgramConfig.PartnerProgramValue> partnerProgramValues = new ArrayList<>();
			partnerProgramValues.add(programValue);

			assertThatThrownBy(() ->  findReservationDAOImpl.findRoomReservation(findReservationReq))
			.isInstanceOf(BusinessException.class);
		} catch (Exception e) {
			logger.error("findRoomReservationV2_ACRSConfNoExceptionTest failed with exception: ", e);
			Assertions.fail("findRoomReservationV2_ACRSConfNoExceptionTest Failed due to unexpected exception");
		}

	}
}


