package com.mgm.services.booking.room.service.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.ResortPrice;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.ResortPriceV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.PerpetaulProgram;
import com.mgm.services.booking.room.model.response.PricingModes;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.transformer.RoomProgramValidateRequestTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

/**
 * Helper class for ResortPriceService v1 and v2 Services.
 * 
 */
@Component
@Log4j2
public class ResortPriceServiceHelper {

    @Autowired
    private AuroraProperties auroraProperties;
    
    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private RoomProgramCacheService programCacheService;

    @Autowired
    private RoomProgramService programService;

    @Autowired
    protected ReferenceDataDAOHelper referenceDataDAOHelper;

    /**
     * Find perpetual program id for a property from list of perpetual programs
     * fetched for the user. Empty string is returned when perpetual program for a
     * property is not found.
     * 
     * @param perpetualPrograms Perpetual programs
     * @param propertyId        Property Identifer
     * @return Returns perpetual program id for a property
     */
    public String findPerpetualProgramForProperty(List<PerpetaulProgram> perpetualPrograms, String propertyId) {

        Optional<PerpetaulProgram> program = perpetualPrograms.stream()
                .filter(p -> p.getPropertyId().equals(propertyId)).findFirst();
        if (program.isPresent()) {
            return program.get().getId();
        }

        return StringUtils.EMPTY;
    }
    
    /**
     * Method performs validation check to ensure program is available in cache and
     * user is applicable for the program. Also, this method checks for the
     * existence of redemption code, if the program is a my vegas program and
     * requested channel is not ice.
     * 
     * @param request Availability v2 request
     */
    public void validateProgram(ResortPriceV2Request request) {

        Optional<RoomProgram> roomProgram = Optional
                .ofNullable(programCacheService.getRoomProgram(request.getProgramId()));
        if (roomProgram.isPresent()) {
            RoomProgram program = roomProgram.get();

            request.setPropertyId(program.getPropertyId());
            request.setProgramId(program.getId());
            RoomProgramValidateRequest validateRequest = RoomProgramValidateRequestTransformer
                    .getRoomProgramValidateRequest(request, program);

            RoomProgramValidateResponse validateResponse = programService.validateProgramV2(validateRequest);
            if (!validateResponse.isEligible()) {
                log.info("Program {} is not applicable for the customer {}", validateRequest.getProgramId(),
                        validateRequest.getCustomerId());
                request.setProgramId(null);
            }

            if (StringUtils.isEmpty(request.getProgramId())) {
                throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
            }

            if (validateResponse.isMyvegas() && StringUtils.isEmpty(request.getRedemptionCode())
                    && !appProps.getBypassMyvegasChannels().contains(CommonUtil.getChannelHeader())) {
                throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
            }

        } else if (ACRSConversionUtil.isAcrsRatePlanGuid(request.getProgramId())
                || ACRSConversionUtil.isAcrsGroupCodeGuid(request.getProgramId())) {

            final String propertyCode = ACRSConversionUtil.getPropertyCode(request.getProgramId());
            request.setPropertyId(referenceDataDAOHelper.retrieveGsePropertyID(propertyCode));

        } else {
            throw new BusinessException(ErrorCode.OFFER_NOT_AVAILABLE);
        }
    }

    /**
     * Iterates through price list and groups prices by room type. Unavailable rooms
     * are excluded.
     * 
     * @param prices         List of prices returned by aurora
     * @param pricingRequest Pricing V2 request
     * @return Returns prices grouped by room type
     */
    public Map<String, List<AuroraPriceResponse>> groupPricesByRoom(List<AuroraPriceResponse> prices,
            ResortPriceV2Request pricingRequest) {

        // Iterate through list of availability and group by room type
        Map<String, List<AuroraPriceResponse>> roomMap = new HashMap<>();
        List<String> unavailableRoomTypes = new ArrayList<>();
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

        // If program is used, remove the rooms which are not priced under the
        // program
        roomMap.entrySet().forEach(item -> {
            if (!isProgramIncluded(item.getValue(), pricingRequest.getProgramId())) {
                unavailableRoomTypes.add(item.getKey());
            }
        });

        // Removing unavailable room types
        for (String roomTypeId : unavailableRoomTypes) {
            roomMap.remove(roomTypeId);
        }

        return roomMap;
    }

