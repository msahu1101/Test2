package com.mgm.services.booking.room.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.mgm.services.booking.room.dao.IATADAO;
import com.mgm.services.booking.room.model.request.OrganizationSearchV2Request;
import com.mgm.services.booking.room.model.response.OrganizationSearchV2Response;
import com.mgm.services.common.exception.ValidationException;

/**
 * Unit test class to validate the publishing of event
 * 
 * @author priyanka
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class IATAServiceV2ImplTest {



	@InjectMocks
	IATAServiceV2Impl iATAServiceV2Impl;

	@Mock
	IATADAO iataDAO;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test(expected=ValidationException.class)
	public void validateCodeTest_validationException()
	{
		iATAServiceV2Impl.validateCode("");
	}

	@Test(expected=ValidationException.class)
	public void validateCodeTest__validationException_DB()
	{
		when(iataDAO.validateCode(Mockito.anyString())).thenReturn(false);
		iATAServiceV2Impl.validateCode("IATA456");
	}

	@Test
	public void validateCodeTest()
	{
		when(iataDAO.validateCode(Mockito.anyString())).thenReturn(true);
		iATAServiceV2Impl.validateCode("IATA456");
	}

	@Test
	public void organizationSearchTest()
	{
		when( iataDAO.organizationSearch(Mockito.any())).thenReturn(getOrganizationSearchV2ResponseList());
		List<OrganizationSearchV2Response> list=iATAServiceV2Impl.organizationSearch(getOrganizationSearchV2Request());
		assertEquals(getOrganizationSearchV2ResponseList(),list);
	}

	private OrganizationSearchV2Request getOrganizationSearchV2Request() {
		OrganizationSearchV2Request organizationSearchRequest=new OrganizationSearchV2Request();
		organizationSearchRequest.setCustomerId(123456);
		organizationSearchRequest.setMlifeNumber("abc123");
		organizationSearchRequest.setIataCode("IATA");
		organizationSearchRequest.setPerpetualPricing(true);
		return organizationSearchRequest;
	}

	private List<OrganizationSearchV2Response> getOrganizationSearchV2ResponseList()
	{
		List<OrganizationSearchV2Response> organizationSearchV2ResponseList=new ArrayList<>();
		OrganizationSearchV2Response organizationSearchV2Response=new OrganizationSearchV2Response();
		organizationSearchV2Response.setFullName("Michael");
		organizationSearchV2Response.setShortName("mich");
		organizationSearchV2ResponseList.add(organizationSearchV2Response);
		return organizationSearchV2ResponseList;
	}

}