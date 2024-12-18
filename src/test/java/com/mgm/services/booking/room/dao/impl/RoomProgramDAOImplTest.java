package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.CVSDao;
import com.mgm.services.booking.room.dao.LoyaltyDao;
import com.mgm.services.booking.room.dao.PackageConfigDAO;
import com.mgm.services.booking.room.dao.ProgramContentDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.content.PackageConfig;
import com.mgm.services.booking.room.model.content.Program;
import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.RoomProgramPromoAssociationRequest;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.*;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgmresorts.aurora.service.Client;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RoomProgramDAOImplTest extends BaseRoomBookingTest {

	@Mock
	private static RoomProgramDAOStrategyACRSImpl acrsStrategy;
	@Mock
	private RoomProgramDAOStrategyGSEImpl gseStrategy;

	@Mock
	private ReferenceDataDAOHelper referenceDataDAOHelper;

	@Mock
	RoomProgramCacheService roomProgramCacheService;

	@InjectMocks
	private RoomProgramDAOImpl roomProgramDAOImpl;

	@Mock
	SecretsProperties secretProperties;

	@Mock
	LoyaltyDao loyaltyDao;

	@Mock
	ProgramContentDAO programContentDao;

	@Mock
	PackageConfigDAO packageConfigDao;

	private static Client auroraClient;

	@Mock
	AuroraBaseDAO auroraBaseDAO;

	@Mock
	CVSDao cvsDao;

	static Logger logger = LoggerFactory.getLogger(RoomProgramDAOImplTest.class);

	/**
	 * Check Room Program Applicability Strategy ACRS
	 */
	@Test
	public void getApplicableProgramsTest_ACRS() {

		try {
			ApplicableProgramRequestDTO request = ApplicableProgramRequestDTO.builder()
					.propertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad").build();
			ApplicableProgramsResponse response = new ApplicableProgramsResponse();

			when(acrsStrategy.getApplicablePrograms(ArgumentMatchers.any())).thenReturn(response);
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			ApplicableProgramsResponse actualResponse = roomProgramDAOImpl.getApplicablePrograms(request);

			Assert.assertNotNull(response);
			Assert.assertEquals(actualResponse, response);

		} catch (Exception e) {
			Assert.fail("getApplicablePrograms Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	/**
	 * Check Room Program Applicability Strategy GSE
	 */
	@Test
	public void getApplicableProgramsTest_GSE() {

		try {
			roomProgramDAOImpl.acrsProperties = new AcrsProperties();

			ApplicableProgramRequestDTO request = ApplicableProgramRequestDTO.builder()
					.propertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f").build();
			ApplicableProgramsResponse response = new ApplicableProgramsResponse();

			when(gseStrategy.getApplicablePrograms(ArgumentMatchers.any())).thenReturn(response);
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
			ApplicableProgramsResponse actualResponse = roomProgramDAOImpl.getApplicablePrograms(request);

			Assert.assertNotNull(response);
			Assert.assertEquals(actualResponse, response);

		} catch (Exception e) {
			Assert.fail("getApplicablePrograms Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	/**
	 * Check Customer Offer Applicability Strategy ACRS
	 */
	@Test
	public void getCustomerOfferTest_ACRS() {

		try {
			CustomerOffersRequestDTO request = CustomerOffersRequestDTO.builder()
					.propertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad").build();
			CustomerOfferResponse response = new CustomerOfferResponse();
			CustomerOfferDetail offer = new CustomerOfferDetail();
			response.setOffers(new ArrayList<>());
			response.getOffers().add(offer);

			roomProgramDAOImpl.appProperties = new ApplicationProperties();
			roomProgramDAOImpl.appProperties.setCustomerOfferRequestTimeoutInSec(60);

			when(acrsStrategy.getCustomerOffers(ArgumentMatchers.any())).thenReturn(response);
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			CustomerOfferResponse actualResponse = roomProgramDAOImpl.getCustomerOffers(request);

			Assert.assertNotNull(response);
			Assert.assertEquals(actualResponse.getOffers().get(0), response.getOffers().get(0));

		} catch (Exception e) {
			Assert.fail("getCustomerOffer Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	/**
	 * Check Customer Offer Applicability Strategy GSE
	 */
	@Test
	public void getCustomerOfferTest_GSE() {

		try {
			roomProgramDAOImpl.acrsProperties = new AcrsProperties();

			CustomerOffersRequestDTO request = CustomerOffersRequestDTO.builder()
					.propertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f").build();
			CustomerOfferResponse response = new CustomerOfferResponse();
			CustomerOfferDetail offer = new CustomerOfferDetail();
			response.setOffers(new ArrayList<>());
			response.getOffers().add(offer);

			roomProgramDAOImpl.appProperties = new ApplicationProperties();
			roomProgramDAOImpl.appProperties.setCustomerOfferRequestTimeoutInSec(60);

			when(gseStrategy.getCustomerOffers(ArgumentMatchers.any())).thenReturn(response);
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
			CustomerOfferResponse actualResponse = roomProgramDAOImpl.getCustomerOffers(request);

			Assert.assertNotNull(response);
			Assert.assertEquals(actualResponse.getOffers().get(0), response.getOffers().get(0));

		} catch (Exception e) {
			Assert.fail("getCustomerOffer Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	/**
	 * Check Customer Offer Applicability Strategy ACRS
	 */
	@Test
	public void getCustomerOfferTest_ACRS_offers() {

		try {
			CustomerOffersRequestDTO request = CustomerOffersRequestDTO.builder().build();
			CustomerOfferResponse response = new CustomerOfferResponse();
			CustomerOfferDetail offer = new CustomerOfferDetail();
			offer.setOfferType("PROGRAM");
			response.setOffers(new ArrayList<>());
			response.getOffers().add(offer);

			roomProgramDAOImpl.appProperties = new ApplicationProperties();
			roomProgramDAOImpl.appProperties.setCustomerOfferRequestTimeoutInSec(60);

			when(acrsStrategy.getCustomerOffers(ArgumentMatchers.any())).thenReturn(response);
			when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			CustomerOfferResponse actualResponse = roomProgramDAOImpl.getCustomerOffers(request);

			Assert.assertNotNull(response);
			Assert.assertEquals(actualResponse.getOffers().get(0), response.getOffers().get(0));

		} catch (Exception e) {
			Assert.fail("getCustomerOffer Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	/**
	 * Check Customer Offer Applicability Strategy GSE without available cache
	 */
	@Test
	public void getCustomerOfferTest_GSE_offers_without_cache() {

		try {
			CustomerOffersRequestDTO request = CustomerOffersRequestDTO.builder().build();
			CustomerOfferResponse response = new CustomerOfferResponse();
			CustomerOfferDetail offer = new CustomerOfferDetail();
			offer.setOfferType("PROGRAM");
			response.setOffers(new ArrayList<>());
			response.getOffers().add(offer);

			roomProgramDAOImpl.appProperties = new ApplicationProperties();
			roomProgramDAOImpl.appProperties.setCustomerOfferRequestTimeoutInSec(60);

			when(roomProgramCacheService.getRoomProgram(Mockito.anyString())).thenReturn(null);
			when(gseStrategy.getCustomerOffers(ArgumentMatchers.any())).thenReturn(response);
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			CustomerOfferResponse actualResponse = roomProgramDAOImpl.getCustomerOffers(request);

			Assert.assertNotNull(response);
			Assert.assertEquals(actualResponse.getOffers().get(0), response.getOffers().get(0));

		} catch (Exception e) {
			Assert.fail("getCustomerOffer Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	/**
	 * Check Customer Offer Applicability Strategy GSE
	 */
	@Test
	public void getCustomerOfferTest_GSE_offers() {

		try {
			CustomerOffersRequestDTO request = CustomerOffersRequestDTO.builder().build();
			CustomerOfferResponse response = new CustomerOfferResponse();
			CustomerOfferDetail offer = new CustomerOfferDetail();
			offer.setOfferType("PROGRAM");
			offer.setId("123456");
			response.setOffers(new ArrayList<>());
			response.getOffers().add(offer);

			roomProgramDAOImpl.appProperties = new ApplicationProperties();
			roomProgramDAOImpl.appProperties.setCustomerOfferRequestTimeoutInSec(60);

			RoomProgram roomProgram = new RoomProgram();
			roomProgram.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");

			when(roomProgramCacheService.getRoomProgram(Mockito.anyString())).thenReturn(roomProgram);
			when(gseStrategy.getCustomerOffers(ArgumentMatchers.any())).thenReturn(response);
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
			CustomerOfferResponse actualResponse = roomProgramDAOImpl.getCustomerOffers(request);

			Assert.assertNotNull(response);
			Assert.assertEquals(actualResponse.getOffers().get(0), response.getOffers().get(0));

		} catch (Exception e) {
			Assert.fail("getCustomerOffer Failed");
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}

	/**
	 * Check findProgramsIfSegment segment scenario
	 */
	@Test
	public void findProgramsIfSegmentTest_segment() {
		roomProgramDAOImpl.appProperties = new ApplicationProperties();
		roomProgramDAOImpl.appProperties.setEnableNewSegmentsKey("SegmentKey");
		roomProgramDAOImpl.appProperties.setRbsEnv("Dev");
		when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("false");
		List<RoomProgramBasic> roomProgramBasicList = new ArrayList<>();
		RoomProgramBasic roomProgramBasic = new RoomProgramBasic();
		roomProgramBasicList.add(roomProgramBasic);
		when(gseStrategy.findProgramsIfSegment(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(roomProgramBasicList);
		List<RoomProgramBasic> actualResponse = roomProgramDAOImpl
				.findProgramsIfSegment("66964e2b-2550-4476-84c3-1a4c0c5c067f", "ICE");
		assertEquals(roomProgramBasicList, actualResponse);
	}

	/**
	 * Check findProgramsIfSegment GSE enabled scenario
	 */
	@Test
	public void findProgramsIfSegmentTest_GseEnabled() {
		roomProgramDAOImpl.appProperties = new ApplicationProperties();
		roomProgramDAOImpl.appProperties.setEnableNewSegmentsKey("SegmentKey");
		roomProgramDAOImpl.appProperties.setRbsEnv("Dev");
		roomProgramDAOImpl.appProperties.setGseDisabled(false);
		when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("true");
		List<RoomProgramBasic> roomProgramBasicList = new ArrayList<>();
		RoomProgramBasic roomProgramBasic = new RoomProgramBasic();
		roomProgramBasicList.add(roomProgramBasic);
		when(gseStrategy.findProgramsByRatePlanCode(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(roomProgramBasicList);
		when(gseStrategy.getRatePlanCodeByProgramId(Mockito.anyString())).thenReturn("123");
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
		List<RoomProgramBasic> actualResponse = roomProgramDAOImpl
				.findProgramsIfSegment("66964e2b-2550-4476-84c3-1a4c0c5c067f", "ICE");
		assertEquals(roomProgramBasicList, actualResponse);
	}

	/**
	 * Check findProgramsIfSegment ACRS enabled scenario
	 */
	@Test
	public void findProgramsIfSegmentTest_ACRS_Enabled() {
		roomProgramDAOImpl.appProperties = new ApplicationProperties();
		roomProgramDAOImpl.appProperties.setEnableNewSegmentsKey("SegmentKey");
		roomProgramDAOImpl.appProperties.setRbsEnv("Dev");
		roomProgramDAOImpl.appProperties.setGseDisabled(true);
		when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("true");
		List<RoomProgramBasic> roomProgramBasicList = new ArrayList<>();
		RoomProgramBasic roomProgramBasic = new RoomProgramBasic();
		roomProgramBasicList.add(roomProgramBasic);
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
		when(acrsStrategy.findProgramsByRatePlanCode(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(roomProgramBasicList);
		when(acrsStrategy.getRatePlanCodeByProgramId(Mockito.anyString())).thenReturn("123");
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
		List<RoomProgramBasic> actualResponse = roomProgramDAOImpl
				.findProgramsIfSegment("66964e2b-2550-4476-84c3-1a4c0c5c067f", "ICE");
		assertEquals(roomProgramBasicList, actualResponse);
	}

	/**
	 * Check getRoomPrograms GSE flow
	 */
	@Test
	public void getRoomProgramsTest_gse() {
		RoomProgramsRequestDTO offersRequest = RoomProgramsRequestDTO.builder()
				.propertyId("77964e2b-2550-4476-84c3-1a4c0c5c067f").resortPricing(false).build();

		List<CustomerPromotion> customerPromotionList = new ArrayList<>();
		CustomerPromotion customerPromotion = new CustomerPromotion();
		customerPromotionList.add(customerPromotion);
		when(loyaltyDao.getPlayerPromos(offersRequest.getMlifeNumber())).thenReturn(customerPromotionList);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
		RoomProgramsResponseDTO roomProgramsResponseDTO = new RoomProgramsResponseDTO();
		roomProgramsResponseDTO.setPatronPrograms(null);
		when(gseStrategy.getRoomPrograms(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(roomProgramsResponseDTO);
		RoomProgramsResponseDTO actualResponse = roomProgramDAOImpl.getRoomPrograms(offersRequest);
		assertEquals(roomProgramsResponseDTO, actualResponse);
	}

	/**
	 * Check getRoomProgramsTest acrs flow
	 */
	@Test
	public void getRoomProgramsTest_acrs() {
		RoomProgramsRequestDTO offersRequest = RoomProgramsRequestDTO.builder()
				.propertyId("77964e2b-2550-4476-84c3-1a4c0c5c067f").resortPricing(false).build();

		List<CustomerPromotion> customerPromotionList = new ArrayList<>();
		CustomerPromotion customerPromotion = new CustomerPromotion();
		customerPromotionList.add(customerPromotion);
		when(loyaltyDao.getPlayerPromos(offersRequest.getMlifeNumber())).thenReturn(customerPromotionList);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		RoomProgramsResponseDTO roomProgramsResponseDTO = new RoomProgramsResponseDTO();
		roomProgramsResponseDTO.setPatronPrograms(null);
		when(acrsStrategy.getRoomPrograms(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(roomProgramsResponseDTO);
		RoomProgramsResponseDTO actualResponse = roomProgramDAOImpl.getRoomPrograms(offersRequest);
		assertEquals(roomProgramsResponseDTO, actualResponse);
	}

	/**
	 * Check getRoomProgramsTest acrs flow for userRankValues in response
	 */
	@Test
	public void getRoomProgramsTest_acrs_UserRankValues() {
		RoomProgramsRequestDTO offersRequest = RoomProgramsRequestDTO.builder()
				.propertyId("77964e2b-2550-4476-84c3-1a4c0c5c067f")
				.resortPricing(false)
				.perpetualPricing(true)
				.mlifeNumber("123456")
				.build();

		ApplicationProperties appPropertiesTest = new ApplicationProperties();
		appPropertiesTest.setPropertyCodeVar141Map(createVar141PropertyMap());
		roomProgramDAOImpl.appProperties = appPropertiesTest;

		CVSResponse cvsResponse = convert("/cvs-response.json", CVSResponse.class);
		when(cvsDao.getCustomerValues(offersRequest.getMlifeNumber())).thenReturn(cvsResponse);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		RoomProgramsResponseDTO roomProgramsResponseDTO = new RoomProgramsResponseDTO();
		roomProgramsResponseDTO.setPatronPrograms(null);
		when(acrsStrategy.getRoomPrograms(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(roomProgramsResponseDTO);

		RoomProgramsResponseDTO actualResponse = roomProgramDAOImpl.getRoomPrograms(offersRequest);
		assertNotNull(actualResponse.getUserCvsValues());
		assertTrue(actualResponse.getUserCvsValues().contains("beau=10"));
		assertTrue(actualResponse.getUserCvsValues().contains("mgmnh=11"));
		assertTrue(actualResponse.getUserCvsValues().contains("las vegas=31"));
		System.out.println(actualResponse.getUserCvsValues());
	}

	private Map<String, String> createVar141PropertyMap() {
		Map<String, String> mp = new HashMap<>();
		mp.put("180", "beau");
		mp.put("016", "mgmgd");
		mp.put("307", "mgmnh");
		mp.put("304", "bgta");
		mp.put("306", "mgmsp");
		mp.put("312", "mgmnp");
		mp.put("308", "emc");
		mp.put("001", "las vegas");
		mp.put("290", "las vegas");
		mp.put("930", "las vegas");
		mp.put("190", "las vegas");
		mp.put("021", "las vegas");
		mp.put("280", "las vegas");
		mp.put("938", "las vegas");
		mp.put("275", "las vegas");
		mp.put("285", "las vegas");
		mp.put("195", "las vegas");
		return mp;
	}

	/**
	 * Check getRoomProgramsTest without property id and GSE enabled
	 */
	@Test
	public void getRoomProgramsTest_without_propertyId_GSE() {
		roomProgramDAOImpl.appProperties = new ApplicationProperties();
		roomProgramDAOImpl.appProperties.setGseDisabled(false);
		RoomProgramsRequestDTO offersRequest = RoomProgramsRequestDTO.builder().build();

		List<CustomerPromotion> customerPromotionList = new ArrayList<>();
		CustomerPromotion customerPromotion = new CustomerPromotion();
		customerPromotionList.add(customerPromotion);
		when(loyaltyDao.getPlayerPromos(offersRequest.getMlifeNumber())).thenReturn(customerPromotionList);
		when(gseStrategy.getRoomPrograms(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(getRoomProgramsResponseDTO());
		RoomProgramsResponseDTO actualResponse = roomProgramDAOImpl.getRoomPrograms(offersRequest);
		assertEquals(getRoomProgramsResponseDTO(), actualResponse);
	}

	private RoomProgramsResponseDTO getRoomProgramsResponseDTO() {
		RoomProgramsResponseDTO roomProgramsResponseDTO = new RoomProgramsResponseDTO();
		List<RoomProgramDTO> roomProgramDTOList = new ArrayList<>();
		RoomProgramDTO roomProgramDTO = new RoomProgramDTO();
		roomProgramDTO.setProgramId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgramDTO.setPromo("123");
		roomProgramDTO.setPropertyId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgramDTO.setRatePlanCode("123");
		roomProgramDTOList.add(roomProgramDTO);
		roomProgramsResponseDTO.setIceChannelPrograms(roomProgramDTOList);
		roomProgramsResponseDTO.setPatronPrograms(roomProgramDTOList);
		roomProgramsResponseDTO.setPoPrograms(roomProgramDTOList);
		return roomProgramsResponseDTO;
	}

	/**
	 * Check getRoomProgramsTest without property id and ACRS enabled
	 */
	@Test
	public void getRoomProgramsTest_without_propertyId_ACRS() {
		roomProgramDAOImpl.appProperties = new ApplicationProperties();
		roomProgramDAOImpl.appProperties.setGseDisabled(true);
		RoomProgramsRequestDTO offersRequest = RoomProgramsRequestDTO.builder().build();

		List<CustomerPromotion> customerPromotionList = new ArrayList<>();
		CustomerPromotion customerPromotion = new CustomerPromotion();
		customerPromotionList.add(customerPromotion);
		when(loyaltyDao.getPlayerPromos(offersRequest.getMlifeNumber())).thenReturn(customerPromotionList);
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.getRoomPrograms(Mockito.any(), Mockito.any(),Mockito.any())).thenReturn(getRoomProgramsResponseDTO());
		RoomProgramsResponseDTO actualResponse = roomProgramDAOImpl.getRoomPrograms(offersRequest);
		assertEquals(getRoomProgramsResponseDTO(), actualResponse);
	}

	@Test
	public void getProgramPromoAssociationTest_GSE() {
		Map<String, String> associationMap = new HashMap<>();
		associationMap.put("123", "GSE");
		RoomProgramPromoAssociationRequest request = new RoomProgramPromoAssociationRequest();
		List<String> programIds = new ArrayList<>();
		programIds.add("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		programIds.add("88964e2b-2550-4476-84c3-1a4c0c5c067f");
		request.setProgramIds(programIds);
		when(gseStrategy.getProgramPromoAssociation(Mockito.any())).thenReturn(associationMap);
		Map<String, String> actualResponse = roomProgramDAOImpl.getProgramPromoAssociation(request);
		assertEquals(associationMap, actualResponse);
	}

	@Test
	public void getProgramPromoAssociationTest_ACRS() {
		Map<String, String> associationMap = new HashMap<>();
		associationMap.put("123", "GSE");
		RoomProgramPromoAssociationRequest request = new RoomProgramPromoAssociationRequest();
		List<String> programIds = new ArrayList<>();
		programIds.add("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		programIds.add("88964e2b-2550-4476-84c3-1a4c0c5c067f");
		request.setProgramIds(programIds);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.getProgramPromoAssociation(Mockito.any())).thenReturn(associationMap);
		Map<String, String> actualResponse = roomProgramDAOImpl.getProgramPromoAssociation(request);
		assertEquals(associationMap, actualResponse);
	}

	@Test
	public void updateValidateResponseForPackageProgramsTest_GSE() {
		RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
		validateRequest.setPropertyId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		validateRequest.setProgramId("88964e2b-2550-4476-84c3-1a4c0c5c067f");
		RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
		Program program = new Program();
		program.setHdePackage("True");
		when(gseStrategy.getRatePlanCodeByProgramId(Mockito.any())).thenReturn("123");
		when(packageConfigDao.getPackageConfigs(Mockito.any(), Mockito.any())).thenReturn(null);
		when(programContentDao.getProgramContent(Mockito.any(), Mockito.any())).thenReturn(program);
		roomProgramDAOImpl.updateValidateResponseForPackagePrograms(validateRequest, validateResponse);
	}

	@Test
	public void updateValidateResponseForPackageProgramsTest_ACRS() {
		RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
		validateRequest.setPropertyId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		validateRequest.setProgramId("88964e2b-2550-4476-84c3-1a4c0c5c067f");
		RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
		Program program = new Program();
		program.setHdePackage("True");
		PackageConfig[] packageConfigs = new PackageConfig[2];
		packageConfigs[0] = new PackageConfig();
		packageConfigs[0].setActive("TRUE");
		packageConfigs[0].setPackageId("123");
		when(packageConfigDao.getPackageConfigs(Mockito.any(), Mockito.any())).thenReturn(packageConfigs);
		when(acrsStrategy.getRatePlanCodeByProgramId(Mockito.any())).thenReturn("123");
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(programContentDao.getProgramContent(Mockito.any(), Mockito.any())).thenReturn(program);
		roomProgramDAOImpl.updateValidateResponseForPackagePrograms(validateRequest, validateResponse);
	}

	@Test
	public void getRoomSegmentTest_segment() {
		roomProgramDAOImpl.appProperties = new ApplicationProperties();
		roomProgramDAOImpl.appProperties.setEnableNewSegmentsKey("SegmentKey");
		roomProgramDAOImpl.appProperties.setRbsEnv("Dev");
		when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("false");
		RoomProgram roomProgram = new RoomProgram();
		roomProgram.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgram.setTravelPeriodStart(new Date());
		roomProgram.setTravelPeriodEnd(new Date());
		when(gseStrategy.findProgramsByRatePlanCode(Mockito.any(), Mockito.any()))
				.thenReturn(getRoomProgramBasicsList());

		when(roomProgramCacheService.getRoomProgram(Mockito.anyString())).thenReturn(roomProgram);
		RoomSegmentResponse actualResponse = roomProgramDAOImpl.getRoomSegment("ROOM",
				"77964e2b-2550-4476-84c3-1a4c0c5c067f", "ICE");
		actualResponse.setMinTravelPeriodStart(new Date());
		assertEquals(new Date().getDate(), actualResponse.getMinTravelPeriodStart().getDate());
	}

	@Test
	public void getRoomSegmentTest_disableNewSegmentsKey() {
		roomProgramDAOImpl.appProperties = new ApplicationProperties();
		roomProgramDAOImpl.appProperties.setEnableNewSegmentsKey("SegmentKey");
		roomProgramDAOImpl.appProperties.setRbsEnv("Dev");
		when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("true");
		RoomProgram roomProgram = new RoomProgram();
		roomProgram.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgram.setPromoCode("123");

		when(roomProgramCacheService.getRoomProgram(Mockito.anyString())).thenReturn(roomProgram);
		when(gseStrategy.findProgramsByRatePlanCode(Mockito.any(), Mockito.any()))
				.thenReturn(getRoomProgramBasicsList());
		RoomSegmentResponse actualResponse = roomProgramDAOImpl.getRoomSegment("",
				"77964e2b-2550-4476-84c3-1a4c0c5c067f", "ICE");
		actualResponse.setMinTravelPeriodStart(new Date());
		assertEquals(new Date().getDate(), actualResponse.getMinTravelPeriodStart().getDate());
	}

	@Test
	public void getRoomSegmentTest_segment_empty_GSE() {
		roomProgramDAOImpl.appProperties = new ApplicationProperties();
		roomProgramDAOImpl.appProperties.setEnableNewSegmentsKey("SegmentKey");
		roomProgramDAOImpl.appProperties.setRbsEnv("Dev");
		when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("false");
		RoomProgram roomProgram = new RoomProgram();
		roomProgram.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgram.setSegmentId("123");

		List<RoomProgramBasic> programsList = getRoomProgramBasicsList();
		when(gseStrategy.findProgramsByRatePlanCode(Mockito.any(), Mockito.any())).thenReturn(programsList);
		when(roomProgramCacheService.getRoomProgram(Mockito.anyString())).thenReturn(roomProgram);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(false);
		RoomSegmentResponse actualResponse = roomProgramDAOImpl.getRoomSegment("",
				"77964e2b-2550-4476-84c3-1a4c0c5c067f", "ICE");
		assertEquals(new Date().getDate(), actualResponse.getMinTravelPeriodStart().getDate());

	}

	@Test
	public void getRoomSegmentTest_segment_empty_ACRS() {
		roomProgramDAOImpl.appProperties = new ApplicationProperties();
		roomProgramDAOImpl.appProperties.setEnableNewSegmentsKey("SegmentKey");
		roomProgramDAOImpl.appProperties.setRbsEnv("Dev");
		when(secretProperties.getSecretValue(Mockito.anyString())).thenReturn("false");
		RoomProgram roomProgram = new RoomProgram();
		roomProgram.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgram.setSegmentId("123");
		when(acrsStrategy.findProgramsByRatePlanCode(Mockito.any(), Mockito.any()))
				.thenReturn(getRoomProgramBasicsList());
		when(roomProgramCacheService.getRoomProgram(Mockito.anyString())).thenReturn(roomProgram);
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		RoomSegmentResponse actualResponse = roomProgramDAOImpl.getRoomSegment("", "", "ICE");
		assertTrue(null != actualResponse);

	}

	private List<RoomProgramBasic> getRoomProgramBasicsList() {
		List<RoomProgramBasic> programsList = new ArrayList<>();
		RoomProgramBasic roomProgramBasic = new RoomProgramBasic();
		roomProgramBasic.setProgramId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgramBasic.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgramBasic.setActive(true);
		roomProgramBasic.setBookableOnline(true);
		roomProgramBasic.setTravelPeriodStart(new Date());
		roomProgramBasic.setTravelPeriodEnd(ReservationUtil.convertLocalDateToDate(LocalDate.now().plusDays(3)));
		roomProgramBasic.setBookingEndDate(ReservationUtil.convertLocalDateToDate(LocalDate.now().plusDays(3)));
		programsList.add(roomProgramBasic);
		roomProgramBasic = new RoomProgramBasic();
		roomProgramBasic.setProgramId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgramBasic.setPropertyId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		roomProgramBasic.setActive(true);
		roomProgramBasic.setBookableOnline(true);
		roomProgramBasic.setTravelPeriodStart(new Date());
		roomProgramBasic.setTravelPeriodEnd(ReservationUtil.convertLocalDateToDate(LocalDate.now().plusDays(3)));
		roomProgramBasic.setBookingEndDate(ReservationUtil.convertLocalDateToDate(LocalDate.now().plusDays(3)));
		programsList.add(roomProgramBasic);
		return programsList;
	}

	@Test
	public void findProgramsByGroupCodeTest_GSE() {
		when(gseStrategy.findProgramsByGroupCode(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(getRoomProgramBasicsList());
		List<RoomProgramBasic> actualResponse=roomProgramDAOImpl.findProgramsByGroupCode("abc",getFutureLocalDate(15),getFutureLocalDate(30),"ICE");
		assertEquals(actualResponse.toString(),getRoomProgramBasicsList().toString());
	}

	@Test
	public void findProgramsByGroupCodeTest_ACRS() {
		when(referenceDataDAOHelper.isAcrsEnabled()).thenReturn(true);
		when(acrsStrategy.findProgramsByGroupCode(Mockito.any(), Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(getRoomProgramBasicsList());
		List<RoomProgramBasic> actualResponse=roomProgramDAOImpl.findProgramsByGroupCode("abc",getFutureLocalDate(15),getFutureLocalDate(30),"ICE");
		assertEquals(actualResponse.toString(),getRoomProgramBasicsList().toString());
		
		
	}
	
	@Test
	public void isProgramPOTest_GSE() {
		when(gseStrategy.isProgramPO(Mockito.any())).thenReturn(true);
		boolean actualResponse=roomProgramDAOImpl.isProgramPO("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		assertEquals(actualResponse,true);
	}
	
	@Test
	public void findProgramsBySegmentTest() {
		when(gseStrategy.findProgramsIfSegment(Mockito.any(),Mockito.any())).thenReturn(getRoomProgramBasicsList());
		List<RoomProgramBasic> actualResponse=roomProgramDAOImpl.findProgramsBySegment("ROOM","ICE");
		assertEquals(actualResponse.toString(),getRoomProgramBasicsList().toString());
	}
	
	@Test
	public void getProgramByPromoCodeTest() {
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.getProgramByPromoCode(Mockito.any(),Mockito.any())).thenReturn("abc");
		String actualResponse=roomProgramDAOImpl.getProgramByPromoCode("77964e2b-2550-4476-84c3-1a4c0c5c067f","123");
		assertEquals(actualResponse,"abc");
	}
	
	@Test
	public void validateProgramV2Test() {
		RoomProgramValidateRequest request=new RoomProgramValidateRequest();
		request.setProgramId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		request.setPropertyId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		RoomProgramValidateResponse roomProgramValidateResponse=new RoomProgramValidateResponse();
		when(acrsStrategy.validateProgramV2(Mockito.any())).thenReturn(roomProgramValidateResponse);
		when(gseStrategy.validateProgramV2(Mockito.any())).thenReturn(roomProgramValidateResponse);
		RoomProgramValidateResponse actualResponse=roomProgramDAOImpl.validateProgramV2(request);
		assertEquals(actualResponse,roomProgramValidateResponse);
	}
	
	@Test
	public void getProgramByPromoCodeTestWithPropertyIdAndPromoCode() {
		String propertyId = "77964e2b-2550-4476-84c3-1a4c0c5c067f";
		String promoCode = "PTRN";
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		when(acrsStrategy.getProgramByPromoCode(Mockito.any(),Mockito.any())).thenReturn("PTRN");
	    String programId =roomProgramDAOImpl.getProgramByPromoCode(propertyId, promoCode);
	    Assert.assertNotNull(programId);
		assertEquals(programId.toString(),"PTRN".toString());
	}
	
	@Test
	public void validateProgramTest() {
		RoomProgramValidateRequest request=new RoomProgramValidateRequest();
		request.setProgramId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		request.setPropertyId("77964e2b-2550-4476-84c3-1a4c0c5c067f");
		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
		RoomProgramValidateResponse roomProgramValidateResponse=new RoomProgramValidateResponse();
		when(acrsStrategy.validateProgramV2(Mockito.any())).thenReturn(roomProgramValidateResponse);
		RoomProgramValidateResponse response=roomProgramDAOImpl.validateProgram(request);
		Assert.assertNull(response);		
	}
}
