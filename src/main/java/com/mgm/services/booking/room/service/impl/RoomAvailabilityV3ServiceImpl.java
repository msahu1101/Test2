package com.mgm.services.booking.room.service.impl;

import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.dao.ProductInventoryDAO;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;

import com.mgm.services.common.util.BaseCommonUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.model.AvailabilityStatus;
import com.mgm.services.booking.room.model.PriceV2Itemized;
import com.mgm.services.booking.room.model.RoomTripPriceV2;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.RoomAvailabilityV3Request;
import com.mgm.services.booking.room.service.RoomAvailabilityV3Service;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.helper.RoomAvailabilityServiceHelper;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgm.services.booking.room.util.ServiceConversionHelper;

import lombok.extern.log4j.Log4j2;

import static com.mgm.services.booking.room.util.CommonUtil.localDateToDate;
import static com.mgm.services.booking.room.util.ReservationUtil.*;
import static java.util.stream.Collectors.*;

/**
 * Implementation class for exposing services to fetching room prices.
 * 
 */
@Component
@Log4j2
@Primary
public class RoomAvailabilityV3ServiceImpl extends BasePriceV2ServiceImpl implements RoomAvailabilityV3Service {

    @Autowired
    private RoomPriceDAO pricingDao;

    @Autowired
    private RoomProgramDAO programDao;

    @Autowired
    private RoomProgramService programService;

    @Autowired
    private RoomAvailabilityServiceHelper availabilityServiceHelper;

    @Autowired
    private ServiceConversionHelper serviceConversionHelper;

    @Autowired
    private RoomProgramCacheService roomProgramCacheService;

    @Autowired
    private ProductInventoryDAO productInventoryDAO;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Override
    public RoomAvailabilityCombinedResponse getRoomAvailability(RoomAvailabilityV3Request request) {

        serviceConversionHelper.convertGuids(request);

        // Perform validation checks if program is available
        if(StringUtils.isNotEmpty(request.getConfirmationNumber()) || StringUtils.isNotEmpty(request.getOperaConfirmationNumber()) ){
            request.setModifyFlow(true);
        }
        validateProgram(request, programService);

        // Drop Po programs for PO qualified user
        List<String> requestedPrograms = new ArrayList<>(request.getProgramIds());
        requestUpdateForPO(request);

        //F1 integration
        boolean isF1Flow = checkF1Flow(request);

        RoomAvailabilityCombinedResponse response = new RoomAvailabilityCombinedResponse();
        TripPricingMetadata metadata = new TripPricingMetadata();

        // Setting shopping flow metadata based on shopper type
        if (request.isPerpetualPricing()) {
            metadata.setShoppingFlow(ShoppingFlow.PERPETUAL);
        } else {
            metadata.setShoppingFlow(ShoppingFlow.RATE_PLANS);
        }

        // for multiple programs, get availability in parallel and add to
        // response
        // Only web is expected to use
        if (!request.getProgramIds()
                .isEmpty()) {

            final Map<String, String> programPromoAssociation = programDao.getProgramPromoAssociation(
                    AuroraPriceRequestTransformer.getPromoAssociationRequest(request));

            final Map<String, RatePlanV2Response> programs = programPromoAssociation.keySet().parallelStream()
                    .map(programId -> getRoomPricesAsRatePlan(request, programId, programPromoAssociation.get(programId)))
                    .collect(Collectors.toMap(RatePlanV2Response::getProgramId, r -> r));

            response.getRatePlans().addAll(request.getProgramIds().stream()
                    .filter(StringUtils::isNotEmpty)
                    .map(programs::get)
                    .collect(Collectors.toList()));

            // set metadata
            metadata.setPricingModeIfEmpty(PricingModes.PROGRAM);

            // return if default rate plans not required
            if (!request.isIncludeDefaultRatePlans()) {
                response.setMetadata(metadata);
                if (isF1Flow) {
                    addF1CasinoDefaultComponentPrices(request, response);
                }
                sortRatesNItemizedDates(response, request);
                return response;
            }
        }

        if (StringUtils.isNotEmpty(request.getProgramId())) {
            metadata.setPricingModeIfEmpty(PricingModes.PROGRAM);

            // If a program price is requested, ignoring perpetual flag
            request.setPerpetualPricing(false);
            String programId = request.getProgramId();

            if (metadata.getShoppingFlow()
                    .equals(ShoppingFlow.PERPETUAL)) {

                // For PO user requesting program prices, just return program
                // only prices
                response.getRatePlans()
                        .add(getRoomPricesAsRatePlan(request, programId, request.getPromo()));
            } else if (!request.isIncludeDefaultRatePlans()) {

                // if includeDefaultRatePlans is false, return program prices
                // only (ICE flow)
                // Keeping it as seperate if block since this requirement may
                // change for ICE
                response.getRatePlans()
                        .add(getRoomPricesAsRatePlan(request, programId, request.getPromo()));
            } else {
                request.setProgramId(null);
                List<RatePlanV2Response> ratePlans = getRatePlans(request);

                // If requested program is not in rate plans response, request
                // program pricing and add it to plans
                if (ratePlans.stream()
                        .noneMatch(r -> r.getProgramId()
                                .equals(programId))) {
                    request.setProgramId(programId);
                    // include it with default rate plans
                    ratePlans.add(0, getRoomPricesAsRatePlan(request, programId, request.getPromo()));
                } else {
                    // reorder to move requested program to first place
                    Optional<RatePlanV2Response> programOpt = ratePlans.stream()
                            .filter(r -> r.getProgramId()
                                    .equals(programId))
                            .findAny();
                    if (programOpt.isPresent()) {
                        RatePlanV2Response program = programOpt.get();
                        ratePlans.remove(program);
                        ratePlans.add(0, program);
                    }
                }
                response.getRatePlans()
                        .addAll(ratePlans);
            }
        } else if (request.isPerpetualPricing() && !isF1Flow) {

            RatePlanV2Response poRatePlan = getRoomPricesAsRatePlan(request, null, request.getPromo());

            // If PO program was in requested program list, maintain the order
            // of requested programs
            if (requestedPrograms.contains(poRatePlan.getProgramId())
                    && requestedPrograms.indexOf(poRatePlan.getProgramId()) < response.getRatePlans()
                            .size()) {
                response.getRatePlans()
                        .add(requestedPrograms.indexOf(poRatePlan.getProgramId()), poRatePlan);
            } else {
                response.getRatePlans()
                        .add(poRatePlan);
            }

            // If none of the rooms are perpetual priced, set the pricing mode
            // to BA
            if (poRatePlan.getRooms()
                    .stream()
                    .noneMatch(RoomAvailabilityV2Response::isPerpetualPricing)) {
                metadata.setPricingModeIfEmpty(PricingModes.BEST_AVAILABLE);
                if (request.isPoProgramRequested()) {
                    metadata.setBookingLimitsApplied(true);
                }
            } else {
                metadata.setPricingModeIfEmpty(PricingModes.PERPETUAL);
                // remove rooms which doesn't have any night under this PO rate
                // plan
                poRatePlan.getRooms()
                        .removeIf(r -> !r.isPerpetualPricing());
            }

        } else {
            metadata.setPricingModeIfEmpty(PricingModes.BEST_AVAILABLE);
            response.getRatePlans()
                    .addAll(getRatePlans(request));
        }

        response.setMetadata(metadata);

        // removing duplicate rate plans
        Set<String> plansAlreadySeen = new HashSet<>();
        response.getRatePlans()
                .removeIf(p -> !plansAlreadySeen.add(p.getProgramId()));

        //F1 - adding seat prices to the availability response
        if (isF1Flow) {
            addF1CasinoDefaultComponentPrices(request, response);
            addF1DefaultPublicTicketComponentPrices(request, response);
        }
        sortRatesNItemizedDates(response,request);
        return response;
    }

