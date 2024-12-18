package com.mgm.services.booking.room.dao.impl;

import java.util.*;

import com.mgm.services.booking.room.properties.ApplicationProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.RoomPriceDAOStrategy;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.TripDetailsV3;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.AuroraPriceV3Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceV3Response;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.model.response.PricingModes;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.DateUtil;
import com.mgmresorts.aurora.common.CalendarRoomPrice;
import com.mgmresorts.aurora.common.DailyPrice;
import com.mgmresorts.aurora.common.DominantPlayType;
import com.mgmresorts.aurora.common.RoomPrice;
import com.mgmresorts.aurora.common.RoomPricingType;
import com.mgmresorts.aurora.messages.GetRoomPricingAndAvailabilityExRequest;
import com.mgmresorts.aurora.messages.GetRoomPricingAndAvailabilityLOSCalendarRequest;
import com.mgmresorts.aurora.messages.GetRoomPricingAndAvailabilityLOSCalendarResponse;
import com.mgmresorts.aurora.messages.GetRoomPricingAndAvailabilityResponse;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class providing DAO services to retrieve room prices by
 * invoking aurora API calls.
 */
@Component
@Log4j2
public class RoomPriceDAOStrategyGSEImpl extends AuroraBaseDAO implements RoomPriceDAOStrategy {

    @Autowired
    private RoomProgramCacheService programCacheService;

    @Autowired
    private ApplicationProperties appProperties;

