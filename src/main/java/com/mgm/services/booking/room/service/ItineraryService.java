package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ItineraryResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;

import java.util.List;

/**
 * Service interface exposing service to authorize and confirm a transaction
 * 
 * @author vararora
 *
 */
public interface ItineraryService {

    /**
     * Updates customer Itinerary in UCP platform
     * 
     * @param roomReservationResponse
     *            the room reservation response object
     */
    void updateCustomerItinerary(RoomReservationV2Response roomReservationResponse);

    /**
     * Creates Itinerary Id in UCP platform
     *
     * @param roomReservationResponse
     *            the room reservation response object
     * @return String itineraryId
     */
    String createCustomerItinerary(RoomReservationV2Response roomReservationResponse);

    /**
     * Get itinerary response by confirmation number
     * 
     * @param confirmationNumber confirmation number
     * @return itinerary response
     */
    ItineraryResponse getCustomerItineraryByConfirmationNumber(String confirmationNumber);

    /**
     *
     * @param sharedReservations
     */
    void createOrUpdateCustomerItinerary(RoomReservationV2Response primaryResv, List<RoomReservation> sharedReservations);
}
