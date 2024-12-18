package com.mgm.services.booking.room.service;

import java.util.List;

import com.mgm.services.booking.room.model.request.ResortPriceRequest;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;

/**
 * Service to expose functionality to retrieve prices at resort level
 */
public interface ResortPriceService {

    /**
     * Returns prices for all resorts based on request params. Sold out resorts
     * are all also included with SOLDOUT status.
     * 
     * @param pricingRequest
     *            Resorts pricing request
     * @return Prices for all resorts.
     */
    List<ResortPriceResponse> getResortPrices(ResortPriceRequest pricingRequest);

    /**
     * Returns perpetual prices for all resorts based on request params.
     * Perpetual prices are fetched by looking up default perpetual program for
     * each property and get prices for that perpetual program. If, default
     * perpetual program is not available for a property, best available prices
     * are returned. Sold out resorts are all also included with SOLDOUT status.
     * 
     * @param pricingRequest
     *            Resorts pricing request
     * @return Prices for all resorts.
     */
    List<ResortPriceResponse> getResortPerpetualPrices(ResortPriceRequest pricingRequest);
}
