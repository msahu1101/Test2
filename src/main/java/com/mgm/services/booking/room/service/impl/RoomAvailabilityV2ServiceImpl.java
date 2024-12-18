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
import com.mgm.services.booking.room.model.PriceV2Itemized;
import com.mgm.services.booking.room.model.RoomTripPriceV2;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV2Request;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.model.response.RatePlanV2Response;
import com.mgm.services.booking.room.model.response.RoomAvailabilityCombinedResponse;
import com.mgm.services.booking.room.model.response.RoomAvailabilityV2Response;
import com.mgm.services.booking.room.service.RoomAvailabilityV2Service;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.helper.RoomAvailabilityServiceHelper;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgmresorts.aurora.common.RoomUnavailabilityReason;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for exposing services to fetching room prices.
 * 
 */
@Component
@Log4j2
@Primary
public class RoomAvailabilityV2ServiceImpl extends BasePriceV2ServiceImpl implements RoomAvailabilityV2Service {

    @Autowired
    private RoomPriceDAO pricingDao;

    @Autowired
    private RoomProgramService programService;

    @Autowired
    private RoomAvailabilityServiceHelper availabilityServiceHelper;

    @Override
    public Set<RoomAvailabilityV2Response> getRoomPrices(RoomAvailabilityV2Request availabilityRequest) {
        // Perform validation checks if program is available
        // Skip the program validation for ice, this is a temporary fix only for v2
        if (!"ice".equalsIgnoreCase(availabilityServiceHelper.getChannelHeader())) {
            validateProgram(programService, availabilityRequest);
        }

        AuroraPriceRequest request = getAuroraPriceRequest(availabilityRequest);

        AuroraPricesResponse pricesList = pricingDao.getRoomPricesV2(request);

        return getAvailableResponse(pricesList.getAuroraPrices(), availabilityRequest);
    }

    @Override
    public RoomAvailabilityCombinedResponse getRoomAvailability(RoomAvailabilityV2Request request) {
        RoomAvailabilityCombinedResponse response = new RoomAvailabilityCombinedResponse();

        if (!request.isEnableMrd()) {
            response.setAvailability(getRoomPrices(request));
        } else {
            // Perform validation checks if program is available
            // Skip the program validation for ice, this is a temporary fix only for v2
            if (!"ice".equalsIgnoreCase(availabilityServiceHelper.getChannelHeader())) {
                validateProgram(programService, request);
            }

            AuroraPriceRequest auroraRequest = getAuroraPriceRequest(request);

            // Iterate through list of availability and group by rate plan or
            // program id
            Map<String, List<AuroraPriceResponse>> ratePlanMap = new LinkedHashMap<>();
            AuroraPricesResponse auroraPrices = pricingDao.getRoomPricesV2(auroraRequest);
            auroraPrices.getAuroraPrices().forEach(price -> availabilityServiceHelper.populatePlanMap(price, ratePlanMap));
            if (auroraPrices.isMrdPricing()) {
                response.setRatePlans(populateRatePlanList(request, ratePlanMap));
            } else {
                response.setAvailability(getAvailableResponse(auroraPrices.getAuroraPrices(), request));
            }
        }
        return response;
    }

    @Override
    public Optional<RoomAvailabilityV2Response> getLowestRoomPrice(RoomAvailabilityV2Request availabilityV2Request) {

        // Skipping program eligibility check here since this call is meant for
        // internal invocation
        AuroraPriceRequest request =
				getAuroraPriceRequest(availabilityV2Request).toBuilder()
						.needLowestPrice(true)
						.build();
		try {
			AuroraPricesResponse auroraPrices = pricingDao.getRoomPricesV2(request);
			Set<RoomAvailabilityV2Response> availableRooms = getAvailableResponse(auroraPrices.getAuroraPrices(), availabilityV2Request);
			// lowest priced room
	        if (!CollectionUtils.isEmpty(availableRooms)) {
	            return Optional.of(availableRooms.iterator().next());
	        }
		} catch (Exception e) {
			log.error("Exception occured while fetching resort pricing for {} property : ", availabilityV2Request.getPropertyId());
		}
        
        // Set is already sorted based on price. So, returning first item for
        
        return Optional.empty();
    }
    
