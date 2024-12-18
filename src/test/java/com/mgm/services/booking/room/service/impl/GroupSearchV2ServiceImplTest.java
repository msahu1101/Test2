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

import com.mgm.services.booking.room.dao.GroupSearchDAO;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;

/**
 * Unit test class to validate the publishing of event
 * 
 * @author priyanka
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupSearchV2ServiceImplTest {

	@InjectMocks
	GroupSearchV2ServiceImpl groupSearchV2ServiceImpl;

	@Mock
	GroupSearchDAO groupSearchDAO;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void searchGroupTest()
	{
		when( groupSearchDAO.searchGroup(Mockito.any())).thenReturn(getGroupSearchV2ResponseList());
		List<GroupSearchV2Response> list=groupSearchV2ServiceImpl.searchGroup(getGroupSearchV2Request());
		assertEquals(getGroupSearchV2ResponseList(),list);
	}

	private GroupSearchV2Request getGroupSearchV2Request() {
		GroupSearchV2Request groupSearchV2Request=new GroupSearchV2Request();
		groupSearchV2Request.setCustomerId(123456);
		groupSearchV2Request.setMlifeNumber("abc123");
		groupSearchV2Request.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		groupSearchV2Request.setPerpetualPricing(true);
		return groupSearchV2Request;
	}

	List<GroupSearchV2Response> getGroupSearchV2ResponseList()
	{
		List<GroupSearchV2Response> groupSearchV2ResponseList=new ArrayList<>();
		GroupSearchV2Response groupSearchV2Response=new GroupSearchV2Response();
		groupSearchV2Response.setActiveFlag(true);
		groupSearchV2Response.setFriday(true);
		groupSearchV2Response.setPropertyId("66964e2b-2550-4476-84c3-1a4c0c5c067f");
		groupSearchV2ResponseList.add(groupSearchV2Response);
		return groupSearchV2ResponseList;
	}

}