package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.properties.AcrsProperties;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ComponentDAOImplTest extends BaseRoomBookingTest {

    @Mock
    private static ComponentDAOStrategyACRSImpl acrsStrategy ;

    @Mock
    private ComponentDAOStrategyGSEImpl gseStrategy;

    @Mock
    private ReferenceDataDAOHelper referenceDataDAOHelper;
    
    @InjectMocks
    private ComponentDAOImpl componentDAOImpl;
   
    static Logger logger = LoggerFactory.getLogger(ComponentDAOImplTest.class);

    /**
     * Check Room Component Availability Strategy
     */
    @Test
    public void getRoomComponentAvailabilityStrategyTest() {

        try {

            componentDAOImpl.acrsProperties = new AcrsProperties();

            RoomComponentRequest request = new RoomComponentRequest();
            request.setRoomTypeId("ROOMCD-v-DPRQ-d-PROP-v-MV021");
            request.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
            request.setTravelEndDate(new Date());
            request.setTravelStartDate(new Date());

            List<RoomRequest> roomRequestList = new ArrayList<>();
            RoomRequest roomRequest = new RoomRequest();
            roomRequest.setSelected(true);
            roomRequest.setNightlyCharge(true);
            roomRequest.setDescription("testDesc");
            roomRequest.setId("ROOMCD-v-DPRQ-d-PROP-v-MV021");
            roomRequest.setPrice(45);
            roomRequestList.add(roomRequest);

            when(acrsStrategy.getRoomComponentAvailability(ArgumentMatchers.any())).thenReturn(roomRequestList);
            when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
            List<RoomRequest> response = componentDAOImpl.getRoomComponentAvailability(request);

            Assert.assertNotNull(response);
        } catch (Exception e) {
            Assert.fail("getRoomComponentAvailabilityStrategyTest Failed");
            logger.error(e.getMessage());
            logger.error("Cause: " + e.getCause());
        }
    }
    
    @Test
    public void getRoomComponentByIdTest() {
    	try {
    		String componentId = "COMPONENTCD-v-PROCESSFEE-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-CCAUTHFEE";
    		String roomTypeId = "ROOMCD-v-DPRQ-d-PROP-v-MV021";
    		String propertyId ="dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
    		
            RoomRequest roomRequest = new RoomRequest();
            roomRequest.setSelected(true);
            roomRequest.setNightlyCharge(true);
            roomRequest.setDescription("testDesc");
            roomRequest.setId("ROOMCD-v-DPRQ-d-PROP-v-MV021");
            roomRequest.setPrice(50);
    		
    		RoomComponent roomComponent = new RoomComponent();
    		roomComponent.setActiveFlag(true);
    		roomComponent.setDescription("testDesc");
    		roomComponent.setId(componentId);
    		roomComponent.setPropertyId(propertyId);

    		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
    		
    		when(acrsStrategy.getRoomComponentById(componentId, roomTypeId)).thenReturn(roomRequest);
    		RoomRequest response = componentDAOImpl.getRoomComponentById(componentId, roomTypeId, propertyId);
    		Assert.assertNotNull(response);
    		
    		when(acrsStrategy.getRoomComponentById(componentId)).thenReturn(roomComponent);
    		RoomComponent res = componentDAOImpl.getRoomComponentById(componentId, propertyId);
    		Assert.assertNotNull(res);
    		
    	}catch (Exception e) {
    		Assert.fail("getRoomComponentByIdTest Failed");
    		logger.error(e.getMessage());
    		logger.error("Cause: " + e.getCause());
    	}
    	
    }
    
    @Test 
    public void getRoomComponentByCodeTest() {
    	try {
    		String roomTypeId = "ROOMCD-v-DPRQ-d-PROP-v-MV021";
    		String propertyId ="dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad";
    		String code = "CCAUTHFEE";
    		String ratePlanId = "RPCD-v-ELLIOTTFBPT055-d-PROP-v-MV021";
    		String mlifeNumber = "33334444";
    		String source = "ICE";
    		Date checkInDate = null;  
    		Date checkOutDate = null;  

    		RoomComponent roomComponent = new RoomComponent();
    		roomComponent.setActiveFlag(true);
    		roomComponent.setDescription("testDesc");
    		roomComponent.setId("COMPONENTCD-v-PROCESSFEE-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-CCAUTHFEE");
    		roomComponent.setPropertyId(propertyId);
    		
    		when(referenceDataDAOHelper.isPropertyManagedByAcrs(ArgumentMatchers.any())).thenReturn(true);
    		when(acrsStrategy.getRoomComponentByCode(propertyId, code, roomTypeId,
                    ratePlanId, checkInDate, checkOutDate, mlifeNumber, source)).thenReturn(new RoomComponent());
    		
    		RoomComponent response = componentDAOImpl.getRoomComponentByCode(propertyId, code, roomTypeId,
                    ratePlanId, checkInDate, checkOutDate, mlifeNumber, source);

    		Assert.assertNotNull(response);
    		
    	}catch (Exception e) {
    		Assert.fail("getRoomComponentByCodeTest Failed");
    		logger.error(e.getMessage());
    		logger.error("Cause: " + e.getCause());
    	}
    }
    
}
