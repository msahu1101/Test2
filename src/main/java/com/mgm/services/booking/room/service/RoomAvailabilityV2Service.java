package com.mgm.services.booking.room.service;

import java.util.Optional;
import java.util.Set;

import com.mgm.services.booking.room.model.request.RoomAvailabilityRequest;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV2Request;
import com.mgm.services.booking.room.model.response.RoomAvailabilityCombinedResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityV2Response;

/**
 * Service interface exposing services to retrieve prices for property based on
 * search/filter criteria.
 */
public interface RoomAvailabilityV2Service {

    /**
     * Returns room availability or ratePlans based on request criteria like dates,
     * program, property id also based on the API response.
     *
     * @param request
     *            Room availability request
     * @return RoomAvailabilityCombinedResponse object either with
     *         <i>availability</i> or <i>ratePlans</i>
     */
    RoomAvailabilityCombinedResponse getRoomAvailability(RoomAvailabilityV2Request request);
    
    /**
     * Returns room prices based on request criteria like dates, program, property
     * id. Also includes unavailable rooms along with reason depending on the flag
     * <i>includeSoldOutRooms</i> in the request.
     * 
     * @param request
     *            Room availability request
     * @return List of rooms available with prices
     */
    Set<RoomAvailabilityV2Response> getRoomPrices(RoomAvailabilityV2Request request);


    /**
     * Returns lowest room prices based on request criteria like dates, program,
     * property id
     * 
     * @param request
     *            Room availability request
     * @return Lowest priced room available with prices
     */
    Optional<RoomAvailabilityV2Response> getLowestRoomPrice(RoomAvailabilityV2Request request);

}
