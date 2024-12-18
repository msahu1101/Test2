package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.ReleaseV2Request;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.AcrsProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CancelReservationDAOImplTest extends BaseRoomBookingTest{
    private CancelReservationDAOStrategyACRSImpl acrsStrategy;
    private CancelReservationDAOStrategyGSEImpl gseStrategy;
    private ReferenceDataDAOHelper referenceDataDAOHelper;
    private AcrsProperties acrsProperties;
    private FindReservationDAO findReservationDAO;

    //@InjectMocks
    private CancelReservationDAOImpl cancelReservationDAO;

    @BeforeAll
    static void setUpBeforeClass() {
        runOnceBeforeClass();
    }

    @BeforeEach
    void init() {
        acrsStrategy = mock(CancelReservationDAOStrategyACRSImpl.class);
        gseStrategy = mock(CancelReservationDAOStrategyGSEImpl.class);
        referenceDataDAOHelper = mock(ReferenceDataDAOHelper.class);
        findReservationDAO = mock(FindReservationDAO.class);
        acrsProperties = new AcrsProperties();

        cancelReservationDAO = new CancelReservationDAOImpl(acrsStrategy, gseStrategy, findReservationDAO,
                acrsProperties, referenceDataDAOHelper);
    }

    @Test
    void cancelReservation_shouldUseAcrsStrategy_whenPropertyManagedByAcrs() {
    	String propertyId = "acrsManagedPropertyId";
    	
    	
        // Arrange
        CancelV2Request cancelV2Request = new CancelV2Request();
        cancelV2Request.setConfirmationNumber("12345");
        cancelV2Request.setPropertyId("acrsManagedPropertyId");
        cancelV2Request.setF1Package(true);
        
        when(cancelReservationDAO.isPropertyManagedByAcrs(propertyId)).thenReturn(true);
        when(acrsStrategy.cancelReservation(cancelV2Request)).thenReturn(new RoomReservation());

        // Act
        RoomReservation result = cancelReservationDAO.cancelReservation(cancelV2Request);

        // Assert
        assertEquals(RoomReservation.class, result.getClass());
    }

    @Test
    void cancelReservation_shouldUseGseStrategy_whenPropertyNotManagedByAcrs() {  	
    	String propertyId = "gseManagedPropertyId";
    	
        // Arrange
        CancelV2Request cancelV2Request = new CancelV2Request();
        cancelV2Request.setConfirmationNumber("12345");
        cancelV2Request.setF1Package(true);
        cancelV2Request.setPropertyId("gseManagedPropertyId");
        
        when(cancelReservationDAO.isPropertyManagedByAcrs(propertyId)).thenReturn(false);
        when(gseStrategy.cancelReservation(cancelV2Request)).thenReturn(new RoomReservation());

        // Act
        RoomReservation result = cancelReservationDAO.cancelReservation(cancelV2Request);

        // Assert
        assertEquals(RoomReservation.class, result.getClass());
    }

    @Test
    void ignoreReservation_withNonAcrsManagedProperty_shouldCallGseStrategy() {
    	
    	String propertyId = "12345";
     
        ReleaseV2Request request = new ReleaseV2Request();
        request.setConfirmationNumber("12345");
        request.setPropertyId("12345");
        request.setF1Package(true);
        
        when(cancelReservationDAO.isPropertyManagedByAcrs(propertyId)).thenReturn(false); 
        when(gseStrategy.ignoreReservation(request)).thenReturn(true);
        
        boolean result = cancelReservationDAO.ignoreReservation(request);
        
        assertTrue(result); // This assertion will fail if the result is false
        verify(gseStrategy, times(1)).ignoreReservation(request);
        verify(acrsStrategy, never()).ignoreReservation(request);   
    }
    
    @Test
    void ignoreReservation_withAcrsManagedProperty_shouldCallAcrsStrategy() {
    	String propertyId = "12345";
  
        ReleaseV2Request request = new ReleaseV2Request();
        request.setConfirmationNumber("12345");
        request.setPropertyId("12345");
        request.setF1Package(true);
        
        when(cancelReservationDAO.isPropertyManagedByAcrs(propertyId)).thenReturn(true);
        when(acrsStrategy.ignoreReservation(request)).thenReturn(true);
        
        boolean result = cancelReservationDAO.ignoreReservation(request);

        assertTrue(result);
        verify(acrsStrategy, times(1)).ignoreReservation(request);
        verify(gseStrategy, never()).ignoreReservation(request);
    }
}
