package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.loyalty.PlayerPromotionResponse;
import com.mgm.services.booking.room.model.loyalty.UpdatePlayerPromotionResponse;

import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.util.CommonUtil;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;


import com.mgm.services.booking.room.model.loyalty.UpdatedPromotion;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;

import lombok.extern.log4j.Log4j2;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Log4j2
@RunWith(MockitoJUnitRunner.Silent.class)
public class LoyaltyDaoImplTest {

	@InjectMocks
	private static DomainProperties domainProperties;

    @InjectMocks
    private static URLProperties urlProperties;

	@InjectMocks
	private static ApplicationProperties applicationProperties;
	
	@InjectMocks
	private static LoyaltyDaoImpl loyaltyDaoImpl;

    @Mock
    private static RestTemplate client;
	@InjectMocks
	private static RestTemplateBuilder restTemplateBuilder;
	@Mock
	private IDMSTokenDAO idmsTokenDAO;

	@BeforeClass
	public static void init() {

		client = Mockito.mock(RestTemplate.class);
		domainProperties = new DomainProperties();
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
		domainProperties.setCrs("https://cfts.hospitality.api.amadeus.com");
		domainProperties.setCrsUcpRetrieveResv("");
        domainProperties.setCvs("test");
		domainProperties.setLoyalty("test");
		urlProperties = new URLProperties();
        urlProperties.setPlayerPromos("test");
        urlProperties.setPatronPromos("test");

		applicationProperties = Mockito.mock(ApplicationProperties.class);
		applicationProperties = new ApplicationProperties();
		applicationProperties.setCrsUcpRetrieveResvEnvironment("test");
		applicationProperties.setLoyaltyConnectionTimeoutInMilliSecs(20);
		applicationProperties.setLoyaltyReadTimeoutInMilliSecs(6);
		applicationProperties.setLoyaltyClientMaxConn(20);
		applicationProperties.setLoyaltyClientMaxConnPerRoute(2);
		applicationProperties.setLoyaltyGetPlayerPromoVersion("Promo123");
		applicationProperties.setSocketTimeOut(20);

		CommonUtil commonUtil = Mockito.spy(CommonUtil.class);
		when(commonUtil.getRetryableRestTemplate(restTemplateBuilder, applicationProperties.isSslInsecure(), true,
				applicationProperties.getLoyaltyClientMaxConnPerRoute(),
				applicationProperties.getLoyaltyClientMaxConn(),
				applicationProperties.getLoyaltyConnectionTimeoutInMilliSecs(),
				applicationProperties.getLoyaltyReadTimeoutInMilliSecs(),
				applicationProperties.getSocketTimeOut(),
				1,
				applicationProperties.getCommonRestTTL())).thenReturn(client);;

		try {
			loyaltyDaoImpl = new LoyaltyDaoImpl(restTemplateBuilder,domainProperties, urlProperties,applicationProperties);

		} catch (Exception e) {
			log.error(e.getMessage());
			log.error("Caused " + e.getCause());
		}
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@Test
	public void testGetPlayerPromos() {
		String mlifeNumber = "mlife123";
		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse.setAccessToken("accessToken");
		CustomerPromotion customerPromotion = new CustomerPromotion();
		customerPromotion.setPromoId("1234");
		customerPromotion.setStatus("Done");
		List<CustomerPromotion> promotionList = new ArrayList<>();
		promotionList.add(customerPromotion);
		PlayerPromotionResponse promotionResponse = new PlayerPromotionResponse();
		promotionResponse.setCustomerPromotions(promotionList);

		when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
		when(client.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(PlayerPromotionResponse.class),
				Mockito.anyMap())).thenReturn(new ResponseEntity<>(promotionResponse,HttpStatus.OK));

		try {
			loyaltyDaoImpl.getPlayerPromos(mlifeNumber);
		}  catch (Exception e) {
			fail("Caught unexpected Exception during GetCustomerValues Test.");
		}
	}

	@Test
	public void testUpdatePlayerPromo() {
		String mlifeNumber = "mlife123";
		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse.setAccessToken("accessToken");
		Collection<UpdatedPromotion> promos = new ArrayList<>();
		UpdatedPromotion updatedPromotion = new UpdatedPromotion();
		updatedPromotion.setPromoId(123);
		promos.add(updatedPromotion);
		UpdatePlayerPromotionResponse updatePlayerPromotionResponse = new UpdatePlayerPromotionResponse();
		updatePlayerPromotionResponse.setMessage("Success");
		when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
		when(client.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(UpdatePlayerPromotionResponse.class),
				Mockito.anyMap())).thenReturn(new ResponseEntity<>(updatePlayerPromotionResponse,HttpStatus.OK));
		try {
			loyaltyDaoImpl.updatePlayerPromo(promos);
		}  catch (Exception e) {
			fail("Caught unexpected Exception during GetCustomerValues Test.");
		}
	}
}
