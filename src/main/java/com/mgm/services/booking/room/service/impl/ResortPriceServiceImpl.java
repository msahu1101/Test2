package com.mgm.services.booking.room.service.impl;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;
import com.mgm.services.booking.room.model.request.ResortPriceRequest;
import com.mgm.services.booking.room.model.request.RoomAvailabilityRequest;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.PerpetaulProgram;
import com.mgm.services.booking.room.model.response.ResortPriceResponse;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.service.ResortPriceService;
import com.mgm.services.booking.room.service.RoomAvailabilityService;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.helper.ResortPriceServiceHelper;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgm.services.booking.room.transformer.PriceResponseTransformer;
import com.mgm.services.booking.room.transformer.RoomProgramValidateRequestTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class exposing services to fetch prices for resorts.
 * 
 */
@Component
@Log4j2
@Primary
public class ResortPriceServiceImpl implements ResortPriceService {

	@Autowired
	private AuroraProperties auroraProperties;

	@Autowired
	private RoomPriceDAO pricingDao;

	@Autowired
	private RoomProgramCacheService programCacheService;

	@Autowired
	private RoomProgramService programService;

	@Autowired
	private RoomAvailabilityService availabilityService;

    @Autowired
    private ResortPriceServiceHelper helper;

    /*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mgm.services.booking.room.service.ResortPriceService#getResortPrices(
	 * com.mgm.services.booking.room.model.request.ResortsPricingRequest)
	 */
	@Override
	public List<ResortPriceResponse> getResortPrices(ResortPriceRequest pricingRequest) {

		List<ResortPriceResponse> priceList = new ArrayList<>();

		String programId = pricingRequest.getProgramId();
		List<RoomProgram> programList = new ArrayList<>();
		if (StringUtils.isNotEmpty(programId)) {
			// If programId is available, check if the id is segment and get
			// programs under the segment
			programList = findProgramsIfSegment(programId);

			if (programList.isEmpty()) {
				// If program list is empty, it's not a segment.
				// Validate as single-property offer
				validateProgram(pricingRequest);

				priceList.addAll(getRoomPrices(pricingRequest));
			} else {
			    
			    priceList.addAll(getProgramPricesAcrossProperties(programList, pricingRequest));
			    
			}

		} else {
			priceList.addAll(getMrdPrices(pricingRequest));
		}

		// Find lowest price for every property
		Map<String, ResortPriceResponse> resortPricingMap = helper.findLowestPriceForProperty(priceList);

		// Add soldout resorts, just for the status
		helper.addSoldOutResorts(resortPricingMap, programList);

		// Filter out the availability, if requested
		helper.filterProperties(resortPricingMap, pricingRequest.getPropertyIds());

		return new ArrayList<>(resortPricingMap.values());
	}
	
	/**
     * Returns the list of room programs information if the programId is a segment.
     * 
     * @param programId Program Id
     * @return Returns the list of room programs information if the programId is a
     *         segment.
     */
    public List<RoomProgram> findProgramsIfSegment(String programId) {

        List<RoomProgram> programList = programCacheService.getProgramsBySegmentId(programId);

        if (programList.isEmpty()) {
            Optional<RoomProgram> roomProgram = Optional.ofNullable(programCacheService.getRoomProgram(programId));
            if (roomProgram.isPresent()) {
                RoomProgram program = roomProgram.get();
                if (StringUtils.isNotEmpty(program.getSegmentId())) {
                    log.info("Program {} belongs to segment {}", programId, program.getSegmentId());
                    programList = programCacheService.getProgramsBySegmentId(program.getSegmentId());
                }
            }
        }

        return programList;

    }

	/**
	 * Method to get room prices for scenarios of offer applied. In this case,
	 * prices from aurora will be requested with MRD disabled
	 * 
	 * @param pricingRequest Pricing request
	 * @return Returns prices for each room returned in response.
	 */
	protected List<ResortPriceResponse> getRoomPrices(ResortPriceRequest pricingRequest) {

		AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraPriceRequest(pricingRequest, false);

		Map<String, List<AuroraPriceResponse>> roomMap = groupPricesByRoom(pricingDao.getRoomPrices(request),
				pricingRequest);

		long tripDuration = ChronoUnit.DAYS.between(pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate());

		List<ResortPriceResponse> priceList = new ArrayList<>();
		// Average out the prices across the trip dates for every room
		priceList.addAll(helper.averageOutPricesByRoom(roomMap, pricingRequest.getCustomerId(), pricingRequest.getProgramId(),
				tripDuration).values());

		return priceList;

	}

