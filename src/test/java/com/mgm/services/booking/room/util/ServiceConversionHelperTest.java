package com.mgm.services.booking.room.util;

import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.ReservationSystemType;
import com.mgm.services.booking.room.model.phoenix.Room;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.service.cache.RoomCacheService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceConversionHelperTest {

    @Mock
    private RoomProgramCacheService programCache;

    @Mock
    private AcrsProperties acrsProperties;

    @Mock
    private RoomCacheService roomCache;

    @Mock
    private ReferenceDataDAOHelper refDataHelper;

    @InjectMocks
    private ServiceConversionHelper serviceConversionHelper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void convertGuids() {

        final RoomReservationRequest reservation = new RoomReservationRequest();
        reservation.setProgramId("aef64735-d34f-4daa-afd3-bcab36a318f0");
        reservation.setPropertyId("def64735-d34f-4daa-afd3-bcab36a318f0");
        reservation.setRoomTypeId("cef65735-d34f-4daa-afd3-bcab36a318f0");

        final List<RoomPrice> bookings = new ArrayList<>();
        final RoomPrice booking = new RoomPrice();
        booking.setProgramId("bef65735-d34f-4daa-afd3-bcab36a318f0");
        booking.setOverrideProgramId("aef75735-d34f-4daa-afd3-bcab36a318f0");
        bookings.add(booking);
        reservation.setBookings(bookings);

        final RoomProgram program = new RoomProgram();
        program.setPromoCode("PREVL");

        final Room room = new Room();
        room.setOperaRoomCode("DLUX");

        when(refDataHelper.retrieveAcrsPropertyID(Mockito.anyString())).thenReturn("MV012");
        when(programCache.getRoomProgram(Mockito.anyString())).thenReturn(program);
        when(refDataHelper.isPropertyManagedByAcrs(Mockito.anyString())).thenReturn(true);
        when(roomCache.getRoom(Mockito.anyString())).thenReturn(room);

        // Reservation Create
        serviceConversionHelper.convertGuids(reservation);

        assertTrue(reservation.getProgramId().contains(program.getPromoCode()));
        assertTrue(reservation.getBookings().get(0).getProgramId().contains(program.getPromoCode()));
        assertTrue(reservation.getBookings().get(0).getOverrideProgramId().contains(program.getPromoCode()));
        assertTrue(reservation.getRoomTypeId().contains(room.getOperaRoomCode()));


        // Reservation Hold
        final CalculateRoomChargesRequest holdRequest = new CalculateRoomChargesRequest();
        holdRequest.setProgramId("aef64735-d34f-4daa-afd3-bcab36a318f0");
        holdRequest.setPropertyId("def64735-d34f-4daa-afd3-bcab36a318f0");
        holdRequest.setRoomTypeId("cef64735-d34f-4daa-afd3-bcab36a318f0");

        serviceConversionHelper.convertGuids(holdRequest);

        assertTrue(holdRequest.getProgramId().contains(program.getPromoCode()));
        assertTrue(holdRequest.getRoomTypeId().contains(room.getOperaRoomCode()));

        // Program Validate
        final RoomProgramValidateRequest validateRequest = new RoomProgramValidateRequest();
        validateRequest.setProgramId("aef64735-d34f-4daa-afd3-bcab36a318f0");
        validateRequest.setPropertyId("def64735-d34f-4daa-afd3-bcab36a318f0");

        serviceConversionHelper.convertGuids(validateRequest);

        assertTrue(validateRequest.getProgramId().contains(program.getPromoCode()));


        // Pricing - Trip
        final RoomAvailabilityV3Request tripRequest = new RoomAvailabilityV3Request();
        tripRequest.setProgramId("aef64735-d34f-4daa-afd3-bcab36a318f0");
        tripRequest.setPropertyId("def64735-d34f-4daa-afd3-bcab36a318f0");

        serviceConversionHelper.convertGuids(tripRequest);

        assertTrue(tripRequest.getProgramId().contains(program.getPromoCode()));


        // Pricing - Calendar
        final CalendarPriceV3Request calendarRequest = new CalendarPriceV3Request();
        calendarRequest.setProgramId("def64735-d34f-4daa-afd3-bcab36a318f0");
        calendarRequest.setPropertyId("def64735-d34f-4daa-afd3-bcab36a318f0");
        final List<String> allRooms = new ArrayList<>();
        allRooms.add("def64735-d34f-4daa-afd3-bcab36a318f0");
        calendarRequest.setRoomTypeIds(allRooms);

        serviceConversionHelper.convertGuids(calendarRequest);

        assertTrue(calendarRequest.getProgramId().contains(program.getPromoCode()));
        assertTrue(calendarRequest.getRoomTypeIds().get(0).contains(room.getOperaRoomCode()));


    }

}
