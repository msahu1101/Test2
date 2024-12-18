package com.mgm.services.booking.room.transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.Room;
import com.mgm.services.booking.room.model.content.CuratedOffer;
import com.mgm.services.booking.room.model.content.CuratedOfferResponse;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.ApplicableProgramsRequest;
import com.mgm.services.booking.room.model.request.CustomerOffersRequest;
import com.mgm.services.booking.room.model.request.CustomerOffersV3Request;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.CustomerOffersRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.response.CustomerOffer;
import com.mgm.services.booking.room.model.response.CustomerOfferDetail;
import com.mgm.services.booking.room.model.response.CustomerOfferResponse;
import com.mgm.services.booking.room.model.response.CustomerOfferType;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.model.response.RoomOfferDetails;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.util.DateUtil;
import com.mgm.services.common.util.ValidationUtil;
import com.mgmresorts.aurora.common.TripParams;
import com.mgmresorts.aurora.messages.GetApplicableProgramsRequest;
import com.mgmresorts.aurora.messages.GetCustomerOffersRequest;
import com.mgmresorts.aurora.messages.GetCustomerOffersResponse;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgm.services.booking.room.util.ReservationUtil;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * This transforms the request from one form to another as it passes multiple
 * layer.
 * 
 * @author jayveera
 *
 */
@Log4j2
@UtilityClass
public class RoomProgramsTransformer {

    /**
     * Transforms the request to DTO.
     * 
     * @param applicableProgramsRequest room program request.
     * @return room program request
     */
    public static ApplicableProgramRequestDTO buildApplicableProgramRequest(
            ApplicableProgramsRequest applicableProgramsRequest) {
        return ApplicableProgramRequestDTO.builder()
                .source(applicableProgramsRequest.getSource())
                .mlifeNumber(applicableProgramsRequest.getMlifeNumber())
                .propertyId(applicableProgramsRequest.getPropertyId())
                .roomTypeId(applicableProgramsRequest.getRoomTypeId())
                .bookDate((applicableProgramsRequest.getBookDate() == null ? null
                        : CommonUtil.getDate(applicableProgramsRequest.getBookDate(),
                                ServiceConstant.ISO_8601_DATE_FORMAT)))
                .travelDate((applicableProgramsRequest.getTravelDate() == null ? null
                        : CommonUtil.getDate(applicableProgramsRequest.getTravelDate(),
                                ServiceConstant.ISO_8601_DATE_FORMAT)))
                .filterBookable(applicableProgramsRequest.isFilterBookable())
                .filterViewable(applicableProgramsRequest.isFilterViewable())
                .checkInDate((applicableProgramsRequest.getCheckInDate() == null ? null
                        : CommonUtil.getDate(applicableProgramsRequest.getCheckInDate(),
                                ServiceConstant.ISO_8601_DATE_FORMAT)))
                .checkOutDate((applicableProgramsRequest.getCheckOutDate() == null ? null
                        : CommonUtil.getDate(applicableProgramsRequest.getCheckOutDate(),
                                ServiceConstant.ISO_8601_DATE_FORMAT)))
                .numAdults(applicableProgramsRequest.getNumAdults())
                .numChildren(applicableProgramsRequest.getNumChildren()).source(applicableProgramsRequest.getSource())
                .customerId(applicableProgramsRequest.getCustomerId()).build();
    }

