/**
 * 
 */
package com.mgm.services.booking.room.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.dao.GroupSearchDAO;
import com.mgm.services.booking.room.dao.ProgramContentDAO;
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.exception.AuroraError;
import com.mgm.services.booking.room.model.OfferType;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.content.CuratedOfferResponse;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.request.ApplicableProgramsRequest;
import com.mgm.services.booking.room.model.request.CustomerOffersRequest;
import com.mgm.services.booking.room.model.request.CustomerOffersV3Request;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.request.PerpetualProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramRequest;
import com.mgm.services.booking.room.model.request.RoomProgramV2Request;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.response.ApplicableProgramsResponse;
import com.mgm.services.booking.room.model.response.CustomerOffer;
import com.mgm.services.booking.room.model.response.CustomerOfferResponse;
import com.mgm.services.booking.room.model.response.CustomerOfferV3Response;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.model.response.PerpetaulProgram;
import com.mgm.services.booking.room.model.response.RoomOfferDetails;
import com.mgm.services.booking.room.model.response.RoomProgram;
import com.mgm.services.booking.room.model.response.RoomProgramSegmentResponse;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import com.mgm.services.booking.room.model.response.RoomSegmentResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.PropertyContentCacheService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.transformer.RoomProgramsTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class to provide services for offer related functionalities
 * like get room offers, validate room offer.
 */
@Component
@Log4j2
public class RoomProgramServiceImpl implements RoomProgramService {

    @Autowired
    private AuroraProperties auroraProperties;
    
    @Autowired
    private SecretsProperties secretProperties;
    
    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private RoomProgramDAO roomProgramDao;

    @Autowired
    private ProgramContentDAO programContentDAO;

    @Autowired
    private RoomProgramCacheService roomProgramCacheService;
    
    @Autowired
    private PropertyContentCacheService propertyCacheService;
    
    @Autowired
    private GroupSearchDAO groupSearchDAO;

    @Autowired
    private ServiceConversionHelper serviceConversionHelper;

