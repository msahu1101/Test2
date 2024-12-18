package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.RoomAvailabilityV3Request;
import com.mgm.services.booking.room.model.response.RoomAvailabilityCombinedResponse;

/**
 * Service interface exposing services to retrieve prices for property based on
 * search/filter criteria.
 */
public interface RoomAvailabilityV3Service {

    /**
     * Returns room availability or ratePlans based on request criteria like
     * dates, program, property id also based on the API response.
     * 
     * Determines if user eligible for perpetual pricing. If the JWT is guest
     * token, read com.mgm.loyalty.perpetual_eligible claim boolean value. If
     * the JWT is non-guest token, uses the value from perpetualPricing boolean
     * param.
     * 
     * If user is eligible for perpetual pricing && programId is not provided,
     * then best available pricing returned for that property within
     * “availability“ attribute in the response i.e., no multiple rate plans
     * 
     * If user is NOT eligible for perpetual pricing AND programId is supplied
     * 
     * If includeDefaultRatePlans=true, will return program prices along with
     * default MRD rate plans in "ratePlans" object If
     * includeDefaultRatePlans=false, will return the program pricing only
     * within “availability“ attribute in the response
     * 
     * If user is NOT eligible for perpetual pricing AND programId is not
     * supplied, return multiple rate plans for that property
     * 
     * A flag named "unavailable" will be set to true if all dates for a room
     * are unavailable or there is partial availability. "includeSoldOutRooms"
     * flag set as true, will return sold out rooms id and unavailability reason
     * in the response. The unavailabilityReason will be displayed for each trip
     * date. All the other pricing fields will not be included.
     *
     * @param availabilityRequest
     *            Room availability request
     * @return RoomAvailabilityCombinedResponse object either with
     *         <i>availability</i> or <i>ratePlans</i>
     */
    RoomAvailabilityCombinedResponse getRoomAvailability(RoomAvailabilityV3Request availabilityRequest);
    RoomAvailabilityCombinedResponse getRoomAvailabilityGrid(RoomAvailabilityV3Request availabilityRequest);

}