    /**
     * Transforms the DTO to aurora request.
     * 
     * @param applicableProgramRequestDTO - room programs request.
     * @return get applicable programs request
     */
    public static GetApplicableProgramsRequest buildAuroraRequestForGetApplicableProgramsRequest(
            ApplicableProgramRequestDTO applicableProgramRequestDTO) {
        GetApplicableProgramsRequest getApplicableProgramsRequest = MessageFactory.createGetApplicableProgramsRequest();
        getApplicableProgramsRequest.setPropertyId(applicableProgramRequestDTO.getPropertyId());
        getApplicableProgramsRequest.setCustomerId(applicableProgramRequestDTO.getCustomerId());
        getApplicableProgramsRequest.setRoomTypeId(applicableProgramRequestDTO.getRoomTypeId());
        getApplicableProgramsRequest.setBookDate(applicableProgramRequestDTO.getBookDate());
        getApplicableProgramsRequest.setTravelDate(applicableProgramRequestDTO.getTravelDate());
        if (applicableProgramRequestDTO.getCheckInDate() != null
                && applicableProgramRequestDTO.getCheckOutDate() != null
                && applicableProgramRequestDTO.getNumAdults() > 0) {
            TripParams tripParams = TripParams.create();
            tripParams.setDepartureDate(DateUtil.toDate(ReservationUtil.convertDateToLocalDateAtSystemDefault(applicableProgramRequestDTO.getCheckOutDate())));
            tripParams.setArrivalDate(DateUtil.toDate(ReservationUtil.convertDateToLocalDateAtSystemDefault(applicableProgramRequestDTO.getCheckInDate())));
            tripParams.setNumAdults(applicableProgramRequestDTO.getNumAdults());
            tripParams.setNumChildren(applicableProgramRequestDTO.getNumChildren());
            getApplicableProgramsRequest.setTripParams(tripParams);
        }
        getApplicableProgramsRequest.setFilterViewable(applicableProgramRequestDTO.isFilterViewable());
        getApplicableProgramsRequest.setFilterBookable(applicableProgramRequestDTO.isFilterBookable());

        log.info("Request sent to getApplicablePrograms as : {}", getApplicableProgramsRequest.toJsonString());
        return getApplicableProgramsRequest;
    }

    /**
     * Transform the request to DTO.
     * 
     * @param request - from controller.
     * @return the DTO
     */
    public static CustomerOffersRequestDTO buildGetCustomerOffersRequest(
            CustomerOffersRequest request) {
        return CustomerOffersRequestDTO.builder()
                .mlifeNumber(request.getMlifeNumber())
                .propertyId(request.getPropertyId())
                .notRolledToSegments(request.isNotRolledToSegments()).notSorted(request.isNotSorted())
                .source(request.getSource())
                .customerId(request.getCustomerId()).mlifeNumber(request.getMlifeNumber()).build();
    }

    /**
     * Transform the request to DTO.
     * 
     * @param request - from controller.
     * @return the DTO
     */
    public static CustomerOffersRequestDTO buildGetCustomerOffersRequest(
            RoomProgramsRequestDTO request) {
        return CustomerOffersRequestDTO.builder()
                .mlifeNumber(request.getMlifeNumber())
                .propertyId(request.getPropertyId())
                .notRolledToSegments(true).notSorted(true)
                .source(request.getSource())
                .customerId(request.getCustomerId()).mlifeNumber(request.getMlifeNumber()).build();
    }

    
    /**
     * builds the Aurora request for GetCustomerOffersRequest API from DTO.
     * 
     * @param requestDTO - dto from service layer.
     * @return aurora request.
     */
    public static GetCustomerOffersRequest buildAuroraGetCustomerOffersRequest(
            CustomerOffersRequestDTO requestDTO) {
        GetCustomerOffersRequest request = MessageFactory.createGetCustomerOffersRequest();
        request.setPropertyId(requestDTO.getPropertyId());
        request.setNotRolledToSegments(requestDTO.isNotRolledToSegments());
        request.setNotSorted(requestDTO.isNotSorted());
        request.setWantCommentary(requestDTO.isWantCommentary());
        request.setCustomerId(requestDTO.getCustomerId());
        log.debug("Request sent to getCustomerOffersRequest as : {}", request.toJsonString());

        return request;
    }

    /**
     * Transforms the Aurora API response to Customer Offer response DTO.
     * 
     * @param response aurora response.
     * @return room offers for the customer
     */
    public static CustomerOfferResponse transform(GetCustomerOffersResponse response) {
        CustomerOfferResponse customerProgramSearchResponse = new CustomerOfferResponse();
        List<CustomerOfferDetail> offerList = new ArrayList<>();
        Arrays.stream(response.getOffers()).forEach(offer -> {
            CustomerOfferDetail offerDetail = new CustomerOfferDetail();
            offerDetail.setId(offer.getId());
            if (offer.getType() != null) {
                offerDetail.setOfferType(offer.getType().name());
            }
            if (offer.getPromotion() != null) {
                offerDetail.setPromoId(offer.getPromotion().getPromoId());
                offerDetail.setName(offer.getPromotion().getName());
                offerDetail.setStatus(offer.getPromotion().getStatus());
                offerDetail.setDescription(offer.getPromotion().getDescription());
                offerDetail.setStartDate(offer.getPromotion().getStartDate());
                offerDetail.setCustomerRank(offer.getPromotion().getCustomerRank());
                offerDetail.setSegmentFrom(offer.getPromotion().getSegmentFrom());
                offerDetail.setSegmentTo(offer.getPromotion().getSegmentTo());
                offerDetail.setDefaultPerpetualOffer(offer.getPromotion().getDefaultPerpetualOffer());
                offerDetail.setPropertyId(offer.getPromotion().getPropertyId());
            }
            offerList.add(offerDetail);
        });
        customerProgramSearchResponse.setOffers(offerList);
        return customerProgramSearchResponse;
    }
    