    private AuroraPriceRequest getAuroraPriceRequest(RoomAvailabilityV2Request availabilityRequest) {
        AuroraPriceRequest request;
        if (StringUtils.isNotEmpty(availabilityRequest.getProgramId()) || StringUtils.isNotEmpty(availabilityRequest.getPromoCode())) {
            request = AuroraPriceRequestTransformer.getAuroraPriceV2Request(availabilityRequest, availabilityRequest.isExcludeNonOffer());
        } else {
            request = AuroraPriceRequestTransformer.getAuroraPriceV2Request(availabilityRequest);
        }
        return request;
    }

    /**
     * Converts the list of unavailable rooms to include the itemized rooms.
     *
     * @param unavailableRoomsMap
     *            List of price responses
     * @param availabilityRequest
     *            Availability request
     * @return Unavailable response by room type
     */
    protected Set<RoomAvailabilityV2Response> convertUnavailableResponse(
            Map<String, List<AuroraPriceResponse>> unavailableRoomsMap, RoomAvailabilityV2Request availabilityRequest) {

        // Using TreeSet to sort room using compareTo function
        Set<RoomAvailabilityV2Response> responseList = new TreeSet<>();
        
        long noOfNights = Duration.between(availabilityRequest.getCheckInDate().atStartOfDay(),
                availabilityRequest.getCheckOutDate().atStartOfDay()).toDays();

        for (Map.Entry<String, List<AuroraPriceResponse>> unavailableRoom : unavailableRoomsMap.entrySet()) {

            RoomAvailabilityV2Response availabilityResponse = new RoomAvailabilityV2Response();
            availabilityResponse.setRoomTypeId(unavailableRoom.getKey());
            availabilityResponse.setUnavailable(true);

            List<PriceV2Itemized> itemizedPriceList = new ArrayList<>();
            List<PriceV2Itemized> itemizedMemberPriceList = new ArrayList<>();

            // Construct itemized prices for each room along
            for (AuroraPriceResponse priceResponse : unavailableRoom.getValue()) {

                PriceV2Itemized itemizedPrice = new PriceV2Itemized();
                if (StringUtils.isNotBlank(priceResponse.getUnavailabilityReason())) {
                    itemizedPrice.setDate(priceResponse.getDate());
                    itemizedPrice.setUnavailabilityReason(priceResponse.getUnavailabilityReason());
                } else {
                    itemizedPrice = availabilityServiceHelper.getItemizedPriceV2(priceResponse);
                }

                itemizedPriceList.add(itemizedPrice);
                
                PriceV2Itemized itemizedMemberPrice = new PriceV2Itemized();
                if (Double.compare(priceResponse.getDiscountedMemberPrice(), 0) == 0
                        || Double.compare(priceResponse.getDiscountedMemberPrice(), -1) == 0) {
                    // Set the unavailabilityReason as SO if memberPrice is 0 or -1
                    itemizedMemberPrice.setDate(priceResponse.getDate());
                    itemizedMemberPrice.setUnavailabilityReason(RoomUnavailabilityReason.SO.name());
                } else {
                    itemizedMemberPrice = availabilityServiceHelper.getItemizedMemberPriceV2(priceResponse);
                }

                itemizedMemberPriceList.add(itemizedMemberPrice);
            }

            RoomTripPriceV2 tripPrice = new RoomTripPriceV2();
            tripPrice.setItemized(itemizedPriceList);
            availabilityResponse.setPrice(tripPrice);
            
            setMemberPrice(availabilityRequest, noOfNights, availabilityResponse, 0, 0,
                    itemizedMemberPriceList);
            
            responseList.add(availabilityResponse);

        }

        return responseList;
    }
    
