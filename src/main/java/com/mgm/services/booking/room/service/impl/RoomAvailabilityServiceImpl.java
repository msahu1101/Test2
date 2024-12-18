package com.mgm.services.booking.room.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.PriceItemized;
import com.mgm.services.booking.room.model.ProgramStartingPrice;
import com.mgm.services.booking.room.model.RoomTripPrice;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.RoomAvailabilityRequest;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.RatePlanResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityResponse;
import com.mgm.services.booking.room.service.RoomAvailabilityService;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for exposing services to fetching room prices.
 * 
 */
@Component
@Log4j2
@Primary
public class RoomAvailabilityServiceImpl extends BasePriceServiceImpl implements RoomAvailabilityService {

    @Autowired
    private RoomPriceDAO pricingDao;

    @Autowired
    private RoomProgramService programService;
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.RoomPricingAvailabilityService#
     * getRoomPrices(com.mgm.services.booking.room.model.request.
     * RoomAvailabilityRequest)
     */
    @Override
    public Set<RoomAvailabilityResponse> getRoomPrices(RoomAvailabilityRequest availabilityRequest) {

        // Perform validation checks if program is available
        validateProgram(programService, availabilityRequest);

        AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraPriceRequest(availabilityRequest);

        List<AuroraPriceResponse> pricesList = pricingDao.getRoomPrices(request);

        return getAvailableResponse(pricesList, availabilityRequest, true);
    }

    @Override
    public Optional<RoomAvailabilityResponse> getLowestRoomPrice(RoomAvailabilityRequest availabilityRequest) {

        // Skipping program eligibility check here since this call is meant for
        // internal invocation

        AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraPriceRequest(availabilityRequest);
        List<AuroraPriceResponse> pricesList = pricingDao.getRoomPrices(request);

        Set<RoomAvailabilityResponse> availableRooms = getAvailableResponse(pricesList, availabilityRequest, true);

        // Set is already sorted based on price. So, returning first item for
        // lowest priced room
        if (!CollectionUtils.isEmpty(availableRooms)) {
            return Optional.of(availableRooms.iterator().next());
        }

        return Optional.empty();
    }

    /**
     * Iterates through list of prices, removes unavailable room types, and
     * converts the list of availabilities to include the itemized prices.'
     * 
     * @param prices
     *            List of price responses
     * @param availabilityRequest
     *            Room availability request
     * @return Availability response by room type
     */
    protected Set<RoomAvailabilityResponse> getAvailableResponse(List<AuroraPriceResponse> prices,
            RoomAvailabilityRequest availabilityRequest, boolean programCheck) {

        // Iterate through list of availability and group by room type
        Map<String, List<AuroraPriceResponse>> roomMap = new HashMap<>();
        Set<String> unavailableRoomTypes = new TreeSet<>();
        prices.forEach(price -> {
            if (price.getStatus().equals(AvailabilityStatus.AVAILABLE)) {
                if (roomMap.containsKey(price.getRoomTypeId())) {
                    roomMap.get(price.getRoomTypeId()).add(price);
                } else {
                    List<AuroraPriceResponse> priceList = new ArrayList<>();
                    priceList.add(price);
                    roomMap.put(price.getRoomTypeId(), priceList);
                }
            } else {
                unavailableRoomTypes.add(price.getRoomTypeId());
            }

        });

        log.info("No of unavailable rooms {}", unavailableRoomTypes.size());
        // Removing unavailable room types
        for (String roomTypeId : unavailableRoomTypes) {
            roomMap.remove(roomTypeId);
        }

        return convertAvailabilityResponse(roomMap, availabilityRequest, programCheck);
    }

