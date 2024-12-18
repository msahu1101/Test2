package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.ItineraryServiceRequest;
import com.mgm.services.booking.room.model.response.ItineraryResponse;

/**
 * DAO interface exposing dao to update customer itinerary in UCP platform
 * 
 * @author vararora
 *
 */
public interface ItineraryDAO {

    /**
     * Updates customer Itinerary in UCP platform
     * 
     * @param itineraryId
     *            the itinerary Id
     * @param itineraryRequest
     *            the itinerary service request
     */
    void updateCustomerItinerary(String itineraryId, ItineraryServiceRequest itineraryRequest);

    /**
     * creates Itinerary Id in UCP platform
     *
     * @param itineraryRequest
     *            the itinerary service request
     * @return String itineraryId
     */
    String createCustomerItinerary(ItineraryServiceRequest itineraryRequest);
    
    
    /**
     * Retrieve ItineraryResponse from UCP Itinerary service
     * 
     * @param roomConfirmationNumber to fetch the Itinerary
     * @return itinerary response
     */
    ItineraryResponse retreiveCustomerItineraryDetailsByConfirmationNumber(String roomConfirmationNumber);
}