    /**
     * Transform the request to DTO.
     * 
     * @param validateRequest
     *            the Room Program Validate Request.
     * @return the Customer Offers Request DTO
     */
    public static CustomerOffersRequestDTO buildGetCustomerOffersRequest(RoomProgramValidateRequest validateRequest) {
        String propertyId = validateRequest.getPropertyId();
        boolean notRolledToSegment = false;
        String source = validateRequest.getSource();
        if (ValidationUtil.isUuid(validateRequest.getSource())) { // non-mlife
            propertyId = validateRequest.getSource();
            notRolledToSegment = true;
        }
        if (StringUtils.isNotEmpty(validateRequest.getPropertyId())) {
            notRolledToSegment = true;
            source = validateRequest.getPropertyId();
        }
        return CustomerOffersRequestDTO.builder().propertyId(propertyId).notRolledToSegments(notRolledToSegment)
                .source(source).customerId(validateRequest.getCustomerId())
                .mlifeNumber(validateRequest.getMlifeNumber()).build();
    }

    /**
     * Transforms the request to DTO. The source is explicitly set to mgmresorts
     * to ensure that the search of programs spans across properties.
     * @param validateRequest
     *            the Room Program Validate Request.
     * @param propertyId
     *            the property Id
     * @return room program request
     */
    public static ApplicableProgramRequestDTO buildApplicableProgramRequest(RoomProgramValidateRequest validateRequest,
            String propertyId) {
        return ApplicableProgramRequestDTO.builder().propertyId(propertyId).bookDate(new Date())
                .source("mgmresorts").customerId(validateRequest.getCustomerId()).build();
    }

    /**
     * Transforms the DTO to aurora request.
     *
     * @param groupSearchV2Request - group program request.
     * @return get applicable programs request
     */
    public static GetApplicableProgramsRequest buildAuroraRequestForGetApplicableProgramsRequest(
            GroupSearchV2Request groupSearchV2Request) {
        GetApplicableProgramsRequest getApplicableProgramsRequest = MessageFactory.createGetApplicableProgramsRequest();
        getApplicableProgramsRequest.setPropertyId(groupSearchV2Request.getPropertyId());
        getApplicableProgramsRequest.setCustomerId(groupSearchV2Request.getCustomerId() <= 0 ? -1 : groupSearchV2Request.getCustomerId());
        //CBSR-2241 Remove book date in request .
        final Date startDate = groupSearchV2Request.getStartDate() != null ? CommonUtil.getDate(groupSearchV2Request.getStartDate(), ServiceConstant.ISO_8601_DATE_FORMAT) : null;
        final Date endDate = groupSearchV2Request.getEndDate() != null ? CommonUtil.getDate(groupSearchV2Request.getEndDate(), ServiceConstant.ISO_8601_DATE_FORMAT) : null;
        getApplicableProgramsRequest.setTravelDate(startDate);
        if (startDate != null && endDate != null) {
            final TripParams tripParams = TripParams.create();
            tripParams.setArrivalDate(startDate);
            tripParams.setDepartureDate(endDate);
            tripParams.setNumAdults(1);
            tripParams.setNumChildren(0);
            getApplicableProgramsRequest.setTripParams(tripParams);
        }
        getApplicableProgramsRequest.setFilterBookable(true);
        log.debug("Request sent to getApplicablePrograms as : {}", getApplicableProgramsRequest.toJsonString());
        return getApplicableProgramsRequest;
    }

