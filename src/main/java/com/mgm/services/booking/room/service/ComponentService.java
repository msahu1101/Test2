package com.mgm.services.booking.room.service;

import java.util.List;

import com.mgm.services.booking.room.model.RoomComponent;
import com.mgm.services.booking.room.model.request.PackageComponentRequest;
import com.mgm.services.booking.room.model.request.PackageComponentRequestV1;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.request.RoomComponentV2Request;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.response.PackageComponentResponse;
import com.mgm.services.booking.room.model.response.PackageComponentResponseV1;

/**
 * Service interface exposing functionality around room requests.
 * 
 */
public interface ComponentService {

    /**
     * Get available room components for a room and trip dates
     * 
     * @param componentRequest
     *            Component request
     * @return List of room requests
     */
    List<RoomRequest> getAvailableRoomComponents(RoomComponentRequest componentRequest);

    /**
     * Get room request detail based on room type id and component id.
     * 
     * @param roomTypeId
     *            Room type Id
     * @param componentId
     *            Component id
     * @return Room request details
     */
    RoomRequest getRoomComponent(String roomTypeId, String componentId);

    /**
     * Get list of available room components for the given room, property and trip
     * dates.
     * 
     * @param componentRequest
     *            Component request
     * @return List of room requests v2
     */
    List<RoomComponent> getAvailableRoomComponents(RoomComponentV2Request componentRequest);

    List<PackageComponentResponse> getAvailablePackageComponents(PackageComponentRequest packageComponentsRequest);
    List<PackageComponentResponseV1> getAvailablePackageComponentsV1(PackageComponentRequestV1 packageComponentsRequest);
}
