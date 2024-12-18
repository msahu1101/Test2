package com.mgm.services.booking.room.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.springframework.web.client.RestTemplate;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;

public class BaseContentDAOImplTest {

    @Mock
    private URLProperties urlProperties;

    @Mock
    private DomainProperties domainProperties;

    @Mock
    private ApplicationProperties applicationProperties;

    @InjectMocks
    private BaseContentDAOImpl contentDAO;

    @BeforeEach
    public void setUp() {
    	applicationProperties = Mockito.mock(ApplicationProperties.class);
    	applicationProperties = new ApplicationProperties();
    	applicationProperties.setContentMaxConnectionPerDaoImpl(10);
    	applicationProperties.setConnectionPerRouteDaoImpl(20);
    	applicationProperties.setConnectionTimeoutContent(30);
    	applicationProperties.setReadTimeOutContent(5);
    	applicationProperties.setSocketTimeOutContent(10);
    	contentDAO = new BaseContentDAOImpl(urlProperties, domainProperties, applicationProperties);
    	contentDAO.setDomainProperties(domainProperties);
    	contentDAO.setApplicationProperties(applicationProperties);
        contentDAO = new BaseContentDAOImpl(urlProperties, domainProperties, applicationProperties);
    }

    @Test
    public void testGetClient() {
        RestTemplate restTemplate = contentDAO.getClient();
        assertEquals(restTemplate, contentDAO.getClient());
    }

    @Test
    public void testGetUrlProperties() {
        assertEquals(urlProperties, contentDAO.getUrlProperties());
    }

    @Test
    public void testGetDomainProperties() {
        assertEquals(domainProperties, contentDAO.getDomainProperties());
    }

    @Test
    public void testSetDomainProperties() {
        DomainProperties newDomainProperties = mock(DomainProperties.class);
        contentDAO.setDomainProperties(newDomainProperties);
        assertEquals(newDomainProperties, contentDAO.getDomainProperties());
    }

    @Test
    public void testGetApplicationProperties() {
        assertEquals(applicationProperties, contentDAO.getApplicationProperties());
    }

    @Test
    public void testSetApplicationProperties() {
        ApplicationProperties newAppProperties = mock(ApplicationProperties.class);
        contentDAO.setApplicationProperties(newAppProperties);
        assertEquals(newAppProperties, contentDAO.getApplicationProperties());
    }  
}
