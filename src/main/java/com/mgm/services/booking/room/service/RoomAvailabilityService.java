package com.mgm.services.booking.room.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mgm.services.booking.room.model.request.RoomAvailabilityRequest;
import com.mgm.services.booking.room.model.response.RatePlanResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityResponse;

/**
 * Service interface exposing services to retrieve prices for property based on
 * search/filter criteria.
 */
public interface RoomAvailabilityService {

    /**
     * Returns room prices based on request criteria like dates, program,
     * property id
     * 
     * @param request
     *            Room availability request
     * @return List of rooms available with prices
     */
    Set<RoomAvailabilityResponse> getRoomPrices(RoomAvailabilityRequest request);

    /**
     * Returns lowest room prices based on request criteria like dates, program,
     * property id
     * 
     * @param request
     *            Room availability request
     * @return Lowest priced room available with prices
     */
    Optional<RoomAvailabilityResponse> getLowestRoomPrice(RoomAvailabilityRequest request);

    /**
     * Returns rate plans based on request criteria like dates, program,
     * property id and rooms within each of the rate plans. If program Id is
     * supplied, response will include requested program prices along with
     * prices of available rate plans
     * 
     * @param request
     *            Room availability request
     * @return Returns rate plans with rooms and prices
     */
    List<RatePlanResponse> getRatePlans(RoomAvailabilityRequest request);
}
