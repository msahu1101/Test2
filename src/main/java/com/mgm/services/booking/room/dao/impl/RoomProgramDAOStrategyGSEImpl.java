package com.mgm.services.booking.room.dao.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.response.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.dao.CVSDao;
import com.mgm.services.booking.room.dao.RoomProgramDAOStrategy;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.CustomerProgramOffer;
import com.mgm.services.booking.room.model.OfferType;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.CustomerOffersRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.transformer.RoomProgramsTransformer;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.util.BaseCommonUtil;
import com.mgm.services.common.util.ValidationUtil;
import com.mgmresorts.aurora.common.CustomerOffer;
import com.mgmresorts.aurora.messages.GetApplicableProgramsResponse;
import com.mgmresorts.aurora.messages.GetCustomerOffersRequest;
import com.mgmresorts.aurora.messages.GetCustomerOffersResponse;
import com.mgmresorts.aurora.messages.GetDefaultOfferBySegmentRequest;
import com.mgmresorts.aurora.messages.GetDefaultOfferBySegmentResponse;
import com.mgmresorts.aurora.messages.GetProgramByOperaPromoCodeRequest;
import com.mgmresorts.aurora.messages.GetProgramByOperaPromoCodeResponse;
import com.mgmresorts.aurora.messages.IsProgramApplicableRequest;
import com.mgmresorts.aurora.messages.IsProgramApplicableResponse;
import com.mgmresorts.aurora.messages.MessageFactory;
import com.mgmresorts.aurora.service.Client;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for RoomProgramDAO providing functionality to provide
 * room program related functionalities.
 *
 */
@Component
@Log4j2
public class RoomProgramDAOStrategyGSEImpl extends AuroraBaseDAO implements RoomProgramDAOStrategy {

    @Autowired
    protected RoomProgramCacheService roomProgramCacheService;
    
    @Autowired
    protected AuroraProperties auroraProps;
    
    @Autowired
    private SecretsProperties secretsProperties;
    
    @Autowired
    private ApplicationProperties appProps;
    
    @Autowired(required=false)
    protected CVSDao cvsDao;

    @Override
    @LogExecutionTime
    public ApplicableProgramsResponse getApplicablePrograms(ApplicableProgramRequestDTO request) {

        try {
            final Client client = StringUtils.isNotEmpty(request.getSource()) ? getAuroraClient(request.getSource())
                    : getDefaultAuroraClient();
            final GetApplicableProgramsResponse response = client.getApplicablePrograms(
                    RoomProgramsTransformer.buildAuroraRequestForGetApplicableProgramsRequest(request));
            if (response != null) {
                log.info("Received the response from getApplicablePrograms as : {}", response.toJsonString());
                return constructApplicableResponse(response);
            }
        } catch (EAuroraException ex) {
            log.error("Exception trying to retrieve applicable programs : ", ex);
            handleAuroraError(ex);
        }
        return null;
    }

