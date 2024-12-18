package com.mgm.services.booking.room.transformer;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.ResortPrice;
import com.mgm.services.booking.room.model.request.RoomAvailabilityRequest;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV2Request;
import com.mgm.services.booking.room.model.response.PricingModes;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityV2Response;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * Utility to transform price response.
 * 
 * @author jayveera
 *
 */
@UtilityClass
@Log4j2
public class PriceResponseTransformer {

    /**
     * Transform the availability response to price response object.
     * 
     * @param availabilityResponseOpt availability response object.
     * @param pricingRequest          pricing request.
     * @return ResortPriceResponse response.
     */
    public static ResortPriceResponse getResortsPriceResponse(
            Optional<RoomAvailabilityResponse> availabilityResponseOpt, RoomAvailabilityRequest pricingRequest) {

        ResortPriceResponse priceResponse = new ResortPriceResponse();
        priceResponse.setPropertyId(pricingRequest.getPropertyId());

        if (availabilityResponseOpt.isPresent()) {

            RoomAvailabilityResponse availabilityResponse = availabilityResponseOpt.get();
            priceResponse.setStatus(AvailabilityStatus.AVAILABLE);
            priceResponse.setComp(availabilityResponse.getPrice().isComp());
            priceResponse.setResortFee(availabilityResponse.getResortFee());
            priceResponse.setPerpetualProgramId(pricingRequest.getProgramId());

            ResortPrice price = new ResortPrice();
            price.setBaseAveragePrice(availabilityResponse.getPrice().getBaseAveragePrice());
            price.setDiscountedAveragePrice(availabilityResponse.getPrice().getDiscountedAveragePrice());
            priceResponse.setPrice(price);

        } else {
            priceResponse.setStatus(AvailabilityStatus.SOLDOUT);
        }

        return priceResponse;
    }

    public static ResortPriceResponse getResortsPriceV2Response(
            Optional<RoomAvailabilityV2Response> availabilityResponseOpt,
            RoomAvailabilityV2Request availabilityRequest) {

        ResortPriceResponse priceResponse = new ResortPriceResponse();
        priceResponse.setPropertyId(availabilityRequest.getPropertyId());

        if (availabilityResponseOpt.isPresent()) {

            RoomAvailabilityV2Response availabilityResponse = availabilityResponseOpt.get();
            priceResponse.setStatus(AvailabilityStatus.AVAILABLE);
            priceResponse.setComp(availabilityResponse.getPrice().isComp());
            priceResponse.setResortFee(availabilityResponse.getResortFee());
            priceResponse.setAmtAftTax(availabilityResponse.getAmtAftTax());
            priceResponse.setBaseAmtAftTax(availabilityResponse.getBaseAmtAftTax());
            priceResponse.setProgramId(StringUtils.isNotEmpty(availabilityResponse.getPoProgramId()) ? availabilityResponse.getPoProgramId() : availabilityResponse.getBarProgramId());
            // Setting the pricingMode based on type of availabilitRequest
            if (availabilityResponse.isPerpetualPricing()) {
                priceResponse.setPricingMode(PricingModes.PERPETUAL);
            } else {
                priceResponse.setPricingMode(PricingModes.BEST_AVAILABLE);
            }
            
            log.debug("Pricing mode: {}", availabilityResponse.isPerpetualPricing());
            
            ResortPrice price = new ResortPrice();
            price.setBaseAveragePrice(availabilityResponse.getPrice().getBaseAveragePrice());
            price.setDiscountedAveragePrice(availabilityResponse.getPrice().getDiscountedAveragePrice());
            priceResponse.setPrice(price);
            if(availabilityRequest.isPackageFlow()) {
            	priceResponse.setRoomTypeId(availabilityResponse.getRoomTypeId());
            }

        } else {
            priceResponse.setStatus(AvailabilityStatus.SOLDOUT);
        }

        return priceResponse;
    }
}