    @Autowired
    protected ReferenceDataDAOHelper referenceDataDAOHelper;
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.service.RoomOfferService#getRoomOffers(com.
     * mgm.services.booking.room.model.request.RoomOffersRequest)
     */
    @Override
    public List<RoomProgram> getRoomOffers(RoomProgramRequest offersRequest) {

        List<RoomProgram> roomOffersList = new ArrayList<>();
        List<String> properties = offersRequest.getPropertyIds();
        roomProgramDao.getRoomOffers(offersRequest).forEach(program -> {
            if (program.getType().equals(OfferType.PROGRAM)) {
                Optional<com.mgm.services.booking.room.model.phoenix.RoomProgram> cacheProgram = Optional
                        .ofNullable(roomProgramCacheService.getRoomProgram(program.getId()));
                // Possibility of program not available in cache
                cacheProgram.ifPresent(cProgram -> {
                    // If list of properties are passed in request, filter to
                    // include offer
                    // from only those properties
                    if (CollectionUtils.isEmpty(properties) || properties.contains(cProgram.getPropertyId())) {
                        program.setBookByDate(cProgram.getBookBy());
                        program.setStartDate(cProgram.getTravelPeriodStart());
                        program.setEndDate(cProgram.getTravelPeriodEnd());
                        roomOffersList.add(program);
                    }
                });
            } else {
                // If check here to skip returning segments when property
                // filtering is used
                if (CollectionUtils.isEmpty(properties)) {
                    // Can't add dates for segments
                    roomOffersList.add(program);
                }
            }

        });

        return roomOffersList;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.RoomProgramService#
     * isProgramApplicable(com.mgm.services.booking.room.model.request.
     * RoomProgramValidateRequest)
     */
    @Override
    public boolean isProgramApplicable(RoomProgramValidateRequest validateRequest) {
        return validateProgram(validateRequest).isEligible();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.RoomProgramService#
     * getProgramByPromoCode(java.lang.String, java.lang.String)
     */
    @Override
    public String getProgramByPromoCode(String promoCode, String propertyId) {
        return roomProgramDao.getProgramByPromoCode(propertyId, promoCode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.RoomProgramService#
     * getDefaultPerpetualPrograms(com.mgm.services.booking.room.model.request.
     * PerpetualProgramRequest)
     */
    @Override
    public List<PerpetaulProgram> getDefaultPerpetualPrograms(PerpetualProgramRequest request) {
        request.setPropertyIds(Optional.ofNullable(request.getPropertyIds()).orElse(auroraProperties.getPropertyIds()));
        return roomProgramDao.getDefaultPerpetualPrograms(request);
    }

    @Override
    public RoomProgramSegmentResponse getProgramSegment(String programId) {
        Optional<com.mgm.services.booking.room.model.phoenix.RoomProgram> cacheProgram = Optional
                .ofNullable(roomProgramCacheService.getRoomProgram(programId));
        if (cacheProgram.isPresent() && StringUtils.isNotEmpty(cacheProgram.get().getSegmentId())) {
            RoomProgramSegmentResponse response = new RoomProgramSegmentResponse();
            response.setSegmentId(cacheProgram.get().getSegmentId());
            List<String> programIds = roomProgramCacheService.getProgramsBySegmentId(response.getSegmentId()).stream()
                    .map(com.mgm.services.booking.room.model.phoenix.RoomProgram::getId).collect(Collectors.toList());
            response.setProgramIds(programIds);
            return response;
        }

        throw new BusinessException(ErrorCode.NO_SEGMENT_ID_FOUND);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.RoomProgramService#
     * getApplicablePrograms(com.mgm.services.booking.room.model.request.
     * ApplicableProgramsRequest)
     */
    @Override
    public ApplicableProgramsResponse getApplicablePrograms(ApplicableProgramsRequest request) {
        final ApplicableProgramRequestDTO applicableProgramRequestDTO = RoomProgramsTransformer.buildApplicableProgramRequest(request);
        return roomProgramDao.getApplicablePrograms(applicableProgramRequestDTO);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.RoomOfferService#
     * getCustomerOffers(com.mgm.services.booking.room.model.request.
     * CustomerOffersRequest)
     */
    @Override
    public CustomerOfferResponse getCustomerOffers(CustomerOffersRequest customerOffersSearchRequest) {
        return roomProgramDao
                .getCustomerOffers(RoomProgramsTransformer.buildGetCustomerOffersRequest(customerOffersSearchRequest));
    }

    @Override
    public List<PerpetaulProgram> getDefaultPerpetualProgramsV2(PerpetualProgramRequest request) {
        request.setPropertyIds(Optional.ofNullable(request.getPropertyIds()).orElse(auroraProperties.getPropertyIds()));
        List<PerpetaulProgram> perpetualPrograms = null;
        try {
            perpetualPrograms = roomProgramDao.getDefaultPerpetualPrograms(request);
        } catch (EAuroraException ex) {
            // This method is added for V2 Aurora Exception handling, later this
            // will be moved to DAO
            String errorType = AuroraError.getErrorType(ex.getErrorCode().name());
            if (AuroraError.FUNCTIONAL_ERROR.equals(errorType)) {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, ex.getMessage());
            } else {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, ex);
            }
        }
        return perpetualPrograms;
    }

    /**
     * This method validates the passed on Program for its different eligibility
     * criteria. Also, this method checks for the existence of redemption code, if
     * the program is a my vegas program and requested channel is not ice.
     * 
     * @param request
     * @return
     */
    @Override
    public RoomProgramValidateResponse validateProgramV2(RoomProgramValidateRequest request) {

        serviceConversionHelper.convertGuids(request);

        RoomProgramValidateResponse validateResponse = roomProgramDao.validateProgramV2(request);
        //ACRS
        if(ACRSConversionUtil.isHDEProgram(request.getProgramId())){
            validateResponse.setHdePackage(true);
        }else {
            //GSE
            roomProgramDao.updateValidateResponseForPackagePrograms(request, validateResponse);
        }
        if (validateResponse.isMyvegas() && StringUtils.isEmpty(request.getRedemptionCode())
                && !appProps.getBypassMyvegasChannels().contains(CommonUtil.getChannelHeader())) {
            validateResponse.setEligible(false);
        }

        // F1 Package flag set
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(validateResponse.getRatePlanTags()) && validateResponse.getRatePlanTags().contains(appProps.getF1PackageTag())) {
            List<String> validF1ProductCodes = appProps.getValidF1ProductCodes();
            Optional<String> productCode = validF1ProductCodes.stream().filter(x -> validateResponse.getRatePlanTags().contains(x)).findFirst();
            if (productCode.isPresent()) {
                validateResponse.setF1Package(true);
            }
        }

        // Soostone program check
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(validateResponse.getRatePlanTags()) &&
                validateResponse.getRatePlanTags().contains(appProps.getSoostoneProgramTag()) &&
                StringUtils.isEmpty(request.getMlifeNumber())) {
            validateResponse.setEligible(false);
        }

        // Loyalty Number required check
        if (validateResponse.isLoyaltyNumberRequired() &&
                StringUtils.isEmpty(request.getMlifeNumber())) {
            validateResponse.setEligible(false);
        }

        
        if (StringUtils.isNotBlank(validateResponse.getProgramId())) {

            String enableNewSegments = secretProperties.getSecretValue(String.format(appProps.getEnableNewSegmentsKey(),appProps.getRbsEnv()));
            if (enableNewSegments.equals("true") && validateResponse.isValid()) {
                // Override segment flag based on rate plan code lookup common
                // to both GSE and ACRS
                List<RoomProgramBasic> programs = roomProgramDao.findProgramsIfSegment(validateResponse.getProgramId(),
                        request.getSource());

                if (programs.size() > 1) {
                    validateResponse.setSegment(true);
                }
            }
        }

        return validateResponse;
    }

    /**
     * This method validates the passed on Program for its different eligibility criteria
     * @param request
     * @return
     */
    @Override
    public RoomProgramValidateResponse validateProgram(RoomProgramValidateRequest request) {
        return roomProgramDao.validateProgram(request);
    }

    /**
     * This program retrieval API will retrieve a Program from GSE cache or ACRS - Group Search or Content APIs calls based on where its available.
     * @param request - RoomProgramV2Request
     * @return - List of RoomOfferDetails
     */
    @Override
    public List<RoomOfferDetails> getProgram(RoomProgramV2Request request) {

        final List<RoomOfferDetails> collectedOffers = new ArrayList<>();
        final Set<String> enrRatePlanIds = new HashSet<>();

        for (String programId : request.getProgramIds()) {

            // Retrieve Program from GSE-Cache
            final Optional<com.mgm.services.booking.room.model.phoenix.RoomProgram> cacheProgram = Optional
                    .ofNullable(roomProgramCacheService.getRoomProgram(programId));

            if (cacheProgram.isPresent()) {
                collectedOffers.add(RoomProgramsTransformer.buildRoomProgramDetail(cacheProgram.get()));
            } else if (ACRSConversionUtil.isAcrsGroupCodeGuid(programId)) {
                // Retrieve the Group plan from ACRs
                final List<GroupSearchV2Response> groupSearchResponses = searchGroupOffer(request, programId);
                if (!CollectionUtils.isEmpty(groupSearchResponses)) {
                    collectedOffers.add(RoomProgramsTransformer.buildRoomProgramDetail(groupSearchResponses.get(0)));
                }
            } else if (ACRSConversionUtil.isAcrsRatePlanGuid(programId)) {
                enrRatePlanIds.add(programId);
            } else {
                log.debug("Program id {} is not valid", programId);
            }
        }

        // Make ENR Search call to retrieve ACRS RatePlans
        if (enrRatePlanIds.size() > 0) {
            request.setProgramIds(new ArrayList<>(enrRatePlanIds));
            collectedOffers.addAll(roomProgramDao.getRatePlanById(request));
        }

        return collectedOffers;
    }

    private List<GroupSearchV2Response> searchGroupOffer(RoomProgramV2Request request, String programId) {
        final List<GroupSearchV2Response> results = new ArrayList<>();
        try {
            final GroupSearchV2Request groupSearchRequest = new GroupSearchV2Request();
            groupSearchRequest.setSource(request.getSource());
            groupSearchRequest.setPropertyId(referenceDataDAOHelper.retrieveGsePropertyID(ACRSConversionUtil.getPropertyCode(programId)));
            groupSearchRequest.setId(programId);
            groupSearchRequest.setStartDate(request.getStartDate());
            groupSearchRequest.setEndDate(request.getEndDate());
            results.addAll(groupSearchDAO.searchGroup(groupSearchRequest));
        } catch (Exception e) {
            log.error("Unable to retrieve Group Program | error : {}", ExceptionUtils.getStackTrace(e)); //continue
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.service.RoomProgramService#getCustomerOffers(com.mgm.services.booking.room.model.request.CustomerOffersV3Request)
     */
    @Override
    public CustomerOfferV3Response getCustomerOffers(CustomerOffersV3Request programsRequest) {

        CustomerOfferV3Response offersResponse = new CustomerOfferV3Response();

        if (programsRequest.getCustomerId() < 0 || StringUtils.isEmpty(programsRequest.getMlifeNumber())) {
            // User is not logged-in
            CuratedOfferResponse curatedOffers = programContentDAO
                    .getCuratedHotelOffers(programsRequest.getPropertyId());
            offersResponse.getOffers().addAll(RoomProgramsTransformer.buildCustomerOffersResponse(curatedOffers,
                    programsRequest.getRegion(), false, programsRequest.isIncludeNonBookableOnline()));
            
            return offersResponse;
        }

        List<CustomerOffer> offers = new LinkedList<>();
        
        RoomProgramsResponseDTO programsDaoResponse = roomProgramDao
                .getRoomPrograms(RoomProgramsTransformer.buildRoomProgramsRequestDTO(programsRequest));

        // Get property details from cache for sorting and filtering
        List<Property> properties = propertyCacheService.getPropertyByRegion(programsRequest.getRegion());

        if (programsRequest.isResortPricing()) {
            offers.addAll(RoomProgramsTransformer.buildCustomerOffersResponseWithSorting(programsDaoResponse.getPoPrograms(), properties));
            offersResponse.getOffers().addAll(RoomProgramsTransformer.filterByRegion(offers, properties));
            offersResponse.setUserCvsValues(programsDaoResponse.getUserCvsValues());
            return offersResponse;
        }

        // Get Curated Offers
        CuratedOfferResponse curatedOffers = programContentDAO
                .getCuratedHotelOffers(programsRequest.getPropertyId());
        List<RoomProgramDTO> patronPrograms = programsDaoResponse.getPatronPrograms();
        String enableNewSegments = secretProperties.getSecretValue(String.format(appProps.getEnableNewSegmentsKey(),appProps.getRbsEnv()));
        List<RoomProgramDTO> icePrograms = programsDaoResponse.getIceChannelPrograms();
        if(!programsRequest.isPerpetualPricing()) {
            // Add patron, curated and ice offers as applicable for the customer
            addOffers(programsRequest, offers, curatedOffers, patronPrograms, enableNewSegments, icePrograms);
        } else {
            // Add PO programs to output if available for the customer
            List<RoomProgramDTO> poPrograms = programsDaoResponse.getPoPrograms();
            boolean poProgramNotAdded = true;
            if (!programsRequest.isPatronFirst()) {
                offers.addAll(RoomProgramsTransformer.buildCustomerOffersResponseWithSorting(poPrograms, properties));
                poProgramNotAdded = false;
            }
            if (!programsRequest.isOnlyPoPrograms()) {
                if (programsRequest.isOnlyPoPatronPrograms()) {
                    // Add patron offers
                    offers.addAll(RoomProgramsTransformer.buildCustomerOffersResponse(patronPrograms, enableNewSegments, programsRequest.getChannel()));
                } else {
                    if (programsRequest.isPatronFirst()) {
                        offers.addAll(RoomProgramsTransformer.buildCustomerOffersResponse(patronPrograms, enableNewSegments, programsRequest.getChannel()));
                        offers.addAll(RoomProgramsTransformer.buildCustomerOffersResponseWithSorting(poPrograms, properties));
                        poProgramNotAdded = false;
                        addOffers(programsRequest, offers, curatedOffers, null, enableNewSegments, icePrograms);
                    } else {
                        // Add patron, curated and ice offers as applicable for the customer
                        addOffers(programsRequest, offers, curatedOffers, patronPrograms, enableNewSegments, icePrograms);
                    }
                }
            }
            if (programsRequest.isPatronFirst() && poProgramNotAdded) {
                offers.addAll(RoomProgramsTransformer.buildCustomerOffersResponseWithSorting(poPrograms, properties));
            }
        }
        
        // filter by region and add to response
        offersResponse.getOffers().addAll(RoomProgramsTransformer.filterByRegion(offers, properties));
        offersResponse.setUserCvsValues(programsDaoResponse.getUserCvsValues());

        return offersResponse;
    }


    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.service.RoomProgramService#getRoomSegment(java.lang.String, java.lang.String)
     */
    @Override
    public RoomSegmentResponse getRoomSegment(String segment, String source) {
        return roomProgramDao.getRoomSegment(segment, source, false);
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.service.RoomProgramService#getRoomSegment(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public RoomSegmentResponse getRoomSegment(String segment, String programId, String source) {
        return roomProgramDao.getRoomSegment(segment, programId, source);
    }

    private void addOffers(CustomerOffersV3Request programsRequest, List<CustomerOffer> offers, CuratedOfferResponse curatedOffers, List<RoomProgramDTO> patronPrograms, String enableNewSegments, List<RoomProgramDTO> icePrograms) {
        // Add Patron programs to output if available for the customer
        if (null != patronPrograms) {
            offers.addAll(RoomProgramsTransformer.buildCustomerOffersResponse(patronPrograms, enableNewSegments, programsRequest.getChannel()));
        }
        // Add Curated offers to output if available for the customer
        offers.addAll(RoomProgramsTransformer.buildCustomerOffersResponse(curatedOffers,
                programsRequest.getRegion(), true, programsRequest.isIncludeNonBookableOnline()));
        // Add ICE only programs, if applicable
        offers.addAll(RoomProgramsTransformer.buildCustomerOffersResponse(icePrograms));
    }

}
