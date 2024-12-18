/**
 * 
 */
package com.mgm.services.booking.room.dao.impl;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.model.Email;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

/**
 * Test class to provide unit test cases for EmailDAONSImpl
 * 
 * @author vararora
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailDAONSImplTest {
    
    @Mock
    private static RestTemplate client;

    @Mock
    private IDMSTokenDAO idmsTokenDAO;

    @InjectMocks
    private static DomainProperties domainProperties;

    @InjectMocks
    private static ApplicationProperties applicationProperties;

    @InjectMocks
    private static RestTemplateBuilder restTemplateBuilder;

    @InjectMocks
    private static URLProperties urlProperties;

    @InjectMocks
    private static EmailDAONSImpl emailDAONSImpl;

    @BeforeClass
    public static void init() {
        client = Mockito.mock(RestTemplate.class);
        domainProperties = new DomainProperties();
        domainProperties.setNs("https://azdeapi-dev.mgmresorts.com/ns/{nsEnvironment}");
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
        applicationProperties = new ApplicationProperties();
        urlProperties = new URLProperties();
        urlProperties.setNotificationService("/v1/emails/generic");

        when(restTemplateBuilder.build()).thenReturn(client);
        emailDAONSImpl = new EmailDAONSImpl(restTemplateBuilder, domainProperties, urlProperties,
                applicationProperties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    public void testSendEmail() {
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("accessToken");
        when(idmsTokenDAO.generateToken()).thenReturn(tokenResponse);
        when(client.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), Mockito.<Class<String>> any(),
                Mockito.anyMap())).thenReturn(new ResponseEntity<String>("response", HttpStatus.OK));
        try {
            emailDAONSImpl.sendEmail(new Email());
        } catch (Exception e) {
            fail("The updateCustomerItinerary method should not have thrown an exception");
        }

        verify(client, times(1)).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                Mockito.<Class<String>> any(), Mockito.anyMap());
    }

}