    @Override
    public RoomAvailabilityCombinedResponse getRoomAvailabilityGrid(RoomAvailabilityV3Request availabilityRequest) {
        availabilityRequest.setDisplaySoPrice(true);
        RoomAvailabilityCombinedResponse tripPricingResponse = getRoomAvailability(availabilityRequest);
        Map<String, Map<String, List<Date>>> rateRoomDateSoMap = getDateRateRoomSoMap(tripPricingResponse);
        if (!rateRoomDateSoMap.isEmpty()){
            List<String> soRoomIds = getSoRoomIds(rateRoomDateSoMap);
            List<String> soProgramIds = getSoProgramIds(rateRoomDateSoMap);
            try {
                //call calendar pricing
                List<AuroraPriceResponse> soAvailabilities = pricingDao.getGridAvailabilityForSoldOut(createAuroraPriceRequest(availabilityRequest, soRoomIds, soProgramIds));
                //filter out room and rates
                List<AuroraPriceResponse> filteredSoAvailabilities = soAvailabilities.stream().filter(pr -> soProgramIds.contains(pr.getProgramId()))
                        .filter(pr -> soRoomIds.contains(pr.getRoomTypeId()))
                        .collect(Collectors.toList());
                Map<String, Map<String, List<Date>>> soRateRoomDateAvailMap = filteredSoAvailabilities.stream().collect(groupingBy(AuroraPriceResponse::getProgramId,
                        groupingBy(AuroraPriceResponse::getRoomTypeId, mapping(AuroraPriceResponse::getDate, toList()))));
                //Merge tripPricingResponse && calPrices
                updateTripPricingResponse(tripPricingResponse, rateRoomDateSoMap, soRateRoomDateAvailMap);
            }catch (Exception ex){
                log.warn("Error while fetching availability for SO Rooms - {} Program-{} -{}",soRoomIds,soProgramIds,ex.getMessage());
            }
        }
    return tripPricingResponse;
    }

