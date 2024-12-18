package com.mgm.services.booking.room.controller;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.CalendarPriceV3Request;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV3Request;
import com.mgm.services.booking.room.model.response.CalendarPriceV3Response;
import com.mgm.services.booking.room.model.response.RoomAvailabilityCombinedResponse;
import com.mgm.services.booking.room.service.CalendarPriceV3Service;
import com.mgm.services.booking.room.service.RoomAvailabilityV3Service;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Controller class to handle room pricing requests like calendar price.
 * 
 * @author laknaray
 *
 */
@RestController
@RequestMapping("/v3")
@V2Controller
public class PriceV3Controller extends ExtendedBaseV2Controller {

    @Autowired
    private RoomAvailabilityV3Service availabilityV3Service;

    @Autowired
    private CalendarPriceV3Service calendarV3Service;

    @Autowired
    private TokenValidator tokenValidator;

    @GetMapping("/availability/calendar")
    public List<CalendarPriceV3Response> getCalendarPrices(@RequestHeader String source,
            @Valid CalendarPriceV3Request calendarRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, calendarRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(calendarRequest, calendarRequest.getPropertyId());
        
        if (calendarRequest.isPerpetualPricing() && !(calendarRequest.getCustomerId() > 0 || StringUtils.isNotBlank(calendarRequest.getMlifeNumber()))) {
        	throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_CUSTOMER.getErrorCode()));
        }
        return calendarV3Service.getLOSBasedCalendarPrices(calendarRequest);
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
    @GetMapping("/availability/trip")
    public RoomAvailabilityCombinedResponse getRoomAvailability(@RequestHeader String source,
            @Valid RoomAvailabilityV3Request availabilityRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, availabilityRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(availabilityRequest, availabilityRequest.getPropertyId());
        
        // if programIds are supplied, ignore any unexpected value in programId
        if (!availabilityRequest.getProgramIds()
                .isEmpty()) {
            availabilityRequest.setProgramId(null);
        }
        return availabilityV3Service.getRoomAvailability(availabilityRequest);
    }

    @GetMapping("/availability/grid")
    public RoomAvailabilityCombinedResponse getGridPrices(@RequestHeader String source,
                                                          @Valid RoomAvailabilityV3Request availabilityRequest, BindingResult result, HttpServletRequest servletRequest,
                                                          @RequestHeader(defaultValue = "false") String enableJwb){

        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, availabilityRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(availabilityRequest, availabilityRequest.getPropertyId());

        // if programIds are supplied, ignore any unexpected value in programId
        if (!availabilityRequest.getProgramIds()
                .isEmpty()) {
            availabilityRequest.setProgramId(null);
        }
        return availabilityV3Service.getRoomAvailabilityGrid(availabilityRequest);
    }
}