	/**
	 * Iterates through price list and groups prices by room type. Unavailable rooms
	 * are excluded.
	 * 
	 * @param prices         List of prices returned by aurora
	 * @param pricingRequest Pricing request
	 * @return Returns prices grouped by room type
	 */
	private Map<String, List<AuroraPriceResponse>> groupPricesByRoom(List<AuroraPriceResponse> prices,
			ResortPriceRequest pricingRequest) {

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
			if (!helper.isProgramIncluded(item.getValue(), pricingRequest.getProgramId())) {
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
	 * Method to get room prices for scenarios where offer is not applied. In this
	 * case, prices from aurora will be requested with MRD enabled.
	 * 
	 * @param pricingRequest Pricing request
	 * @return Returns all available prices across all rate plans
	 */
	private List<ResortPriceResponse> getMrdPrices(ResortPriceRequest pricingRequest) {

		AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraPriceRequest(pricingRequest, true);

		// Iterate through list of availability and group by rate plan or
		// program id
		Map<String, List<AuroraPriceResponse>> ratePlanMap = new LinkedHashMap<>();
		List<AuroraPriceResponse> prices = pricingDao.getRoomPrices(request);
		log.info("Started");
		prices.forEach(price -> helper.populatePlanMap(price, ratePlanMap));

		long tripDuration = ChronoUnit.DAYS.between(pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate());

		List<ResortPriceResponse> priceList = new LinkedList<>();
		// Iterate through rooms for each rate plan
		ratePlanMap.keySet().forEach(key -> {

			// For each rate plan, group prices by room and remove unavailable rooms
			Map<String, List<AuroraPriceResponse>> roomMap = groupPricesByRoom(ratePlanMap.get(key), pricingRequest);
			// find average prices for each room and collect them all
			priceList.addAll(
					helper.averageOutPricesByRoom(roomMap, pricingRequest.getCustomerId(), StringUtils.EMPTY, tripDuration)
							.values());

		});

		return priceList;

	}

	/**
	 * Method performs validation check to ensure program is available in cache and
	 * user is applicable for the program.
	 * 
	 * @param request Availability request
	 */
	protected void validateProgram(ResortPriceRequest request) {

		Optional<RoomProgram> roomProgram = Optional
				.ofNullable(programCacheService.getRoomProgram(request.getProgramId()));
		if (roomProgram.isPresent()) {
			RoomProgram program = roomProgram.get();

			request.setPropertyId(program.getPropertyId());
			request.setProgramId(program.getId());
			RoomProgramValidateRequest validateRequest = RoomProgramValidateRequestTransformer
					.getRoomProgramValidateRequest(request, program);

			if (!programService.isProgramApplicable(validateRequest)) {
				log.info("Program {} is not applicable for the customer {}", validateRequest.getProgramId(),
						validateRequest.getCustomerId());
				request.setProgramId(null);
			}

			if (StringUtils.isEmpty(request.getProgramId())) {
				throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
			}
			CommonUtil.isEligibleForMyVegasRedemption(request.getMyVegasRedemptionItems(), program,
					request.getCustomer());
		} else {
			throw new BusinessException(ErrorCode.OFFER_NOT_AVAILABLE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mgm.services.booking.room.service.ResortPriceService#
	 * getResortPerpetualPrices(com.mgm.services.booking.room.model.request.
	 * ResortPriceRequest)
	 */
	@Override
	public List<ResortPriceResponse> getResortPerpetualPrices(ResortPriceRequest pricingRequest) {

		return createAvailabilityRequests(pricingRequest).parallelStream().map(request ->

		PriceResponseTransformer.getResortsPriceResponse(availabilityService.getLowestRoomPrice(request), request)

		).collect(Collectors.toList());

	}

	protected List<RoomAvailabilityRequest> createAvailabilityRequests(ResortPriceRequest pricingRequest) {

		PerpetualProgramRequest perpetualRequest = new PerpetualProgramRequest();
		perpetualRequest.setSource(pricingRequest.getSource());
		perpetualRequest.setCustomerId(pricingRequest.getCustomerId());
		perpetualRequest.setMlifeNumber(pricingRequest.getMlifeNumber());
		List<PerpetaulProgram> perpetualPrograms = programService.getDefaultPerpetualPrograms(perpetualRequest);

		List<RoomAvailabilityRequest> requests = new ArrayList<>();

		List<String> propertyIds = CollectionUtils.isEmpty(pricingRequest.getPropertyIds())
				? auroraProperties.getPropertyIds()
				: pricingRequest.getPropertyIds();

		propertyIds.forEach(propertyId -> {
			RoomAvailabilityRequest request = new RoomAvailabilityRequest();
			request.setAuroraItineraryIds(pricingRequest.getAuroraItineraryIds());
			request.setCheckInDate(pricingRequest.getCheckInDate());
			request.setCheckOutDate(pricingRequest.getCheckOutDate());
			request.setCustomer(pricingRequest.getCustomer());
			request.setCustomerId(pricingRequest.getCustomerId());
			request.setMlifeNumber(pricingRequest.getMlifeNumber());
			request.setMyVegasRedemptionItems(pricingRequest.getMyVegasRedemptionItems());
			request.setNumGuests(pricingRequest.getNumGuests());
			request.setPropertyId(propertyId);
			request.setSource(pricingRequest.getSource());
			request.setProgramId(helper.findPerpetualProgramForProperty(perpetualPrograms, propertyId));
			requests.add(request);
		});

		return requests;
	}
	
	private List<ResortPriceResponse> getProgramPricesAcrossProperties(List<RoomProgram> programList,
	        ResortPriceRequest pricingRequest) {

        return programList.parallelStream().map(program -> {
            // Copy to avoid request conflicts
            ResortPriceRequest request = new ResortPriceRequest();
            BeanUtils.copyProperties(pricingRequest, request);
            
            request.setProgramId(program.getId());
            request.setPropertyId(program.getPropertyId());

            // Gets the list of available room prices per property for the
            // program and finds the lowest among them.
            return getRoomPrices(request);
        }).collect(Collectors.toList()).stream().flatMap(List::stream).collect(Collectors.toList());

    }

}