    /**
     * This method transform the Phoenix Program information into Room Program Detail Object
     * @param program
     * @return
     */
    public static RoomOfferDetails buildRoomProgramDetail(RoomProgram program) {

        RoomOfferDetails roomProgramResponse = null;

        if (null != program) {

            roomProgramResponse = RoomOfferDetails.builder()
                    .id(program.getId())
                    .name(program.getName())
                    .propertyId(program.getPropertyId())
                    .active(program.isActiveFlag())
                    .category(program.getCategory())
                    .description(program.getDescription())
                    .shortDescription(program.getShortDescription())
                    .termsAndConditions(program.getTermsAndConditions())
                    .agentText(program.getAgentText())
                    .learnMoreDescription(program.getLearnMoreDescription())
                    .minNights(program.getMinNights())
                    .maxNights(program.getMaxNights())
                    .barProgram(null != program.getPublicProgram() && program.getPublicProgram())
                    .publicProgram(null != program.getPublicOfferFlag() && program.getPublicOfferFlag())
                    .patronPromoId(program.getPatronPromoId())
                    .promoCode(program.getPromoCode())
                    .operaGuaranteeCode(program.getOperaGuaranteeCode())
                    .periodStartDate(program.getPeriodStartDate())
                    .periodEndDate(program.getPeriodEndDate())
                    .travelPeriodStart(program.getTravelPeriodStart())
                    .travelPeriodEnd(program.getTravelPeriodEnd())
                    .cutOffDate(program.getBookBy())
                    .groupCode(program.getOperaBlockCode())
                    .operaBlockCode(program.getOperaBlockCode())
                    .operaBlockName(program.getOperaBlockName())
                    .reservationMethod(program.getReservationMethod())
                    .customerRank(program.getCustomerRank())
                    .segmentFrom(program.getSegmentFrom())
                    .segmentTo(program.getSegmentTo())
                    .multiRateSequenceNo(program.getMultiRateSequenceNo())
                    .roomIds(Arrays.asList(null != program.getRooms() ? program.getRooms() : new String[0]))
                    .tags(Arrays.asList(null != program.getTags() ? program.getTags() : new String[0]))
                    //.playerTiers()
                    .build();
        }

        return roomProgramResponse;
    }

    /**
     * This method transform the Phoenix Program information into Room Program Detail Object
     * @param program
     * @return
     */
    public static RoomOfferDetails buildRoomProgramDetail(GroupSearchV2Response program) {
        RoomOfferDetails roomProgramResponse = null;

        if (null != program) {
            roomProgramResponse = RoomOfferDetails.builder()
                    .id(program.getId())
                    .name(program.getName())
                    .propertyId(program.getPropertyId())
                    .active(program.isActiveFlag())
                    .category(program.getCategory())
                    .description(program.getDescription())
                    .shortDescription(program.getShortDescription())
                    //.termsAndConditions()
                    .agentText(program.getAgentText())
                    .learnMoreDescription(program.getLearnMoreDescription())
                    .barProgram(false)
                    .publicProgram(false)
                    .promoCode(null)
                    //.operaGuaranteeCode()
                    .periodStartDate(program.getPeriodStartDate())
                    .periodEndDate(program.getPeriodEndDate())
                    .travelPeriodStart(program.getTravelPeriodStart())
                    .travelPeriodEnd(program.getTravelPeriodEnd())
                    // Group Plan related fields
                    .groupCode(program.getGroupCode())
                    .operaBlockCode(program.getOperaBlockCode())
                    .operaBlockName(program.getOperaBlockName())
                    .reservationMethod(program.getReservationMethod())
                    .availableInIce(true)
                    .bookableByProperty(true)
                    .bookableOnline(true)
                    .viewOnline(true)
                    .viewableByProperty(true)
                    .roomIds(program.getRooms() != null ? program.getRooms().stream().map(Room :: getId).collect(Collectors.toList()) : new ArrayList<>())
                    .build();
        }

        return roomProgramResponse;
    }

