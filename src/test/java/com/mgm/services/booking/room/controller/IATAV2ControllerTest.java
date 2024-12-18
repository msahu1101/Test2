package com.mgm.services.booking.room.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.validator.HibernateValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.request.OrganizationSearchV2Request;
import com.mgm.services.booking.room.model.request.RatePlanSearchV2Request;
import com.mgm.services.booking.room.model.response.OrganizationSearchV2Response;
import com.mgm.services.booking.room.model.response.RatePlanSearchV2Response;
import com.mgm.services.booking.room.service.IATAV2Service;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;
import com.mgm.services.common.model.ServicesSession;

/**
 * Unit test class to validate the request object with various param.
 * 
 * @author vararora
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class IATAV2ControllerTest extends BaseRoomBookingTest {

	@InjectMocks
	private IATAV2Controller iATAV2Controller;

	@Mock
	private IATAV2Service iataService;

	@Mock
	private TokenValidator tokenValidator;

	private LocalValidatorFactoryBean localValidatorFactory;

	@Mock
	protected ServicesSession sSession;

	@Before
	public void setUp() {
		localValidatorFactory = new LocalValidatorFactoryBean();
		localValidatorFactory.setProviderClass(HibernateValidator.class);
		localValidatorFactory.afterPropertiesSet();
	}

	@After
	public void tearDown() {
		localValidatorFactory = null;
	}

	@Test
	public void searchRatePlans_invalidOrganizationSearchRequest_validateErrorMessage() {
		OrganizationSearchV2Request request = new OrganizationSearchV2Request();
		organizationSearchTest(request, ErrorCode.INVALID_ORGANIZATION_SEARCH_REQUEST);
	}

	/*@Test
	public void searchRatePlans_invalidDataCode_validateErrorMessage() {
		OrganizationSearchV2Request request = new OrganizationSearchV2Request();
		request.setIataCode("123456");
		organizationSearchTest(request, ErrorCode.INVALID_IATA_CODE);
	}*/

	private void organizationSearchTest(OrganizationSearchV2Request request, ErrorCode errorCode) {
		BindingResult result = new DirectFieldBindingResult(request, "OrganizationSearchV2Request");
		boolean validationExceptionOccured = false;
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);

		try {
			localValidatorFactory.validate(request, result);
			iATAV2Controller.organizationSearch(TestConstant.ICE, request, mockRequest);
		} catch (ValidationException e) {
			validationExceptionOccured = true;
			assertEquals(errorCode.getErrorCode(), e.getErrorCodes().get(0));
		} finally {
			assertTrue("Controller should throw ValidationException", validationExceptionOccured);
		}
	}

	@Test
	public void searchRatePlansTestPositive() {
		OrganizationSearchV2Request request = new OrganizationSearchV2Request();
		request.setIataCode("12345678");
		request.setOrgName("Test");
		HttpServletRequest mockRequest = mock(HttpServletRequest.class);
		BindingResult result = new DirectFieldBindingResult(request, "OrganizationSearchV2Request");
		localValidatorFactory.validate(request, result);
		List<OrganizationSearchV2Response> organizationSearchV2ResponseList = new ArrayList<>();
		OrganizationSearchV2Response organizationSearchV2Response = new OrganizationSearchV2Response();
		organizationSearchV2Response.setFullName("Test");
		organizationSearchV2ResponseList.add(organizationSearchV2Response);
		Mockito.when(iataService.organizationSearch(request)).thenReturn(organizationSearchV2ResponseList);
		List<OrganizationSearchV2Response> response = iATAV2Controller.organizationSearch(TestConstant.ICE, request,
				mockRequest);
		assertEquals(response, organizationSearchV2ResponseList);
	}
	
	@Test(expected=ValidationException.class)
	public void validateCodeTest() {
		iATAV2Controller.validateCode(null);
	}
	
	@Test
	public void validateCodeTest_positive() {
		iATAV2Controller.validateCode("123456");
	}
}
