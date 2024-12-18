package com.mgm.services.booking.room.service;

import java.util.List;

import com.mgm.services.booking.room.model.request.ResortPriceV2Request;
import com.mgm.services.booking.room.model.request.ResortPriceWithTaxV2Request;
import com.mgm.services.booking.room.model.request.dto.ResortPriceWithTaxDTO;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;

/**
 * Service to expose functionality to retrieve prices at resort level
 */
public interface ResortPriceV2Service {

    /**
     * Returns prices for all the resorts based on the request params.
     * 
     * <ol>
     * <li>Requests with segment or participating program as <code>programId</code>,
     * fetches all the participating programs in that segment and then pricing will
     * be requested with those. For non-participating and SOLDOUT properties BAR
     * pricing will be requested with <code>enableMRD</code> true.</li>
     * <li>Requests with single property program as <code>programId</code>, pricing
     * will be requested with that. For other and SOLDOUT properties BAR pricing
     * will be requested with <code>enableMRD</code> true.</li>
     * <li>Requests with no <code>programId</code>, all the properties BAR pricing
     * will be requested with <code>enableMRD</code> true.</li>
     * </ol>
     * 
     * @param pricingRequest
     *            Resorts pricing request
     * @return Prices for all resorts.
     */
    List<ResortPriceResponse> getResortPrices(ResortPriceV2Request pricingRequest);

    List<ResortPriceResponse> getResortPricesWithTax(ResortPriceWithTaxDTO pricingRequest);

    /**
     * Returns best available prices for all resorts based on request params. Sold
     * out resorts are all also included with SOLDOUT status.
     * 
     * @param pricingRequest
     *            Resorts pricing request
     * @return Prices for all resorts.
     */
    List<ResortPriceResponse> getResortPerpetualPrices(ResortPriceV2Request pricingRequest);

    void requestUpdateForPO(ResortPriceV2Request request);

}
