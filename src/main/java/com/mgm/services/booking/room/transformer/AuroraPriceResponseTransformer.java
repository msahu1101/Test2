package com.mgm.services.booking.room.transformer;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.request.CalendarPriceRequest;
import com.mgm.services.booking.room.model.request.CalendarPriceV2Request;
import com.mgm.services.booking.room.model.request.CalendarPriceV3Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.CalendarPriceResponse;
import com.mgm.services.booking.room.model.response.CalendarPriceV2Response;
import com.mgm.services.booking.room.model.response.CalendarPriceV3Response;
import com.mgm.services.booking.room.model.response.TripDetailsV3Response;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * Transformer class to create and return CalendarPriceResponse from
 * AuroraPriceResponse object
 */
@Log4j2
@UtilityClass
public class AuroraPriceResponseTransformer {

    /**
     * Create and return CalendarPriceResponse from AuroraPriceResponse object.
     * 
     * @param response
     *            AuroraPriceResponse Object
     * @param calendarRequest
     *            Calendar price request
     * @return Returns calendar price response with status
     */
    public static CalendarPriceResponse getCalendarPriceResponse(AuroraPriceResponse response,
            CalendarPriceRequest calendarRequest) {
        CalendarPriceResponse priceResponse = new CalendarPriceResponse();
        priceResponse.setComp(response.isComp());
        priceResponse.setDate(response.getDate());
        priceResponse.setStatus(response.getStatus());
        if (calendarRequest.getProgramId() != null && StringUtils.isNotEmpty(response.getProgramId())
                && response.getProgramId().equals(calendarRequest.getProgramId())) {
            priceResponse.setStatus(AvailabilityStatus.OFFER);
        }
        if (response.isCloseToArrival()) {
            priceResponse.setStatus(AvailabilityStatus.NOARRIVAL);
        }
        priceResponse.setPrice(response.getDiscountedPrice());
        priceResponse.setProgramId(response.getProgramId());
        double memberDiscPrice = response.getDiscountedMemberPrice();

        // Include member price only if it's lower than transient price
        // Using >= zero check here as aurora returns -1 for sold-out or
        // unavailable
        if (calendarRequest.getCustomerId() < 0 && calendarRequest.getProgramId() == null && memberDiscPrice >= 0
                && memberDiscPrice < response.getDiscountedPrice()) {
            log.info("Member price is not included");
            priceResponse.setMemberPrice(response.getDiscountedMemberPrice());
            priceResponse.setMemberProgramId(response.getMemberProgramId());
        }
        return priceResponse;
    }

    /**
     * Create and return CalendarPriceResponse from AuroraPriceResponse object.
     * 
     * @param response
     *            AuroraPriceResponse Object
     * @param calendarRequest
     *            Calendar price request
     * @return Returns calendar price response with status
     */
    public static CalendarPriceV2Response getCalendarPriceV2Response(AuroraPriceResponse response,
            CalendarPriceV2Request calendarRequest) {
        CalendarPriceV2Response priceV2Response = new CalendarPriceV2Response();
        priceV2Response.setComp(response.isComp());
        priceV2Response.setDate(response.getDate());
        priceV2Response.setStatus(response.getStatus());
        priceV2Response.setPOApplicable(response.isPOApplicable());
        priceV2Response.setRoomTypeId(response.getRoomTypeId());
        if (calendarRequest.getProgramId() != null && StringUtils.isNotEmpty(response.getProgramId())
                && response.getProgramId().equals(calendarRequest.getProgramId())) {
            priceV2Response.setStatus(AvailabilityStatus.OFFER);
        }
        if (response.isCloseToArrival()) {
            priceV2Response.setStatus(AvailabilityStatus.NOARRIVAL);
        }
        priceV2Response.setPrice(response.getDiscountedPrice());
        priceV2Response.setProgramId(response.getProgramId());

        return priceV2Response;
    }

    public static CalendarPriceV3Response getCalendarPriceV3Response(AuroraPriceV3Response auroraResponse,
            CalendarPriceV3Request calendarRequest) {
        CalendarPriceV3Response priceV3Response = new CalendarPriceV3Response();
        priceV3Response.setStatus(auroraResponse.getStatus());
        priceV3Response.setDate(auroraResponse.getDate());
        priceV3Response.setRoomTypeId(auroraResponse.getRoomTypeId());
        List<TripDetailsV3Response> listOfTripDetailsV3Response = new LinkedList<>();
        int totalCompNights = 0;
        if(auroraResponse.getTripDetails() != null){
	        auroraResponse.getTripDetails().forEach(tripDetails -> {
	            TripDetailsV3Response tripDetailsResponse = new TripDetailsV3Response();
	            tripDetailsResponse.setDate(tripDetails.getDate());
	            tripDetailsResponse.setProgramId(tripDetails.getProgramId());
	            tripDetailsResponse.setComp(tripDetails.isComp());
	            listOfTripDetailsV3Response.add(tripDetailsResponse);
	        });
	        
	        totalCompNights = auroraResponse.getTripDetails().stream().filter(tD -> tD.isComp())
	                .collect(Collectors.toList()).size();
        }
        priceV3Response.setTripDetails(listOfTripDetailsV3Response);
        
        priceV3Response.setPOApplicable(isPerpetualPricing(totalCompNights));
        priceV3Response.setPricingMode(auroraResponse.getPricingMode());
        priceV3Response.setTotalCompNights(totalCompNights);
        double totalNightlyTripPrice = auroraResponse.getTotalNightlyTripPrice();
        priceV3Response.setTotalNightlyTripPrice(totalNightlyTripPrice);
        double totalNightlyTripBasePrice = auroraResponse.getTotalNightlyTripBasePrice();
        priceV3Response.setTotalNightlyTripBasePrice(totalNightlyTripBasePrice);
        // Avoid calculating avg. nightly price for SOLDOUT trips
        double averageNightlyTripPrice = totalNightlyTripPrice == -1 ? totalNightlyTripPrice
                : totalNightlyTripPrice / calendarRequest.getTotalNights();
        priceV3Response.setAverageNightlyTripPrice(averageNightlyTripPrice);
        double averageNightlyTripBasePrice = totalNightlyTripBasePrice == -1 ? totalNightlyTripBasePrice
                : totalNightlyTripBasePrice / calendarRequest.getTotalNights();
        priceV3Response.setAverageNightlyTripBasePrice(averageNightlyTripBasePrice);
        priceV3Response.setPromo(auroraResponse.getPromo());
        return priceV3Response;
    }
    
	private static boolean isPerpetualPricing(int totalCompNights) {
		return totalCompNights > 0;
	} 

}
