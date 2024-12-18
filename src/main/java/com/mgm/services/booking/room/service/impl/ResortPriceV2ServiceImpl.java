package com.mgm.services.booking.room.service.impl;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.dao.ProductInventoryDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.request.dto.RateOrGroupDTO;
import com.mgm.services.booking.room.model.request.dto.ResortPriceWithTaxDTO;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.common.exception.BusinessException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.model.ResortPrice;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.service.ResortPriceV2Service;
import com.mgm.services.booking.room.service.RoomAvailabilityV2Service;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.service.helper.ResortPriceServiceHelper;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgm.services.booking.room.transformer.PriceResponseTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;

import lombok.extern.log4j.Log4j2;

import static com.mgm.services.booking.room.util.CommonUtil.localDateToDate;
import static com.mgm.services.booking.room.util.ReservationUtil.*;

/**
 * Implementation class exposing services to fetch prices for resorts.
 * 
 */
@Component
@Log4j2
@Primary
public class ResortPriceV2ServiceImpl implements ResortPriceV2Service {

	@Autowired
	private AuroraProperties auroraProperties;

	@Autowired
	private RoomPriceDAO pricingDao;
	
	@Autowired
    private RoomProgramDAO programDao;

	@Autowired
	private RoomAvailabilityV2Service availabilityV2Service;

	@Autowired
	private ResortPriceServiceHelper helper;

    @Autowired
    private ProductInventoryDAO productInventoryDAO;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private ApplicationProperties applicationProperties;
    
    @Autowired
    private ReservationServiceHelper reservationServiceHelper;

