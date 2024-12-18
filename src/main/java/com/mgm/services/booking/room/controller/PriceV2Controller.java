package com.mgm.services.booking.room.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import cern.jet.math.Mult;
import com.mgm.services.booking.room.model.request.ResortPriceWithTaxV2Request;
import com.mgm.services.booking.room.model.request.dto.MultiDateDTO;
import com.mgm.services.booking.room.model.request.dto.ResortPriceWithTaxDTO;
import com.mgm.services.booking.room.model.response.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.request.CalendarPriceV2Request;
import com.mgm.services.booking.room.model.request.ResortPriceV2Request;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV2Request;
import com.mgm.services.booking.room.service.CalendarPriceV2Service;
import com.mgm.services.booking.room.service.ResortPriceV2Service;
import com.mgm.services.booking.room.service.RoomAvailabilityV2Service;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.booking.room.validator.TokenValidator;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;

/**
 * Controller class to handle all room pricing requests like calendar price,
 * room availability and calendar price.
 */
@RestController
@RequestMapping("/v2")
@V2Controller
public class PriceV2Controller extends ExtendedBaseV2Controller {

    @Autowired
    private RoomAvailabilityV2Service availabilityV2Service;

    @Autowired
    private CalendarPriceV2Service calendarV2Service;

    @Autowired
    private ResortPriceV2Service resortPriceV2Service;

    @Autowired
    private TokenValidator tokenValidator;

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
    @GetMapping("/availability/calendar")
    public List<CalendarPriceV2Response> getCalendarPrices(@RequestHeader String source,
            @Valid CalendarPriceV2Request calendarRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, calendarRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(calendarRequest, calendarRequest.getPropertyId());
        return calendarV2Service.getCalendarPrices(calendarRequest);
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
            @Valid RoomAvailabilityV2Request availabilityRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, availabilityRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(availabilityRequest, availabilityRequest.getPropertyId());
        return availabilityV2Service.getRoomAvailability(availabilityRequest);
    }

    /**
     * 
     * @param source
     * @param pricingRequest
     * @param result
     * @param servletRequest
     * @param enableJwb
     * @return
     */
    @GetMapping("/availability/resorts")
    public List<ResortPriceResponse> getResortsAvailability(@RequestHeader String source,
            @Valid ResortPriceV2Request pricingRequest, BindingResult result, HttpServletRequest servletRequest,
            @RequestHeader(
                    defaultValue = "false") String enableJwb) {
    	tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, pricingRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(pricingRequest, pricingRequest.getPropertyId());
        pricingRequest.setSource("mgmri");

        if (pricingRequest.isPerpetualPricing() && pricingRequest.getCustomerId() <= 0) {
            throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_CUSTOMER.getErrorCode()));
        }

        resortPriceV2Service.requestUpdateForPO(pricingRequest);
        // If programId is supplied, perpetual pricing is ignored
        if (pricingRequest.isPerpetualPricing() && (StringUtils.isEmpty(pricingRequest.getProgramId())
                && StringUtils.isEmpty(pricingRequest.getSegment()) && StringUtils.isEmpty(pricingRequest.getGroupCode()))) {
            return resortPriceV2Service.getResortPerpetualPrices(pricingRequest);
        } else {
            return resortPriceV2Service.getResortPrices(pricingRequest);
        }
    }

    /**
     *
     * @param source
     * @param request
     * @param result
     * @param servletRequest
     * @param enableJwb
     * @return
     */
    @PostMapping ("/availability/resortswithtax")
    public MultiDateResortPriceResponse getResortsAvailabilityWithTaxAmt(@RequestHeader String source,
                                                                         @RequestBody ResortPriceWithTaxV2Request request, BindingResult result, HttpServletRequest servletRequest,
                                                                         @RequestHeader(
                                                                    defaultValue = "false") String enableJwb) {
        ResortPriceWithTaxDTO pricingRequest = request.getRequest();
        tokenValidator.validateToken(servletRequest, RBSTokenScopes.GET_ROOM_AVAILABILITY);
        preprocess(source, pricingRequest, result, servletRequest, enableJwb);
        //CBSR-1452 set the perpetual pricing flag based on perpetual Eligible Property IDs from the JWT instead of the perpetual eligible flag to accommodate ACRS.
        preProcessPerpetualPricing(pricingRequest, pricingRequest.getPropertyId());
        if (pricingRequest.isPerpetualPricing() && pricingRequest.getCustomerId() <= 0) {
            throw new ValidationException(Collections.singletonList(ErrorCode.INVALID_CUSTOMER.getErrorCode()));
        }
        return getMultiDateResortPrices(pricingRequest);

    }

    private MultiDateResortPriceResponse getMultiDateResortPrices(ResortPriceWithTaxDTO pricingRequest) {
        MultiDateResortPriceResponse response = new MultiDateResortPriceResponse();
        List<MultiDateResortPrice> multiDateResortPrices = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(pricingRequest.getDates())){
            List<ResortPriceWithTaxDTO> resortPriceWithTaxDTOList = getPricingRequestList(pricingRequest);
            ExecutorService executor = Executors.newFixedThreadPool(resortPriceWithTaxDTOList.size());
            try {
                List<CompletableFuture<MultiDateResortPrice>> listOfCompletableFutures = resortPriceWithTaxDTOList.stream()
                        .map(resortPriceWithTaxDTO -> CompletableFuture.supplyAsync(
                                () -> getResortPriceResponse(resortPriceWithTaxDTO), executor))
                        .collect(Collectors.toList());
                multiDateResortPrices.addAll(listOfCompletableFutures.stream().map(CompletableFuture::join).collect(Collectors.toList()));

            } finally {
                executor.shutdown();
            }
        }
        response.setMultiDateResortPrices(multiDateResortPrices);
        return response;
    }

    private List<ResortPriceWithTaxDTO> getPricingRequestList(ResortPriceWithTaxDTO pricingRequest) {
        List<ResortPriceWithTaxDTO> pricingRequestWithDates = new ArrayList<>();
        for(MultiDateDTO date: pricingRequest.getDates()) {
            ResortPriceWithTaxDTO pricingRequestCopy = new ResortPriceWithTaxDTO();
            BeanUtils.copyProperties(pricingRequest,pricingRequestCopy);
            // set check in / check out in the request
            pricingRequestCopy.setCheckInDate(date.getCheckIn());
            pricingRequestCopy.setCheckOutDate(date.getCheckOut());
            pricingRequestWithDates.add(pricingRequestCopy);
        }

        return pricingRequestWithDates;
    }

    private MultiDateResortPrice getResortPriceResponse(ResortPriceWithTaxDTO pricingRequest) {
        MultiDateResortPrice multiDateResortPrice = new MultiDateResortPrice();
        multiDateResortPrice.setDate(new MultiDateDTO(pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate()));
        pricingRequest.setPackageFlow(true);
        if (pricingRequest.isPerpetualPricing() && (StringUtils.isEmpty(pricingRequest.getProgramId())
                && StringUtils.isEmpty(pricingRequest.getSegment()) && StringUtils.isEmpty(pricingRequest.getGroupCode()))) {
            multiDateResortPrice.setResortPrices(resortPriceV2Service.getResortPerpetualPrices(pricingRequest));
        } else {
            multiDateResortPrice.setResortPrices(resortPriceV2Service.getResortPricesWithTax(pricingRequest));
        }

        return multiDateResortPrice;
    }

}
