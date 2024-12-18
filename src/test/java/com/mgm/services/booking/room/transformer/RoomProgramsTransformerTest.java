package com.mgm.services.booking.room.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.request.dto.RoomProgramDTO;
import com.mgm.services.booking.room.model.response.CustomerOffer;
import com.mgm.services.booking.room.model.response.CustomerOfferType;

public class RoomProgramsTransformerTest extends BaseRoomBookingTest {

    @Test
    public void testBuildCustomerOffersResponse_whenNewSegmentsDisabled_expectSimpleTransformation() {
        
        List<RoomProgramDTO> programsList = new ArrayList<>();
        RoomProgramDTO program1 = new RoomProgramDTO("program 1", "MV001", "ZAAART", null);
        RoomProgramDTO program2 = new RoomProgramDTO("program 2", "", "ZAAART", null);
        programsList.add(program1);
        programsList.add(program2);
        
        List<CustomerOffer> customerOffers = RoomProgramsTransformer.buildCustomerOffersResponse(programsList, "false",null);
        
        assertEquals(2, customerOffers.size());
        assertTrue(customerOffers.stream().anyMatch(o -> o.getId().equals("program 1") && o.getType().equals(CustomerOfferType.PROGRAM)));
        assertTrue(customerOffers.stream().anyMatch(o -> o.getId().equals("program 2") && o.getType().equals(CustomerOfferType.SEGMENT)));
    }
    
    @Test
    public void testBuildCustomerOffersResponse_whenNewSegmentsEnabledAndMatchingRatePlanCodes_expectSegments() {
        
        List<RoomProgramDTO> programsList = new ArrayList<>();
        RoomProgramDTO program1 = new RoomProgramDTO("program 1", "MV001", "ZAAART", null);
        RoomProgramDTO program2 = new RoomProgramDTO("program 2", "MV002", "ZAAART", null);
        programsList.add(program1);
        programsList.add(program2);
        
        List<CustomerOffer> customerOffers = RoomProgramsTransformer.buildCustomerOffersResponse(programsList, "true",null);
        
        assertEquals(1, customerOffers.size());
        assertTrue(customerOffers.stream().anyMatch(o -> o.getId().equals("ZAAART") && o.getType().equals(CustomerOfferType.SEGMENT)));
    }
    
    @Test
    public void testBuildCustomerOffersResponse_whenNewSegmentsEnabledAndNonMatchingRatePlanCodes_expectIndividualPrograms() {
        
        List<RoomProgramDTO> programsList = new ArrayList<>();
        RoomProgramDTO program1 = new RoomProgramDTO("program 1", "MV001", "ZAAART", null);
        RoomProgramDTO program2 = new RoomProgramDTO("program 2", "MV002", "TMLIFE", null);
        programsList.add(program1);
        programsList.add(program2);
        
        List<CustomerOffer> customerOffers = RoomProgramsTransformer.buildCustomerOffersResponse(programsList, "true",null);
        
        assertEquals(2, customerOffers.size());
        assertTrue(customerOffers.stream().anyMatch(o -> o.getId().equals("program 1") && o.getType().equals(CustomerOfferType.PROGRAM)));
        assertTrue(customerOffers.stream().anyMatch(o -> o.getId().equals("program 2") && o.getType().equals(CustomerOfferType.PROGRAM)));
    }
    
    @Test
    public void testBuildCustomerOffersResponseWithSorting_expectProgramsAreSortedCorrectly() {
        
        List<RoomProgramDTO> programsList = new ArrayList<>();
        RoomProgramDTO program1 = new RoomProgramDTO("program 1", "MV001", "ZAAART", null);
        RoomProgramDTO program2 = new RoomProgramDTO("program 2", "MV002", "TMLIFE", null);
        RoomProgramDTO program3 = new RoomProgramDTO("program 3", "MV003", "TMLIFE", null);
        RoomProgramDTO program4 = new RoomProgramDTO("program 4", "MV004", "TMLIFE", null);
        RoomProgramDTO program5 = new RoomProgramDTO("program 5", "MV001", "ZAAART", null);
        programsList.add(program1);
        programsList.add(program2);
        programsList.add(program3);
        programsList.add(program4);
        programsList.add(program5);
        
        List<Property> properties = new ArrayList<>();
        Property property1 = new Property("MV001", null, null, null, null, "10");
        Property property2 = new Property("MV002", null, null, null, null, null);
        Property property3 = new Property("MV003", null, null, null, null, "5");
        Property property4 = new Property("MV004", null, null, null, null, "1");
        properties.add(property1);
        properties.add(property2);
        properties.add(property3);
        properties.add(property4);
        
        List<CustomerOffer> customerOffers = RoomProgramsTransformer.buildCustomerOffersResponseWithSorting(programsList, properties);
        assertEquals(5, customerOffers.size());
        assertTrue(customerOffers.get(0).getId().equals(program4.getProgramId()));
        assertTrue(customerOffers.get(1).getId().equals(program3.getProgramId()));
        assertTrue(customerOffers.get(2).getId().equals(program1.getProgramId()));
        assertTrue(customerOffers.get(3).getId().equals(program5.getProgramId()));
        assertTrue(customerOffers.get(4).getId().equals(program2.getProgramId()));
    }
    
    @Test
    public void testFilterByRegion_expectProgramsAreFilteredCorrectly() {
        
        List<CustomerOffer> programsList = new ArrayList<>();
        CustomerOffer program1 = new CustomerOffer("program 1", "MV001", null, CustomerOfferType.PROGRAM, false, null, null);
        CustomerOffer program2 = new CustomerOffer("program 2", "MV002", null, CustomerOfferType.PROGRAM, false, null, null);
        CustomerOffer program3 = new CustomerOffer("program 3", null, null, CustomerOfferType.SEGMENT, false, null, null);
        CustomerOffer program4 = new CustomerOffer("program 4", "MV004", null, CustomerOfferType.PROGRAM, false, null, null);
        programsList.add(program1);
        programsList.add(program2);
        programsList.add(program3);
        programsList.add(program4);
        
        List<Property> properties = new ArrayList<>();
        Property property1 = new Property("MV001", null, null, null, "LV", "10");
        Property property2 = new Property("MV002", null, null, null, "LV", null);
        properties.add(property1);
        properties.add(property2);
        
        List<CustomerOffer> customerOffers = RoomProgramsTransformer.filterByRegion(programsList, properties);
        assertEquals(3, customerOffers.size());
        assertEquals(customerOffers.get(0).getId(), program1.getId());
        assertEquals(customerOffers.get(1).getId(), program2.getId());
        assertEquals(customerOffers.get(2).getId(), program3.getId());
    }
}
