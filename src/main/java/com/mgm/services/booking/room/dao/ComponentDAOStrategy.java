package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.reservation.RoomRequest;

import java.util.Date;
import java.util.List;

/**
 * DAO interface to expose services to fetch available components for a room and
 * trip dates.
 *
 */
public interface ComponentDAOStrategy {

    /**
     * Gets available components for a room and trip dates.
     * 
     * @param request
     *            Component request
     * @return List of room component ids.
     */
    List<RoomRequest> getRoomComponentAvailability(RoomComponentRequest request);
    
    /**
     * Gets component details by component ID.
     * 
     * @param componentId
     *            Component ID
     * @param roomTypeId
     *            Room type ID
     * @return
     */
    RoomRequest getRoomComponentById(String componentId, String roomTypeId);

    /**
     * Gets component details by component identifier.
     *
     * @param code
     *            Component external code
     * @return
     */
    RoomComponent getRoomComponentByCode(String propertyId, String code, String roomTypeId,
                                         String ratePlanId, Date checkInDate, Date checkOutDate,
                                         String mlifeNumber, String source);

    /**
     * Gets component details by component ID.
     *
     * @param componentId
     *            Component ID
     * @return
     */
    RoomComponent getRoomComponentById(String componentId);
}