    /*
     * (non-Javadoc)
     *
     * @see
     * com.mgm.services.booking.room.dao.RoomPricingDAO#getRoomPrices(com.mgm.
     * services.booking.room.model.request.AuroraPricingRequest)
     */
    @Override
    public List<AuroraPriceResponse> getRoomPrices(AuroraPriceRequest pricingRequest) {

        return getPrices(getPricingRequest(pricingRequest, RoomPricingType.TripPricing), pricingRequest.getSource(), pricingRequest);

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.mgm.services.booking.room.dao.RoomPricingDAO#getCalendarPrices(com.
     * mgm.services.booking.room.model.request.AuroraPricingRequest)
     */
    @Override
    public List<AuroraPriceResponse> getCalendarPrices(AuroraPriceRequest pricingRequest) {

        return getPrices(getPricingRequest(pricingRequest, RoomPricingType.CalendarPricing),
                pricingRequest.getSource(), pricingRequest);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.mgm.services.booking.room.dao.RoomPricingDAO#
     * getIterableCalendarPrices(com.
     * mgm.services.booking.room.model.request.AuroraPricingRequest)
     */
    @Override
    public List<AuroraPriceResponse> getIterableCalendarPrices(AuroraPriceRequest pricingRequest) {

        return getPrices(getPricingRequest(pricingRequest, RoomPricingType.CalendarPricing),
                pricingRequest.getSource(), pricingRequest);

    }

    @Override
    public List<AuroraPriceResponse> getCalendarPricesV2(AuroraPriceRequest pricingRequest) {

        return getPrices(getPricingV2Request(pricingRequest, RoomPricingType.CalendarPricing),
                pricingRequest.getSource(), pricingRequest);

    }

    @Override
    public List<AuroraPriceResponse> getIterableCalendarPricesV2(AuroraPriceRequest pricingRequest) {

        return getPrices(getPricingV2Request(pricingRequest, RoomPricingType.CalendarPricing),
                pricingRequest.getSource(), pricingRequest);

    }

    /**
     * Constructs aurora pricing request based on the input parameters
     * available.
     *
     * @param pricingRequest Pricing Request
     * @param type           Pricing type
     * @return Aurora Pricing Request object
     */
    private GetRoomPricingAndAvailabilityExRequest getPricingRequest(AuroraPriceRequest pricingRequest,
                                                                     RoomPricingType type) {

        GetRoomPricingAndAvailabilityExRequest request = MessageFactory.createGetRoomPricingAndAvailabilityExRequest();
        request.setPropertyId(pricingRequest.getPropertyId());
        request.setCheckInDate(DateUtil.toDate(pricingRequest.getCheckInDate()));
        request.setCheckOutDate(DateUtil.toDate(pricingRequest.getCheckOutDate()));
        request.setNumAdults(pricingRequest.getNumGuests());
        request.setCustomerId(pricingRequest.getCustomerId());
        request.setPricingType(type);
        if (!CollectionUtils.isEmpty(pricingRequest.getRoomTypeIds())) {
            request.setRoomTypeIds(
                    pricingRequest.getRoomTypeIds().toArray(new String[pricingRequest.getRoomTypeIds().size()]));
        }
        if (StringUtils.isNotEmpty(pricingRequest.getProgramId()) && !programCacheService.isProgramPO(pricingRequest.getProgramId())) {
            request.setProgramId(pricingRequest.getProgramId());
            request.setProgramRate(pricingRequest.isProgramRate());
        }
        request.setEnableMRDPricing(pricingRequest.isEnableMrd());
        if (!CollectionUtils.isEmpty(pricingRequest.getAuroraItineraryIds())) {
            request.setItineraryIds(pricingRequest.getAuroraItineraryIds().stream().toArray(String[]::new));
        }

        return request;
    }

    /**
     * update the availability request object for the additional fields.
     *
     * @param pricingRequest Pricing Request
     * @param type           Pricing type
     * @return Aurora Pricing Request object
     */
    private GetRoomPricingAndAvailabilityExRequest getPricingV2Request(AuroraPriceRequest pricingRequest,
                                                                       RoomPricingType type) {

        GetRoomPricingAndAvailabilityExRequest request = getPricingRequest(pricingRequest, type);

        request.setNumChildren(pricingRequest.getNumChildren());
        request.setIgnoreChannelMargins(pricingRequest.isIgnoreChannelMargins());

        if (pricingRequest.getNumRooms() > 0) {
            request.setNumRooms(pricingRequest.getNumRooms());
        }
        if (StringUtils.isNotEmpty(pricingRequest.getOperaConfirmationNumber())) {
            request.setOperaConfirmationNumber(pricingRequest.getOperaConfirmationNumber());
        }
        if (StringUtils.isNotEmpty(pricingRequest.getCustomerDominantPlay())) {
            request.setCustomerDominantPlay(DominantPlayType.valueOf(pricingRequest.getCustomerDominantPlay()));
        }
        request.setCustomerRank(pricingRequest.getCustomerRank());

        return request;
    }

    /**
     * Invokes aurora pricing API for the request provided and returns pricing
     * response returned based on input criteria.
     *
     * @param request Aurora pricing request object
     * @param source  Source or channel identifier
     * @return List of aurora pricing response
     */

    @LogExecutionTime
    public List<AuroraPriceResponse> getPrices(GetRoomPricingAndAvailabilityExRequest request, String source, AuroraPriceRequest pricingRequest) {

        log.debug("Sent the request to RoomPricingAndAvailability as : {}", request.toJsonString());
        log.debug("Source: {}", source);


        GetRoomPricingAndAvailabilityResponse response = getAuroraClient(source)
                .getRoomPricingAndAvailabilityEx(request);

        log.debug("Received the response from RoomPricingAndAvailability as : {}", response.toJsonString());

        if (null == response.getPrices()) {
            return new ArrayList<>();
        }

        if (StringUtils.equalsIgnoreCase(appProperties.getTcolvPropertyId(), pricingRequest.getPropertyId())) {
            List<AuroraPriceResponse> auroraPrices = populatePriceList(response);
            populateTCOLVBasePriceList(auroraPrices, pricingRequest, pricingRequest.getSource());
            return auroraPrices;
        }
        return populatePriceList(response);
    }

    @LogExecutionTime
    public AuroraPricesResponse getRoomPricesV2(AuroraPriceRequest pricingRequest) {
        AuroraPricesResponse auroraPrices = getPricesAndMrdFlag(getPricingV2Request(pricingRequest, RoomPricingType.TripPricing), pricingRequest.getSource());
        if (StringUtils.equalsIgnoreCase(appProperties.getTcolvPropertyId(), pricingRequest.getPropertyId())) {
            populateTCOLVBasePriceList(auroraPrices.getAuroraPrices(), pricingRequest, pricingRequest.getSource());
        }
        return auroraPrices;
    }

    /**
     * Invokes aurora pricing API for the request provided and returns pricing
     * response returned based on input criteria.
     *
     * @param request Aurora pricing request object
     * @param source  Source or channel identifier
     * @return List of aurora pricing response
     */

    @LogExecutionTime
    public AuroraPricesResponse getPricesAndMrdFlag(GetRoomPricingAndAvailabilityExRequest request, String source) {

        log.debug("Sent the request to RoomPricingAndAvailability as : {}", request.toJsonString());
        log.debug("Source: {}", source);

        AuroraPricesResponse auroraPrices = new AuroraPricesResponse();

        try {
            GetRoomPricingAndAvailabilityResponse response = getAuroraClient(source)
                    .getRoomPricingAndAvailabilityEx(request);

            log.debug("Received the response from RoomPricingAndAvailability as : {}", response.toJsonString());

            if (null == response.getPrices()) {
                auroraPrices.setMrdPricing(response.getIsMrdPricing());
                auroraPrices.setAuroraPrices(new ArrayList<>());
                return auroraPrices;
            }

            auroraPrices.setMrdPricing(response.getIsMrdPricing());

            List<AuroraPriceResponse> priceList = populatePriceList(response);
            auroraPrices.setAuroraPrices(priceList);


        } catch (EAuroraException ex) {
            log.error("Exception while trying to get room price and availability : ", ex);
            if (ServiceConstant.CUST_ID_NOT_FOUND_ERROR_MSG_PATTERN.matcher(ex.getMessage()).find()) {
                throw new BusinessException(ErrorCode.INVALID_GSE_CUSTOMER_ID);
            } else {
                handleAuroraError(ex);
            }
        }

        return auroraPrices;
    }

    /**
     * @param priceList
     * @param auroraPriceRequest
     * @param source
     */
    private void populateTCOLVBasePriceList(List<AuroraPriceResponse> priceList, AuroraPriceRequest auroraPriceRequest, String source) {
        try {
            GetRoomPricingAndAvailabilityExRequest request = getPricingV2Request(auroraPriceRequest, RoomPricingType.TripPricing);
            request.setProgramId(appProperties.getBaseTCOLVRatePlan());
            request.setEnableMRDPricing(false);
            log.debug("Sent the request to RoomPricingAndAvailability for TCOLV baseprice as : {}", request.toJsonString());
            log.debug("Source: {}", source);
            GetRoomPricingAndAvailabilityResponse response = getAuroraClient(source)
                    .getRoomPricingAndAvailabilityEx(request);

            log.debug("Received the response from RoomPricingAndAvailability for TCOLV baseprice as : {}", response.toJsonString());

            //Create a map with Room Type and Map of Date and base price
            Map<String, Map<Date, Double>> roomTypeMap = new HashMap<>();
            // Loop Through the price response
            for (RoomPrice price : response.getPrices()) {
                //Filter sold out dates
                if (Double.compare(price.getPrice(), -1.0) != 0 && (null == System.getenv("tcolvDefaultRateAmount")
                        || (null != System.getenv("tcolvDefaultRateAmount")
                        && price.getPrice() < Double.parseDouble(System.getenv("tcolvDefaultRateAmount"))))) {
                    Map<Date, Double> datePriceMap = new HashMap<>();
                    //Create a map of date and respective price
                    datePriceMap.put(price.getDate(), price.getPrice());
                    //If the room type is already seen before merge the maps.
                    if (roomTypeMap.containsKey(price.getRoomType())) {
                        Map<Date, Double> existingDatePriceMap = roomTypeMap.get(price.getRoomType());
                        existingDatePriceMap.putAll(datePriceMap);
                    } else {
                        //Room type doesn't exist in the map
                        roomTypeMap.put(price.getRoomType(), datePriceMap);
                    }
                }
            }

            //Setting the base price to actual response of the requested pricing
            for (AuroraPriceResponse priceResponse : priceList) {
                //filter sold out dates
                if (!StringUtils.equalsIgnoreCase(AvailabilityStatus.SOLDOUT.toString(),
                        priceResponse.getStatus().toString())) {
                    //If the data for any of the room types is present
                    if (!CollectionUtils.isEmpty(roomTypeMap)) {
                        //Get the price for the given date and set it to response.
                        Map<Date, Double> datePriceMap = roomTypeMap.get(priceResponse.getRoomTypeId());
                        if (!CollectionUtils.isEmpty(datePriceMap) && null != datePriceMap.get(priceResponse.getDate())) {
                            priceResponse.setBasePrice(datePriceMap.get(priceResponse.getDate()));
                        }
                    }
                }
            }

        } catch (Exception ex) {
            //Ignore any exceptions, should not stop the entire flow.
            log.error("Exception while trying to get TCOLV base room price and availability : ", ex);
        }
    }

    private List<AuroraPriceResponse> populatePriceList(GetRoomPricingAndAvailabilityResponse response) {

        Map<String, Boolean> programPoMap = new HashMap<>();

        List<AuroraPriceResponse> pricesList = new LinkedList<>();
        // Convert response from aurora into application specific object
        for (RoomPrice price : response.getPrices()) {
            AuroraPriceResponse priceResponse = new AuroraPriceResponse();
            priceResponse.setDate(price.getDate());
            priceResponse.setComp(price.getIsComp());
            priceResponse.setCloseToArrival(price.getIsCTA());
            if ((Double.compare(price.getPrice(), -1.0) == 0) || (null != System.getenv("tcolvDefaultRateAmount") &&
                    price.getPrice() >= Double.parseDouble(System.getenv("tcolvDefaultRateAmount")))) {
                priceResponse.setStatus(AvailabilityStatus.SOLDOUT);
                priceResponse.setUnavailabilityReason("SO");
                // Setting unavailability reason if price is -1 and reason exists
                if (price.getUnavailabilityReason() != null) {
                    priceResponse.setUnavailabilityReason(price.getUnavailabilityReason().name());
                }
            } else {
                priceResponse.setStatus(AvailabilityStatus.AVAILABLE);
            }
            priceResponse.setPropertyId(price.getPropertyId());
            priceResponse.setRoomTypeId(price.getRoomType());
            priceResponse.setProgramId(price.getProgramId());
            priceResponse.setBasePrice(price.getBasePrice());
            priceResponse.setDiscountedPrice(price.getPrice());
            priceResponse.setBaseMemberPrice(price.getMemberBasePrice());
            priceResponse.setDiscountedMemberPrice(price.getMemberPrice());
            priceResponse.setMemberProgramId(price.getMemberProgramId());
            priceResponse.setResortFee(price.getResortFeePrice());
            priceResponse.setPricingRuleId(price.getPricingRuleId());
            priceResponse.setProgramIdIsRateTable(price.getProgramIdIsRateTable());

            String programId = price.getProgramId();
            if (programPoMap.containsKey(programId)) {
                priceResponse.setPOApplicable(programPoMap.get(programId));
            } else {
                boolean isPo = programCacheService.isProgramPO(programId);
                programPoMap.put(programId, isPo);
                priceResponse.setPOApplicable(isPo);
            }

            pricesList.add(priceResponse);
        }
        return pricesList;
    }

    @Override
    @LogExecutionTime
    public List<AuroraPriceV3Response> getLOSBasedCalendarPrices(AuroraPriceV3Request pricingRequest) {
        List<AuroraPriceV3Response> pricesList = new LinkedList<>();
        try {
            GetRoomPricingAndAvailabilityLOSCalendarRequest request = getLOSCalendarRequest(pricingRequest);
            log.debug("Sent the request to RoomPricingAndAvailabilityCalendarPrice as : {}", request.toJsonString());
            log.debug("Source: {}", pricingRequest.getSource());

            GetRoomPricingAndAvailabilityLOSCalendarResponse response = getAuroraClient(pricingRequest.getSource())
                    .getRoomPricingAndAvailabilityCalendarPrice(request);

            log.debug("Received the response from RoomPricingAndAvailabilityCalendarPrice as : {}",
                    response.toJsonString());

            pricesList = populatePriceV3List(pricingRequest, response);
        } catch (EAuroraException ex) {
            if (ServiceConstant.CUST_ID_NOT_FOUND_ERROR_MSG_PATTERN.matcher(ex.getMessage()).find()) {
                throw new BusinessException(ErrorCode.INVALID_GSE_CUSTOMER_ID);
            } else {
                handleAuroraError(ex);
            }
        }

        return pricesList;
    }

    @Override
    public List<AuroraPriceResponse> getGridAvailabilityForSoldOut(AuroraPriceRequest auroraPriceRequest) {
        throw new UnsupportedOperationException("getGridAvailabilityForSoldOut not supported by GSE flow.");
    }

    private GetRoomPricingAndAvailabilityLOSCalendarRequest getLOSCalendarRequest(AuroraPriceV3Request pricingRequest) {
        GetRoomPricingAndAvailabilityLOSCalendarRequest request = MessageFactory
                .createGetRoomPricingAndAvailabilityLOSCalendarRequest();

        request.setPropertyId(pricingRequest.getPropertyId());
        request.setCheckInDate(DateUtil.toDate(pricingRequest.getCheckInDate()));
        request.setCheckOutDate(DateUtil.toDate(pricingRequest.getCheckOutDate()));
        request.setNumAdults(pricingRequest.getNumGuests());
        request.setCustomerId(pricingRequest.getCustomerId());
        if (!CollectionUtils.isEmpty(pricingRequest.getRoomTypeIds())) {
            request.setRoomTypeIds(
                    pricingRequest.getRoomTypeIds().toArray(new String[pricingRequest.getRoomTypeIds().size()]));
        }
        if (StringUtils.isNotEmpty(pricingRequest.getProgramId())) {
            request.setProgramId(pricingRequest.getProgramId());
            request.setProgramRate(pricingRequest.isProgramRate());
        }
        if (!CollectionUtils.isEmpty(pricingRequest.getAuroraItineraryIds())) {
            request.setItineraryIds(pricingRequest.getAuroraItineraryIds().stream().toArray(String[]::new));
        }

        request.setNumChildren(pricingRequest.getNumChildren());
        request.setIgnoreChannelMargins(pricingRequest.isIgnoreChannelMargins());

        if (pricingRequest.getNumRooms() > 0) {
            request.setNumRooms(pricingRequest.getNumRooms());
        }
        if (StringUtils.isNotEmpty(pricingRequest.getOperaConfirmationNumber())) {
            request.setOperaConfirmationNumber(pricingRequest.getOperaConfirmationNumber());
        }
        if (StringUtils.isNotEmpty(pricingRequest.getCustomerDominantPlay())) {
            request.setCustomerDominantPlay(DominantPlayType.valueOf(pricingRequest.getCustomerDominantPlay()));
        }
        request.setCustomerRank(pricingRequest.getCustomerRank());

        request.setTripLength(pricingRequest.getTripLength());

        return request;
    }

    private List<AuroraPriceV3Response> populatePriceV3List(AuroraPriceV3Request request, GetRoomPricingAndAvailabilityLOSCalendarResponse calLOSresponse) {
        List<AuroraPriceV3Response> pricesList = new LinkedList<>();
        for (CalendarRoomPrice roomPrice : calLOSresponse.getCalendarPricesEmptyIfNull()) {
            AuroraPriceV3Response priceResponse = new AuroraPriceV3Response();
            if (roomPrice.getUnavailabilityReason() != null || (null != System.getenv("tcolvDefaultRateAmount") &&
                    roomPrice.getTotalNightlyTripPrice() >= Double.parseDouble(System.getenv("tcolvDefaultRateAmount")))) {
                priceResponse.setStatus(AvailabilityStatus.SOLDOUT);
                if (null != roomPrice.getUnavailabilityReason()) {
                    priceResponse.setUnavailabilityReason(roomPrice.getUnavailabilityReason().name());
                }
            } else {
                priceResponse.setStatus(AvailabilityStatus.AVAILABLE);
            }
            priceResponse.setDate(roomPrice.getDate());
            priceResponse.setRoomTypeId(roomPrice.getRoomTypeId());
            List<TripDetailsV3> listOfTripDetails = new LinkedList<>();
            double totalNightlyTripBasePrice = 0.0;
            for (DailyPrice dailyPrice : roomPrice.getTripDetailsEmptyIfNull()) {
                TripDetailsV3 tripDetails = new TripDetailsV3();
                tripDetails.setDate(dailyPrice.getDate());
                if (null != System.getenv("tcolvDefaultRateAmount") &&
                        roomPrice.getTotalNightlyTripPrice() >= Double.parseDouble(System.getenv("tcolvDefaultRateAmount"))) {
                    tripDetails.setProgramId(null);
                } else {
                    tripDetails.setProgramId(dailyPrice.getProgramId());
                }
                tripDetails.setComp(dailyPrice.getIsComp());
                totalNightlyTripBasePrice += dailyPrice.getBasePrice() != -1 ? dailyPrice.getBasePrice()
                        : dailyPrice.getPrice();
                listOfTripDetails.add(tripDetails);
            }
            priceResponse.setTripDetails(listOfTripDetails);
            priceResponse.setPOApplicable(roomPrice.getIsPOApplicable());
            if (null != System.getenv("tcolvDefaultRateAmount") &&
                    roomPrice.getTotalNightlyTripPrice() >= Double.parseDouble(System.getenv("tcolvDefaultRateAmount"))) {
                priceResponse.setUnavailabilityReason("SO");
                priceResponse.setTotalNightlyTripPrice(-1.0);
                priceResponse.setTotalNightlyTripBasePrice(-1.0);
                priceResponse.setPricingMode(PricingModes.BEST_AVAILABLE);
            } else {
                priceResponse.setTotalNightlyTripPrice(roomPrice.getTotalNightlyTripPrice());
                priceResponse.setTotalNightlyTripBasePrice(totalNightlyTripBasePrice);
            }
            if (null == priceResponse.getPricingMode()) {
                if (StringUtils.isNotEmpty(request.getProgramId()) && !listOfTripDetails.isEmpty()
                        && request.getProgramId().equals(listOfTripDetails.get(0).getProgramId())) {
                    priceResponse.setPricingMode(PricingModes.PROGRAM);
                } else if (roomPrice.getIsPOApplicable()) {
                    priceResponse.setPricingMode(PricingModes.PERPETUAL);
                } else {
                    priceResponse.setPricingMode(PricingModes.BEST_AVAILABLE);
                }
            }
            pricesList.add(priceResponse);
        }

        return pricesList;

    }
}