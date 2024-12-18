package com.mgm.services.booking.room.service;

import java.util.List;

import com.mgm.services.booking.room.model.request.CalendarPriceRequest;
import com.mgm.services.booking.room.model.response.CalendarPriceResponse;

/**
 * Service interface exposing service to retrieve calendar prices for property
 * based on search/filter criteria.
 */
public interface CalendarPriceService {

    /**
     * Returns prices for calendar duration requested based on other search
     * criteria like dates, property and program.
     * 
     * @param request
     *            Pricing request
     * @return Prices for each calendar day
     */
    List<CalendarPriceResponse> getCalendarPrices(CalendarPriceRequest request);
}
