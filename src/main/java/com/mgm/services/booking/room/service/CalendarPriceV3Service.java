package com.mgm.services.booking.room.service;

import java.util.List;

import com.mgm.services.booking.room.model.request.CalendarPriceV3Request;
import com.mgm.services.booking.room.model.response.CalendarPriceV3Response;

/**
 * Service interface exposing service to retrieve calendar prices for property
 * based on search/filter criteria.
 */
public interface CalendarPriceV3Service {

    /**
     * Returns trip prices for the requested duration based on other search
     * criteria like dates, property, program and length of stay.
     * 
     * @param request
     *            Pricing request
     * @return Prices for the trips in the given calendar duration
     */
    List<CalendarPriceV3Response> getLOSBasedCalendarPrices(CalendarPriceV3Request request);
}