    private void updateTripPricingResponse(RoomAvailabilityCombinedResponse tripPricingResponse, Map<String, Map<String, List<Date>>> rateRoomDateSoMap, Map<String, Map<String, List<Date>>> rateRoomDateAvailCalMap) {

        tripPricingResponse.getRatePlans().forEach(rate ->{
            Map<String, List<Date>> availRoomDatesCalMap = rateRoomDateAvailCalMap.get(rate.getProgramId());
            if(null != availRoomDatesCalMap){
                Set<RoomAvailabilityV2Response> roomPrices = rate.getRooms();
                roomPrices.forEach( roomPrice -> {
                    if (availRoomDatesCalMap.containsKey(roomPrice.getRoomTypeId())) {
                        List<Date> availCalDates = availRoomDatesCalMap.get(roomPrice.getRoomTypeId());
                        Map<String, List<Date>> soRoomDateMap = rateRoomDateSoMap.get(rate.getProgramId());
                        if(null != soRoomDateMap) {
                            List<Date> soDates = soRoomDateMap.get(roomPrice.getRoomTypeId());
                            if(CollectionUtils.isNotEmpty(soDates)) {
                                List<Date> actualAvailDates = soDates.stream().filter(availCalDates::contains).collect(toList());
                                List<Date> actualSoDates = soDates.stream().filter(soDate -> !availCalDates.contains(soDate)).collect(toList());
                                updateSOItemized(roomPrice.getPrice(), actualSoDates);
                                updateAvailItemized(roomPrice.getPrice(), actualAvailDates);
                            }
                        }
                    }

                });
            }
        });

    }

    private void updateAvailItemized(RoomTripPriceV2 price, List<Date> actualAvailDates) {
        actualAvailDates.forEach( availDate ->{
            price.getItemized().forEach(item->{
                if(item.getDate().equals(availDate)){
                    item.setUnavailabilityReason(null);
                }
            });
        });
    }

    private void updateSOItemized(RoomTripPriceV2 price, List<Date> actualSoDates) {
        actualSoDates.forEach( soDate ->{
            price.getItemized().forEach(item->{
                if(item.getDate().equals(soDate)){
                    item.setBasePrice(null);
                    item.setDiscountedPrice(null);
                    item.setDiscount(null);
                    item.setProgramId(null);
                    item.setBaseAmtAftTax(0);
                    item.setAmtAftTax(0);
                    item.setF1RoomAndTicketPrice(0);
                    item.setComp(false);

                }
            });
        });
    }

    private List<String> getSoProgramIds(Map<String, Map<String, List<Date>>> rateRoomDateSoMap) {
        return new ArrayList<>(rateRoomDateSoMap.keySet());
    }

    private List<String> getSoRoomIds(Map<String, Map<String, List<Date>>> rateRoomDateSoMap) {
        return rateRoomDateSoMap.values().stream().map(Map::keySet).collect(Collectors.toList()).stream().flatMap(Set::stream).collect(Collectors.toList());
    }

    private Map<String, Map<String, List<Date>>>getDateRateRoomSoMap(RoomAvailabilityCombinedResponse tripPricingResponse) {
        Map<String, Map<String, List<Date>>> rateRoomDateSoMap = new HashMap<>();
        tripPricingResponse.getRatePlans().forEach( ratePlan -> {
            List<RoomAvailabilityV2Response> soRooms = ratePlan.getRooms().stream().filter(RoomAvailabilityV2Response::isUnavailable).collect(Collectors.toList());
            if(CollectionUtils.isNotEmpty(soRooms)){
                Map<String, List<Date>> roomDateSoMap = new HashMap<>();
                rateRoomDateSoMap.put(ratePlan.getProgramId(),roomDateSoMap);
                soRooms.forEach(soRoom ->{
                    if(CollectionUtils.isNotEmpty(soRoom.getPrice().getItemized())){
                        List<Date> dates = new ArrayList<>();
                        soRoom.getPrice().getItemized().forEach(i ->{
                            if(StringUtils.isNotBlank(i.getUnavailabilityReason())){
                                dates.add(i.getDate());
                            }
                        });
                        roomDateSoMap.put(soRoom.getRoomTypeId(),dates);
                    }
                });
        }
        });
        return rateRoomDateSoMap;
    }

    private AuroraPriceRequest createAuroraPriceRequest(RoomAvailabilityV3Request availabilityRequest, List<String> soRoomIds, List<String> programIds){
        return   AuroraPriceRequest.builder()
                .propertyId(availabilityRequest.getPropertyId())
                .checkOutDate(availabilityRequest.getCheckOutDate())
                .checkInDate(availabilityRequest.getCheckInDate())
                .numGuests(availabilityRequest.getNumAdults())
                .source(availabilityRequest.getSource())
                .promo(availabilityRequest.getPromo())
                .customerId(availabilityRequest.getCustomerId())
                .customerRank(availabilityRequest.getCustomerRank())
                .mlifeNumber(availabilityRequest.getMlifeNumber())
                .promoCode(availabilityRequest.getPromoCode())
                .customerDominantPlay(availabilityRequest.getCustomerDominantPlay())
                .isGroupCode(availabilityRequest.getIsGroupCode())
                .numRooms(availabilityRequest.getNumRooms())
                .roomTypeIds(soRoomIds)
                .programIds(programIds)
                .isPerpetualOffer(availabilityRequest.isPerpetualPricing())
                .groupCnfNumber(availabilityRequest.getGroupCnfNumber())
                .build();
    }

