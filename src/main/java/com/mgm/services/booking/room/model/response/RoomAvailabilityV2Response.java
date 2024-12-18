package com.mgm.services.booking.room.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mgm.services.booking.room.model.RoomTripPriceV2;

import lombok.Data;

/**
 * RoomAvailability V2 response. This response contains additional field
 * <i>unavailabilityReason</i> and customer <i>compareTo</i> to logic.
 *
 * @author laknaray
 *
 */
@JsonInclude(Include.NON_NULL)
public @Data class RoomAvailabilityV2Response implements Comparable<RoomAvailabilityV2Response> {

    private String roomTypeId;
    private Double resortFee;
    private RoomTripPriceV2 price;
    private RoomTripPriceV2 memberPrice;
    private boolean unavailable;
    private boolean perpetualPricing;
    private double amtAftTax;
    private double baseAmtAftTax;
    @JsonIgnore
    private String poProgramId;
    @JsonIgnore
    private String barProgramId;

    /**
     * Overriding compareTo function to sort available room prices responses based
     * on price in the order of low to high. Also, move all the sold out rooms to
     * the bottom of the response. This is needed to determine program's starting
     * price.
     */
    @Override
    public int compareTo(RoomAvailabilityV2Response response) {
        int returnValue = -1;

        if (isThisCostlierOrSameCost(response) || isBothUnavailable(response) || isOnlyThisUnavailable(response)) {
            returnValue = 1;
        }

        return returnValue;
    }

    /**
     * Check whether <i>this</i> object is <i>unavailable</i> and response object is
     * available.
     *
     * @param response
     *            RoomAvailabilityV2Response object
     * @return returns true iff <i>this</i> object is unavailable and
     *         <i>response</i> object is available.
     */
    private boolean isOnlyThisUnavailable(RoomAvailabilityV2Response response) {
        return (this.unavailable) && (null != response.price && null != response.price.getDiscountedAveragePrice());
    }

    /**
     * Check whether both the objects are <i>unavailable</i> or not.
     *
     * @param response
     *            RoomAvailabilityV2Response object
     * @return returns true if both the objects are sold out.
     */
    private boolean isBothUnavailable(RoomAvailabilityV2Response response) {
        return this.unavailable && response.unavailable;
    }

    /**
     * Compares <i>discountedAveragePrice</i> between <i>this</i> and <i>response</i>
     * objects.
     *
     * @param response
     *            RoomAvailabilityV2Response object
     * @return returns true iff <i>discountedAveragePrice</i> in <i>this</i> object
     *         is greater than or equal to response object
     */
    private boolean isThisCostlierOrSameCost(RoomAvailabilityV2Response response) {
        boolean isThisAvailable = (null != this.price && null != this.price.getDiscountedAveragePrice());
        boolean isResponseAvailable = (null != response.price && null != response.price.getDiscountedAveragePrice());
        return isThisAvailable && isResponseAvailable
                && this.price.getDiscountedAveragePrice() >= response.price.getDiscountedAveragePrice();
    }
}
