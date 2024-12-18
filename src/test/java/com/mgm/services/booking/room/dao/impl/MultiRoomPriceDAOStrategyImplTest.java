package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseRoomPriceBookingTest;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.crs.searchoffers.SuccessfulMultiAvailability;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
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
import java.util.*;

import static org.mockito.Mockito.when;

public class MultiRoomPriceDAOStrategyImplTest extends BaseRoomPriceBookingTest {

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
    private static ReferenceDataDAOHelper referenceDataDAOHelper;
    
    @InjectMocks
    private static ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl;
    
    @InjectMocks
    private static RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;

    static Logger logger = LoggerFactory.getLogger(MultiRoomPriceDAOStrategyImplTest.class);

    /**
     * Return Resort Pricing/Multi Availability from JSON mock file.
     */
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
    
    private void setMockAuthToken() {
		Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = new HashMap<String, ACRSAuthTokenResponse>();
		ACRSAuthTokenResponse tokenRes = new ACRSAuthTokenResponse();
		tokenRes.setToken("token");
		acrsAuthTokenResponseMap.put("ICECC", tokenRes);
        when(acrsOAuthTokenDAOImpl.generateToken()).thenReturn(acrsAuthTokenResponseMap);
    }

	private void setMockReferenceDataDAOHelper() {
        when(referenceDataDAOHelper.getAcrsVendor(Mockito.any())).thenReturn("ICECC");
    }

	@BeforeAll
	public static void init() {
		BaseRoomPriceBookingTest.staticInit();
        client = Mockito.mock(RestTemplate.class);
        domainProperties = new DomainProperties();
        domainProperties.setCrs("");
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
        referenceDataDAOHelper = Mockito.mock(ReferenceDataDAOHelper.class);
        acrsOAuthTokenDAOImpl = Mockito.mock(ACRSOAuthTokenDAOImpl.class);
        applicationProperties = Mockito.mock(ApplicationProperties.class);
        secretsProperties = Mockito.mock(SecretsProperties.class);
        acrsProperties = new AcrsProperties();
        acrsProperties.setModifyDateStartPath("modifyDateStartPath");
        acrsProperties.setModifyDateEndPath("modifyDateEndPath");
        acrsProperties.setModifySpecialRequestPath("modifySpecialRequestPath");
        acrsProperties.setLiveCRS(true);
        acrsProperties.setMaxPropertiesForResortPricing(5);
        urlProperties = new URLProperties();
        urlProperties.setAcrsMultiAvailabilityReservation(
                "/hotel-platform/cit/mgm/v6/hotel/reservations/MGM?property_code={property_code}&start_date={start_date}&end_date={end_date}");
        CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
        when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),1, applicationProperties.getCrsRestTTL())).thenReturn(client);
        roomPriceDAOStrategyACRSImpl = new RoomPriceDAOStrategyACRSImpl(urlProperties, domainProperties, applicationProperties,
                acrsProperties, restTemplateBuilder,null, referenceDataDAOHelper, acrsOAuthTokenDAOImpl, secretsProperties);
    }
    

    /**
     * Check Resort Pricing/Multi Availability if available for given dates.
     */
    @Test
	void getACRSMultiRoomAvailabilitySuccessTest() {

        try {
			setMockAuthToken();
			setMockReferenceDataDAOHelper();
			setMockForMultiRoomAvail();

			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2021-03-11"),
					LocalDate.parse("2021-03-12"))
					.propertyIds(Arrays.asList("MV021", "ARIA1"))
					.propertyId(null)
					.programId("CATST")
					.promo("ZNVLCLS")
					.build();
            List<AuroraPriceResponse> response = roomPriceDAOStrategyACRSImpl.getResortPrices(auroraPriceRequest);

			Assertions.assertNotNull(response);
			Assertions.assertEquals(AvailabilityStatus.AVAILABLE, response.get(0).getStatus());
			Assertions.assertEquals("MV021", response.get(0).getPropertyId());
			Assertions.assertEquals(19.0, response.get(0).getBasePrice(), 0);

        } catch (Exception e) {
            e.printStackTrace();
			Assertions.fail("ACRSMultiRoomAvailabilitySuccessTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }

    /**
     * Check Resort Pricing/Multi Availability if sold out for given dates.
     */
    @Test
	void getACRSMultiRoomSoldOutSuccessTest() {

        try {
			setMockAuthToken();
			setMockReferenceDataDAOHelper();
			setMockForMultiRoomAvail();

			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2021-03-06"),
					LocalDate.parse("2021-03-09"))
					.propertyIds(Collections.singletonList("ARIA1"))
					.propertyId("ARIA1")
					.programId("CATST")
					.promo("ZNVLCLS")
					.build();
			List<AuroraPriceResponse> response = roomPriceDAOStrategyACRSImpl.getResortPrices(auroraPriceRequest);

			Assertions.assertNotNull(response);
			Assertions.assertEquals(AvailabilityStatus.NOARRIVAL, response.get(3).getStatus());
			Assertions.assertEquals("ARIA1", response.get(3).getPropertyId());

        } catch (Exception e) {
            e.printStackTrace();
			Assertions.fail("ACRSMultiRoomSoldOutSuccessTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }
    
    /**
     * Check Resort Pricing/Multi Availability resort fee data.
     */
    @Test
	void getACRSMultiRoomResortFeeTest() {

        try {
			setMockAuthToken();
			setMockReferenceDataDAOHelper();
			setMockForMultiRoomAvail();

			AuroraPriceRequest auroraPriceRequest = makeBaseAuroraPriceRequest(LocalDate.parse("2021-03-06"),
					LocalDate.parse("2021-03-09"))
					.propertyIds(Collections.singletonList("ARIA1"))
					.propertyId("ARIA1")
					.programId("CATST")
					.promo("ZNVLCLS")
					.build();
            List<AuroraPriceResponse> response = roomPriceDAOStrategyACRSImpl.getResortPrices(auroraPriceRequest);

			Assertions.assertNotNull(response);
			Assertions.assertEquals(37.0, response.get(0).getResortFee(), 0);

        } catch (Exception e) {
            e.printStackTrace();
			Assertions.fail("ACRSMultiRoomResortFeeTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }
}
