package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.mgm.services.booking.room.dao.MyVegasDAO;
import com.mgm.services.booking.room.dao.impl.RestMyVegasDAOImpl;
import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.booking.room.model.response.MyVegasResponse;
import com.mgm.services.booking.room.service.helper.MyVegasServiceHelper;
import com.mgm.services.common.model.RedemptionValidationResponse;
import com.mgm.services.common.model.RedemptionValidationResponse.DatesRedemptionIsUnAvailable;

/**
 * Unit test class to validate the MyVegas APIs
 * 
 * @author vararora
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MyVegasServiceImplTest {

    @InjectMocks
    private static MyVegasServiceImpl myVegasService;

    @InjectMocks
    @Spy
    private MyVegasServiceHelper myVegasServiceHelper;

    @Mock
    private RestMyVegasDAOImpl restMyVegasDAO;

    private static final String SAMPLE_REDEMPTION_CODE = "A1A1A";
    private static final String ROOMS_REWARD_TYPE = "Rooms";
    private static final String RESERVED_STATUS = "Reserved";

    @Before
    public void setup() {
        Map<String, MyVegasDAO> myVegasDAOMap = new HashMap<>();
        myVegasDAOMap.put("RestMyVegasDAOImpl", restMyVegasDAO);
        ReflectionTestUtils.setField(myVegasService, "myVegasServiceHelper", myVegasServiceHelper);
        ReflectionTestUtils.setField(myVegasService, "myVegasDAOMap", myVegasDAOMap);
    }

    @Test
    public void validateRedemptionCodeV2_shouldReturnResponse() {
        ReflectionTestUtils.setField(myVegasService, "isRestClientEnabled", true);
        MyVegasRequest myVegasRequest = new MyVegasRequest();
        myVegasRequest.setRedemptionCode(SAMPLE_REDEMPTION_CODE);
        myVegasRequest.setSkipCache(false);
        myVegasRequest.setCustomerId(0);
        when(restMyVegasDAO.validateRedemptionCode(myVegasRequest)).thenReturn(getSampleRedemptionValidationResponse());
        MyVegasResponse resp = myVegasService.validateRedemptionCodeV2(myVegasRequest, "");
        assertNotNull(resp);
        assertEquals(resp.getRedemptionCode(), SAMPLE_REDEMPTION_CODE);
        assertEquals(resp.getRewardType(), ROOMS_REWARD_TYPE);
        assertEquals(resp.getStatus(), RESERVED_STATUS);
    }

    private RedemptionValidationResponse getSampleRedemptionValidationResponse() {
        RedemptionValidationResponse redemptionValidationResponse = new RedemptionValidationResponse();
        redemptionValidationResponse.setStatus(RESERVED_STATUS);
        redemptionValidationResponse.setRedemptionCode(SAMPLE_REDEMPTION_CODE);
        redemptionValidationResponse
                .setCouponCode("66964e2b-2550-4476-84c3-1a4c0c5c067f_d540ca3a-9047-4bf3-ad95-b14aa270d5d5");
        redemptionValidationResponse.setRewardType(ROOMS_REWARD_TYPE);
        List<DatesRedemptionIsUnAvailable> datesRedemptionIsUnAvailable = new ArrayList<DatesRedemptionIsUnAvailable>();
        redemptionValidationResponse.setDatesRedemptionIsUnAvailable(datesRedemptionIsUnAvailable);
        return redemptionValidationResponse;
    }

}