    /**
     * Converts the list of availabilities to include the itemized prices for
     * each room along with resort fee, total prices and averages.
     * 
     * @param roomMap
     *            List of availabilities
     * @param availabilityRequest
     *            Availability request
     * @param programCheck
     *            Flag for strict program check
     * @return Availability response by room type
     */
    protected Set<RoomAvailabilityResponse> convertAvailabilityResponse(Map<String, List<AuroraPriceResponse>> roomMap,
            RoomAvailabilityRequest availabilityRequest, boolean programCheck) {

        // Using TreeSet to sort room using compareTo function
        Set<RoomAvailabilityResponse> responseList = new TreeSet<>();
        String programId = availabilityRequest.getProgramId();
        long customerId = availabilityRequest.getCustomerId();

        long noOfNights = Duration.between(availabilityRequest.getCheckInDate().atStartOfDay(),
                availabilityRequest.getCheckOutDate().atStartOfDay()).toDays();

        removePartialRooms(roomMap, noOfNights);

        for (List<AuroraPriceResponse> tripPrices : roomMap.values()) {

            // If program is applied and none of the nights includes the
            // program, ignore the room. programCheck flag is to avoid program
            // existence check as needed
            if (programCheck && StringUtils.isNotEmpty(programId) && !isProgramIncluded(tripPrices, programId)) {
                continue;
            }
            double price = 0;
            double discPrice = 0;
            double memberPrice = 0;
            double memberDiscPrice = 0;
            double resortFee = 0;

            List<PriceItemized> itemizedPriceList = new ArrayList<>();
            List<PriceItemized> itemizedMemberPriceList = new ArrayList<>();
            String roomTypeId = null;

            // Construct itemized prices and member prices for each room along
            // with total of prices
            for (AuroraPriceResponse priceResponse : tripPrices) {
                roomTypeId = priceResponse.getRoomTypeId();

                itemizedPriceList.add(getItemizedPrice(priceResponse));

                itemizedMemberPriceList.add(getItemizedMemberPrice(priceResponse));

                price += priceResponse.getBasePrice();
                discPrice += priceResponse.isComp() ? 0 : priceResponse.getDiscountedPrice();
                memberPrice += priceResponse.getBaseMemberPrice();
                memberDiscPrice += priceResponse.getDiscountedMemberPrice();
                resortFee += priceResponse.getResortFee();
            }

            RoomAvailabilityResponse availabilityResponse = new RoomAvailabilityResponse();
            availabilityResponse.setRoomTypeId(roomTypeId);
            // Set property level resort fee
            availabilityResponse.setResortFee(resortFee / noOfNights);
            // Include base price totals and averages
            availabilityResponse.setPrice(getRoomTripPrice(noOfNights, price, discPrice, itemizedPriceList));

            // Include member price only if it's lower than transient price
            // Using >= zero check here as aurora returns -1 for sold-out or
            // unavailable
            if (isMemberPriceApplicable(customerId, programId, memberDiscPrice, discPrice)) {
                availabilityResponse.setMemberPrice(
                        getRoomTripMemberPrice(noOfNights, memberPrice, memberDiscPrice, itemizedMemberPriceList));
            }

            responseList.add(availabilityResponse);
        }

        return responseList;
    }

    /**
     * If the room map doesn't have availability for all days for the trip,
     * remove those rooms as those rooms are partially sold-out/
     * 
     * @param roomMap
     *            Map of room and availability
     * @param noOfNights
     *            Number of nights in the trip
     */
    private void removePartialRooms(Map<String, List<AuroraPriceResponse>> roomMap, long noOfNights) {

        Map<String, List<AuroraPriceResponse>> roomMapCopy = new HashMap<>(roomMap);

        for (Map.Entry<String, List<AuroraPriceResponse>> roomEntry : roomMapCopy.entrySet()) {

            if (roomEntry.getValue().size() < noOfNights) {
                roomMap.remove(roomEntry.getKey());
            }
        }

    }