    public CustomerOfferResponse getCustomerOffers(CustomerOffersRequestDTO customerOffersRequest) {
        GetCustomerOffersRequest request = RoomProgramsTransformer
                .buildAuroraGetCustomerOffersRequest(customerOffersRequest);
        try {

            log.info("Sent the request to getCustomerOffersV2 as : {}", request.toJsonString());

            GetCustomerOffersResponse response = getAuroraClient(customerOffersRequest.getSource())
                    .getCustomerOffers(request);

            log.info("Received the response from getCustomerOffersV2 as : {}", response.toJsonString());

            return RoomProgramsTransformer.transform(response);
        } catch (EAuroraException ex) {
            log.error("Exception trying to retrieve customer offers: ", ex);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.mgm.services.booking.room.service.RoomProgramService#
     * isProgramApplicable(java.lang.String, java.lang.String)
     */
    @Override
    public RoomProgramValidateResponse validateProgram(RoomProgramValidateRequest validateRequest) {

        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();

        // Get Program Id for the promo code. If both Promo Code and ProgramId
        // are present, then Promo Code will take precedence
        if (StringUtils.isNotEmpty(validateRequest.getPromoCode())) {

            String programId = getProgramByPromoCode(validateRequest.getPropertyId(), validateRequest.getPromoCode());

            if (StringUtils.isEmpty(programId)) {
                validateResponse.setValid(false);
            } else {
                validateRequest.setProgramId(programId);
            }
        }

        // Check if program is in cache
        if (validateRequest.getProgramId() != null) {
            Optional<com.mgm.services.booking.room.model.phoenix.RoomProgram> cacheProgram = Optional
                    .ofNullable(roomProgramCacheService.getRoomProgram(validateRequest.getProgramId()));
            if (cacheProgram.isPresent()) {
                setProgramValidity(cacheProgram.get(), validateResponse);
                validateResponse.setProgramId(validateRequest.getProgramId());
                validateResponse.setPropertyId(cacheProgram.get().getPropertyId());
                if (StringUtils.isNotEmpty(cacheProgram.get().getSegmentId())) {
                    validateResponse.setSegment(true);
                }
                // set propertyId based on cache instead of request
                validateRequest.setPropertyId(cacheProgram.get().getPropertyId());
                validateResponse.setMyvegas(BaseCommonUtil.isContainMyVegasTags(cacheProgram.get().getTags()));
                // set the patronProgram flag depending on patronPromoId
                validateResponse.setPatronProgram(StringUtils.isNotEmpty(cacheProgram.get().getPatronPromoId()));
            } else {
                log.debug("Program ID {} not available in cache", validateRequest.getProgramId());
            }
        }

        if (validateResponse.isValid()) {
            // Check if customer is logged in
            if (validateRequest.getCustomerId() < 0) {
                validateResponse.setEligible(isProgramApplicable(validateRequest));
            } else {
                return checkEligibilityForLoggedInUser(validateRequest, validateResponse);
            }
        }

        return validateResponse;

    }

    @Override
    public RoomProgramValidateResponse validateProgramV2(RoomProgramValidateRequest validateRequest) {

        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        // Get Program Id for the promo code. If both Promo Code and ProgramId
        // are present, then Promo Code will take precedence
        if (StringUtils.isNotEmpty(validateRequest.getPromoCode())) {
            String programId = getProgramByPromoCode(validateRequest.getPropertyId(), validateRequest.getPromoCode());

            if (StringUtils.isEmpty(programId)) {
                validateResponse.setValid(false);
            } else {
                validateRequest.setProgramId(programId);
            }
        }

        // Check if program is in cache
        if (validateRequest.getProgramId() != null) {
            Optional<com.mgm.services.booking.room.model.phoenix.RoomProgram> cacheProgram = Optional
                    .ofNullable(roomProgramCacheService.getRoomProgram(validateRequest.getProgramId()));
            if (cacheProgram.isPresent()) {
                setProgramValidity(cacheProgram.get(), validateResponse);
                validateRequest.setPropertyId(cacheProgram.get().getPropertyId());
                updateValidateResponse(validateRequest.getProgramId(), validateResponse, cacheProgram);
            } else {
                log.debug("Program ID {} not available in cache", validateRequest.getProgramId());
            }
        }

        if (validateResponse.isValid()) {
            if (validateResponse.isPatronProgram() && !validateRequest.isModifyFlow()) {
                return checkEligibilityForPatronProgram(validateRequest, validateResponse);
            } else {
                validateResponse.setEligible(isProgramApplicable(validateRequest));
            }
        }

        return validateResponse;

    }

    /**
     * Check eligibility for logged in user. The below steps are involved :- 1.
     * Make a call to getCustomerOffers and check if the requested program is in
     * the response. If found, user is eligible to use the program 2. Else,
     * check if the program in cache has a non-empty patronPromoId. If
     * patronPromoId is not empty, mark the program as non-eligible 3. Else,
     * make a call to getApplicablePrograms GSE API and see if the program is
     * included in the response
     *
     * @param validateRequest
     *            the request object
     * @return the response object with eligibility
     */
    private RoomProgramValidateResponse checkEligibilityForLoggedInUser(RoomProgramValidateRequest validateRequest,
            RoomProgramValidateResponse validateResponse) {
        RoomProgramRequest programRequest = new RoomProgramRequest();
        programRequest.setPropertyIds(Arrays.asList(validateRequest.getPropertyId()));
        programRequest.setSource(validateRequest.getSource());
        programRequest.setCustomerId(validateRequest.getCustomerId());
        programRequest.setMlifeNumber(validateRequest.getMlifeNumber());
        // 1. Check if the customer offers has the program id user is
        // requesting

        List<com.mgm.services.booking.room.model.response.RoomProgram> programList = getRoomOffers(programRequest);

        for (com.mgm.services.booking.room.model.response.RoomProgram program : programList) {
            if (validateRequest.getProgramId().equals(program.getId())) {
                validateResponse.setEligible(true);
                break;
            }
        }
        if (!validateResponse.isEligible()) {
            checkPatronPromoAndApplicablePrograms(validateRequest, validateResponse);
        }
        
        log.info("Eligibility Response - Customer ID: {}, Program ID: {}, Eligible: {}", validateRequest.getCustomerId(),
                validateRequest.getProgramId(), validateResponse.isEligible());

        return validateResponse;
    }
    
    /**
     * Check eligibility for a patron program. The following steps are involved :
     * Make a call to getCustomerOffers and check if the requested program is in
     * the response. If found, user is eligible to use the program
     *
     * @param validateRequest
     *            the request object
     * @return the response object with eligibility
     */
    protected RoomProgramValidateResponse checkEligibilityForPatronProgram(RoomProgramValidateRequest validateRequest,
            RoomProgramValidateResponse validateResponse) {
        RoomProgramRequest programRequest = new RoomProgramRequest();
        programRequest.setPropertyIds(Arrays.asList(validateRequest.getPropertyId()));
        programRequest.setSource(validateRequest.getSource());
        programRequest.setCustomerId(validateRequest.getCustomerId());
        programRequest.setMlifeNumber(validateRequest.getMlifeNumber());
        // 1. Check if the customer offers has the program id user is
        // requesting

        List<com.mgm.services.booking.room.model.response.RoomProgram> programList = getRoomOffers(programRequest);

        for (com.mgm.services.booking.room.model.response.RoomProgram program : programList) {
            if (validateRequest.getProgramId().equals(program.getId())) {
                validateResponse.setEligible(true);
                break;
            }
        }
        
        log.info("Eligibility Response - Customer ID: {}, Program ID: {}, Eligible: {}", validateRequest.getCustomerId(),
                validateRequest.getProgramId(), validateResponse.isEligible());

        return validateResponse;
    }

    @Override
    public List<com.mgm.services.booking.room.model.response.RoomProgram> getRoomOffers(
            RoomProgramRequest offersRequest) {
        GetCustomerOffersRequest getCustomerOffersRequest = MessageFactory.createGetCustomerOffersRequest();

        getCustomerOffersRequest.setCustomerId(offersRequest.getCustomerId());
        if (ValidationUtil.isUuid(offersRequest.getSource())) { // non-mlife
            getCustomerOffersRequest.setPropertyId(offersRequest.getSource());
            getCustomerOffersRequest.setNotRolledToSegments(true);
        }

        // If a single propertyId is supplied, request offers for that property
        // using it's respective channel
        List<String> properties = offersRequest.getPropertyIds();
        if (!CollectionUtils.isEmpty(properties) && properties.size() == 1) {
            getCustomerOffersRequest.setPropertyId(properties.get(0));
            getCustomerOffersRequest.setNotRolledToSegments(true);
            offersRequest.setSource(properties.get(0));
        }

        log.info("Sent the request to getCustomerOffers as : {}", getCustomerOffersRequest.toJsonString());
        final GetCustomerOffersResponse response = getAuroraClient(offersRequest.getSource())
                .getCustomerOffers(getCustomerOffersRequest);

        List<com.mgm.services.booking.room.model.response.RoomProgram> roomOffersList = new ArrayList<>();
        log.info("Received the response from getCustomerOffers as : {}", response.toJsonString());

        if (null != response.getOffers()) {
            final CustomerOffer[] custOfferArr = response.getOffers();
            for (final CustomerOffer customerOffer : custOfferArr) {
                com.mgm.services.booking.room.model.response.RoomProgram roomOffer = new com.mgm.services.booking.room.model.response.RoomProgram();
                roomOffer.setId(customerOffer.getId());
                if (customerOffer.getType().name().equalsIgnoreCase(OfferType.SEGMENT.name())) {
                    roomOffer.setType(OfferType.SEGMENT);
                } else {
                    roomOffer.setType(OfferType.PROGRAM);
                }
                roomOffersList.add(roomOffer);
            }
        }
        return roomOffersList;
    }

    @Override
    public String getProgramByPromoCode(String propertyId, String promoCode) {

        GetProgramByOperaPromoCodeRequest request = MessageFactory.createGetProgramByOperaPromoCodeRequest();
        request.setPropertyId(propertyId);
        request.setOperaPromoCode(promoCode);

        try {

            log.info("Sent the request to getProgramByOperaPromoCode as : {}", request.toJsonString());

            final GetProgramByOperaPromoCodeResponse response = getAuroraClient(propertyId)
                    .getProgramByOperaPromoCode(request);

            log.info("Received the response from getProgramByPatronPromoId as : {}", response.toJsonString());

            return response.getProgramId();
        } catch (EAuroraException ex) {
            log.error("Error looking up programId from promo code: {}", ex);
            return StringUtils.EMPTY;
        }

    }

    private List<CustomerProgramOffer> getApplicablePrograms(OfferRequest offerRequest) {

        final ApplicableProgramRequestDTO request = ApplicableProgramRequestDTO.builder()
                .source(offerRequest.getSource()).bookDate(new Date()).customerId(offerRequest.getCustomerId())
                .propertyId(offerRequest.getPropertyId()).build();

        final ApplicableProgramsResponse applicableProgramResponse = getApplicablePrograms(request);

        final List<CustomerProgramOffer> customerOffers = new ArrayList<>();
        if (null != applicableProgramResponse) {
            final List<String> programIds = applicableProgramResponse.getProgramIds();
            log.info("Received the response from ApplicablePrograms. Number of programs returned : {}",
                    programIds.size());
            for (final String programId : programIds) {
                final CustomerProgramOffer programType = new CustomerProgramOffer();
                programType.setId(programId);
                programType.setType(OfferType.PROGRAM);
                customerOffers.add(programType);
            }
        }
        return customerOffers;
    }

    private void setProgramValidity(com.mgm.services.booking.room.model.phoenix.RoomProgram program,
            RoomProgramValidateResponse validateResponse) {

        LocalDate currentDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        if (program.isActiveFlag()) {
            validateResponse.setValid(true);
            // if bookby date has passed, set valid to false and expired to true
            if (null != program.getBookBy() && program.getBookBy().toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDate().isBefore(currentDate)) {
                validateResponse.setValid(false);
                validateResponse.setExpired(true);
            }
            validateResponse.setHdePackage(ReservationUtil.isBlockCodeHdePackage(program.getOperaBlockCode()));
        } else {
            validateResponse.setValid(false);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.mgm.services.booking.room.dao.RoomOfferDAO#isOfferApplicable(com.mgm.
     * services.booking.room.model.request.RoomOfferValidateRequest)
     */
    protected boolean isProgramApplicable(RoomProgramValidateRequest validateRequest) {

        try {
            IsProgramApplicableRequest isProgramApplicableRequest = MessageFactory.createIsProgramApplicableRequest();

            isProgramApplicableRequest.setCustomerId(validateRequest.getCustomerId());
            isProgramApplicableRequest.setProgramId(validateRequest.getProgramId());

            log.info("Sent the request to isProgramApplicable as : {}", isProgramApplicableRequest.toJsonString());

            final IsProgramApplicableResponse isProgramApplicableResponse = getDefaultAuroraClient()
                    .isProgramApplicable(isProgramApplicableRequest);

            log.info("Received the response from isProgramApplicable as : {}",
                    isProgramApplicableResponse.toJsonString());

            boolean applicableStatus = false;
            applicableStatus = isProgramApplicableResponse.getIsApplicable();

            return applicableStatus;
        } catch (EAuroraException ex) {
            log.error("Exception trying to check isProgramApplicable : ", ex);
            handleAuroraError(ex);
        }
        return false;

    }

    /**
     * Check if the program is available in cache and has a patronPromo
     * associated. If no patron Promo id is associated, then check in the list
     * of all the available programs for the user
     *
     * @param validateRequest
     *            Validation request
     * @param validateResponse
     *            Validation response
     */
    private void checkPatronPromoAndApplicablePrograms(RoomProgramValidateRequest validateRequest,
            RoomProgramValidateResponse validateResponse) {
        // 2. Check if the program is available in the program cache and
        // has a patron program id associated to it
        Optional<com.mgm.services.booking.room.model.phoenix.RoomProgram> cacheProgram = Optional
                .ofNullable(roomProgramCacheService.getRoomProgram(validateRequest.getProgramId()));
        // Possibility of program not available in cache
        cacheProgram.ifPresent(cProgram -> {
            if (StringUtils.isEmpty(cProgram.getPatronPromoId())) {
                OfferRequest offerRequest = new OfferRequest();
                offerRequest.setSource(validateRequest.getSource());
                offerRequest.setCustomerId(validateRequest.getCustomerId());
                offerRequest.setPropertyId(cProgram.getPropertyId());

                List<CustomerProgramOffer> offers = getApplicablePrograms(offerRequest);
                for (CustomerProgramOffer offer : offers) {
                    if (validateRequest.getProgramId().equals(offer.getId())) {
                        validateResponse.setEligible(true);
                        break;
                    }
                }
            }
        });
    }

    private ApplicableProgramsResponse constructApplicableResponse(GetApplicableProgramsResponse gseResponse) {
        final ApplicableProgramsResponse response = new ApplicableProgramsResponse();
        for (String programId : gseResponse.getProgramIds()) {
            response.getProgramIds().add(programId);
            final Optional<com.mgm.services.booking.room.model.phoenix.RoomProgram> cacheProgram = Optional
                    .ofNullable(roomProgramCacheService.getRoomProgram(programId));
            if (cacheProgram.isPresent()) {
                final com.mgm.services.booking.room.model.phoenix.RoomProgram roomProgram = cacheProgram.get();
                response.getPrograms().add(ApplicableRoomProgram.builder().id(programId).name(roomProgram.getName())
                        .category(roomProgram.getCategory()).patronPromoId(roomProgram.getPatronPromoId())
                        .promoCode(roomProgram.getPromoCode()).operaBlockCode(roomProgram.getOperaBlockCode())
                        .operaBlockName(roomProgram.getOperaBlockName())
                        .reservationMethod(roomProgram.getReservationMethod())
                        .tags(roomProgram.getTags() != null ? Arrays.asList(roomProgram.getTags()) : new ArrayList<>())
                        .build());
            }
        }
        return response;
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.dao.RoomProgramDAOStrategy#findProgramsByRatePlanCode(java.lang.String)
     */
    @Override
    public List<RoomProgramBasic> findProgramsByRatePlanCode(String ratePlanCode, String source) {

        List<RoomProgramBasic> programList = new ArrayList<>();
        roomProgramCacheService.getProgramsByPromoCode(ratePlanCode)
                .forEach(program -> programList.add(new RoomProgramBasic(program.getId(), program.getPropertyId(),
                        ratePlanCode, program.isActiveFlag(), program.isBookableOnline(), program.getBookingStartDate(), program.getBookingEndDate(),
                        program.getTravelPeriodStart(), program.getTravelPeriodEnd(), program.getTags(), program.getPromoCode())));
        return programList;
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.dao.RoomProgramDAOStrategy#findProgramsIfSegment(java.lang.String)
     */
    @Override
    public List<RoomProgramBasic> findProgramsIfSegment(String programId, String source) {

        List<RoomProgramBasic> programList = new ArrayList<>();
        Optional<RoomProgram> roomProgram = Optional.ofNullable(roomProgramCacheService.getRoomProgram(programId));

        if (roomProgram.isPresent()) {
            RoomProgram program = roomProgram.get();

            if (StringUtils.isNotEmpty(program.getSegmentId())) {
                List<RoomProgram> programs = roomProgramCacheService.getProgramsBySegmentId(program.getSegmentId());

                if (!programs.isEmpty()) {
                    programs.forEach(p -> programList.add(new RoomProgramBasic(p.getId(), p.getPropertyId(), null,
                            p.isActive(), p.isBookableOnline(), p.getBookingStartDate(), p.getBookingEndDate(),
                            p.getTravelPeriodStart(), p.getTravelPeriodEnd(), p.getTags(), p.getPromoCode())));
                }
            }

        } else {
            // Backward compatibility for GSE segment GUID passed as programId
            // Will be removed once client teams have moved to segment code
            // approach

            List<RoomProgram> programs = roomProgramCacheService.getProgramsBySegmentId(programId);

            if (!programs.isEmpty()) {
                programs.forEach(p -> programList.add(new RoomProgramBasic(p.getId(), p.getPropertyId(), null,
                        p.isActive(), p.isBookableOnline(), p.getBookingStartDate(), p.getBookingEndDate(),
                        p.getTravelPeriodStart(), p.getTravelPeriodEnd(), p.getTags(), p.getPromoCode())));
            }
        }

        return programList;
    }

    @Override
    public List<RoomProgramBasic> findProgramsByGroupCode(String groupCode, LocalDate checkInDate, LocalDate checkOutDate, String source) {

        List<RoomProgramBasic> programList = new ArrayList<>();
        List<RoomProgram> roomProgramList = roomProgramCacheService.getProgramsByGroupCode(groupCode);

        if (!roomProgramList.isEmpty()) {
            roomProgramList.forEach(p -> programList.add(new RoomProgramBasic(p.getId(), p.getPropertyId(), null,
                    p.isActive(), p.isBookableOnline(), p.getBookingStartDate(), p.getBookingEndDate(),
                    p.getTravelPeriodStart(), p.getTravelPeriodEnd(), p.getTags(), p.getPromoCode())));
        }
        return programList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.RoomProgramDAOStrategy#
     * getPromoCodeByProgramId(java.lang.String)
     */
    @Override
    public String getRatePlanCodeByProgramId(String programId) {
        return roomProgramCacheService.getPromoCodeByProgramId(programId);
    }

    /**
     * Find and return patron programs that may be available for the customer
     * 
     * @param offersRequest
     * @return
     */
    private List<RoomProgramDTO> getPatronPrograms(RoomProgramsRequestDTO offersRequest, List<CustomerPromotion> patronOffers) {

        // Extract promoIds from promotions returned by loyalty service
        List<String> promoIds = new ArrayList<>();
        patronOffers.forEach(promo -> promoIds.add(promo.getPromoId()));

        List<RoomProgramDTO> programList = new ArrayList<>();
        Map<String, List<RoomProgramDTO>> segmentsMap = new HashMap<>();
        
        if (secretsProperties.getSecretValue(String.format(appProps.getEnableNewSegmentsKey(), appProps.getRbsEnv())).equals("true") || StringUtils.isNotBlank(offersRequest.getPropertyId())) {
            roomProgramCacheService.getProgramsByPatronPromoIds(promoIds)
                    .forEach(p -> programList.add(new RoomProgramDTO(p.getId(), p.getPropertyId(),  p.getPromoCode(), null)));

            return filterProgramsByProperty(programList, offersRequest.getPropertyId());
        }

        // Perform roll into GSE segments if new segments approach is not enabled
        // Lookup programs using promoIds
        roomProgramCacheService.getProgramsByPatronPromoIds(promoIds).forEach(p -> {
            // Add programs if its not part of segment and group programs in GSE
            // segment for further processing
            if (StringUtils.isEmpty(p.getSegmentId())) {
                programList.add(new RoomProgramDTO(p.getId(), p.getPropertyId(), p.getPromoCode(), null));
            } else {
                if (segmentsMap.containsKey(p.getSegmentId())) {
                    segmentsMap.get(p.getSegmentId())
                            .add(new RoomProgramDTO(p.getId(), p.getPropertyId(), p.getPromoCode(), null));
                } else {
                    List<RoomProgramDTO> programsList = new ArrayList<>();
                    programsList.add(new RoomProgramDTO(p.getId(), p.getPropertyId(), p.getPromoCode(), null));
                    segmentsMap.put(p.getSegmentId(), programsList);
                }

            }
        });

        // If segment has only program, add it as single property program
        // Otherwise use segment instead
        segmentsMap.keySet().forEach(key -> {
            if (segmentsMap.get(key).size() > 1) {
                programList.add(new RoomProgramDTO(key, null, null, null));
            } else {

                programList.addAll(segmentsMap.get(key));
            }
        });
        
        return programList;
    }

    /**
     * Find and return default perpetual programs for the customer
     * 
     * @param offersRequest
     * @return
     */
    private List<RoomProgramDTO> getPoPrograms(RoomProgramsRequestDTO offersRequest,CVSResponse customerValues) {
    	
    	GetDefaultOfferBySegmentResponse response = null;
        GetDefaultOfferBySegmentRequest getDefaultOfferBySegmentRequest = MessageFactory
                .createGetDefaultOfferBySegmentRequest();

        getDefaultOfferBySegmentRequest.setCustomerId(offersRequest.getCustomerId());
        List<String> propertyIds = auroraProps.getPropertyIds();
        getDefaultOfferBySegmentRequest.setPropertyIds(propertyIds.toArray(new String[propertyIds.size()]));
        log.info("Sent the request to getDefaultOfferBySegment as : {}",
                getDefaultOfferBySegmentRequest.toJsonString());
        try {
        response = getAuroraClient(offersRequest.getSource())
                .getDefaultOfferBySegment(getDefaultOfferBySegmentRequest);
        
        log.info("Received the response from getDefaultOfferBySegment as : {}", response.toJsonString());
        
        }catch(Exception ex) {
        	log.info("Received error during getDefaultOfferBySegment: {}", ex.getMessage());
        }

        List<RoomProgramDTO> roomProgramList = new ArrayList<>();
        if (null != response && null != response.getOffers()) {
            CustomerOffer[] custOfferArr = response.getOffers();
            for (CustomerOffer customerOffer : custOfferArr) {
                roomProgramList
                        .add(new RoomProgramDTO(customerOffer.getId(), customerOffer.getPromotion().getPropertyId(), null, null));
            }
        }
        
        // find PO programs from regionals using CVS and programs search
        Optional.ofNullable(customerValues)
                .ifPresent(cvsResponse -> {
                    Map<String, Integer> rankByPropertyIdMap = cvsResponse.getRanks();

                    rankByPropertyIdMap.keySet()
                            .forEach(property -> {
                                if (null != rankByPropertyIdMap.get(property) && rankByPropertyIdMap.get(property) > 0) {
                                    Optional<RoomProgram> programOpt = Optional.ofNullable(roomProgramCacheService
                                            .getProgramByCustomerRank(rankByPropertyIdMap.get(property), property));
                                    programOpt.ifPresent(program -> roomProgramList
                                            .add(new RoomProgramDTO(program.getId(), property, null, null)));
                                }
                            });
                });
        
        return filterProgramsByProperty(roomProgramList, offersRequest.getPropertyId());
    }

    /**
     * Find and return programs which are configured for ICE only and patron
     * promos available for the customer. Patron promos are read from
     * getCustomerOffers GSE api itself to avoid making duplicate call to get
     * patron promos.
     * 
     * @param offersRequest
     * @param offersResponse
     */
    private void setIceAndPatronPrograms(RoomProgramsRequestDTO offersRequest, RoomProgramsResponseDTO offersResponse) {

        CustomerOffersRequestDTO custOffersRequest = RoomProgramsTransformer
                .buildGetCustomerOffersRequest(offersRequest);

        CustomerOfferResponse custOffersResponse = getCustomerOffers(custOffersRequest);

        List<RoomProgramDTO> iceOnlyPrograms = new ArrayList<>();
        List<RoomProgramDTO> patronPrograms = new ArrayList<>();
        if(null != custOffersResponse) {
        custOffersResponse.getOffers()
                .forEach(offer -> {
                    RoomProgram program = roomProgramCacheService.getRoomProgram(offer.getId());
                    if (null != program) {
                        if (StringUtils.isNotEmpty(offer.getPromoId())) {
                            patronPrograms.add(new RoomProgramDTO(offer.getId(), program.getPropertyId(), program.getPromoCode(), null));
                        } else if (isProgramIceOnly(program)) {
                            iceOnlyPrograms.add(new RoomProgramDTO(offer.getId(), program.getPropertyId(), program.getPromoCode(), null));
                        }
                    }
                });
        }

        offersResponse.setIceChannelPrograms(iceOnlyPrograms);
        offersResponse.setPatronPrograms(patronPrograms);
    }
    
    /**
     * Returns true if the program is configured for ICE only.
     * 
     * @param program
     *            Room Program
     * @return Returns true if program is configured for ICE only.
     */
    public boolean isProgramIceOnly(RoomProgram program) {
        return null != program && !program.isBookableOnline() && !program.isBookableByProperty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.RoomProgramDAOStrategy#
     * getCustomerOffers(com.mgm.services.booking.room.model.request.dto.
     * RoomProgramsRequestDTO)
     */
    @Override
    public RoomProgramsResponseDTO getRoomPrograms(RoomProgramsRequestDTO offersRequest, List<CustomerPromotion> patronOffers, CVSResponse customerValues) {

        RoomProgramsResponseDTO offersResponse = new RoomProgramsResponseDTO();

        if (offersRequest.getChannel().equalsIgnoreCase("ice")) {
            setIceAndPatronPrograms(offersRequest, offersResponse);
        } else {
            offersResponse.setPatronPrograms(getPatronPrograms(offersRequest, patronOffers));
        }
        
        if (offersRequest.isPerpetualPricing()) {
            offersResponse.setPoPrograms(getPoPrograms(offersRequest,customerValues));
        }

        return offersResponse;
    }

    private List<RoomProgramDTO> filterProgramsByProperty(List<RoomProgramDTO> programList, String propertyId) {

        if (StringUtils.isNotEmpty(propertyId)) {
            return programList.stream()
                    .filter(p -> StringUtils.isNotBlank(p.getPropertyId()) && p.getPropertyId()
                            .equals(propertyId))
                    .collect(Collectors.toList());
        }
        return programList;
    }
  
    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.RoomProgramDAOStrategy#isSegmentGUID(
     * java.lang.String)
     */
    @Override
    public boolean isSegmentGUID(String programId) {

        List<RoomProgram> programs = roomProgramCacheService.getProgramsBySegmentId(programId);

        return !programs.isEmpty();

    }

    @Override
    public List<RoomOfferDetails> getRatePlanById(RoomProgramV2Request request) {
        // Not Used
        return Collections.emptyList();
    }

    /**
     * 
     */
    @Override
    public boolean isProgramPO(String programId) {
        return roomProgramCacheService.isProgramPO(programId);
    }

    /**
     * This method adds up all combinations of programIds to promo for GSE
     * @param request
     * @return
     */
    @Override
    public Map<String, String> getProgramPromoAssociation(RoomProgramPromoAssociationRequest request) {
        final Map<String, String> associationMap = new HashMap<>();
        final List<String> programIds = request.getProgramIds();
        if (CollectionUtils.isNotEmpty(programIds)) {
            programIds.stream().forEach(programId -> {
                associationMap.put(programId, request.getPromo());
            });
        }
        return associationMap;
    }

    private void updateValidateResponse(String programId, RoomProgramValidateResponse validateResponse, Optional<RoomProgram> cacheProgram) {
        validateResponse.setProgramId(programId);
        validateResponse.setPropertyId(cacheProgram.get().getPropertyId());
        if (StringUtils.isNotEmpty(cacheProgram.get().getSegmentId())) {
            validateResponse.setSegment(true);
        }
        // set propertyId based on cache instead of request
        // set rate plan tags
        validateResponse.setRatePlanTags(ArrayUtils.isNotEmpty(cacheProgram.get().getTags()) ? Arrays.asList(cacheProgram.get().getTags()) : new ArrayList<>());
        validateResponse.setMyvegas(BaseCommonUtil.isContainMyVegasTags(cacheProgram.get().getTags()));
        // set the patronProgram flag depending on patronPromoId
        validateResponse.setPatronProgram(StringUtils.isNotEmpty(cacheProgram.get().getPatronPromoId()));
        validateResponse.setRatePlanCode(cacheProgram.get().getPromoCode());
        validateResponse.setGroupCode(cacheProgram.get().getOperaBlockCode());
    }

}