    private void sortRatesNItemizedDates(RoomAvailabilityCombinedResponse response, RoomAvailabilityV3Request request) {
        if(CollectionUtils.isNotEmpty( response.getRatePlans())) {
            response.getRatePlans().stream()
                    .filter(ratePlan -> CollectionUtils.isNotEmpty(ratePlan.getRooms()))
                    .forEach(ratePlan -> ratePlan.getRooms().stream()
                            .filter(room -> null != room.getPrice()
                                    && CollectionUtils.isNotEmpty(room.getPrice().getItemized()))
                            .forEach(room -> {
                                List<PriceV2Itemized> itemized = room.getPrice().getItemized()
                                        .stream()
                                        .sorted(Comparator.comparing(PriceV2Itemized::getDate))
                                        .collect(Collectors.toList());
                                room.getPrice().setItemized(itemized);
                            })
                    );
            //sort ratePlans based on total discount price.
            if (StringUtils.isBlank(request.getProgramId()) && CollectionUtils.isEmpty(request.getProgramIds())) {
                if(null != applicationProperties.getTcolvPropertyId() && !applicationProperties.getTcolvPropertyId().equalsIgnoreCase(request.getPropertyId()) ) {
                    response.setRatePlans(response.getRatePlans().stream()
                            .filter(x -> null != x.getStartingPrice())
                            .sorted(Comparator.comparing(x -> x.getStartingPrice().getDiscountedAveragePrice()))
                            .collect(toList()));
                }
            }
        }
    }