    private void setMemberPrice(RoomAvailabilityV2Request availabilityRequest, long noOfNights,
            RoomAvailabilityV2Response availabilityResponse, double memberPrice, double memberDiscPrice,
            List<PriceV2Itemized> itemizedMemberPriceList) {
        if (availabilityServiceHelper.isMemberPriceApplicable(availabilityRequest.getCustomerId(),
                availabilityRequest.getProgramId())) {
            if (memberPrice > 0 && memberDiscPrice > 0) {
                availabilityResponse.setMemberPrice(
                        availabilityServiceHelper.getRoomTripMemberPriceV2(noOfNights, memberPrice, memberDiscPrice, itemizedMemberPriceList));
            } else {
                RoomTripPriceV2 tripMemberPrice = new RoomTripPriceV2();
                tripMemberPrice.setItemized(itemizedMemberPriceList);
                availabilityResponse.setMemberPrice(tripMemberPrice);
            }
        }
    }

    protected Set<RoomAvailabilityV2Response> getAvailableResponse(List<AuroraPriceResponse> prices,
			RoomAvailabilityV2Request availabilityRequest) {

		Map<String, List<AuroraPriceResponse>> allRoomsMap = new HashMap<>();
		prices.forEach(price -> {

			// Populates the map with all the rooms irrespective of their status
			if (allRoomsMap.containsKey(price.getRoomTypeId())) {
				allRoomsMap.get(price.getRoomTypeId()).add(price);
			} else {
				List<AuroraPriceResponse> priceList = new ArrayList<>();
				priceList.add(price);
				allRoomsMap.put(price.getRoomTypeId(), priceList);
			}

		});

		// Iterate through list of availability and group by available vs unavailable
		Map<String, List<AuroraPriceResponse>> availableRoomsMap = new HashMap<>();
		Map<String, List<AuroraPriceResponse>> unavailableRoomsMap = new HashMap<>();

		for (Map.Entry<String, List<AuroraPriceResponse>> roomEntry : allRoomsMap.entrySet()) {

			// As long as one of the night is unavailable, mark the room as unavailable
			boolean isUnavailable = roomEntry.getValue().stream()
					.anyMatch(price -> !price.getStatus().equals(AvailabilityStatus.AVAILABLE));

			if (isUnavailable) {
				unavailableRoomsMap.put(roomEntry.getKey(), roomEntry.getValue());
			} else {
				availableRoomsMap.put(roomEntry.getKey(), roomEntry.getValue());
			}

		}		
		log.debug("No of total rooms = {}, No of unavailable rooms = {}, No of available rooms = {}", allRoomsMap.size(),
				unavailableRoomsMap.size(), availableRoomsMap.size());

		Set<RoomAvailabilityV2Response> roomAvailability = convertAvailabilityResponse(availableRoomsMap,
				availabilityRequest);
		// Add sold-out rooms if clients requested it
		if (availabilityRequest.isIncludeSoldOutRooms()) {
			roomAvailability.addAll(convertUnavailableResponse(unavailableRoomsMap, availabilityRequest));
		}		
		return roomAvailability;
	}

