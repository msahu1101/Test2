package com.mgm.services.booking.room.dao.impl;

import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;

@RunWith(MockitoJUnitRunner.class)
public class GroupSearchDAOImplTest extends BaseRoomBookingTest {

	@Mock
	private static GroupSearchDAOStrategyACRSImpl acrsStrategy;

	@Mock
	private ReferenceDataDAOHelper referenceDataDAOHelper;

	@InjectMocks
	private GroupSearchDAOImpl groupSearchDAOImpl  = new GroupSearchDAOImpl();

	static Logger logger = LoggerFactory.getLogger(GroupSearchDAOImplTest.class);

	private List<GroupSearchV2Response> groupSearch() {
		File file = new File(getClass().getResource("/groupblock-search-res.json").getPath());
		List<GroupSearchV2Response> data = convert(file, mapper.getTypeFactory().constructCollectionType(List.class, GroupSearchV2Response.class));
		return data;
	}

	@Test
	public void groupSearchTest() {
		try {

			GroupSearchV2Request groupSearchReq = new GroupSearchV2Request();
			groupSearchReq.setChannel("ice");
			groupSearchReq.setSource("ice");
			groupSearchReq.setStartDate("2024-03-07");
			groupSearchReq.setEndDate("2024-03-08");
			groupSearchReq.setPropertyId("e5d3f1c9-833a-83f1-e053-d303fe0ad83c");
			
			when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
			when(acrsStrategy.searchGroup(ArgumentMatchers.any())).thenReturn(groupSearch());
			List<GroupSearchV2Response> response = groupSearchDAOImpl.searchGroup(groupSearchReq);

			Assert.assertNotNull(response);
			Assert.assertEquals("GRPCD-v-CASINO2024-d-PROP-v-MV021", response.get(0).getId());
			Assert.assertEquals("CASINO2024", response.get(0).getGroupCode());
			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("groupSearchTest Failed");
			System.err.println(e.getMessage());
			logger.error(e.getMessage());
			logger.error("Cause: " + e.getCause());
		}
	}
}
