package com.mgm.services.booking.room.service;

import java.util.List;

/**
 * Service to provide publishing of event.
 */
public interface EventPublisherService <T>{

    /**
     * Publish a list of events of a specific event type
     * 
     * @param eventsList
     * 				- List of events of type <T>
     * @param eventType
     *            type of event
     */
    void publishEvent(List<T> eventsList, String eventType);

}