    /**
     * Calculates and returns the average prices across all days in the trip for
     * every room.
     * 
     * @param roomMap      Map of average prices information by room type
     * @param customerId   Customer Identifier
     * @param programId    Program Identifier
     * @param tripDuration Duration of the trip
     * @return Map of average prices per room type
     */
    public Map<String, ResortPriceResponse> averageOutPricesByRoom(Map<String, List<AuroraPriceResponse>> roomMap,
            long customerId, String programId, long tripDuration) {

        Map<String, ResortPriceResponse> roomPricingMap = new HashMap<>();
        for (List<AuroraPriceResponse> pricingList : roomMap.values()) {
            pricingList = getLowestPrice(pricingList, tripDuration);
            if (null == pricingList) {
                continue;
            }
            ResortPriceResponse priceResponse = new ResortPriceResponse();
            priceResponse.setProgramId(programId);

            // Total out prices across all days in room
            double price = 0;
            double discPrice = 0;
            double resortFee = 0;
            String propertyId = null;
            String roomTypeId = null;
            for (AuroraPriceResponse pricing : pricingList) {
                price += pricing.getBasePrice();
                discPrice += pricing.isComp() ? 0 : pricing.getDiscountedPrice();
                resortFee += pricing.getResortFee();
                propertyId = pricing.getPropertyId();
                roomTypeId = pricing.getRoomTypeId();
            }

            priceResponse.setPropertyId(propertyId);
            priceResponse.setStatus(AvailabilityStatus.AVAILABLE);
            

            if (Double.compare(discPrice, 0) == 0) {
                priceResponse.setComp(true);
            }

            int noOfNights = pricingList.size();
            priceResponse.setResortFee(resortFee / noOfNights);

            // Find average prices using no of nights
            ResortPrice basePrice = new ResortPrice();
            basePrice.setBaseAveragePrice(price / noOfNights);
            basePrice.setDiscountedAveragePrice(discPrice / noOfNights);
            priceResponse.setPrice(basePrice);

            roomPricingMap.put(roomTypeId, priceResponse);
        }

        return roomPricingMap;
    }
    
    /**
     * Calculates and returns the average prices across all days in the trip for
     * every room.
     * 
     * @param roomMap      Map of average prices information by room type
     * @param customerId   Customer Identifier
     * @param programId    Program Identifier
     * @param tripDuration Duration of the trip
     * @param pricingMode  Mode of pricing
     * @return Map of average prices per room type
     */
    public Map<String, ResortPriceResponse> averageOutPricesByRoomV2(Map<String, List<AuroraPriceResponse>> roomMap,
            long customerId, String programId, long tripDuration, PricingModes pricingMode) {

        Map<String, ResortPriceResponse> roomPricingMap = new HashMap<>();
        for (List<AuroraPriceResponse> pricingList : roomMap.values()) {
            if (!pricingMode.equals(PricingModes.PERPETUAL)) {
                pricingList = getLowestPrice(pricingList, tripDuration);
            }
            if (null == pricingList) {
                continue;
            }
            ResortPriceResponse priceResponse = new ResortPriceResponse();
            
            // Default of BEST_AVAILABLE which get overridden
            priceResponse.setPricingMode(PricingModes.BEST_AVAILABLE);
            if (StringUtils.isNotEmpty(programId)) {
                priceResponse.setPricingMode(PricingModes.PROGRAM);
                priceResponse.setProgramId(programId);
            }

            // Total out prices across all days in room
            double price = 0;
            double discPrice = 0;
            double resortFee = 0;
            double amtAftTax = 0;
            double baseAmtAftTax = 0;
            String propertyId = null;
            String roomTypeId = null;
            for (AuroraPriceResponse pricing : pricingList) {
                price += pricing.getBasePrice();
                discPrice += pricing.isComp() ? 0 : pricing.getDiscountedPrice();
                resortFee += pricing.getResortFee();
                amtAftTax += pricing.getAmtAftTax();
                baseAmtAftTax += pricing.getBaseAmtAftTax();
                propertyId = pricing.getPropertyId();
                roomTypeId = pricing.getRoomTypeId();
                
                if (pricing.isPOApplicable()) {
                    priceResponse.setPricingMode(PricingModes.PERPETUAL);
                    priceResponse.setProgramId(pricing.getProgramId());
                }
            }

            priceResponse.setPropertyId(propertyId);
            priceResponse.setStatus(AvailabilityStatus.AVAILABLE);
            

            if (Double.compare(discPrice, 0) == 0) {
                priceResponse.setComp(true);
            }

            int noOfNights = pricingList.size();
            priceResponse.setResortFee(resortFee / noOfNights);

            // Find average prices using no of nights
            ResortPrice basePrice = new ResortPrice();
            basePrice.setBaseAveragePrice(price / noOfNights);
            basePrice.setDiscountedAveragePrice(discPrice / noOfNights);
            priceResponse.setPrice(basePrice);
            priceResponse.setAmtAftTax(amtAftTax);
            priceResponse.setBaseAmtAftTax(baseAmtAftTax);
            priceResponse.setRoomTypeId(roomTypeId);

            roomPricingMap.put(roomTypeId, priceResponse);
        }

        return roomPricingMap;
    }

