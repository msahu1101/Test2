package com.mgm.services.booking.room.dao;

import java.util.List;

import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.AuroraPriceV3Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;

/**
 * DAO interface to expose services for room pricing related functionalities.
 *
 */
public interface RoomPriceDAOStrategy {

    /**
     * Fetch room prices from aurora based on pricing request criteria.
     * 
     * @param pricingRequest
     *            Pricing request
     * @return Pricing response based on the request criteria
     */
    List<AuroraPriceResponse> getRoomPrices(AuroraPriceRequest pricingRequest);

    /**
     * Fetches lowest price available for each day to display prices on the
     * calendar.
     * 
     * @param pricingRequest
     *            Pricing request
     * @return Pricing response based on the request criteria
     */
    List<AuroraPriceResponse> getCalendarPrices(AuroraPriceRequest pricingRequest);
    
    /**
     * Fetches lowest price available for each day to display prices on the
     * calendar.
     * 
     * @param pricingRequest
     *            Pricing request
     * @return Pricing response based on the request criteria
     */
    List<AuroraPriceResponse> getIterableCalendarPrices(AuroraPriceRequest pricingRequest);
    
    /**
     * Fetches lowest price available for each day to display prices on the
     * calendar.
     * 
     * @param pricingRequest
     *            Pricing request
     * @return Pricing response based on the request criteria
     */
    List<AuroraPriceResponse> getCalendarPricesV2(AuroraPriceRequest pricingRequest);

    /**
     * Fetches lowest price available for each day to display prices on the
     * calendar.
     * 
     * @param pricingRequest
     *            Pricing request
     * @return Pricing response based on the request criteria
     */
    List<AuroraPriceResponse> getIterableCalendarPricesV2(AuroraPriceRequest pricingRequest);

    /**
     * Fetch room prices from aurora based on pricing request criteria. Also include isMrdPricing flag.
     *
     * @param pricingRequest
     *            Pricing request
     * @return Pricing response based on the request criteria
     */
    AuroraPricesResponse getRoomPricesV2(AuroraPriceRequest pricingRequest);

    /**
     * 
     * @param pricingRequest
     * @return
     */
    List<AuroraPriceV3Response> getLOSBasedCalendarPrices(AuroraPriceV3Request pricingRequest);

    List<AuroraPriceResponse> getGridAvailabilityForSoldOut(AuroraPriceRequest auroraPriceRequest);
}