    @Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mgm.services.booking.room.service.ResortPriceService#getResortPrices(
	 * com.mgm.services.booking.room.model.request.ResortsPricingRequest)
	 */
	@Override
	public List<ResortPriceResponse> getResortPrices(ResortPriceV2Request pricingRequest) {

        Map<String, ResortPriceResponse> resortPricingMap = new HashMap<>();

        String programId = pricingRequest.getProgramId();
        String segmentCode = pricingRequest.getSegment();
        String groupCode = pricingRequest.getGroupCode();
        List<RoomProgramBasic> programList = new ArrayList<>() ;
        boolean isF1Flow = false;
        Set<String> roomProgramTags = new HashSet<>();
        
        if (StringUtils.isNotEmpty(segmentCode)) {

            if (CommonUtil.isUuid(segmentCode)) {
                // Flow for GSE segment GUID
                programList = programDao.findProgramsBySegment(segmentCode, pricingRequest.getSource());
            } else {
                programList = programDao.findProgramsByRatePlanCode(segmentCode, pricingRequest.getSource(), false);
            }

            log.info("Segment code {} resolves to {} programs", segmentCode, programList.size());
            List<ResortPriceResponse> listOfAvailableRoomPrices = getProgramPricesAcrossProperties(programList, pricingRequest);
            resortPricingMap.putAll(helper.findLowestPriceForProperty(listOfAvailableRoomPrices));

            // Client will use this flag to get participating resorts only for HDE packaging
            // bookings.
            if (!pricingRequest.isParticipatingResortsOnly()) {
                addFallbackPricing(pricingRequest, resortPricingMap);
            }
        } else if (StringUtils.isNotEmpty(groupCode)) {
            programList = programDao.findProgramsByGroupCode(groupCode,pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate(), pricingRequest.getSource());
            log.info("Group code {} resolves to {} programs/GroupIds", groupCode, programList.size());
            List<ResortPriceResponse> listOfAvailableRoomPrices = getProgramPricesAcrossProperties(programList, pricingRequest);
            resortPricingMap.putAll(helper.findLowestPriceForProperty(listOfAvailableRoomPrices));
        } else if (StringUtils.isNotEmpty(programId)) {
            // If programId is available, check if the id is segment and get
            // programs under the segment
            programList = programDao.findProgramsIfSegment(programId, pricingRequest.getSource());
            List<ResortPriceResponse> listOfAvailableRoomPrices;
            if (programList.isEmpty()) {
                // If program list is empty, it's not a segment.
                // Validate as single-property offer
                helper.validateProgram(pricingRequest);

                // Gets the list of available room prices per property for the program and finds
                // the lowest among them.
                listOfAvailableRoomPrices = getRoomPrices(pricingRequest, PricingModes.PROGRAM);
                resortPricingMap.putAll(helper.findLowestPriceForProperty(listOfAvailableRoomPrices));
            } else {
                // Get pricing for individual programs under the segment
                listOfAvailableRoomPrices = getProgramPricesAcrossProperties(programList, pricingRequest);
                resortPricingMap.putAll(helper.findLowestPriceForProperty(listOfAvailableRoomPrices));
            }

            // Client will use this flag to get participating resorts only for HDE packaging
            // bookings.
            if (!pricingRequest.isParticipatingResortsOnly()) {
                addFallbackPricing(pricingRequest, resortPricingMap);
            }
            

        } else {
            // Gets the list of available room prices per property and finds the lowest
            // among them.
            resortPricingMap.putAll(helper
                    .findLowestPriceForProperty(getMrdPrices(pricingRequest, PricingModes.BEST_AVAILABLE)));
        }

        if (pricingRequest.isParticipatingResortsOnly()) {
        	//CBSR-1130 need to return sold out resorts for Premium packages
        	helper.addSoldOutResortsForProgram(resortPricingMap, programList);
        } else {
        	// Add soldout resorts, just for the status
        	// Passing the empty programList so the default list of propertyIds will be
        	// considered.
        	helper.addSoldOutResortsForProgram(resortPricingMap, new ArrayList<>());
        }

        // Adding the flag to remove soldOut resorts for PKG.20
        if(pricingRequest.isIgnoreSO()) {
            helper.removeSoldOutResorts(resortPricingMap);
        }

        // Filter out the availability, if requested
        helper.filterProperties(resortPricingMap, pricingRequest.getPropertyIds());
		int numNights = Period.between(pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate()).getDays();
        resortPricingMap.values().forEach(resortPrice -> calculatePriceSubTotals(resortPrice, numNights));

        //F1 integration
        for (RoomProgramBasic rp : programList) {
            if (null != rp.getRatePlanTags() && rp.getRatePlanTags().length > 0)
                roomProgramTags.addAll(Arrays.asList(rp.getRatePlanTags()));
        }
        isF1Flow = checkF1Flow(pricingRequest, roomProgramTags);

        if (isF1Flow) {
            resortPricingMap.values().forEach(resortPrice -> updateF1ComponentPrices(resortPrice, roomProgramTags, pricingRequest));
        }

        return new ArrayList<>(resortPricingMap.values());
    }

    @Override
    public List<ResortPriceResponse> getResortPricesWithTax(ResortPriceWithTaxDTO pricingRequest) {
        Map<String, ResortPriceResponse> resortPricingMap = new HashMap<>();
	    List<RoomProgramBasic> programList = new ArrayList<>() ;
	    // create ACRS format
        programList = createProgramList(pricingRequest);
        List<ResortPriceResponse> listOfAvailableRoomPrices = getProgramPricesAcrossProperties(programList, pricingRequest);
        resortPricingMap.putAll(helper.findLowestPriceForProperty(listOfAvailableRoomPrices));

        List<String> missingResorts = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(pricingRequest.getRates())) {
            for (RateOrGroupDTO rate : pricingRequest.getRates()) {
                if (!resortPricingMap.containsKey(rate.getPropertyId())) {
                    missingResorts.add(rate.getPropertyId());
                }
            }
        }

        if(CollectionUtils.isNotEmpty(pricingRequest.getGroups())) {
            for (RateOrGroupDTO group : pricingRequest.getGroups()) {
                if (!resortPricingMap.containsKey(group.getPropertyId())) {
                    missingResorts.add(group.getPropertyId());
                }
            }
        }

        if(!missingResorts.isEmpty() && pricingRequest.isPerpetualPricing()) {
            addFallbackPricingForPropertyList(pricingRequest, resortPricingMap, missingResorts);
        }

        return new ArrayList<>(resortPricingMap.values());
    }

    private List<RoomProgramBasic> createProgramList(ResortPriceWithTaxDTO pricingRequest) {
        List<RoomProgramBasic> programList = new ArrayList<>() ;
        if(CollectionUtils.isNotEmpty(pricingRequest.getRates())) {
            programList.addAll(pricingRequest.getRates().stream()
                    .map(p -> createRoomProgramBasic(p, true))
                    .collect(Collectors.toList()));
        }
        if(CollectionUtils.isNotEmpty(pricingRequest.getGroups())) {
            programList.addAll(pricingRequest.getGroups().stream()
                    .map(p -> createRoomProgramBasic(p, false))
                    .collect(Collectors.toList()));
        }
        return programList;
    }

    private RoomProgramBasic createRoomProgramBasic(RateOrGroupDTO p, boolean isRatePlanCode) {
        RoomProgramBasic program = new RoomProgramBasic();
        String programId ;
                if(isRatePlanCode){
                    programId = ACRSConversionUtil.createRatePlanCodeGuid(p.getCode(),
                            referenceDataDAOHelper.retrieveAcrsPropertyID(p.getPropertyId()));
                }else{
                    programId = ACRSConversionUtil.createGroupCodeGuid(p.getCode(),
                            referenceDataDAOHelper.retrieveAcrsPropertyID(p.getPropertyId()));
                }
        program.setProgramId(programId);
        program.setPropertyId(p.getPropertyId());
        return program;
    }

    private void updateF1ComponentPrices(ResortPriceResponse resortPrice, Set<String> roomProgramTags, ResortPriceV2Request pricingRequest) {
        if (resortPrice.getStatus().toString().equalsIgnoreCase(AvailabilityStatus.AVAILABLE.toString())) {
            addF1CasinoDefaultComponentPrices(resortPrice, roomProgramTags, pricingRequest);
            addF1DefaultPublicTicketComponentPrices(resortPrice, roomProgramTags, pricingRequest);
        }
    }

    private void addF1CasinoDefaultComponentPrices(ResortPriceResponse resortPrice, Set<String> roomProgramTags, ResortPriceV2Request request) {
        ResortPrice originalPrice = resortPrice.getPrice();
        String componentCode = getF1DefaultCasinoComponentCode(new ArrayList<>(roomProgramTags));
        if (StringUtils.isNotEmpty(componentCode) && !componentCode.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG)) {
            RoomComponent component = componentDAO.getRoomComponentByCode(resortPrice.getPropertyId(),
                    componentCode, resortPrice.getRoomTypeId(), resortPrice.getProgramId(),
                    localDateToDate(request.getCheckInDate(), applicationProperties.getDefaultTimezone()),
                    localDateToDate(request.getCheckOutDate(), applicationProperties.getDefaultTimezone()),
                    request.getMlifeNumber(), request.getSource());
            double toAdd = 0.0;
            if (null != component && null != component.getPrice()) {
                toAdd = getRoomComponentPrice(component,
                        localDateToDate(request.getCheckInDate(), applicationProperties.getDefaultTimezone()),
                        localDateToDate(request.getCheckOutDate(), applicationProperties.getDefaultTimezone()));
                if (null != originalPrice) {
                    originalPrice.setDiscountedAveragePrice(originalPrice.getDiscountedAveragePrice() + toAdd);
                    resortPrice.setPrice(originalPrice);
                    resortPrice.setComp(false);
                }
            }
        }
    }

    private void addF1DefaultPublicTicketComponentPrices(ResortPriceResponse resortPrice, Set<String> roomProgramTags, ResortPriceV2Request request) {
        String componentCode = ReservationUtil.getF1DefaultPublicTicketComponentCode(new ArrayList<>(roomProgramTags));
		
        if (null != resortPrice && null != resortPrice.getPropertyId() && StringUtils
				.equalsIgnoreCase(applicationProperties.getTcolvPropertyId(), resortPrice.getPropertyId())) {
			componentCode = ReservationUtil.getTCOLVF1TicketComponentCode(new ArrayList<>(roomProgramTags));
        }
        
        if (StringUtils.isNotEmpty(componentCode)) {
            //int ticketCount = ReservationUtil.getF1TicketCountFromF1Tag(new ArrayList<>(roomProgramTags), applicationProperties);
            double ticketComponentPrice = 0.0;
            RoomComponent component = componentDAO.getRoomComponentByCode(resortPrice.getPropertyId(),
                    componentCode, resortPrice.getRoomTypeId(), resortPrice.getProgramId(),
                    localDateToDate(request.getCheckInDate(), applicationProperties.getDefaultTimezone()),
                    localDateToDate(request.getCheckOutDate(), applicationProperties.getDefaultTimezone()),
                    request.getMlifeNumber(), request.getSource());
            if (null != component) {
                ticketComponentPrice = ReservationUtil.componentPriceToAdd(request.getCheckInDate(), request.getCheckOutDate(), component, false, applicationProperties);
            }
    /*if (ticketCount > 1) {
        String additionalTicketComponentCode = ReservationUtil.getF1AdditionalPublicTicketComponentCode(new ArrayList<>(roomProgramTags), applicationProperties);
        if (StringUtils.isNotEmpty(additionalTicketComponentCode)) {
            RoomComponent additionalTicketComponent = componentDAO.getRoomComponentByCode(resortPrice.getPropertyId(),
                    additionalTicketComponentCode, resortPrice.getRoomTypeId(), resortPrice.getProgramId(),
                    localDateToDate(request.getCheckInDate(), applicationProperties.getDefaultTimezone()),
                    localDateToDate(request.getCheckOutDate(), applicationProperties.getDefaultTimezone()),
                    request.getMlifeNumber(), request.getSource());
            if (null != additionalTicketComponent) {
                ticketComponentPrice += ReservationUtil.componentPriceToAdd(request.getCheckInDate(), request.getCheckOutDate(), additionalTicketComponent, false, applicationProperties);
            }
        }
    }*/
            resortPrice.setF1TicketPrice(ticketComponentPrice);
        }
    }

    private void calculatePriceSubTotals(ResortPriceResponse resortPrice, int numNights) {
        ResortPrice price = resortPrice.getPrice();
        if (null != price) {
            price.setBaseSubtotal(price.getBaseAveragePrice() * numNights);
            price.setDiscountedSubtotal(price.getDiscountedAveragePrice() * numNights);
            resortPrice.setPrice(price);
        }
    }

	private void addFallbackPricing(ResortPriceV2Request pricingRequest, Map<String, ResortPriceResponse> resortPricingMap) {
	    
	    // Get the list of propertyIds, which do not have pricing yet. Reason for no
        // pricing could be due to it is not part of the program/segment requested or
        // program rates are SOLDOUT for that property
        List<String> propertyIdsForFallBackPricing = helper.getPropertyIdsForFallBackPricing(resortPricingMap);

        addFallbackPricingForPropertyList(pricingRequest, resortPricingMap, propertyIdsForFallBackPricing);

	}

    private void addFallbackPricingForPropertyList(ResortPriceV2Request pricingRequest, Map<String, ResortPriceResponse> resortPricingMap, List<String> propertyIdsForFallBackPricing) {

        if (propertyIdsForFallBackPricing.isEmpty()) {
            return;
        }

        // Loop through the list of propertyIds and find fallback pricing for each of them.
        // Fallback pricing will be either PO prices or MRD prices based on perpetualPricing flag.
        // Once the fallback room rates are available per
        // property, then the room with lowest price will be determined. Property id and
        // lowest priced room will be added to the resortPricingMap.
        List<ResortPriceResponse> prices = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(propertyIdsForFallBackPricing.size());
        try {

            List<CompletableFuture<List<ResortPriceResponse>>> futures = propertyIdsForFallBackPricing.stream()
                    .map(propertyId -> CompletableFuture.supplyAsync(() -> {
                        // Copy to avoid request conflicts
                        ResortPriceV2Request request = new ResortPriceV2Request();
                        BeanUtils.copyProperties(pricingRequest, request);

                        log.debug("Requesting fallback pricing for the property :: {}", propertyId);
                        request.setProgramId(null);
                        request.setPropertyId(propertyId);
                        request.setPerpetualPricing(CommonUtil.isPerpetualPricingEligible(request.getPerpetualEligiblePropertyIds(), propertyId));
                        if (request.isPerpetualPricing()) {
                            return getRoomPrices(request, PricingModes.PERPETUAL);
                        } else {
                            return getMrdPrices(request, PricingModes.BEST_AVAILABLE);
                        }
                    }, executor)
                                .exceptionally(ex -> {
                                log.error(ex.getMessage());
                                return null;
                            }))
                    .collect(Collectors.toList());

            prices = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
                    .stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        } finally {
            executor.shutdown();
        }

        resortPricingMap.putAll(helper.findLowestPriceForProperty(prices));
    }

	/**
     * Method to get room prices for scenarios of offer applied. In this case,
     * prices from aurora will be requested with MRD disabled
     * 
	 * @param pricingRequest Pricing v2 request
	 * @param pricingMode mode in which pricing is requested
	 * @return Returns prices for each room returned in response.
	 */
	protected List<ResortPriceResponse> getRoomPrices(ResortPriceV2Request pricingRequest, PricingModes pricingMode) {

        // For ACRS, we are setting perpetual flag based on rate plan
        if (StringUtils.isNotEmpty(pricingRequest.getProgramId()) && ACRSConversionUtil.isAcrsRatePlanGuid(pricingRequest.getProgramId())) {
            String aCrsRatePlanCode = ACRSConversionUtil.getRatePlanCode(pricingRequest.getProgramId());
            pricingRequest.setPerpetualPricing(ACRSConversionUtil.isPORatePlan(aCrsRatePlanCode));
        }

		AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraPriceRequest(pricingRequest, false);

		AuroraPricesResponse priceRes = getRoomPricesV2IgnoreException(request);
		Map<String, List<AuroraPriceResponse>> roomMap = helper.groupPricesByRoom( priceRes.getAuroraPrices(),pricingRequest);

		long tripDuration = ChronoUnit.DAYS.between(pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate());

		List<ResortPriceResponse> priceList = new ArrayList<>();
		// Average out the prices across the trip dates for every room
		Collection<ResortPriceResponse> resortPriceResponseCollection = helper.averageOutPricesByRoomV2(roomMap, pricingRequest.getCustomerId(), pricingRequest.getProgramId(),
                tripDuration, pricingMode).values();
        
		priceList.addAll(resortPriceResponseCollection);
		return priceList;

	}

	/**
	 * Method to get room prices for scenarios where offer is not applied. In this
	 * case, prices from aurora will be requested with MRD enabled.
	 * 
	 * @param pricingRequest Pricing request
	 * @param pricingMode mode in which pricing is requested
	 * @return Returns all available prices across all rate plans
	 */
	private List<ResortPriceResponse> getMrdPrices(ResortPriceV2Request pricingRequest, PricingModes pricingMode) {

		AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraPriceRequest(pricingRequest, true);

		// Iterate through list of availability and group by rate plan or
		// program id
		Map<String, List<AuroraPriceResponse>> ratePlanMap = new LinkedHashMap<>();
		AuroraPricesResponse prices = getRoomPricesV2IgnoreException(request);
 		
		// List to track properties which already has a rate plan added
        // This will help pick only first rate plan for a property in multi-rate queue
        List<String> processedPropertyList = new ArrayList<>();
		prices.getAuroraPrices().forEach(price -> helper.populatePlanMap(price, ratePlanMap, processedPropertyList));
		
		log.debug("Rate Plan Map: {}", ratePlanMap.keySet());

		long tripDuration = ChronoUnit.DAYS.between(pricingRequest.getCheckInDate(), pricingRequest.getCheckOutDate());

		List<ResortPriceResponse> priceList = new LinkedList<>();
		
		// Iterate through rooms for each rate plan
		ratePlanMap.keySet().forEach(key -> {

			// For each rate plan, group prices by room and remove unavailable rooms
			Map<String, List<AuroraPriceResponse>> roomMap = helper.groupPricesByRoom(ratePlanMap.get(key), pricingRequest);
			// find average prices for each room and collect them all

            Collection<ResortPriceResponse> resortPriceResponseCollection = helper
                    .averageOutPricesByRoomV2(roomMap, pricingRequest.getCustomerId(), pricingRequest.isPackageFlow() ? key : StringUtils.EMPTY, tripDuration, pricingMode)
                    .values();
            if (!resortPriceResponseCollection.isEmpty()) {
                resortPriceResponseCollection.stream().forEach(resortPriceResponse -> resortPriceResponse.setPricingMode(pricingMode));
            }
            priceList.addAll(resortPriceResponseCollection);

		});
		return priceList;

	}
    /*
	 * (non-Javadoc)
	 * 
	 * @see com.mgm.services.booking.room.service.ResortPriceService#
	 * getResortPerpetualPrices(com.mgm.services.booking.room.model.request.
	 * ResortPriceRequest)
	 */
	@Override
	public List<ResortPriceResponse> getResortPerpetualPrices(ResortPriceV2Request pricingRequest) {
        List<ResortPriceResponse> resortPrices = null;

        List<RoomAvailabilityV2Request> requests = createAvailabilityRequests(pricingRequest);

        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        try {

            List<CompletableFuture<ResortPriceResponse>> futures = requests.stream()
                    .map(request -> CompletableFuture.supplyAsync(() -> PriceResponseTransformer
                            .getResortsPriceV2Response(availabilityV2Service.getLowestRoomPrice(request), request),
                            executor)
                    		 .exceptionally(ex -> {
                                 log.error(ex.getMessage());
                                 return null;
                             }))
                    .collect(Collectors.toList());

            resortPrices = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList());
        } finally {
            executor.shutdown();
        }
        return resortPrices;
    }


	protected List<RoomAvailabilityV2Request> createAvailabilityRequests(ResortPriceV2Request pricingRequest) {

		List<RoomAvailabilityV2Request> requests = new ArrayList<>();

		List<String> propertyIds = CollectionUtils.isEmpty(pricingRequest.getPropertyIds())
				? auroraProperties.getPropertyIds()
				: pricingRequest.getPropertyIds();

		propertyIds.forEach(propertyId -> {
		    RoomAvailabilityV2Request request = new RoomAvailabilityV2Request();
			request.setAuroraItineraryIds(pricingRequest.getAuroraItineraryIds());
			request.setCheckInDate(pricingRequest.getCheckInDate());
			request.setCheckOutDate(pricingRequest.getCheckOutDate());
			request.setCustomerId(pricingRequest.getCustomerId());
			request.setMlifeNumber(pricingRequest.getMlifeNumber());
			request.setNumAdults(pricingRequest.getNumAdults());
			request.setPropertyId(propertyId);
			request.setSource(pricingRequest.getSource());
            request.setPerpetualPricing(CommonUtil.isPerpetualPricingEligible(pricingRequest.getPerpetualEligiblePropertyIds(), propertyId));
			request.setPackageFlow(pricingRequest.isPackageFlow());
            requests.add(request);
		});

		return requests;
	}
	
    private List<ResortPriceResponse> getProgramPricesAcrossProperties(List<RoomProgramBasic> programList,
            ResortPriceV2Request pricingRequest) {

        List<ResortPriceResponse> programPrices = programList.parallelStream().map(program -> {
            // Copy to avoid request conflicts
            ResortPriceV2Request request = new ResortPriceV2Request();
            BeanUtils.copyProperties(pricingRequest, request);
            
            request.setProgramId(program.getProgramId());
            request.setPropertyId(program.getPropertyId());

            // Gets the list of available room prices per property for the
            // program and finds the lowest among them.
            return getRoomPrices(request, PricingModes.PROGRAM);
        }).collect(Collectors.toList()).stream().flatMap(List::stream).collect(Collectors.toList());

        // For new segments approach, get the first rate plan code and set it to all property responses
        // This is for GQL to stitch with respective content
        programList.stream().filter(p -> StringUtils.isNotEmpty(p.getRatePlanCode())).findFirst()
                .ifPresent(programBasic -> programPrices
                        .forEach(response -> response.setRatePlanCode(programBasic.getRatePlanCode())));
        
        return programPrices;
        
    }

    private boolean checkF1Flow(ResortPriceV2Request pricingRequest, Set<String> roomProgramTags) {
        if (roomProgramTags.isEmpty() && StringUtils.isNotEmpty(pricingRequest.getProgramId())
                && ACRSConversionUtil.isAcrsRatePlanGuid(pricingRequest.getProgramId())
                && !ACRSConversionUtil.isAcrsGroupCodeGuid(pricingRequest.getProgramId())) {
            RoomProgramV2Request acrsProgramDetailsRequest = createRoomV2Request(pricingRequest.getProgramId(),
                    pricingRequest.getCheckInDate().toString(), pricingRequest.getCheckOutDate().toString());
            List<RoomOfferDetails> details = programDao.getRatePlanById(acrsProgramDetailsRequest);
            for (RoomOfferDetails roomOfferDetail : details) {
                if (CollectionUtils.isNotEmpty(roomOfferDetail.getTags())) {
                    roomProgramTags.addAll(roomOfferDetail.getTags());
                }
            }
        }
        boolean isF1Flow = false;
        if (!roomProgramTags.isEmpty() && roomProgramTags.contains(applicationProperties.getF1PackageTag())) {
            for (String validProductCode : applicationProperties.getValidF1ProductCodes()) {
                if (roomProgramTags.contains(validProductCode)) {
                	int ticketCount = ReservationUtil.getF1TicketCountFromF1Tag(roomProgramTags.stream().collect(Collectors.toList()), applicationProperties);
                	validProductCode = ReservationUtil.getProductCodeForF1Program(validProductCode, roomProgramTags.stream().collect(Collectors.toList()),applicationProperties);
                    checkInventoryAvailability(productInventoryDAO.getInventory(validProductCode, true), ticketCount);
                    isF1Flow = true;
                    break;
                }
            }
        }
        return isF1Flow;
    }

	private AuroraPricesResponse getRoomPricesV2IgnoreException(AuroraPriceRequest request){
		try{
			return pricingDao.getRoomPricesV2(request);
		} catch (BusinessException ex) {
			// Catching and ignoring business Exception
			AuroraPricesResponse response = new AuroraPricesResponse();
			response.setAuroraPrices(new ArrayList<>());
			return response;
		}
	}

    @Override
    public void requestUpdateForPO(ResortPriceV2Request request) {

        // if PO program is supplied for PO qualified user, drop the program
        if (request.isPerpetualPricing() &&StringUtils.isNotEmpty(request.getProgramId()) && programDao.isProgramPO(request.getProgramId())) {
            request.setProgramId(null);
        }
    }
}
