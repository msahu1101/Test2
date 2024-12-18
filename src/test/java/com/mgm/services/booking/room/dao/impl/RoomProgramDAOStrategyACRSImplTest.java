package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.CVSDao;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.model.ReservationSystemType;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.CustomerOffersRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.response.ApplicableProgramsResponse;
import com.mgm.services.booking.room.model.response.CVSResponse;
import com.mgm.services.booking.room.model.response.CustomerOfferResponse;
import com.mgm.services.booking.room.model.response.ENRRatePlanSearchResponse;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.model.response.RoomOfferDetails;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.transformer.GroupSearchTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;

public class RoomProgramDAOStrategyACRSImplTest extends BaseRoomBookingTest {

    @Mock
    private static RestTemplate client;

    @Mock
    private static IDMSTokenDAO idmsTokenDAO;

    @Mock
    private static CVSDao cvsDao;

    @Mock
    private static DomainProperties domainProperties;

	@Mock
	SecretsProperties secretProperties;
	
    @Mock
    private static ApplicationProperties applicationProperties;

    @Mock
    private static RestTemplateBuilder restTemplateBuilder;

    @Mock
    private static URLProperties urlProperties;

    @Mock
    private static AcrsProperties acrsProperties;

    @Mock
    private static ReferenceDataDAOHelper referenceDataDAOHelper;
   
    @Mock
    private static ACRSConversionUtil acrsConversionUtil;   

    
    private static GroupSearchDAOStrategyACRSImpl groupSearchDAOStrategyACRSImpl;
    @Mock
   private static GroupSearchTransformer groupSearchTransformer;
    
    @Mock
    private static SecretsProperties secretProps;
    @InjectMocks
    private static RoomProgramDAOStrategyACRSImpl roomProgramDAOStrategyACRSImpl;

    static Logger logger = LoggerFactory.getLogger(RoomProgramDAOStrategyACRSImplTest.class);


    @BeforeClass
    public static void init() throws SSLException, ParseException {
        client = Mockito.mock(RestTemplate.class);
        domainProperties = new DomainProperties();
        domainProperties.setEnrSearch("http://enrSearch");
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
        applicationProperties = Mockito.mock(ApplicationProperties.class);
        referenceDataDAOHelper = Mockito.mock(ReferenceDataDAOHelper.class);
        idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);
        cvsDao = Mockito.mock(CVSDao.class);
        acrsProperties = new AcrsProperties();
        acrsProperties.setLiveCRS(true);
        urlProperties = new URLProperties();
        urlProperties.setEnrChannelRatePlanSearch("/enrSearch");
        urlProperties.setEnrPromoChannelRatePlanSearch("/enrSearch");
        applicationProperties.setApigeeEnvironment("nonprod-dev");
        applicationProperties.setAcrsPropertyListSecretKey("acrs-properties");
        secretProps = Mockito.mock(SecretsProperties.class);
        CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
        groupSearchDAOStrategyACRSImpl = Mockito.mock(GroupSearchDAOStrategyACRSImpl.class);

