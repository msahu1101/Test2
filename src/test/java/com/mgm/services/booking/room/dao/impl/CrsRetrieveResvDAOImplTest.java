
package com.mgm.services.booking.room.dao.impl;

import static org.mockito.Mockito.when;

import com.mgm.services.booking.room.BaseAcrsRoomBookingTest;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRetrieveResReservation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.PaymentDAO;
import com.mgm.services.booking.room.model.request.FindReservationRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.util.CommonUtil;

public class CrsRetrieveResvDAOImplTest extends BaseAcrsRoomBookingTest {

	@Mock
	private static RestTemplate client;

	@Mock
	private static DomainProperties domainProperties;

	@Mock
	private static RestTemplateBuilder restTemplateBuilder;

	@Mock
	private static AcrsProperties acrsProperties;

	@Mock
	private static IDMSTokenDAO idmsTokenDAO;
	
	@Mock
    private static PaymentDAO paymentDao;

	private static RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;

	@InjectMocks
	private static FindReservationDAOStrategyACRSImpl crsRetrieveResvDAOImpl;

	private void setMockForFindSuccess() {

		when(client.exchange(ArgumentMatchers.contains("reservations/MGM"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>>any(),
				Mockito.anyMap())).thenReturn(getCrsRetrieveResv());
		when(client.exchange(ArgumentMatchers.contains("reservation/retrieve"), ArgumentMatchers.any(HttpMethod.class),
				ArgumentMatchers.any(), ArgumentMatchers.<Class<ReservationRetrieveResReservation>>any(),
				Mockito.anyMap())).thenReturn(getCrsRetrieveResv());

	}
	
    private void setMockReferenceDataDAOHelper() {
		setMockForRoomPropertyCode("ACRS", "ACRS", "ACRS", "ACRS", "ICECC");
    }

	@BeforeEach
	public void init() {
		super.init();
		client = Mockito.mock(RestTemplate.class);
		domainProperties = new DomainProperties();
		domainProperties.setCrs("");
		restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		roomPriceDAOStrategyACRSImpl = Mockito.mock(RoomPriceDAOStrategyACRSImpl.class);
		acrsProperties = new AcrsProperties();
		paymentDao = Mockito.mock(PaymentDAO.class);
		urlProperties.setAcrsRetrieveReservation("reservations/MGM");
		acrsProperties.setLiveCRS(true);
		idmsTokenDAO = Mockito.mock(IDMSTokenDAO.class);
		domainProperties.setCrsUcpRetrieveResv("");
		applicationProperties.setCrsUcpRetrieveResvEnvironment("test");
		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(),
				acrsProperties.isLiveCRS(),applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
				applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
				applicationProperties.getConnectionTimeout(),
				applicationProperties.getReadTimeOut(),
				applicationProperties.getSocketTimeOut(),1, applicationProperties.getCrsRestTTL())).thenReturn(client);
		urlProperties.setCrsUcpRetrieveResvUrl("v1/crs/reservation/retrieve?confirmationNumber={confirmationNumber}");
		try {
			crsRetrieveResvDAOImpl = new FindReservationDAOStrategyACRSImpl(urlProperties, domainProperties,
					applicationProperties, acrsProperties, restTemplateBuilder, referenceDataDAOHelper,
					acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);
			crsRetrieveResvDAOImpl.setIdmsTokenDAO(idmsTokenDAO);
			crsRetrieveResvDAOImpl.setPaymentDao(paymentDao);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		crsRetrieveResvDAOImpl.client = client;
	}

	// Test Retrieve room reservation when confirmation number is correct.
	@SuppressWarnings("unchecked")
	@Test
	public void CrsRetrieveResvPositiveTest() {
		setMockAuthToken();
		setMockReferenceDataDAOHelper();
		FindReservationRequest request = new FindReservationRequest();
		request.setSource("mgmresorts");
		request.setConfirmationNumber("1271274619");
		request.setFirstName("Test");
		request.setLastName("Test");
		ReservationRetrieveResReservation reservationPendingRes = getCrsRetrieveResv().getBody();
        Mockito.doReturn(reservationPendingRes).when(paymentDao)
        .sendRetrieveRequestToPaymentExchangeToken(ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any(),ArgumentMatchers.any());
   
		//setMockForFindSuccess();
		try {
			TokenResponse tknRes = new TokenResponse();
			tknRes.setAccessToken("1234");
			Mockito.doReturn(tknRes).when(idmsTokenDAO).generateToken();
			RoomReservation roomReservation = crsRetrieveResvDAOImpl.findRoomReservation(request);
			Assertions.assertNotNull(roomReservation);
			Assertions.assertEquals(request.getConfirmationNumber(), roomReservation.getConfirmationNumber());

		} catch (Exception ex) {
			ex.printStackTrace();
			Assertions.fail("CrsRetrieveResvPositiveTest Failed");
		}
	}

}