    private void addF1CasinoDefaultComponentPrices(RoomAvailabilityV3Request request, RoomAvailabilityCombinedResponse response) {
        for (RatePlanV2Response ratePlan : response.getRatePlans()) {
            String name = ratePlan.getProgramId();
            String componentCode = getF1DefaultCasinoComponentCode(getRoomProgramTags(request, name));
            if (StringUtils.isNotEmpty(componentCode) && !componentCode.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG)) {
                Optional<String> roomTypeId = ratePlan.getRooms().stream().filter(x ->
                        StringUtils.isNotEmpty(x.getRoomTypeId())).findFirst().map(RoomAvailabilityV2Response::getRoomTypeId);
                if (roomTypeId.isPresent()) {
                    RoomComponent comp = componentDAO.getRoomComponentByCode(request.getPropertyId(),
                            componentCode, roomTypeId.get(), ratePlan.getProgramId(),
                            localDateToDate(request.getCheckInDate(), applicationProperties.getDefaultTimezone()),
                            localDateToDate(request.getCheckOutDate(), applicationProperties.getDefaultTimezone()),
                            request.getMlifeNumber(), request.getSource());
                    for (RoomAvailabilityV2Response room : ratePlan.getRooms()) {
                        if (null != comp && null != comp.getPrice()) {
                            double toAdd = getRoomComponentPrice(comp,
                                    localDateToDate(request.getCheckInDate(), applicationProperties.getDefaultTimezone()),
                                    localDateToDate(request.getCheckOutDate(), applicationProperties.getDefaultTimezone()));
                            room.getPrice().setTripSubtotal(room.getPrice().getTripSubtotal() + comp.getPrice());
                            room.getPrice().setBaseSubtotal(room.getPrice().getBaseSubtotal() + comp.getPrice());
                            room.getPrice().setDiscountedSubtotal(room.getPrice().getDiscountedSubtotal() + comp.getPrice());
                            room.getPrice().setDiscountedAveragePrice(room.getPrice().getDiscountedAveragePrice() + toAdd);
                            room.getPrice().setBaseAveragePrice(room.getPrice().getBaseAveragePrice() + toAdd);
                            for (PriceV2Itemized item : room.getPrice().getItemized()) {
                                item.setBasePrice(item.getBasePrice() + toAdd);
                                if (Boolean.TRUE.equals(item.getComp())) {
                                    item.setDiscountedPrice(0.0);
                                }
                                item.setDiscountedPrice(item.getDiscountedPrice() + toAdd);
                                item.setComp(false);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addF1DefaultPublicTicketComponentPrices(RoomAvailabilityV3Request request, RoomAvailabilityCombinedResponse response) {
        for (RatePlanV2Response ratePlan : response.getRatePlans()) {
            String name = ratePlan.getProgramId();
            List<String> roomProgramTags = getRoomProgramTags(request, name);
            String componentCode = ReservationUtil.getF1DefaultPublicTicketComponentCode(new ArrayList<>(roomProgramTags));

            if (null != request && null != request.getPropertyId() && StringUtils
					.equalsIgnoreCase(applicationProperties.getTcolvPropertyId(), request.getPropertyId())) {
            	componentCode = ReservationUtil.getTCOLVF1TicketComponentCode(new ArrayList<>(roomProgramTags));
            }
            if (StringUtils.isNotEmpty(componentCode)) {
                double dailyTicketComponentPriceToAdd = 0.0;
                double ticketComponentPrice = 0.0;
                Optional<RoomAvailabilityV2Response> roomAvailabilityV2Response = ratePlan.getRooms().stream().filter(x->
                                StringUtils.isNotEmpty(x.getRoomTypeId())).findFirst();

                if (roomAvailabilityV2Response.isPresent()) {
                    RoomComponent component = componentDAO.getRoomComponentByCode(request.getPropertyId(),
                            componentCode, roomAvailabilityV2Response.get().getRoomTypeId(), name,
                            localDateToDate(request.getCheckInDate(), applicationProperties.getDefaultTimezone()),
                            localDateToDate(request.getCheckOutDate(), applicationProperties.getDefaultTimezone()),
                            request.getMlifeNumber(), request.getSource());
                    if (null != component) {
                        dailyTicketComponentPriceToAdd = ReservationUtil.componentPriceToAdd(request.getCheckInDate(), request.getCheckOutDate(), component, true, applicationProperties);
                        ticketComponentPrice = ReservationUtil.componentPriceToAdd(request.getCheckInDate(), request.getCheckOutDate(), component, false, applicationProperties);
                    }
                    for (RoomAvailabilityV2Response room : ratePlan.getRooms()) {
                        RoomTripPriceV2 roomTripPrice = room.getPrice();
                        if (null != roomTripPrice) {
                            roomTripPrice.setF1TicketPrice(ticketComponentPrice);
                            //Adding null check since DiscountedAveragePrice is Double
                            if (null != roomTripPrice.getDiscountedAveragePrice()) {
                                roomTripPrice.setAverageNightlyF1TripPrice(roomTripPrice.getDiscountedAveragePrice() + dailyTicketComponentPriceToAdd);
                            } else {
                                roomTripPrice.setAverageNightlyF1TripPrice(dailyTicketComponentPriceToAdd);
                            }
                            if (null != roomTripPrice.getItemized()) {
                                for (PriceV2Itemized item : roomTripPrice.getItemized()) {
                                    //Adding null check since DiscountedPrice is Double
                                    if (null != item.getDiscountedPrice()) {
                                        item.setF1RoomAndTicketPrice(item.getDiscountedPrice() + dailyTicketComponentPriceToAdd);
                                    } else {
                                        //Should never happen
                                        item.setF1RoomAndTicketPrice(dailyTicketComponentPriceToAdd);
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }


    private List<String> getRoomProgramTags(RoomAvailabilityV3Request request, String programId) {
        List<String> roomProgramTags = new ArrayList<>();
        if (CommonUtil.isUuid(programId)) {
            // Flow for GSE segment GUID
            String[] gseProgramTags = roomProgramCacheService.getRoomProgram(programId).getTags();
            if (null != gseProgramTags && 0 != gseProgramTags.length) {
                roomProgramTags.addAll(Arrays.asList(gseProgramTags));
            }
        } else if (!ACRSConversionUtil.isAcrsGroupCodeGuid(programId)){
            log.debug("getRoomProgramTags called for nonUUID programID {} and request {}", programId, request);
            // For all non-group codes, do an ENR lookup
            RoomProgramV2Request acrsProgramDetailsRequest = createRoomV2Request (programId,
                    request.getCheckInDate().toString(), request.getCheckOutDate().toString());
            List<RoomOfferDetails> details = programDao.getRatePlanById(acrsProgramDetailsRequest);

            for (RoomOfferDetails roomOfferDetail : details) {
                if(CollectionUtils.isNotEmpty(roomOfferDetail.getTags())) {
                    roomProgramTags.addAll(roomOfferDetail.getTags());
                }
            }
        }

        return roomProgramTags;
    }

    private boolean checkF1Flow(RoomAvailabilityV3Request request) {
        //Adding in case we receive a call with programIds field
        List<String> programIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(request.getProgramIds())) {
            programIds.addAll(request.getProgramIds());
        } else if (StringUtils.isNotEmpty(request.getProgramId())) {
            programIds.add(request.getProgramId());
        }

        if (!programIds.isEmpty()) {
            Set<String> roomProgramTags = new HashSet<>();
            for (String rpcd : programIds) {
                roomProgramTags.addAll(getRoomProgramTags(request, rpcd));
            }
            if (!roomProgramTags.isEmpty() && roomProgramTags.contains(applicationProperties.getF1PackageTag())) {
                for (String validProductCode : applicationProperties.getValidF1ProductCodes()) {
                    if (roomProgramTags.contains(validProductCode)) {
                    	int ticketCount = ReservationUtil.getF1TicketCountFromF1Tag(roomProgramTags.stream().collect(Collectors.toList()), applicationProperties);
                    	validProductCode = ReservationUtil.getProductCodeForF1Program(validProductCode, roomProgramTags.stream().collect(Collectors.toList()),applicationProperties);
                        checkInventoryAvailability(productInventoryDAO.getInventory(validProductCode, true), ticketCount);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void validateProgram(RoomAvailabilityV3Request request, RoomProgramService programService) {

        if (request.getProgramIds()
                .size() == 1) {
            // validate program only if single program is used
            request.setProgramId(request.getProgramIds()
                    .get(0));

            validateProgram(programService, request);

            // reset program id once validation is done
            request.setProgramId(null);
        } else {
            validateProgram(programService, request);
        }
    }

    private void requestUpdateForPO(RoomAvailabilityV3Request request) {

        // if PO program is supplied for PO qualified user, drop the program
        if (request.isPerpetualPricing()) {
            if (StringUtils.isNotEmpty(request.getProgramId()) && programDao.isProgramPO(request.getProgramId())) {
                request.setProgramId(null);
                request.setPoProgramRequested(true);
            }
            request.setPoProgramRequested(request.getProgramIds()
                    .removeIf(p -> programDao.isProgramPO(p)));

        }
    }

    private RatePlanV2Response getRoomPricesAsRatePlan(RoomAvailabilityV3Request availabilityRequest, String replacementProgramId,
                                                       String promo) {

        // For ACRS, we are setting perpetual flag based on incoming rate plan
        if (null != replacementProgramId && !BaseCommonUtil.isUUID(replacementProgramId)) {
            String aCrsRatePlanCode = referenceDataDAOHelper.retrieveRatePlanDetail(availabilityRequest.getPropertyId(),
                    replacementProgramId);
            availabilityRequest.setPerpetualPricing(ACRSConversionUtil.isPORatePlan(aCrsRatePlanCode));
        }

        AuroraPriceRequest request = AuroraPriceRequestTransformer.getAuroraPriceV3Request(availabilityRequest, false,
                replacementProgramId, promo);

        AuroraPricesResponse pricesList = pricingDao.getRoomPricesV2(request);

        final List<AuroraPriceResponse> auroraPrices = pricesList.getAuroraPrices();
        Set<RoomAvailabilityV2Response> availResponse = getAvailableResponse(auroraPrices, availabilityRequest);

        RatePlanV2Response programResponse = new RatePlanV2Response();
        programResponse.setPromo(!CollectionUtils.isEmpty(auroraPrices) ? auroraPrices.get(0)
                .getPromo() : null);
        programResponse.setRooms(availResponse);
        // Set program starting price based on lowest priced room
        availabilityServiceHelper.setProgramStartingPrice(programResponse);

        if (StringUtils.isNotEmpty(replacementProgramId)) {
            programResponse.setProgramId(replacementProgramId);
        } else {
            String derivedProgramId = deriveDominantProgram(availResponse);
            if (StringUtils.isNotEmpty(derivedProgramId)) {
                programResponse.setProgramId(derivedProgramId);
            } else {
                log.warn("Derived Program Id is empty for availRequest {} and availResponse {}", availabilityRequest,
                        availResponse);
            }
        }

        return programResponse;
    }

    private String deriveDominantProgram(Set<RoomAvailabilityV2Response> availResponse) {

        if (availResponse.isEmpty()) {
            return StringUtils.EMPTY;
        }

        String programId = StringUtils.EMPTY;

        // If pricing response has at least 1 room with perpetualPricing=true,
        // use that PO program as dominant
        Optional<RoomAvailabilityV2Response> availOpt = availResponse.stream()
                .filter(RoomAvailabilityV2Response::isPerpetualPricing)
                .findFirst();
        if (availOpt.isPresent()) {
            programId = availOpt.get()
                    .getPoProgramId();
        } else {
            // if no PO pricing is found, find the dominant program for the
            // cheapest room i.e., program with most nights or in case of tie,
            // program on arrival date
            Optional<Entry<String, Long>> entry = availResponse.iterator()
                    .next()
                    .getPrice()
                    .getItemized()
                    .stream()
                    .filter(x -> StringUtils.isNotEmpty(x.getProgramId()))
                    .collect(groupingBy(PriceV2Itemized::getProgramId, Collectors.counting()))
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .findFirst();

            if (entry.isPresent()) {
                programId = entry.get()
                        .getKey();
            }
        }

        return programId;
    }

    private List<RatePlanV2Response> getRatePlans(RoomAvailabilityV3Request request) {
        AuroraPriceRequest auroraRequest = AuroraPriceRequestTransformer.getAuroraPriceV3Request(request, true,
                null, request.getPromo());

        // Iterate through list of availability and group by rate plan
        Map<String, List<AuroraPriceResponse>> ratePlanMap = new LinkedHashMap<>();
        AuroraPricesResponse auroraPrices = pricingDao.getRoomPricesV2(auroraRequest);
        auroraPrices.getAuroraPrices()
                .forEach(price -> availabilityServiceHelper.populatePlanMap(price, ratePlanMap));
        return populateRatePlanList(request, ratePlanMap);
    }

    /**
     * Converts the list of unavailable rooms to include the itemized rooms.
     *
     * @param unavailableRoomsMap
     *            List of price responses
     * @return Unavailable response by room type
     */
    protected Set<RoomAvailabilityV2Response> convertUnavailableResponse(
            Map<String, List<AuroraPriceResponse>> unavailableRoomsMap, boolean displaySoPrice) {

        // Using TreeSet to sort room using compareTo function
        Set<RoomAvailabilityV2Response> responseList = new TreeSet<>();

        for (Map.Entry<String, List<AuroraPriceResponse>> unavailableRoom : unavailableRoomsMap.entrySet()) {

            RoomAvailabilityV2Response availabilityResponse = new RoomAvailabilityV2Response();
            availabilityResponse.setRoomTypeId(unavailableRoom.getKey());
            availabilityResponse.setUnavailable(true);

            List<PriceV2Itemized> itemizedPriceList = new ArrayList<>();

            // Construct itemized prices for each room along
            for (AuroraPriceResponse priceResponse : unavailableRoom.getValue()) {

                PriceV2Itemized itemizedPrice = new PriceV2Itemized();
                if (StringUtils.isNotBlank(priceResponse.getUnavailabilityReason())) {
                    if(displaySoPrice){
                        itemizedPrice = availabilityServiceHelper.getItemizedPriceV2(priceResponse);
                        itemizedPrice.setUnavailabilityReason(priceResponse.getUnavailabilityReason());
                    }else {
                        itemizedPrice.setDate(priceResponse.getDate());
                        itemizedPrice.setUnavailabilityReason(priceResponse.getUnavailabilityReason());
                    }
                } else {
                    itemizedPrice = availabilityServiceHelper.getItemizedPriceV2(priceResponse);
                }

                // If at least 1 night isPOApplicable=true, then set PerpetualPricing=true at availabilityResponse level.
                if (priceResponse.isPOApplicable()) {
                    availabilityResponse.setPerpetualPricing(true);
                    availabilityResponse.setPoProgramId(priceResponse.getProgramId());
                }

                itemizedPriceList.add(itemizedPrice);
            }

            RoomTripPriceV2 tripPrice = new RoomTripPriceV2();
            tripPrice.setItemized(itemizedPriceList);
            availabilityResponse.setPrice(tripPrice);
            availabilityResponse.setResortFee(0.0);

            responseList.add(availabilityResponse);

        }

        return responseList;
    }

    protected Set<RoomAvailabilityV2Response> getAvailableResponse(List<AuroraPriceResponse> prices,
            RoomAvailabilityV3Request availabilityRequest) {

        Map<String, List<AuroraPriceResponse>> allRoomsMap = new HashMap<>();
        prices.forEach(price -> {

            // Populates the map with all the rooms irrespective of their status
            if (allRoomsMap.containsKey(price.getRoomTypeId())) {
                allRoomsMap.get(price.getRoomTypeId())
                        .add(price);
            } else {
                List<AuroraPriceResponse> priceList = new ArrayList<>();
                priceList.add(price);
                allRoomsMap.put(price.getRoomTypeId(), priceList);
            }

        });

        long noOfNights = Duration.between(availabilityRequest.getCheckInDate()
                .atStartOfDay(),
                availabilityRequest.getCheckOutDate()
                        .atStartOfDay())
                .toDays();

        // Iterate through list of availability and group by available vs
        // unavailable
        Map<String, List<AuroraPriceResponse>> availableRoomsMap = new HashMap<>();
        Map<String, List<AuroraPriceResponse>> unavailableRoomsMap = new HashMap<>();

        for (Map.Entry<String, List<AuroraPriceResponse>> roomEntry : allRoomsMap.entrySet()) {

            // As long as one of the night is unavailable, mark the room as
            // unavailable
            boolean isUnavailable = roomEntry.getValue()
                    .size() != noOfNights || roomEntry.getValue()
                            .stream()
                            .anyMatch(price -> !price.getStatus()
                                    .equals(AvailabilityStatus.AVAILABLE));

            log.debug("Room: {}, Available: {}, Size: {}", roomEntry.getKey(), !isUnavailable, roomEntry.getValue().size());
            
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
            roomAvailability.addAll(convertUnavailableResponse(unavailableRoomsMap,availabilityRequest.isDisplaySoPrice()));
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
    protected Set<RoomAvailabilityV2Response> convertAvailabilityResponse(
            Map<String, List<AuroraPriceResponse>> roomMap, RoomAvailabilityV3Request availabilityRequest) {

        // Using TreeSet to sort room using compareTo function
        Set<RoomAvailabilityV2Response> responseList = new TreeSet<>();
        String programId = availabilityRequest.getProgramId();

        long noOfNights = Duration.between(availabilityRequest.getCheckInDate()
                .atStartOfDay(),
                availabilityRequest.getCheckOutDate()
                        .atStartOfDay())
                .toDays();

        for (List<AuroraPriceResponse> tripPrices : roomMap.values()) {

            // If program is applied and none of the nights includes the
            // program, ignore the room. programCheck flag is to avoid program
            // existence check as needed
            if (StringUtils.isNotEmpty(programId)
                    && !availabilityServiceHelper.isProgramIncluded(tripPrices, programId)) {
                continue;
            }

            double price = 0;
            double discPrice = 0;
            double resortFee = 0;
            double amtAftTax = 0;
            double baseAmtAftTax = 0;

            List<PriceV2Itemized> itemizedPriceList = new ArrayList<>();
            String roomTypeId = null;
            RoomAvailabilityV2Response availabilityResponse = new RoomAvailabilityV2Response();

            // Construct itemized prices and member prices for each room along
            // with total of prices
            for (AuroraPriceResponse priceResponse : tripPrices) {
                roomTypeId = priceResponse.getRoomTypeId();
                itemizedPriceList.add(availabilityServiceHelper.getItemizedPriceV2(priceResponse));
                price += priceResponse.getBasePrice();
                discPrice += priceResponse.isComp() ? 0 : priceResponse.getDiscountedPrice();
                amtAftTax += priceResponse.isComp() ? 0 : priceResponse.getAmtAftTax();
                baseAmtAftTax += priceResponse.getBaseAmtAftTax();
                resortFee += priceResponse.getResortFee();
                if (priceResponse.isPOApplicable()) {
                    setPoProgram(availabilityResponse, priceResponse);
                }
            }
            availabilityResponse.setRoomTypeId(roomTypeId);
            // Set property level resort fee
            availabilityResponse.setResortFee(resortFee / noOfNights);
            availabilityResponse.setBaseAmtAftTax(baseAmtAftTax);
            availabilityResponse.setAmtAftTax(amtAftTax);
            // Include base price totals and averages
            availabilityResponse.setPrice(availabilityServiceHelper.getRoomTripPriceV2(noOfNights, price, discPrice,
                    itemizedPriceList, resortFee));

            responseList.add(availabilityResponse);
        }

        return responseList;
    }

    protected List<RatePlanV2Response> populateRatePlanList(RoomAvailabilityV3Request request,
            Map<String, List<AuroraPriceResponse>> ratePlanMap) {
        // Iterate through rooms for each program, remove unavailable rooms, get
        // itemized price responses
        List<RatePlanV2Response> ratePlanList = new LinkedList<>();
        ratePlanMap.keySet()
                .forEach(key -> {
                    RatePlanV2Response response = new RatePlanV2Response();
                    response.setProgramId(key);
                    log.debug("Program Id {}: No of rooms {}", key, ratePlanMap.get(key)
                            .size());
                    response.setRooms(getAvailableResponse(ratePlanMap.get(key), request));
                    response.setPromo(!CollectionUtils.isEmpty(ratePlanMap.get(key)) ? ratePlanMap.get(key)
                            .get(0)
                            .getPromo() : null);
                    // Set program starting price based on lowest priced room
                    availabilityServiceHelper.setProgramStartingPrice(response);
                    ratePlanList.add(response);
                });
        return ratePlanList;
    }

    private void setPoProgram(RoomAvailabilityV2Response availabilityResponse, AuroraPriceResponse priceResponse) {
        availabilityResponse.setPerpetualPricing(true);
        if (!ACRSConversionUtil.isAcrsRatePlanGuid(priceResponse.getProgramId())) {
            availabilityResponse.setPoProgramId(priceResponse.getProgramId());
        } else {
            if (StringUtils.isEmpty(availabilityResponse.getPoProgramId())) {
                availabilityResponse.setPoProgramId(priceResponse.getProgramId());
            } else {
                String existingPOProgram = ACRSConversionUtil.getRatePlanCode(availabilityResponse.getPoProgramId());
                String priceRespPOProgram = ACRSConversionUtil.getRatePlanCode(priceResponse.getProgramId());
                if (priceRespPOProgram.startsWith(ServiceConstant.COMP_STRING)) {
                    availabilityResponse.setPoProgramId(priceResponse.getProgramId());
                } else if (!existingPOProgram.startsWith(ServiceConstant.COMP_STRING) &&
                        priceRespPOProgram.startsWith(ServiceConstant.CASH_STRING)) {
                    availabilityResponse.setPoProgramId(priceResponse.getProgramId());
                }
            }
        }
    }
}