    /**
     * Since aurora pricing is made with enableMrd=true, room prices will be
     * repeated for each MRD program. This method iterates through multiple program
     * prices for a room and find the cheapest prices.
     * 
     * @param pricingList  List of prices for each room
     * @param tripDuration Duration of trip
     * @return Returns lowest price available for the room across programs
     */
    private List<AuroraPriceResponse> getLowestPrice(List<AuroraPriceResponse> pricingList, long tripDuration) {

        Map<String, List<AuroraPriceResponse>> programMap = new HashMap<>();
        pricingList.forEach(price -> {
            if (StringUtils.isNotEmpty(price.getProgramId())) {
                if (programMap.containsKey(price.getProgramId())) {
                    programMap.get(price.getProgramId()).add(price);
                } else {
                    List<AuroraPriceResponse> priceList = new LinkedList<>();
                    priceList.add(price);
                    programMap.put(price.getProgramId(), priceList);
                }
            }
        });

        double lowProgramPrice = -1;
        String lowPriceProgram = "";

        for (Map.Entry<String, List<AuroraPriceResponse>> entry : programMap.entrySet()) {
            List<AuroraPriceResponse> list = entry.getValue();
            double programDiscPrice = 0;

            // if not enough nights, ignore and proceed
            if (list.size() != tripDuration) {
                continue;
            }

            // find the lowest price for program
            for (AuroraPriceResponse price : list) {
                programDiscPrice += price.getDiscountedPrice();
            }

            // if the current program price is low, pick that
            if (lowProgramPrice == -1 || programDiscPrice < lowProgramPrice) {
                lowProgramPrice = programDiscPrice;
                lowPriceProgram = entry.getKey();
            }

        }

        return programMap.get(lowPriceProgram);
    }