    /**
     * Checks if its applicable to return member price. Include member price
     * only if it's lower than transient price Using >= zero check here as
     * aurora returns -1 for sold-out or unavailable
     * 
     * @param customerId
     *            Customer Identifier
     * @param programId
     *            Program Identifier
     * @param memberDiscPrice
     *            Discounted price for member
     * @param discPrice
     *            Discounted price for transient member
     * @return
     */
    protected boolean isMemberPriceApplicable(long customerId, String programId, double memberDiscPrice,
            double discPrice) {
        return customerId < 0 && StringUtils.isEmpty(programId) && memberDiscPrice >= 0 && memberDiscPrice < discPrice;
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
    protected boolean isProgramIncluded(List<AuroraPriceResponse> tripPrices, String programId) {
        for (AuroraPriceResponse price : tripPrices) {
            if (price.getProgramId().equals(programId)) {
                return true;
            }
        }
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
    protected void populatePlanMap(AuroraPriceResponse price, Map<String, List<AuroraPriceResponse>> ratePlanMap) {

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
     * Builds <i>RoomTripPrice</i> object with the given params and returns.
     */
    private RoomTripPrice getRoomTripMemberPrice(long noOfNights, double memberPrice, double memberDiscPrice,
            List<PriceItemized> itemizedMemberPriceList) {
        RoomTripPrice tripMemberPrice = new RoomTripPrice();
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
     * Builds <i>RoomTripPrice</i> object with the given params and returns.
     */
    private RoomTripPrice getRoomTripPrice(long noOfNights, double price, double discPrice,
            List<PriceItemized> itemizedPriceList) {
        RoomTripPrice tripPrice = new RoomTripPrice();
        tripPrice.setBaseSubtotal(price);
        tripPrice.setDiscountedSubtotal(discPrice);
        tripPrice.setBaseAveragePrice(price / noOfNights);
        tripPrice.setDiscountedAveragePrice(discPrice / noOfNights);
        tripPrice.setItemized(itemizedPriceList);
        if (Double.compare(discPrice, 0) == 0) {
            tripPrice.setComp(true);
        }
        return tripPrice;
    }

    /**
     * Builds <i>PriceItemized</i> object by fetching it from passed <i>priceResponse</i> object and returns.
     */
    private PriceItemized getItemizedMemberPrice(AuroraPriceResponse priceResponse) {
        PriceItemized itemizedMemberPrice = new PriceItemized();
        itemizedMemberPrice.setDate(priceResponse.getDate());
        itemizedMemberPrice.setProgramId(priceResponse.getMemberProgramId());
        itemizedMemberPrice.setBasePrice(priceResponse.getBaseMemberPrice());
        itemizedMemberPrice.setDiscountedPrice(priceResponse.getDiscountedMemberPrice());
        return itemizedMemberPrice;
    }

    /**
     * Builds <i>PriceItemized</i> object by fetching it from passed <i>priceResponse</i> object and returns.
     */
    private PriceItemized getItemizedPrice(AuroraPriceResponse priceResponse) {
        PriceItemized itemizedPrice = new PriceItemized();
        itemizedPrice.setDate(priceResponse.getDate());
        itemizedPrice.setComp(priceResponse.isComp());
        itemizedPrice.setProgramId(priceResponse.getProgramId());
        itemizedPrice.setBasePrice(priceResponse.getBasePrice());
        itemizedPrice.setDiscountedPrice(priceResponse.getDiscountedPrice());
        return itemizedPrice;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.RoomAvailabilityService#
     * getRatePlans(com.mgm.services.booking.room.model.request.
     * RoomAvailabilityRequest)
     */
    @Override
    public List<RatePlanResponse> getRatePlans(RoomAvailabilityRequest request) {
        // Perform validation checks if program is available
        validateProgram(programService, request);

        // Saving the programId and fetching rate plans to see if selected
        // program is already part of rate plans
        String programId = request.getProgramId();
        request.setProgramId(null);

        AuroraPriceRequest auroraRequest = AuroraPriceRequestTransformer.getAuroraPriceRequest(request, true, false);

        // Iterate through list of availability and group by rate plan or
        // program id
        Map<String, List<AuroraPriceResponse>> ratePlanMap = new LinkedHashMap<>();
        pricingDao.getRoomPrices(auroraRequest).forEach(price -> populatePlanMap(price, ratePlanMap));

        // If requested program is not in rate plans response, request program
        // pricing and add it to plans
        if (StringUtils.isNotEmpty(programId) && !ratePlanMap.keySet().contains(programId)) {
            request.setProgramId(programId);

            auroraRequest = AuroraPriceRequestTransformer.getAuroraPriceRequest(request, false, true);
            pricingDao.getRoomPrices(auroraRequest).forEach(price -> populatePlanMap(price, ratePlanMap));
        }

        return populateRatePlanList(request, ratePlanMap);
    }

    protected List<RatePlanResponse> populateRatePlanList(RoomAvailabilityRequest request,
            Map<String, List<AuroraPriceResponse>> ratePlanMap) {
        // Iterate through rooms for each program, remove unavailable rooms, get
        // itemized price responses
        List<RatePlanResponse> ratePlanList = new LinkedList<>();
        ratePlanMap.keySet().forEach(key -> {
            RatePlanResponse response = new RatePlanResponse();
            response.setProgramId(key);
            log.debug("Program Id {}: No of rooms {}", key, ratePlanMap.get(key).size());
            response.setRooms(getAvailableResponse(ratePlanMap.get(key), request, false));
            // Set program starting price based on lowest priced room
            setProgramStartingPrice(response);
            ratePlanList.add(response);
        });

        return ratePlanList;
    }

    private void setProgramStartingPrice(RatePlanResponse response) {

        Optional<RoomAvailabilityResponse> responseOptional = response.getRooms().stream().findFirst();

        if (responseOptional.isPresent()) {
            RoomAvailabilityResponse roomResponse = responseOptional.get();
            ProgramStartingPrice startingPrice = new ProgramStartingPrice();
            startingPrice.setResortFee(roomResponse.getResortFee());

            // Use member price when available
            RoomTripPrice price = roomResponse.getPrice();
            if (null != roomResponse.getMemberPrice()) {
                price = roomResponse.getMemberPrice();
            }
            startingPrice.setBaseAveragePrice(price.getBaseAveragePrice());
            startingPrice.setDiscountedAveragePrice(price.getDiscountedAveragePrice());
            startingPrice.setBaseSubtotal(price.getBaseSubtotal());
            startingPrice.setDiscountedSubtotal(price.getDiscountedSubtotal());
            startingPrice.setComp(price.isComp());

            response.setStartingPrice(startingPrice);
        }
    }

}