        try {
            when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
                    applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
                    applicationProperties.getConnectionTimeout(),
                    applicationProperties.getReadTimeOut(),
                    applicationProperties.getSocketTimeOut(),
                    1,applicationProperties.getEnrRestTTL())).thenReturn(client);
            roomProgramDAOStrategyACRSImpl = new RoomProgramDAOStrategyACRSImpl(urlProperties, domainProperties, applicationProperties,
                    acrsProperties, restTemplateBuilder, referenceDataDAOHelper, null,secretProps);
            roomProgramDAOStrategyACRSImpl.setIdmsTokenDAO(idmsTokenDAO);
            roomProgramDAOStrategyACRSImpl.setCvsDao(cvsDao);
            roomProgramDAOStrategyACRSImpl.setGroupSearch(groupSearchDAOStrategyACRSImpl);
        } catch (SSLException e) {
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }


    /**
     * Check getApplicableProgramsTest.
     */
    

    @Test
    public void getApplicableProgramsTest() {

        try {
            final File file = new File(getClass().getResource("/search-enr-rateplans.json").getPath());
            ResponseEntity<ENRRatePlanSearchResponse[]> response = new ResponseEntity<ENRRatePlanSearchResponse[]>(
                    convertCrs(file, ENRRatePlanSearchResponse[].class), HttpStatus.OK);

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken("token");

            when(referenceDataDAOHelper.retrieveAcrsPropertyID(anyString())).thenReturn("MV021");
            when(referenceDataDAOHelper.retrieveRatePlanDetail(anyString(), anyString(), anyBoolean())).thenReturn("RPCD-v-EXPLD-d-PROP-v-MV021");

            when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);

            when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                    ArgumentMatchers.any(), ArgumentMatchers.<Class<ENRRatePlanSearchResponse[]>>any(), Mockito.anyMap()))
                    .thenReturn(response);

            final ApplicableProgramRequestDTO request = ApplicableProgramRequestDTO.builder().propertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad").build();

            final ApplicableProgramsResponse programResponse = roomProgramDAOStrategyACRSImpl.getApplicablePrograms(request);

            Assert.assertNotNull(programResponse);
            Assert.assertEquals(1, programResponse.getProgramIds().size());
            Assert.assertEquals(4, programResponse.getPrograms().size());
            Assert.assertTrue(programResponse.getProgramIds().contains("RPCD-v-EXPLD-d-PROP-v-MV021"));

        } catch (Exception e) {
            Assert.fail("getApplicableProgramsTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }

    /**
     * Check validateProgramV2.
     */
    @Test
    public void validateProgramV2() {

        try {
            final File file = new File(getClass().getResource("/search-enr-rateplans.json").getPath());
            ResponseEntity<ENRRatePlanSearchResponse[]> response = new ResponseEntity<ENRRatePlanSearchResponse[]>(
                    convertCrs(file, ENRRatePlanSearchResponse[].class), HttpStatus.OK);
            for (ENRRatePlanSearchResponse singleResponse : response.getBody()) {
                singleResponse.setDescription(ServiceConstant.MY_VEGAS_KEYWORDS[0]);
            }

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken("token");

            when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);

            when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                    ArgumentMatchers.any(), ArgumentMatchers.<Class<ENRRatePlanSearchResponse[]>>any(), Mockito.anyMap()))
                    .thenReturn(response);

            final RoomProgramValidateRequest request = new RoomProgramValidateRequest();
            request.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            request.setProgramId("RPCD-v-MVG01-d-PROP-v-MV021");

            RoomProgramValidateResponse roomProgramValidateResponse = roomProgramDAOStrategyACRSImpl.validateProgramV2(request);

            Assert.assertNotNull(roomProgramValidateResponse);
            Assert.assertTrue(roomProgramValidateResponse.isValid());
            Assert.assertFalse(roomProgramValidateResponse.isExpired());
            Assert.assertTrue(roomProgramValidateResponse.isMyvegas());

        } catch (Exception e) {
            Assert.fail("validateProgramV2 Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }

    /**
     * Check findProgramsByRatePlanCode.
     */
    @Test
    public void findProgramsByRatePlanCodeOrProgramId() {

        try {
            final File file = new File(getClass().getResource("/search-enr-rateplans.json").getPath());
            ResponseEntity<ENRRatePlanSearchResponse[]> response = new ResponseEntity<ENRRatePlanSearchResponse[]>(
                    convertCrs(file, ENRRatePlanSearchResponse[].class), HttpStatus.OK);
         
            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken("token");

            Map<String, ReservationSystemType> propertyIds = new HashMap<>();
            propertyIds.put("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", ReservationSystemType.GSE);
            when(referenceDataDAOHelper.isPropertyManagedByAcrs(anyString())).thenReturn(true);

            when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);

            when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                    ArgumentMatchers.any(), ArgumentMatchers.<Class<ENRRatePlanSearchResponse[]>>any(), Mockito.anyMap()))
                    .thenReturn(response);

            List<RoomProgramBasic> programs = roomProgramDAOStrategyACRSImpl.findProgramsByRatePlanCode("PREVL", "mgmri");
            assertTrue(programs.size() > 1);

            programs = roomProgramDAOStrategyACRSImpl.findProgramsIfSegment("RPCD-v-PREVL-d-PROP-v-MV021", "mgmri");
            assertTrue(programs.size() > 1);


        } catch (Exception e) {
            Assert.fail("findProgramsByRatePlanCodeOrProgramId call Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }

    /**
     * Check getRoomBasicPrograms.
     */
    @Test
    public void getRoomBasicPrograms() {

        try {
            int segment = 10;

            ENRRatePlanSearchResponse response1 = new ENRRatePlanSearchResponse();
            response1.setRateCode("CASHS" + segment);
            response1.setPropertyCode("MV021");
            response1.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            response1.setRatePlanId(ACRSConversionUtil.createRatePlanCodeGuid(response1.getRateCode(), response1.getPropertyCode()));

            ENRRatePlanSearchResponse response2 = new ENRRatePlanSearchResponse();
            response2.setRateCode("COMPS" + segment);
            response2.setPropertyCode("MV021");
            response2.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            response2.setRatePlanId(ACRSConversionUtil.createRatePlanCodeGuid(response2.getRateCode(), response2.getPropertyCode()));

            int patronPromo = 309306;
            ENRRatePlanSearchResponse response3 = new ENRRatePlanSearchResponse();
            response3.setRateCode("RPCDPTRN" + patronPromo);
            response3.setPromo("PTRN" + patronPromo);
            response3.setPropertyCode("MV021");
            response3.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            response3.setRatePlanId(ACRSConversionUtil.createRatePlanCodeGuid(response3.getRateCode(), response3.getPropertyCode()));

            ENRRatePlanSearchResponse response4 = new ENRRatePlanSearchResponse();
            response4.setRateCode("PREVL");
            response4.setPropertyCode("MV021");
            response4.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            response4.setRatePlanId(ACRSConversionUtil.createRatePlanCodeGuid(response4.getRateCode(), response4.getPropertyCode()));

            ResponseEntity<ENRRatePlanSearchResponse[]> response = new ResponseEntity<ENRRatePlanSearchResponse[]>(
                    new ENRRatePlanSearchResponse[] {response1, response2, response3, response4}, HttpStatus.OK);

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken("token");

            Map<String, ReservationSystemType> propertyIds = new HashMap<>();
            propertyIds.put("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", ReservationSystemType.GSE);

            when(referenceDataDAOHelper.isPropertyManagedByAcrs(anyString())).thenReturn(true);

            final CVSResponse cvsResponse = new CVSResponse();
            final CVSResponse.CVSCustomer customer = new CVSResponse.CVSCustomer();
            cvsResponse.setCustomer(customer);
            final CVSResponse.CVSCustomerValue value1 = new CVSResponse.CVSCustomerValue();
            customer.setCustomerValues(new CVSResponse.CVSCustomerValue[]{value1});
            value1.setProperty("012");
            value1.setGsePropertyIds(Arrays.asList("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad"));
            final CVSResponse.CVSValue cvsValue = new CVSResponse.CVSValue();
            value1.setValue(cvsValue);
            final CVSResponse.CVSCustomerGrade grade = new CVSResponse.CVSCustomerGrade();
            cvsValue.setCustomerGrade(grade);
            grade.setSegment(segment);
            grade.setPowerRank(segment);
            grade.setDominantPlay("S");

            List<CustomerPromotion> patronOffers = new ArrayList<>();
            CustomerPromotion promotion1 = new CustomerPromotion();
            patronOffers.add(promotion1);
            promotion1.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            promotion1.setPromoId(String.valueOf(patronPromo));

            when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
            when(cvsDao.getCustomerValues(anyString())).thenReturn(cvsResponse);

            when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                    ArgumentMatchers.any(), ArgumentMatchers.<Class<ENRRatePlanSearchResponse[]>>any(), Mockito.anyMap()))
                    .thenReturn(response);
            when(secretProps.getSecretValue(applicationProperties.getAcrsPropertyListSecretKey())).thenReturn("MV021");
            RoomProgramsRequestDTO request = RoomProgramsRequestDTO.builder().mlifeNumber("80662215").source(ServiceConstant.ICE).build();

            RoomProgramsResponseDTO roomPrograms = roomProgramDAOStrategyACRSImpl.getRoomPrograms(request, patronOffers,cvsResponse);
            assertEquals(1, roomPrograms.getPoPrograms().size());
            assertEquals(1, roomPrograms.getPatronPrograms().size());
            assertEquals(1, roomPrograms.getIceChannelPrograms().size());

        } catch (Exception e) {
            Assert.fail("getRoomBasicPrograms call Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }

    /**
     * Check getRoomBasicPrograms.
     */
    @Test
    public void getRatePlanById() {

        try {

            // Case I: Patron Promo retrieval

            int patronPromo = 309306;
            ENRRatePlanSearchResponse response1 = new ENRRatePlanSearchResponse();
            response1.setRateCode("RPCDPTRN" + patronPromo);
            response1.setPromo("PTRN" + patronPromo);
            response1.setPropertyCode("MV021");
            response1.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            response1.setRatePlanId(ACRSConversionUtil.createRatePlanCodeGuid(response1.getRateCode(), response1.getPropertyCode()));

            ENRRatePlanSearchResponse response2 = new ENRRatePlanSearchResponse();
            response2.setRateCode("RPCDPTRN" + patronPromo);
            response2.setPromo("PTRN" + patronPromo);
            response2.setPropertyCode("MV021");
            response2.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            response2.setRatePlanId(ACRSConversionUtil.createRatePlanCodeGuid(response2.getRateCode(), response2.getPropertyCode()));


            ResponseEntity<ENRRatePlanSearchResponse[]> response = new ResponseEntity<ENRRatePlanSearchResponse[]>(
                    new ENRRatePlanSearchResponse[] {response1, response2}, HttpStatus.OK);

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken("token");

            Map<String, ReservationSystemType> propertyIds = new HashMap<>();
            propertyIds.put("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad", ReservationSystemType.GSE);

            when(referenceDataDAOHelper.isPropertyManagedByAcrs(anyString())).thenReturn(true);

            when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);

            when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                    ArgumentMatchers.any(), ArgumentMatchers.<Class<ENRRatePlanSearchResponse[]>>any(), Mockito.anyMap()))
                    .thenReturn(response);

            RoomProgramV2Request request = new RoomProgramV2Request();
            request.setProgramIds(new ArrayList<>());
            request.getProgramIds().add(response1.getRatePlanId());

            List<RoomOfferDetails> offers = roomProgramDAOStrategyACRSImpl.getRatePlanById(request);
            assertEquals(1, offers.size());
            assertEquals(response1.getPromo(), offers.get(0).getPatronPromoId());
            assertEquals(response1.getPromo(), offers.get(0).getPromo());


            //Case II: PO retrieval
            int segment = 10;
            response1 = new ENRRatePlanSearchResponse();
            response1.setRateCode("COS" + segment);
            response1.setPropertyCode("MV021");
            response1.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            response1.setRatePlanId(ACRSConversionUtil.createRatePlanCodeGuid(response1.getRateCode(), response1.getPropertyCode()));

            response = new ResponseEntity<ENRRatePlanSearchResponse[]>(
                    new ENRRatePlanSearchResponse[] {response1}, HttpStatus.OK);

            when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                    ArgumentMatchers.any(), ArgumentMatchers.<Class<ENRRatePlanSearchResponse[]>>any(), Mockito.anyMap()))
                    .thenReturn(response);
            request.setProgramIds(new ArrayList<>());
            request.getProgramIds().add(response1.getRatePlanId());
            offers = roomProgramDAOStrategyACRSImpl.getRatePlanById(request);
            assertEquals(1, offers.size());
            assertEquals(segment, offers.get(0).getSegmentTo().intValue());
            assertEquals(response1.getRatePlanId(), offers.get(0).getId());



            //Case III: Normal RatePlan retrieval

            response1 = new ENRRatePlanSearchResponse();
            response1.setRateCode("PREVL");
            response1.setPropertyCode("MV021");
            response1.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            response1.setRatePlanId(ACRSConversionUtil.createRatePlanCodeGuid(response1.getRateCode(), response1.getPropertyCode()));

            response = new ResponseEntity<ENRRatePlanSearchResponse[]>(
                    new ENRRatePlanSearchResponse[] {response1}, HttpStatus.OK);

            when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                    ArgumentMatchers.any(), ArgumentMatchers.<Class<ENRRatePlanSearchResponse[]>>any(), Mockito.anyMap()))
                    .thenReturn(response);

            request.setProgramIds(new ArrayList<>());
            request.getProgramIds().add(response1.getRatePlanId());
            offers = roomProgramDAOStrategyACRSImpl.getRatePlanById(request);
            assertEquals(1, offers.size());
            assertEquals(response1.getRatePlanId(), offers.get(0).getId());

        } catch (Exception e) {
            Assert.fail("getRatePlanById call Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }
    
    @Test
    public void getCustomerOffersTest() {
        try {
            CustomerOffersRequestDTO request = CustomerOffersRequestDTO.builder().propertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad").build();
            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken("token");
            when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
            
            final File file = new File(getClass().getResource("/search-enr-rateplans.json").getPath());
            ResponseEntity<ENRRatePlanSearchResponse[]> res = new ResponseEntity<ENRRatePlanSearchResponse[]>(
                    convertCrs(file, ENRRatePlanSearchResponse[].class), HttpStatus.OK);
            when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
                    ArgumentMatchers.any(), ArgumentMatchers.<Class<ENRRatePlanSearchResponse[]>>any(), Mockito.anyMap()))
                    .thenReturn(res);   
            when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
          
            CustomerOfferResponse actualResponse = roomProgramDAOStrategyACRSImpl.getCustomerOffers(request);
            Assert.assertNotNull(actualResponse);
        } catch (Exception e) {
            Assert.fail("getCustomerOffersTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }
    

    @Test
    public void getRatePlanCodeByProgramIdTest() {
    	String programId = "dcbf639c-b6fa-4055-8921-fc4948c81145";    	
        String response = roomProgramDAOStrategyACRSImpl.getRatePlanCodeByProgramId(programId);
        Assert.assertNull(response);
    	}
    
    @Test
    public void isProgramPOTest() {
    	String programId = "dcbf639c-b6fa-4055-8921-fc4948c81145";
        boolean response = roomProgramDAOStrategyACRSImpl.isProgramPO(programId);
        Assert.assertFalse(response);	
    }
    
    @Test
    public void getProgramByPromoCodeTest() {
    	String propertyId = "dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
    	String promoCode = "PTRN";
        String redisIntegrationKey = "rbsENRRedisIntegrationEnabledKey"; 
    	
    	MockHttpServletRequest request = new MockHttpServletRequest();
    	request.addHeader(ServiceConstant.X_MGM_CHANNEL, "channel");
    	request.addHeader(ServiceConstant.X_MGM_SOURCE, "source");
    	RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    	
      TokenResponse tokenResponse = new TokenResponse();
      tokenResponse.setAccessToken("token");
      when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
      
      final File file = new File(getClass().getResource("/search-enr-rateplans.json").getPath());
      ResponseEntity<ENRRatePlanSearchResponse[]> res = new ResponseEntity<ENRRatePlanSearchResponse[]>(
    		  convertCrs(file, ENRRatePlanSearchResponse[].class), HttpStatus.OK);

      when(client.exchange(ArgumentMatchers.any(), ArgumentMatchers.any(HttpMethod.class),
              ArgumentMatchers.any(), ArgumentMatchers.<Class<ENRRatePlanSearchResponse[]>>any(), Mockito.anyMap()))
              .thenReturn(res);
      when(applicationProperties.getRbsENRRedisIntegrationEnabled()).thenReturn(redisIntegrationKey);
      when(applicationProperties.getRbsEnv()).thenReturn("dev"); 
  
       String programId =roomProgramDAOStrategyACRSImpl.getProgramByPromoCode(propertyId, promoCode);
       Assert.assertNotNull(programId);
       assertEquals("RPCD-v-EXPLD-d-PROP-v-MV021",programId.toString());
    }
    
    @Test
    public void findProgramsByGroupCodeTest() throws ParseException {
    	
    	String groupCode = "GRPCD-v-GRP1-d-PROP-v-MV021";
    	LocalDate checkInDate=LocalDate.parse("2024-03-07"); 
    	LocalDate checkOutDate =LocalDate.parse("2024-03-08");
    	String source = "ice";
    	 
    	  GroupSearchV2Request req = new GroupSearchV2Request();
    	  req.setSource(source);
    	  req.setStartDate(checkInDate.toString());
    	  req.setEndDate(checkOutDate.toString());
    	  req.setId(groupCode);
    	  
        List<GroupSearchV2Request> groupSearchReq = new ArrayList<GroupSearchV2Request>();
        groupSearchReq.add(req);
        
    	GroupSearchV2Response response = new GroupSearchV2Response();
    	Date periodStartDate = new SimpleDateFormat("yyyy-MM-dd").parse(checkInDate.toString());
    	Date periodEndDate = new SimpleDateFormat("yyyy-MM-dd").parse(checkOutDate.toString());

    	response.setGroupCode(groupCode);
    	response.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
    	response.setPeriodStartDate(periodStartDate);
    	response.setPeriodEndDate(periodEndDate);
    	response.setId("dcbf639c-b6fa-4055-8921-fc4948c81145");
    	
        File file = new File(getClass().getResource("/groupblock-search-res.json").getPath());
    	List<GroupSearchV2Response> grpSearchRes = convert(file, mapper.getTypeFactory().constructCollectionType(List.class, GroupSearchV2Response.class));    	
    	 grpSearchRes.add(response);    		
    	 when(groupSearchDAOStrategyACRSImpl.searchGroup(any())).thenReturn(grpSearchRes);

    	 List<RoomProgramBasic> programsList =roomProgramDAOStrategyACRSImpl.findProgramsByGroupCode(groupCode,checkInDate,checkOutDate,source);
    	 Assert.assertNotNull(programsList);
         Assert.assertEquals("GRPCD-v-CASINO2024-d-PROP-v-MV021", programsList.get(0).getProgramId());
         Assert.assertEquals(2, programsList.size());
         assertTrue(programsList.size() > 1);
    }
 
    @Test
    public void testHasError() throws IOException {
    	RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		boolean result = errorHandler.hasError(httpResponse);

		// Assertion
		assertTrue(result);
	}
    
    @Test
    public void testHandleErrorSystemExceptionSYSTEM_ERROR() throws IOException {
    	RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);

		// Assertion
		SystemException ex = assertThrows(SystemException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
	}
    
    @Test
    public void testHandleErrorBusinessExceptionElse() throws IOException {
    	RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		acrsError.setTitle("Some Error Occured");
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.CONTINUE);

		// Assertion
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, ex.getErrorCode());
	}
    
    @Test
    public void testHandleErrorBusinessExceptionINVALID_DATES() throws IOException {
    	RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

		// Assertion
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.INVALID_DATES, ex.getErrorCode());
	}
    
    @Test
    public void testHandleErrorBusinessExceptionOFFER_NOT_AVAILABLE() throws IOException {
    	RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler errorHandler = 
    			new RoomProgramDAOStrategyACRSImpl.RestTemplateResponseErrorHandler();
		ClientHttpResponse httpResponse = Mockito.mock(ClientHttpResponse.class);
		ACRSErrorDetails acrsError = new ACRSErrorDetails();
		String acrsErrorJson = CommonUtil.convertObjectToJsonString(acrsError);
		InputStream is = new ByteArrayInputStream(acrsErrorJson.getBytes());
		when(httpResponse.getBody()).thenReturn(is);
		when(httpResponse.getHeaders()).thenReturn(new HttpHeaders());
		when(httpResponse.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
		
		// Assertion
		BusinessException ex = assertThrows(BusinessException.class, () -> errorHandler.handleError(httpResponse));
		assertSame(ErrorCode.OFFER_NOT_AVAILABLE, ex.getErrorCode());
	}
}