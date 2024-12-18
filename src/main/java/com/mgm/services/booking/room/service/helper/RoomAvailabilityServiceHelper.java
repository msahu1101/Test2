package com.mgm.services.booking.room.service.helper;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.util.PropertyConfig;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.PriceV2Itemized;
import com.mgm.services.booking.room.model.ProgramStartingPrice;
import com.mgm.services.booking.room.model.RoomTripPriceV2;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.RatePlanV2Response;
import com.mgm.services.booking.room.model.response.RoomAvailabilityV2Response;

/**
 * Helper class for RoomAvailability v1 and v2 Services.
 * 
 */
@Component
public class RoomAvailabilityServiceHelper {
    @Autowired
    PropertyConfig propertyConfig;

    /**
     * Checks if its applicable to return member price.
     * 
     * @param customerId
     *            Customer Identifier
     * @param programId
     *            Program Identifier
     * @return returns true if member price is applicable otherwise false
     */
    public boolean isMemberPriceApplicable(long customerId, String programId) {
        return customerId < 0 && StringUtils.isEmpty(programId);
    }

    /**
     * Checks if the room is priced with requested program at least for 1 night.
     * 
     * @param tripPrices
     *            Itemized prices for the room
     * @param programId
     *            Program Id
     * @return Returns true if the room is using the requested program at least
     *         for 1 night.
     */
    public boolean isProgramIncluded(List<AuroraPriceResponse> tripPrices, String programId) {
        for (AuroraPriceResponse price : tripPrices) {
            if(isProgramIdBridgeProperty(price.getProgramId())){
                return compareBridgeProgramIds(price.getProgramId(), programId);
            }
            if (price.getProgramId().equals(programId)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Checks if the rate plan from request and from trip prices are same.
     *
     * @param priceProgramId
     *            rateplan for prices objects
     * @param requestProgramId
     *            Program Id from the request for trip pricing
     * @return Returns true if both rate plans are same
     *
     */

    private boolean compareBridgeProgramIds(String priceProgramId, String requestProgramId){
        String priceRatePlanCode,requestRatePlanCode = null;
        if(ACRSConversionUtil.isAcrsGroupCodeGuid(priceProgramId)){
            priceRatePlanCode = ACRSConversionUtil.getGroupCode(priceProgramId);
            requestRatePlanCode = ACRSConversionUtil.getGroupCode(requestProgramId);
        }
        else {
            priceRatePlanCode = ACRSConversionUtil.getRatePlanCode(priceProgramId);
            requestRatePlanCode = ACRSConversionUtil.getRatePlanCode(requestProgramId);
        }

        if(priceRatePlanCode.equals(requestRatePlanCode))
            return true;
        return false;
    }

    /**
     * Checks if the program is from a psuedo property.
     *
     * @param programId
     *            programId from trip prices object

     * @return Returns true if property id has masterPropertyCode associated
     *
     */
    private boolean isProgramIdBridgeProperty(String programId){
        String propertyId =null;
        if(ACRSConversionUtil.isAcrsRatePlanGuid(programId) || ACRSConversionUtil.isAcrsGroupCodeGuid(programId)) {
            propertyId = ACRSConversionUtil.getPropertyCode(programId);
        }
        else
            return false;
        String masterPropertyCode = propertyConfig.getPropertyValuesMap().get(propertyId)!=null ?
                propertyConfig.getPropertyValuesMap().get(propertyId).getMasterPropertyCode() : null;
        if(null != masterPropertyCode)
            return true;
       return false;
    }
    /**
     * Groups the aurora price response into respective rate plan list.
     * 
     * @param price
     *            Aurora price response
     * @param ratePlanMap
     *            Price responses grouped by program
     */
    public void populatePlanMap(AuroraPriceResponse price, Map<String, List<AuroraPriceResponse>> ratePlanMap) {

        if (StringUtils.isNotEmpty(price.getProgramId())) {
            if (ratePlanMap.containsKey(price.getProgramId())) {
                ratePlanMap.get(price.getProgramId()).add(price);
            } else {
                List<AuroraPriceResponse> priceList = new LinkedList<>();
                priceList.add(price);
                ratePlanMap.put(price.getProgramId(), priceList);
            }
        }
    }

    /**
     * This method will determine whether memberPrice is cheap over price or not.
     * 
     * @param price price object
     * @param memberPrice memberPrice object
     * @return true if memberPrice available and cheaper than price, otherwise false
     */
    public boolean isMemberPriceCheaper(RoomTripPriceV2 price, RoomTripPriceV2 memberPrice) {
        return null != memberPrice && memberPrice.getDiscountedSubtotal() != null
                && memberPrice.getDiscountedSubtotal() < price.getDiscountedSubtotal();
    }

    public void setProgramStartingPrice(RatePlanV2Response response) {

        Optional<RoomAvailabilityV2Response> responseOptional = response.getRooms().stream().findFirst();

        if (responseOptional.isPresent()) {
            RoomAvailabilityV2Response roomResponse = responseOptional.get();
            ProgramStartingPrice startingPrice = new ProgramStartingPrice();
            startingPrice.setResortFee(roomResponse.getResortFee());

            // Use member price when available
            RoomTripPriceV2 price = roomResponse.getPrice();
            RoomTripPriceV2 memberPrice = roomResponse.getMemberPrice();
            // Consider memberPrive over price, if it is cheaper
            if (isMemberPriceCheaper(price, memberPrice)) {
                price = memberPrice;
            }
            startingPrice.setBaseAveragePrice(price.getBaseAveragePrice());
            if (null != price.getDiscountedAveragePrice()) {
                startingPrice.setDiscountedAveragePrice(price.getDiscountedAveragePrice());
            }
            startingPrice.setBaseSubtotal(price.getBaseSubtotal());
            startingPrice.setDiscountedSubtotal(price.getDiscountedSubtotal());
            startingPrice.setComp(price.isComp());

            response.setStartingPrice(startingPrice);
        }
    }

    /**
     * Builds <i>RoomTripPriceV2</i> object with the given params and returns.
     */
    public RoomTripPriceV2 getRoomTripMemberPriceV2(long noOfNights, double memberPrice, double memberDiscPrice,
            List<PriceV2Itemized> itemizedMemberPriceList) {
        RoomTripPriceV2 tripMemberPrice = new RoomTripPriceV2();
        tripMemberPrice.setBaseSubtotal(memberPrice);
        tripMemberPrice.setDiscountedSubtotal(memberDiscPrice);
        tripMemberPrice.setBaseAveragePrice(memberPrice / noOfNights);
        tripMemberPrice.setDiscountedAveragePrice(memberDiscPrice / noOfNights);
        tripMemberPrice.setItemized(itemizedMemberPriceList);
        if (Double.compare(memberDiscPrice, 0) == 0) {
            tripMemberPrice.setComp(true);
        }
        return tripMemberPrice;
    }

    /**
     * Builds <i>RoomTripPriceV2</i> object with the given params and returns.
     */
    public RoomTripPriceV2 getRoomTripPriceV2(long noOfNights, double price, double discPrice,
            List<PriceV2Itemized> itemizedPriceList, double resortFee) {
        RoomTripPriceV2 tripPrice = new RoomTripPriceV2();
        tripPrice.setBaseSubtotal(price);
        tripPrice.setDiscountedSubtotal(discPrice);
        tripPrice.setBaseAveragePrice(price / noOfNights);
        tripPrice.setDiscountedAveragePrice(discPrice / noOfNights);
        tripPrice.setItemized(itemizedPriceList);
        if (Double.compare(discPrice, 0) == 0) {
            tripPrice.setComp(true);
        }
        tripPrice.setResortFeeTotal(resortFee);
        double discountsTotal = price - discPrice;
        tripPrice.setDiscountsTotal(discountsTotal);
        tripPrice.setDiscounted(discountsTotal > 0);
        tripPrice.setTripSubtotal(discPrice + resortFee);
        return tripPrice;
    }

    /**
     * Builds <i>PriceV2Itemized</i> object by fetching it from passed <i>priceResponse</i> object and returns.
     */
    public PriceV2Itemized getItemizedMemberPriceV2(AuroraPriceResponse priceResponse) {
        PriceV2Itemized itemizedMemberPrice = new PriceV2Itemized();
        itemizedMemberPrice.setDate(priceResponse.getDate());
        itemizedMemberPrice.setProgramId(priceResponse.getMemberProgramId());
        itemizedMemberPrice.setBasePrice(priceResponse.getBaseMemberPrice());
        itemizedMemberPrice.setDiscountedPrice(priceResponse.getDiscountedMemberPrice());
        return itemizedMemberPrice;
    }

    /**
     * Builds <i>PriceV2Itemized</i> object by fetching it from passed <i>priceResponse</i> object and returns.
     */
    public PriceV2Itemized getItemizedPriceV2(AuroraPriceResponse priceResponse) {
        PriceV2Itemized itemizedPrice = new PriceV2Itemized();
        itemizedPrice.setDate(priceResponse.getDate());
        itemizedPrice.setComp(priceResponse.isComp());
        itemizedPrice.setProgramId(priceResponse.getProgramId());
        itemizedPrice.setBasePrice(priceResponse.getBasePrice());
        itemizedPrice.setDiscountedPrice(priceResponse.getDiscountedPrice());
        itemizedPrice.setPricingRuleId(priceResponse.getPricingRuleId());
        itemizedPrice.setProgramIdIsRateTable(priceResponse.isProgramIdIsRateTable());
        itemizedPrice.setAmtAftTax(priceResponse.getAmtAftTax());
        itemizedPrice.setBaseAmtAftTax(priceResponse.getBaseAmtAftTax());
        double discount = priceResponse.getBasePrice() - priceResponse.getDiscountedPrice();
        itemizedPrice.setDiscount(discount);
        itemizedPrice.setDiscounted(discount > 0);
        return itemizedPrice;
    }
    
    /**
     * Return the channel from the request header <code>x-mgm-channel</code>.
     * 
     * @return channel header value as String
     */
    public String getChannelHeader() {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        return httpRequest.getHeader(ServiceConstant.X_MGM_CHANNEL);
    }
}