    public static GroupSearchV2Response buildGroupSearchResponse(RoomProgram roomProgram) {
        return GroupSearchV2Response.builder()
                .id(roomProgram.getId())
                .activeFlag(roomProgram.isActiveFlag())
                .category(roomProgram.getCategory())
                .name(roomProgram.getName())
                .description(roomProgram.getDescription())
                .shortDescription(roomProgram.getShortDescription())
                .learnMoreDescription(roomProgram.getLearnMoreDescription())
                .groupCode(roomProgram.getOperaBlockCode())
                .operaBlockCode(roomProgram.getOperaBlockCode())
                .operaBlockName(roomProgram.getOperaBlockName())
                .operaGuaranteeCode(roomProgram.getOperaGuaranteeCode())
                .reservationMethod(roomProgram.getReservationMethod())
                .periodStartDate(roomProgram.getPeriodStartDate())
                .periodEndDate(roomProgram.getPeriodEndDate())
                .travelPeriodStart(roomProgram.getTravelPeriodStart())
                .travelPeriodEnd(roomProgram.getTravelPeriodEnd())
                .rooms(roomProgram.getRooms() != null ? Arrays.stream(roomProgram.getRooms()).map(Room::new).collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    public static List<CustomerOffer> buildCustomerOffersResponse(CuratedOfferResponse curatedOffers, String region, boolean memberPrograms, boolean includeNonBookableOnline) {
        
        List<CustomerOffer> customerOffers = new ArrayList<>();
        
        List<CuratedOffer> filterCuratedOffers = new ArrayList<>();
        
        if (StringUtils.isEmpty(region) || region.equalsIgnoreCase("all")) {
            // return offers across all regions
            curatedOffers.getCuratedOffersList().forEach(category ->
                {
                    if(CollectionUtils.isNotEmpty(category.getOffers())) {
                        filterCuratedOffers.addAll(category.getOffers());
                    }
                });
        } else if (region.equalsIgnoreCase("LV")) {
            // return only LV resorts offers
            curatedOffers.getCuratedOffersList().forEach(category -> {
                if (category.getCategory().equalsIgnoreCase("hotel-offers-lv")) {
                    filterCuratedOffers.addAll(category.getOffers());
                }
            });
        } else if (region.equalsIgnoreCase("NONLV")) {
            // return only non-LV resorts offers
            curatedOffers.getCuratedOffersList().forEach(category -> {
                if (category.getCategory().equalsIgnoreCase("hotel-offers-non-lv")) {
                    filterCuratedOffers.addAll(category.getOffers());
                }
            });
        }
        
        // filter non bookable programs
        if (!includeNonBookableOnline) {
            filterCuratedOffers.removeIf(o -> !o.isBookableOnline());
        }
        
        // filter programs to get member only programs or transient programs
        filterCuratedOffers.removeIf(o -> o.isMemberOffer() != memberPrograms);
        
        filterCuratedOffers.forEach(offer -> {
            CustomerOffer customerOffer = new CustomerOffer();
            customerOffer.setId(offer.getId());
            // add curated offers path
            if (StringUtils.isNotEmpty(offer.getPath())) {
                customerOffer.setContentPath(offer.getPath());
            }
            // add curated offers content type
            if(StringUtils.isNotEmpty(offer.getContentType())) {
            	customerOffer.setContentType(offer.getContentType());
            }
            if(offer.isMultiOffer()) {
                customerOffer.setType(CustomerOfferType.SEGMENT);
            } else {
                customerOffer.setType(CustomerOfferType.PROGRAM);
                customerOffer.setPropertyId(offer.getPropertyId());
            }
            customerOffers.add(customerOffer);
        });
        
        return customerOffers;
    }
    
    public static List<CustomerOffer> filterByRegion(List<CustomerOffer> customerOffers, List<Property> properties) {

        List<String> propertyIds = properties.stream().map(Property::getId).collect(Collectors.toList());

        if (properties.isEmpty()) {
            // no filtering to be applied
            return customerOffers;
        } else {
            return customerOffers.stream().filter(
                    o -> o.getType().equals(CustomerOfferType.SEGMENT) || propertyIds.contains(o.getPropertyId()))
                    .collect(Collectors.toList());
        }
    }
    
    public static List<CustomerOffer> buildCustomerOffersResponse(List<RoomProgramDTO> programsList) {
        
        List<CustomerOffer> customerOffers = new ArrayList<>();
        
        programsList.forEach(program -> {
            CustomerOffer customerOffer = new CustomerOffer();
            customerOffer.setId(program.getProgramId());
            customerOffer.setPropertyId(program.getPropertyId());
            customerOffer.setType(StringUtils.isEmpty(program.getPropertyId()) ? CustomerOfferType.SEGMENT : CustomerOfferType.PROGRAM);
            customerOffers.add(customerOffer);
        });
        
        return customerOffers;
    }
    
    public static List<CustomerOffer> buildCustomerOffersResponseWithSorting(List<RoomProgramDTO> programsList,
            List<Property> properties) {

        List<CustomerOffer> customerOffers = new LinkedList<>();

        Map<String, Integer> propSortOrderMap = new HashMap<>();
        properties.forEach(property -> {
            if (StringUtils.isNotBlank(property.getCorporateSortOrder())
                    && NumberUtils.isNumber(property.getCorporateSortOrder())) {
                propSortOrderMap.put(property.getId(), Integer.parseInt(property.getCorporateSortOrder()));
            } else {
                log.info("Property {} doesn't have corporate sort number from content", property.getId());
                // property with no corporate sort order will be pushed to
                // bottom in sorted list
                propSortOrderMap.put(property.getId(), 99);
            }

        });

        programsList.forEach(program -> {
            CustomerOffer customerOffer = new CustomerOffer();
            customerOffer.setId(program.getProgramId());
            customerOffer.setPropertyId(program.getPropertyId());
            customerOffer.setType(StringUtils.isEmpty(program.getPropertyId()) ? CustomerOfferType.SEGMENT
                    : CustomerOfferType.PROGRAM);
            customerOffer.setPoProgram(true);
            customerOffers.add(customerOffer);
        });
        
        customerOffers.sort((o1, o2) -> propSortOrderMap.getOrDefault(o1.getPropertyId(), 99).compareTo(propSortOrderMap.getOrDefault(o2.getPropertyId(), 99)));

        return customerOffers;
    }
    
    public static List<CustomerOffer> buildCustomerOffersResponse(List<RoomProgramDTO> programsList,
            String enableNewSegments, String channel) {

        List<CustomerOffer> customerOffers = new ArrayList<>();

        if (enableNewSegments.equals("false") || ServiceConstant.ICE.equalsIgnoreCase(channel)) {
            log.info("Using GSE segments");
            // if new segments is not enabled, GSE DAO would have returned segments
            programsList.forEach(program -> {
                CustomerOffer customerOffer = new CustomerOffer();
                customerOffer.setId(program.getProgramId());
                customerOffer.setPropertyId(program.getPropertyId());
                customerOffer.setPromo(program.getPromo());
                customerOffer.setType(StringUtils.isEmpty(program.getPropertyId()) ? CustomerOfferType.SEGMENT : CustomerOfferType.PROGRAM);
                customerOffers.add(customerOffer);
            });

        } else {
            log.info("Using new segments approach by rate plan code");
            // group by rate plan code
			Map<String, List<RoomProgramDTO>> ratePlanMap = programsList.stream()
					.filter(roomProgram -> roomProgram.getRatePlanCode() != null)
					.collect(Collectors.groupingBy(RoomProgramDTO::getRatePlanCode));
            ratePlanMap.keySet().forEach(key -> {

                if (ratePlanMap.get(key).size() > 1) {
                    // if rate plan code has multiple programs, treat it as segment
                    CustomerOffer customerOffer = new CustomerOffer();
                    customerOffer.setId(key);
                    customerOffer.setType(CustomerOfferType.SEGMENT);
                    customerOffers.add(customerOffer);
                } else {
                    // Otherwise return as single-property programs
                    ratePlanMap.get(key).forEach(p -> {
                        CustomerOffer customerOffer = new CustomerOffer();
                        customerOffer.setId(p.getProgramId());
                        customerOffer.setPropertyId(p.getPropertyId());
                        customerOffer.setPromo(p.getPromo());
                        customerOffer.setType(CustomerOfferType.PROGRAM);
                        customerOffers.add(customerOffer);
                    });
                }
            });
        }

        return customerOffers;
    }
    
    public static RoomProgramsRequestDTO buildRoomProgramsRequestDTO(CustomerOffersV3Request offersRequest) {
        return RoomProgramsRequestDTO.builder().channel(offersRequest.getChannel()).source(offersRequest.getSource())
                .customerId(offersRequest.getCustomerId()).mlifeNumber(offersRequest.getMlifeNumber())
                .perpetualPricing(offersRequest.isPerpetualPricing()).propertyId(offersRequest.getPropertyId())
                .resortPricing(offersRequest.isResortPricing()).build();
    }

}
