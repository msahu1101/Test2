package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.CalculateRoomChargesRequest;
import com.mgm.services.booking.room.model.response.CalculateRoomChargesResponse;
/**
 * Service to calculate room and component charges.
 *  
 * @author uttam
 */
public interface RoomAndComponentChargesService {
    /**
     * calculate room and component charges for the given request.
     * 
     * @param roomReservationProgramRequest request.
     * @return RoomReservationChargesResponse response.
     */
    CalculateRoomChargesResponse calculateRoomAndComponentCharges(
            CalculateRoomChargesRequest calculateRoomChargesRequest);
}
