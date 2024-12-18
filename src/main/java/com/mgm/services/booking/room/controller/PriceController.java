package com.mgm.services.booking.room.controller;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.model.request.CalendarPriceRequest;
import com.mgm.services.booking.room.model.request.ResortPriceRequest;
import com.mgm.services.booking.room.model.request.RoomAvailabilityRequest;
import com.mgm.services.booking.room.model.response.CalendarPriceResponse;
import com.mgm.services.booking.room.model.response.RatePlanResponse;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityResponse;
import com.mgm.services.booking.room.service.CalendarPriceService;
import com.mgm.services.booking.room.service.ResortPriceService;
import com.mgm.services.booking.room.service.RoomAvailabilityService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

import lombok.extern.log4j.Log4j2;

/**
 * Controller class to handle all room pricing requests like calendar price,
 * room availability and calendar price.
 */
@RestController
@Log4j2
@RequestMapping("/v1")
public class PriceController extends ExtendedBaseController {

    @Autowired
    private ResortPriceService pricingService;

    @Autowired
    private RoomAvailabilityService availabilityService;

    @Autowired
    private CalendarPriceService calendarService;

    /**
     * Returns lowest price available for each of the resorts for the trip
     * parameters supplied i.e., checkInDate, checkOutDate and numGuests. Best
     * available prices are returned when no programId or promoCode is supplied.
     * 
     * @param source
     *            Source header
     * @param pricingRequest
     *            Resort pricing request object
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwb
     *            enableJwb header
     * @return Returns lowest price available for each of the resorts
     */
    @GetMapping("/resorts/room-price")
    public List<ResortPriceResponse> getResortPrices(@RequestHeader String source,
            @Valid ResortPriceRequest pricingRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        preprocess(source, pricingRequest, result, servletRequest, enableJwb);
        pricingRequest.setAuroraItineraryIds(CommonUtil.getAuroraItinerariesInCart(sSession.getCartItems()));
        pricingRequest.setMyVegasRedemptionItems(sSession.getMyVegasRedemptionItems());
        pricingRequest.setCustomer(sSession.getCustomer());
        pricingRequest.setSource("mgmri");

        // If programId is supplied, perpetual pricing is ignored
        if (pricingRequest.isPerpetualPricing() && StringUtils.isEmpty(pricingRequest.getProgramId())) {

            if (pricingRequest.getCustomerId() == -1) {
                throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_CUSTOMER.getErrorCode()));
            }
            return pricingService.getResortPerpetualPrices(pricingRequest);
        } else {
            return pricingService.getResortPrices(pricingRequest);
        }

    }

    /**
     * Returns pricing and availability by room types for a selected property,
     * trip dates, and/or offer. Includes days-wise breakdown of the prices.
     * 
     * @param source
     *            Source header
     * @param availabilityRequest
     *            Room availability request
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwb
     *            enableJwb header
     * @return Returns pricing and availability by room types for a selected
     *         property, trip dates, and/or offer.
     */
    @GetMapping("/room/availability")
    public Set<RoomAvailabilityResponse> getRoomAvailability(@RequestHeader String source,
            @Valid RoomAvailabilityRequest availabilityRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        preprocess(source, availabilityRequest, result, servletRequest, enableJwb);
        availabilityRequest.setAuroraItineraryIds(CommonUtil.getAuroraItinerariesInCart(sSession.getCartItems()));
        availabilityRequest.setMyVegasRedemptionItems(sSession.getMyVegasRedemptionItems());
        availabilityRequest.setCustomer(sSession.getCustomer());
        return availabilityService.getRoomPrices(availabilityRequest);
    }

    /**
     * Returns lowest room rate for each day for a calendar period for a
     * specific property. Offer prices are overlayed on top of best available
     * prices when program Id is passed..
     * 
     * @param source
     *            Source header
     * @param calendarRequest
     *            Calendar price request object
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwb
     *            enableJwb header
     * @return Returns lowest room rate for each day for a calendar period
     */
    @GetMapping("/room/calendar/price")
    public List<CalendarPriceResponse> getCalendarPrices(@RequestHeader String source,
            @Valid CalendarPriceRequest calendarRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        preprocess(source, calendarRequest, result, servletRequest, enableJwb);
        calendarRequest.setAuroraItineraryIds(CommonUtil.getAuroraItinerariesInCart(sSession.getCartItems()));
        calendarRequest.setMyVegasRedemptionItems(sSession.getMyVegasRedemptionItems());
        calendarRequest.setCustomer(sSession.getCustomer());
        return calendarService.getCalendarPrices(calendarRequest);
    }

    /**
     * Returns all available rate plans for the selected trip and includes
     * pricing and availability by room types for each of the rate plans. Each
     * room entry will include days-wise breakdown of the prices. When a program
     * is supplied, response will include pricing and availability for the
     * program along with available rate plans.
     * 
     * @param source
     *            Source header
     * @param availabilityRequest
     *            Room availability request
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwb
     *            enableJwb header
     * @return Returns all available rate plans for the selected trip and
     *         includes pricing and availability by room types for each of the
     *         rate plans.
     */
    @GetMapping("/room/rate-plans")
    public List<RatePlanResponse> getRatePlans(@RequestHeader String source,
            @Valid RoomAvailabilityRequest availabilityRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        log.info(availabilityRequest);
        preprocess(source, availabilityRequest, result, servletRequest, enableJwb);
        availabilityRequest.setAuroraItineraryIds(CommonUtil.getAuroraItinerariesInCart(sSession.getCartItems()));
        availabilityRequest.setMyVegasRedemptionItems(sSession.getMyVegasRedemptionItems());
        availabilityRequest.setCustomer(sSession.getCustomer());
        return availabilityService.getRatePlans(availabilityRequest);
    }
}
