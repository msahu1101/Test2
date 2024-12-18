package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.RoomReservationChargesRequest;
import com.mgm.services.booking.room.model.response.RoomReservationChargesResponse;
/**
 * Service to calculate reservation charges.
 *  
 * @author jayveera
 */
public interface RoomReservationChargesService {
    /**
     * calculate room reservation charges for the given request.
     * 
     * @param roomReservationProgramRequest request.
     * @return RoomReservationChargesResponse response.
     */
    RoomReservationChargesResponse calculateRoomReservationCharges(
            RoomReservationChargesRequest roomReservationProgramRequest);
}