    /**
     * Converts the list of availabilities to include the itemized prices for
     * each room along with resort fee, total prices and averages.
     * 
     * @param roomMap
     *            List of availabilities
     * @param availabilityRequest
     *            Availability request
     * @return Availability response by room type
     */
    protected Set<RoomAvailabilityV2Response> convertAvailabilityResponse(Map<String, List<AuroraPriceResponse>> roomMap,
            RoomAvailabilityV2Request availabilityRequest) {

        // Using TreeSet to sort room using compareTo function
        Set<RoomAvailabilityV2Response> responseList = new TreeSet<>();
        long noOfNights = Duration.between(availabilityRequest.getCheckInDate().atStartOfDay(),
                availabilityRequest.getCheckOutDate().atStartOfDay()).toDays();

        for (List<AuroraPriceResponse> tripPrices : roomMap.values()) {

            double price = 0;
            double memberPrice = 0;
            double discPrice = 0;
            double memberDiscPrice = 0;
            double resortFee = 0;
            double amtAftTax = 0;
            double baseAmtAftTax = 0;
            List<PriceV2Itemized> itemizedPriceList = new ArrayList<>();
            List<PriceV2Itemized> itemizedMemberPriceList = new ArrayList<>();
            String roomTypeId = null;
            RoomAvailabilityV2Response availabilityResponse = new RoomAvailabilityV2Response();

            // Construct itemized prices and member prices for each room along
            // with total of prices
            for (AuroraPriceResponse priceResponse : tripPrices) {
                roomTypeId = priceResponse.getRoomTypeId();

                itemizedPriceList.add(availabilityServiceHelper.getItemizedPriceV2(priceResponse));
                price += priceResponse.getBasePrice();
                discPrice += priceResponse.isComp() ? 0 : priceResponse.getDiscountedPrice();
                amtAftTax += priceResponse.getAmtAftTax();
                baseAmtAftTax += priceResponse.getBaseAmtAftTax();
                PriceV2Itemized itemizedMemberPrice = new PriceV2Itemized();
                if (Double.compare(priceResponse.getDiscountedMemberPrice(), -1) == 0) {
                    // Set the unavailabilityReason as SO if memberPrice is -1
                    itemizedMemberPrice.setDate(priceResponse.getDate());
                    itemizedMemberPrice.setUnavailabilityReason(RoomUnavailabilityReason.SO.name());
                } else {
                    itemizedMemberPrice = availabilityServiceHelper.getItemizedMemberPriceV2(priceResponse);
                    memberPrice += priceResponse.getBaseMemberPrice();
                    memberDiscPrice += priceResponse.getDiscountedMemberPrice();
                }

                itemizedMemberPriceList.add(itemizedMemberPrice);

                resortFee += priceResponse.getResortFee();
                
                if (priceResponse.isPOApplicable()) {
                    availabilityResponse.setPerpetualPricing(true);
                    availabilityResponse.setPoProgramId(priceResponse.getProgramId());
                }else if(availabilityRequest.isPackageFlow()) {
                	availabilityResponse.setBarProgramId(priceResponse.getProgramId());
                }
                
            }            

            availabilityResponse.setRoomTypeId(roomTypeId);
            // Set property level resort fee
            availabilityResponse.setResortFee(resortFee / noOfNights);
            // Include base price totals and averages
            availabilityResponse.setPrice(availabilityServiceHelper.getRoomTripPriceV2(noOfNights, price, discPrice, itemizedPriceList, resortFee));
            availabilityResponse.setAmtAftTax(amtAftTax);
            availabilityResponse.setBaseAmtAftTax(baseAmtAftTax);
            setMemberPrice(availabilityRequest, noOfNights, availabilityResponse, memberPrice, memberDiscPrice,
                    itemizedMemberPriceList);  
            
            responseList.add(availabilityResponse);
        }

        return responseList;
    }

    protected List<RatePlanV2Response> populateRatePlanList(RoomAvailabilityV2Request request,
            Map<String, List<AuroraPriceResponse>> ratePlanMap) {
        // Iterate through rooms for each program, remove unavailable rooms, get
        // itemized price responses
        List<RatePlanV2Response> ratePlanList = new LinkedList<>();
        ratePlanMap.keySet().forEach(key -> {
            RatePlanV2Response response = new RatePlanV2Response();
            response.setProgramId(key);
            log.debug("Program Id {}: No of rooms {}", key, ratePlanMap.get(key).size());
            response.setRooms(getAvailableResponse(ratePlanMap.get(key), request));
            // Set program starting price based on lowest priced room
            availabilityServiceHelper.setProgramStartingPrice(response);
            ratePlanList.add(response);
        });
        return ratePlanList;
    }

}