    /**
     * Checks if the program is used at least for one night in the room.
     * 
     * @param prices    Prices for the trip
     * @param programId Program Id
     * @return Returns true if either program is not used or program is used at
     *         least for 1 night.
     */
    public boolean isProgramIncluded(List<AuroraPriceResponse> prices, String programId) {

        if (StringUtils.isNotEmpty(programId)) {
            for (AuroraPriceResponse price : prices) {
                if (price.getProgramId().equals(programId)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Groups the aurora price response into respective rate plan list.
     * 
     * @param price       Aurora price response
     * @param ratePlanMap Price responses grouped by program
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
     * Groups the aurora price response into respective rate plan list. Also,
     * helps to just keep rooms only from first rate plan in multi-rate queue
     * and ignore others
     * 
     * @param price
     *            Aurora price response
     * @param ratePlanMap
     *            Price responses grouped by program
     */
    public void populatePlanMap(AuroraPriceResponse price, Map<String, List<AuroraPriceResponse>> ratePlanMap,
            List<String> processedPropertyList) {

        if (StringUtils.isNotEmpty(price.getProgramId())) {
            if (ratePlanMap.containsKey(price.getProgramId())) {
                ratePlanMap.get(price.getProgramId()).add(price);
            } else {
                List<AuroraPriceResponse> priceList = new LinkedList<>();
                priceList.add(price);
                if (!processedPropertyList.contains(price.getPropertyId())) {
                    ratePlanMap.put(price.getProgramId(), priceList);
                    processedPropertyList.add(price.getPropertyId());
                }
            }
        }

    }

    /**
     * Iterate through average prices for all rooms and finds lowest prices room by
     * property.
     * 
     * @param roomPricingMap Map of average prices information by room type
     * @return Map of lowest price room by property
     */
    public Map<String, ResortPriceResponse> findLowestPriceForProperty(List<ResortPriceResponse> priceList) {

        Map<String, ResortPriceResponse> resortPricingMap = new HashMap<>();
        if (!priceList.isEmpty()) {
            for (ResortPriceResponse pricingResponse : priceList) {
                String propertyId = pricingResponse.getPropertyId();
                if (resortPricingMap.containsKey(propertyId)) {
                    ResortPriceResponse existingPrice = resortPricingMap.get(propertyId);
                    if (pricingResponse.isComp() || pricingResponse.getPrice().getDiscountedAveragePrice() < existingPrice
                            .getPrice().getDiscountedAveragePrice()) {
                        resortPricingMap.put(propertyId, pricingResponse);
                    }
                } else {
                    resortPricingMap.put(propertyId, pricingResponse);
                }
            }
        }

        return resortPricingMap;
    }

    /**
     * Adds sold out properties information into the resorts list.
     * 
     * @param resortPricingMap Map of lowest price room by property
     */
    @Deprecated
    public void addSoldOutResorts(Map<String, ResortPriceResponse> resortPricingMap, List<RoomProgram> programList) {

        List<String> propertyIds = new ArrayList<>();
        if (programList.isEmpty()) {
            propertyIds = auroraProperties.getPropertyIds();
        } else {
            for (RoomProgram program : programList) {
                propertyIds.add(program.getPropertyId());
            }
        }

        for (String propertyId : propertyIds) {
            if (!resortPricingMap.containsKey(propertyId)) {
                ResortPriceResponse priceResponse = new ResortPriceResponse();
                priceResponse.setPropertyId(propertyId);
                priceResponse.setStatus(AvailabilityStatus.SOLDOUT);
                resortPricingMap.put(propertyId, priceResponse);
            }
        }
    }
    
    /**
     * Adds sold out properties information into the resorts list.
     * 
     * @param resortPricingMap Map of lowest price room by property
     * @param programList list of programs requested
     */
    public void addSoldOutResortsForProgram(Map<String, ResortPriceResponse> resortPricingMap, List<RoomProgramBasic> programList) {
        List<String> propertyIds = new ArrayList<>();
        if (programList.isEmpty()) {
            propertyIds = auroraProperties.getPropertyIds();
        } else {
            for (RoomProgramBasic program : programList) {
                propertyIds.add(program.getPropertyId());
            }
        }
        for (String propertyId : propertyIds) {
            if (!resortPricingMap.containsKey(propertyId)) {
                ResortPriceResponse priceResponse = new ResortPriceResponse();
                priceResponse.setPropertyId(propertyId);
                priceResponse.setStatus(AvailabilityStatus.SOLDOUT);
                resortPricingMap.put(propertyId, priceResponse);
            }
        }
    }

    /**
     * Determines the list of the propertyIds for fallback pricing. Reads the list
     * of all propertyIds and look for them in the resortPricingMap, if not found
     * consider it for fallback pricing.
     * 
     * @param resortPricingMap
     *            map of propertyId and it's lowest priced room
     * @return list of propertyIds eligible for fallback pricing
     */
    public List<String> getPropertyIdsForFallBackPricing(Map<String, ResortPriceResponse> resortPricingMap) {

        List<String> propertyIdsForPricing = new ArrayList<>();

        List<String> fullPropertyIdList = auroraProperties.getPropertyIds();

        for (String propertyId : fullPropertyIdList) {
            if (!resortPricingMap.containsKey(propertyId)) {
                propertyIdsForPricing.add(propertyId);
            }
        }

        return propertyIdsForPricing;
    }

    /**
     * Filters out the availability to only include the resorts if requested.
     * 
     * @param resortPricingMap Map of lowest price room by property
     * @param propertyIdsList  List of properties passed in request
     */
    public void filterProperties(Map<String, ResortPriceResponse> resortPricingMap, List<String> propertyIdsList) {
        Optional<List<String>> propertyIds = Optional.ofNullable(propertyIdsList);
        propertyIds.ifPresent(properties -> {

            // Get all property ids and remove the ones which are not in
            // requested properties
            Set<String> allProperties = resortPricingMap.keySet();
            Set<String> filteredProperties = new TreeSet<>();
            for (String id : allProperties) {
                if (!properties.contains(id)) {
                    filteredProperties.add(id);
                }
            }

            for (String id : filteredProperties) {
                resortPricingMap.remove(id);
            }

        });
    }

    public void removeSoldOutResorts(Map<String, ResortPriceResponse> resortPricingMap) {
        resortPricingMap.entrySet().removeIf(entry -> entry.getValue().getStatus().equals(AvailabilityStatus.SOLDOUT));
    }
}
