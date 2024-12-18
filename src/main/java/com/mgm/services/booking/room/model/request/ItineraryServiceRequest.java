/**
 * Request object for creating party room reservations.
 */
package com.mgm.services.booking.room.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * The Itinerary Service Request class
 * @author vararora
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class ItineraryServiceRequest {

    private Itinerary itinerary;
    
    /**
     * The Itinerary class
     * @author vararora
     *
     */
    public static @Data class Itinerary {
        private String itineraryName;
        private String customerId;
        private RoomReservationBasic roomReservationBasic;
        private TripParams tripParams;
    }

}
