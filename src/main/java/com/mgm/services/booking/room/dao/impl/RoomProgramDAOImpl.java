/**
 * 
 */
package com.mgm.services.booking.room.dao.impl;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.mgm.services.booking.room.dao.*;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.response.*;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.OfferType;
import com.mgm.services.booking.room.model.ReservationSystemType;
import com.mgm.services.booking.room.model.RoomProgramBasic;
import com.mgm.services.booking.room.model.content.PackageConfig;
import com.mgm.services.booking.room.model.content.PackageConfigParam;
import com.mgm.services.booking.room.model.loyalty.CustomerPromotion;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.request.dto.CustomerOffersRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.response.RoomSegmentResponse.Program;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgmresorts.aurora.common.CustomerOffer;
import com.mgmresorts.aurora.messages.GetDefaultOfferBySegmentRequest;
import com.mgmresorts.aurora.messages.GetDefaultOfferBySegmentResponse;
import com.mgmresorts.aurora.messages.MessageFactory;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for room program DAO operations by invoking respective
 * aurora API calls.
 */
@Component
@Log4j2
public class RoomProgramDAOImpl extends AuroraBaseDAO implements RoomProgramDAO {

    @Autowired
    private RoomProgramDAOStrategyACRSImpl acrsStrategy;

    @Autowired
    private RoomProgramDAOStrategyGSEImpl gseStrategy;

    @Autowired
    ApplicationProperties appProperties;
    
    @Autowired
    private SecretsProperties secretProperties;
    
    @Autowired
    private RoomProgramCacheService roomProgramCacheService;

    @Autowired
    protected LoyaltyDao loyaltyDao;
    @Autowired
    protected CVSDao cvsDao;

    @Autowired
    private ProgramContentDAO programContentDao;

    @Autowired
    private PackageConfigDAO packageConfigDao;

    private ExecutorService executor = Executors.newCachedThreadPool();

    class HostResponse<T> {
        ReservationSystemType type;
        T response;

        public HostResponse(ReservationSystemType type,
                            T response) {
            this.type = type;
            this.response = response;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.RoomOfferDAO#getRoomOffers(com.mgm.
     * services.booking.room.model.request.RoomOffersRequest)
     */
    @Override
    public List<RoomProgram> getRoomOffers(RoomProgramRequest request) {
        RoomProgramDAOStrategy strategy = gseStrategy;
        Optional<String> firstProperty = request.getPropertyIds().stream().findFirst();
        if ( firstProperty.isPresent() && isPropertyManagedByAcrs(firstProperty.get()) ){
            strategy = acrsStrategy;
        }
        final String uniqueId = "PROPERTY_ID:" + request.getPropertyIds();
        log.debug(createStrategyLogEntry("getRoomOffers", uniqueId, strategy));
        return strategy.getRoomOffers(request);
    }

    /**
     * This method validates the passed on Program for its different eligibility criteria
     * @param request
     * @return
     */
    @Override
    @LogExecutionTime
    public RoomProgramValidateResponse validateProgramV2(RoomProgramValidateRequest request) {
        RoomProgramDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(request.getPropertyId())
                || ACRSConversionUtil.isAcrsRatePlanGuid(request.getProgramId())
                || ACRSConversionUtil.isAcrsGroupCodeGuid(request.getProgramId())) {
            strategy = acrsStrategy;
        }
        
        if(StringUtils.isNotBlank(request.getProgramId()) && CommonUtil.isUUID(request.getProgramId())) {
        	strategy = gseStrategy;
        }
        
        final String uniqueId = "PROPERTY_ID:" + request.getPropertyId();
        log.debug(createStrategyLogEntry("validateProgramV2", uniqueId, strategy));
        return strategy.validateProgramV2(request);
    }

    /**
     * This method validates the passed on Program for its different eligibility criteria
     * @param request
     * @return
     */
    @Override
    @LogExecutionTime
    public RoomProgramValidateResponse validateProgram(RoomProgramValidateRequest request) {
        RoomProgramDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(request.getPropertyId()) ){
            strategy = acrsStrategy;
        }
        final String uniqueId = "PROPERTY_ID:" + request.getPropertyId();
        log.debug(createStrategyLogEntry("validateProgram", uniqueId, strategy));
        return strategy.validateProgram(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.RoomProgramDAO#getProgramByPromoCode(
     * java.lang.String, java.lang.String)
     */
    @Override
    public String getProgramByPromoCode(String propertyId, String promoCode) {

        RoomProgramDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(propertyId)) {
            strategy = acrsStrategy;
        }

        final String uniqueId = String.format("PROPERTY_ID: %s, PROMO_CODE: %s", propertyId, promoCode);
        log.debug(createStrategyLogEntry("getProgramByPromoCode", uniqueId, strategy));
        return strategy.getProgramByPromoCode(propertyId, promoCode);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.RoomProgramDAO#
     * getDefaultPerpetualPrograms(com.mgm.services.booking.room.model.request.
     * PerpetualProgramRequest)
     */
    @Override
    public List<PerpetaulProgram> getDefaultPerpetualPrograms(PerpetualProgramRequest request) {

        final GetDefaultOfferBySegmentRequest getDefaultOfferBySegmentRequest = MessageFactory
                .createGetDefaultOfferBySegmentRequest();

        getDefaultOfferBySegmentRequest.setCustomerId(request.getCustomerId());
        List<String> propertyIds = request.getPropertyIds();
        getDefaultOfferBySegmentRequest.setPropertyIds(propertyIds.toArray(new String[propertyIds.size()]));
        log.info("Sent the request to getDefaultOfferBySegment as : {}",
                getDefaultOfferBySegmentRequest.toJsonString());

        final GetDefaultOfferBySegmentResponse response = getAuroraClient(request.getSource())
                .getDefaultOfferBySegment(getDefaultOfferBySegmentRequest);

        log.info("Received the response from getDefaultOfferBySegment as : {}", response.toJsonString());

        List<PerpetaulProgram> roomProgramList = new ArrayList<>();
        if (null != response.getOffers()) {
            final CustomerOffer[] custOfferArr = response.getOffers();
            for (final CustomerOffer customerOffer : custOfferArr) {
                PerpetaulProgram program = new PerpetaulProgram();
                program.setId(customerOffer.getId());
                program.setPropertyId(customerOffer.getPromotion().getPropertyId());
                roomProgramList.add(program);
            }
        }

        return roomProgramList;
    }

    @Override
    @LogExecutionTime
    public ApplicableProgramsResponse getApplicablePrograms(ApplicableProgramRequestDTO request) {
        RoomProgramDAOStrategy strategy = gseStrategy;
        if ( isPropertyManagedByAcrs(request.getPropertyId()) ){
            strategy = acrsStrategy;
        }
        final String uniqueId = "CID:" + request.getCustomerId() + "TD:" + request.getTravelDate() + ">CIND:" + request.getCheckInDate() + ">COD:" + request.getCheckOutDate();
        log.debug(createStrategyLogEntry("getApplicablePrograms", uniqueId, strategy));
        return strategy.getApplicablePrograms(request);
    }

    private String createStrategyLogEntry(String method, String uniqueId, RoomProgramDAOStrategy strategy) {
        String strategyString = strategy == null ? "GSEAndACRSStrategy" :
                (strategy instanceof RoomProgramDAOStrategyGSEImpl) ? "GSEStrategy" : "ACRSStrategy";
        return "RoomProgramDAOImpl > "
                + method
                + " | ID: "
                + uniqueId
                + " | "
                + strategyString;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.mgm.services.booking.room.dao.RoomProgramDAO#
     * getCustomerOffers(com.mgm.services.booking.room.model.request.dto.
     * CustomerOffersSearchRequestDTO)
     */
    @Override
    @LogExecutionTime
    public CustomerOfferResponse getCustomerOffers(CustomerOffersRequestDTO request) {

        final List<Future<HostResponse>> allFutures = new ArrayList<>();
        for (RoomProgramDAOStrategy strategy : getRoomProgramDAOStrategies(request)) {
            allFutures.add(executor.submit(() -> {
                return new HostResponse<CustomerOfferResponse>(
                        RoomProgramDAOStrategyGSEImpl.class.isInstance(strategy) ? ReservationSystemType.GSE : ReservationSystemType.ACRS,
                        strategy.getCustomerOffers(request)
                );
            }));
        }

        return getCustomerOfferAggregatedResponse(allFutures);
    }

    private List<RoomProgramDAOStrategy> getRoomProgramDAOStrategies(CustomerOffersRequestDTO request) {

        final List<RoomProgramDAOStrategy> daoStrategies = new ArrayList<>();
        if (request.getPropertyId() == null) {
            daoStrategies.add(gseStrategy);
            if (isAcrsEnabled()) {
                daoStrategies.add(acrsStrategy);
            }
        } else {
            daoStrategies.add(isPropertyManagedByAcrs(request.getPropertyId())  ? acrsStrategy : gseStrategy);
        }

        final String uniqueId = "CID:" + request.getCustomerId() + "MLife#:" + request.getMlifeNumber() + ">PID:" + request.getPropertyId()
                + ">NoRolled:" + request.isNotRolledToSegments();
        log.debug(createStrategyLogEntry("getCustomerOffers", uniqueId, daoStrategies.size() == 1 ? daoStrategies.get(0) : null));
        return daoStrategies;
    }

    private CustomerOfferResponse getCustomerOfferAggregatedResponse(List<Future<HostResponse>> allFutures) {

        final CustomerOfferResponse finalResponse = new CustomerOfferResponse();
        finalResponse.setOffers(new ArrayList<>());

        allFutures.stream().forEach(future -> {
            try {
                final HostResponse<CustomerOfferResponse> hostResponse = future.get(appProperties.getCustomerOfferRequestTimeoutInSec(), TimeUnit.SECONDS);
                if (null != hostResponse && null != hostResponse.response) {

                    final List<CustomerOfferDetail> offers = hostResponse.response.getOffers();
                    if (CollectionUtils.isNotEmpty(offers)) {

                        offers.stream().forEach(offer -> {

                            switch(getOfferType(offer))  {

                                case PROGRAM:

                                    switch(hostResponse.type) {

                                        case GSE:
                                            final Optional<com.mgm.services.booking.room.model.phoenix.RoomProgram> cacheProgram = Optional
                                                    .ofNullable(roomProgramCacheService.getRoomProgram(offer.getId()));
                                            if (cacheProgram.isPresent()) {
                                                final com.mgm.services.booking.room.model.phoenix.RoomProgram roomProgram = cacheProgram.get();
                                                if (!isPropertyManagedByAcrs(roomProgram.getPropertyId())) {
                                                    finalResponse.getOffers().add(offer);
                                                }
                                                // Don't add the Program if its managed by ACRS
                                            } else {
                                                // Add in case cache entry not found
                                                finalResponse.getOffers().add(offer);
                                            }
                                            break;

                                        case ACRS:
                                                final String propertyId = offer.getPropertyId();
                                            if (propertyId == null || isPropertyManagedByAcrs(propertyId)) {
                                                finalResponse.getOffers().add(offer);
                                            }
                                            // If the ACRS RatePlan Property is currently not activated in RBS then don't send it back in the response
                                            break;
                                        default:
                                    }
                                    break;

                                default:
                                    // Add it in case of Segments
                                    finalResponse.getOffers().add(offer);
                            }

                        });
                    }

                }

            }
            catch (TimeoutException | InterruptedException | ExecutionException e) {
                final String errorMessage = e.getMessage();
                log.error("getCustomerOffer invocation Exception : {}", ExceptionUtils.getStackTrace(e));
                // Return empty response in case of data not found
                if (null != errorMessage && !errorMessage.contains(BusinessException.class.getSimpleName()))  throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
            }
        });

        return finalResponse;
    }

    private OfferType getOfferType(CustomerOfferDetail offer) {
        for (OfferType type : OfferType.values()) {
            if (type.name().equalsIgnoreCase(offer.getOfferType())) {
                return type;
            }
        }
        // Defaulted as Segment
        return OfferType.SEGMENT;
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.dao.RoomProgramDAO#findProgramsByRatePlanCode(java.lang.String)
     */
    @Override
    public List<RoomProgramBasic> findProgramsByRatePlanCode(String ratePlanCode, String source,  boolean isPromoRatePlan) {

        Map<String, RoomProgramBasic> programMap = new HashMap<>();

        log.info("GSE Disabled: {}", appProperties.isGseDisabled());
        // Get info from GSE as long as it's not fully disabled
        if (!appProperties.isGseDisabled()) {
            
            log.info("Fetching programs by rate plan code from GSE");
            List<RoomProgramBasic> programs = gseStrategy.findProgramsByRatePlanCode(ratePlanCode, source);

            programs.forEach(program -> {
                // Program for ACRS managed property will be dropped here
                if(!isPropertyManagedByAcrs(program.getPropertyId())) {
                    programMap.put(program.getPropertyId(), program);
                }
            });
        }

        // Override info from ACRS as long as at least 1 property enabled for
        // ACRS
        if (isAcrsEnabled()) {
            log.info("Fetching programs by rate plan code from ACRS");
            List<RoomProgramBasic> acrsPrograms = new ArrayList<>();
            if (isPromoRatePlan) {
                Set<String> ratePlanCodes = CommonUtil.promoCodes(ratePlanCode);
                for (String ratePlan : ratePlanCodes) {
                    acrsPrograms = acrsStrategy.findProgramsByRatePlanCode(ratePlan, source);
                    if (CollectionUtils.isNotEmpty(acrsPrograms)) {
                        break;
                    }
                }
            } else {
                acrsPrograms = acrsStrategy.findProgramsByRatePlanCode(ratePlanCode, source);
            }
            acrsPrograms.forEach(program -> programMap.put(program.getPropertyId(), program));
        }
        return new ArrayList<>(programMap.values());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.RoomProgramDAO#findProgramsIfSegment(
     * java.lang.String)
     */
    @Override
    public List<RoomProgramBasic> findProgramsIfSegment(String programId, String source) {

        String ratePlanCode = StringUtils.EMPTY;
        
        // if new segments approach is not enabled, use GSE segments
        String enableNewSegments = secretProperties.getSecretValue(String.format(appProperties.getEnableNewSegmentsKey(),appProperties.getRbsEnv()));
        if (enableNewSegments.equals("false")) {
            return gseStrategy.findProgramsIfSegment(programId, source);
        }

        // Get info from GSE as long as it's not fully disabled
        if (!appProperties.isGseDisabled()) {
            ratePlanCode = gseStrategy.getRatePlanCodeByProgramId(programId);
        }

        // Override info from ACRS as long as at least 1 property enabled for
        // ACRS
        if (StringUtils.isEmpty(ratePlanCode) && isAcrsEnabled()) {
            ratePlanCode = acrsStrategy.getRatePlanCodeByProgramId(programId);
        }

        if (StringUtils.isNotEmpty(ratePlanCode)) {
            return findProgramsByRatePlanCode(ratePlanCode, source, false);
        }

        return new ArrayList<>();
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.dao.RoomProgramDAO#getRoomPrograms(com.mgm.services.booking.room.model.request.dto.RoomProgramsRequestDTO)
     */
    @Override
    public RoomProgramsResponseDTO getRoomPrograms(RoomProgramsRequestDTO offersRequest) {

        // Steps:
        // - Get patron offers
        // - Get GSE/ACRS offers (patron, po, ice)

        List<CustomerPromotion> patronOfferList = new ArrayList<>();
        // Retrieve Patron Offers, excluding only for GSE and ICE channel combination
        if (!offersRequest.isResortPricing()) {
            patronOfferList = fetchPatronOffers(offersRequest);
        }
        //Get customer values and pass it to GSE and ACRS strategies
        CVSResponse customerValues;
        if(offersRequest.isPerpetualPricing()) {
            customerValues = cvsDao.getCustomerValues(offersRequest.getMlifeNumber());
        } else {
            customerValues = null;
        }
        // Create userRank values from CVS Response
        String userRankValues="";
        if(customerValues != null) {
            userRankValues = createUserRankValuesFromCVSResponse(customerValues);
        }

        RoomProgramsResponseDTO roomProgramsResponseDTO;

        final List<CustomerPromotion> patronOffers = patronOfferList;
        // If programs are requested for a specific property, use respective
        // strategy
        if (StringUtils.isNotEmpty(offersRequest.getPropertyId())) {
            if (isPropertyManagedByAcrs(offersRequest.getPropertyId())) {
                roomProgramsResponseDTO = acrsStrategy.getRoomPrograms(offersRequest, patronOffers,customerValues);
            } else {
                roomProgramsResponseDTO = gseStrategy.getRoomPrograms(offersRequest, patronOffers,customerValues);
            }
            if(StringUtils.isNotBlank(userRankValues)) {
                roomProgramsResponseDTO.setUserCvsValues(userRankValues);
            }
            return roomProgramsResponseDTO;
        }


        // Make parallel call to GSE and ACRS
        List<HostResponse<RoomProgramsResponseDTO>> hostResponseList = getRoomProgramDAOStrategies().parallelStream()
                .map(strategy ->
                new HostResponse<RoomProgramsResponseDTO>(
                        RoomProgramDAOStrategyGSEImpl.class.isInstance(strategy) ? ReservationSystemType.GSE
                                : ReservationSystemType.ACRS,
                        strategy.getRoomPrograms(offersRequest, patronOffers,customerValues)))
                .collect(Collectors.toList());

        RoomProgramsResponseDTO programsResponse = new RoomProgramsResponseDTO();

        hostResponseList.forEach(hostResponse -> {
            if (hostResponse.type.equals(ReservationSystemType.GSE)) {
                RoomProgramsResponseDTO gseProgramsResponse = hostResponse.response;

                // Program for ACRS managed property will be dropped here
                dropProgramsManagedByAcrs(gseProgramsResponse.getPatronPrograms());
                dropProgramsManagedByAcrs(gseProgramsResponse.getPoPrograms());
                dropProgramsManagedByAcrs(gseProgramsResponse.getIceChannelPrograms());

                programsResponse.getPatronPrograms().addAll(gseProgramsResponse.getPatronPrograms());
                programsResponse.getPoPrograms().addAll(gseProgramsResponse.getPoPrograms());
                programsResponse.getIceChannelPrograms().addAll(gseProgramsResponse.getIceChannelPrograms());
            } else {
                RoomProgramsResponseDTO acrsProgramsResponse = hostResponse.response;
                
                if (null != acrsProgramsResponse) {

                    // Program for GSE managed property will be dropped here
                    dropProgramsManagedByGse(acrsProgramsResponse.getPatronPrograms());
                    dropProgramsManagedByGse(acrsProgramsResponse.getPoPrograms());
                    dropProgramsManagedByGse(acrsProgramsResponse.getIceChannelPrograms());

                    programsResponse.getPatronPrograms().addAll(acrsProgramsResponse.getPatronPrograms());
                    programsResponse.getPoPrograms().addAll(acrsProgramsResponse.getPoPrograms());
                    programsResponse.getIceChannelPrograms().addAll(acrsProgramsResponse.getIceChannelPrograms());

                }
            }
        });
        if(StringUtils.isNotBlank(userRankValues)) {
            programsResponse.setUserCvsValues(userRankValues);
        }
        return programsResponse;
    }

    private String createUserRankValuesFromCVSResponse(CVSResponse customerValues) {
        Set<String> userRankValuesSet =
                Arrays.stream(customerValues.getCustomer().getCustomerValues()).
                        map(this::createUserRankFromCustomerValue).filter(StringUtils::isNotBlank)
                        .collect(Collectors.toSet());

        String cvsValues = String.join(ServiceConstant.CVS_VALUES_DELIMITER, userRankValuesSet);
        String unRankedValues = createUnRankedValuesForRemainingProperties(cvsValues);
        return Stream.of(cvsValues, unRankedValues)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(ServiceConstant.CVS_VALUES_DELIMITER));
    }

    private String createUnRankedValuesForRemainingProperties(String cvsValuesString) {
        Map<String, String> var141PropertyMap = appProperties.getPropertyCodeVar141Map();
        Set<String> unRankedCvsValuesSet =
                var141PropertyMap.values().stream()
                        .filter(property -> !cvsValuesString.contains(property))
                        .map(property -> property + ServiceConstant.EQUAL + ServiceConstant.UNRANKED_CVS_VALUE)
                        .collect(Collectors.toSet());

        return String.join(ServiceConstant.CVS_VALUES_DELIMITER, unRankedCvsValuesSet);
    }

    private String createUserRankFromCustomerValue(CVSResponse.CVSCustomerValue customerValue) {
        StringBuilder userRank = new StringBuilder();
        String var141PropertyValue = appProperties.getVar141ValueFromHotelCode(customerValue.getProperty());
        String customerScore = getCustomerScoreFromSegmentAndRank(customerValue);

        if(var141PropertyValue != null && !customerScore.equalsIgnoreCase(ServiceConstant.INVALID)) {
            userRank.append(var141PropertyValue);
            userRank.append(ServiceConstant.EQUAL);
            userRank.append(customerScore);
        }
        return userRank.toString();
    }

    /*
     * if segment=0, powerRank=0, customerScore="unranked"
     * if segment=0, powerRank!=0, customerScore=powerRank
     * if segment!=0, powerRank=0, customerScore=segment
     * if segment!=0, powerRank!=0, customerScore="invalid"
     */
    private String getCustomerScoreFromSegmentAndRank(CVSResponse.CVSCustomerValue customerValue) {
        int segment = customerValue.getValue().getCustomerGrade().getSegment();
        int powerRank = customerValue.getValue().getCustomerGrade().getPowerRank();
        return (segment == 0) ?
                ((powerRank == 0) ? ServiceConstant.UNRANKED_CVS_VALUE : Integer.toString(powerRank)) :
                ((powerRank == 0) ? Integer.toString(segment) : ServiceConstant.INVALID);
    }

    @Override
    public List<RoomOfferDetails> getRatePlanById(RoomProgramV2Request request) {
        return acrsStrategy.getRatePlanById(request);
    }

    private List<CustomerPromotion> fetchPatronOffers(RoomProgramsRequestDTO offersRequest) {
        final List<CustomerPromotion> promotions = new ArrayList<>();
        if (StringUtils.isEmpty(offersRequest.getPropertyId())
                || isPropertyManagedByAcrs(offersRequest.getPropertyId())
                || !StringUtils.equalsIgnoreCase(ServiceConstant.ICE, offersRequest.getChannel()) ) {
            promotions.addAll(loyaltyDao.getPlayerPromos(offersRequest.getMlifeNumber()));
        }
        return promotions;
    }

    private void dropProgramsManagedByAcrs(List<RoomProgramDTO> programList) {

        programList.removeIf(p -> isPropertyManagedByAcrs(p.getPropertyId()));

    }
    
    private void dropProgramsManagedByGse(List<RoomProgramDTO> programList) {

        programList.removeIf(p -> !isPropertyManagedByAcrs(p.getPropertyId()));

    }
    
    private List<RoomProgramDAOStrategy> getRoomProgramDAOStrategies() {

        final List<RoomProgramDAOStrategy> daoStrategies = new ArrayList<>();
        
        if (!appProperties.isGseDisabled()) {
            daoStrategies.add(gseStrategy);
        }
        if (isAcrsEnabled()) {
            daoStrategies.add(acrsStrategy);
        }

        return daoStrategies;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.RoomProgramDAO#isProgramPO(java.lang.
     * String)
     */
    @Override
    public boolean isProgramPO(String programId) {

        if (ACRSConversionUtil.isAcrsRatePlanGuid(programId)) {
            return acrsStrategy.isProgramPO(programId);
        } else {
            return gseStrategy.isProgramPO(programId);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.dao.RoomProgramDAO#findProgramsBySegment(java.lang.String, java.lang.String)
     */
    @Override
    public List<RoomProgramBasic> findProgramsBySegment(String segmentId, String source) {
        return gseStrategy.findProgramsIfSegment(segmentId, source);
    }

    @Override
    public List<RoomProgramBasic> findProgramsByGroupCode(String groupCode, LocalDate checkInDate,
            LocalDate checkOutDate, String source) {
        List<RoomProgramBasic> groupBlockList = new ArrayList<RoomProgramBasic>();

        List<RoomProgramBasic> gseProgramList = gseStrategy.findProgramsByGroupCode(groupCode, checkInDate, checkOutDate, source);
            groupBlockList.addAll(gseProgramList);
         if(isAcrsEnabled()) {
             List<RoomProgramBasic> acrsGroupList = acrsStrategy.findProgramsByGroupCode(groupCode, checkInDate,
                     checkOutDate, source);
             groupBlockList.addAll(acrsGroupList);
         }
        return groupBlockList;
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.dao.RoomProgramDAO#getRoomSegment(java.lang.String, java.lang.String)
     */
    @Override
    public RoomSegmentResponse getRoomSegment(String segment, String source, boolean isPromoRatePlan) {
        
        if (StringUtils.isBlank(segment)) {
            return new RoomSegmentResponse();
        }

        // if GSE guid for segment, use GSE programs cache based lookup
        if (CommonUtil.isUuid(segment)) {
            List<com.mgm.services.booking.room.model.phoenix.RoomProgram> programs = roomProgramCacheService
                    .getProgramsBySegmentId(segment);
            List<Program> programList = new ArrayList<>();
            
            // drop programs managed by ACRS
            programs.removeIf(p -> isPropertyManagedByAcrs(p.getPropertyId()));

            if (!programs.isEmpty()) {
                programs.forEach(p -> programList.add(new Program(p.getId(), p.getPropertyId(), p.getTravelPeriodStart(), p.getTravelPeriodEnd())));
            }

            return new RoomSegmentResponse(segment, programList);
        }

        // if segment code was provided
        List<RoomProgramBasic> programs = findProgramsByRatePlanCode(segment, source, isPromoRatePlan);
        List<Program> programList = new ArrayList<>();

        String finalSegment = segment;
        if (!programs.isEmpty()) {
            for (RoomProgramBasic p : programs) {
                if(CommonUtil.isProgramValid(p)) {
                    programList.add(new Program(p.getProgramId(), p.getPropertyId(), p.getTravelPeriodStart(), p.getTravelPeriodEnd()));
                    if (isPropertyManagedByAcrs(p.getPropertyId()) && StringUtils.isNotEmpty(p.getRatePlanCode())) {
                        finalSegment = p.getRatePlanCode();
                    }
                    if (!isPropertyManagedByAcrs(p.getPropertyId()) && StringUtils.isNotEmpty(p.getPromoCode())) {
                        finalSegment = p.getPromoCode();
                    }
                }
            }
        }
        return new RoomSegmentResponse(finalSegment, programList);

    }

    @Override
    public RoomSegmentResponse getRoomSegment(String segment, String programId, String source) {

        if (StringUtils.isNotBlank(segment)) {
            return getRoomSegment(segment, source, false);
        }

        // if new segments approach is not enabled, use GSE segments
        String enableNewSegments = secretProperties.getSecretValue(String.format(appProperties.getEnableNewSegmentsKey(),appProperties.getRbsEnv()));

        if (CommonUtil.isUUID(programId)) {
            Optional<com.mgm.services.booking.room.model.phoenix.RoomProgram> programOpt = Optional
                    .ofNullable(roomProgramCacheService.getRoomProgram(programId));

            if (programOpt.isPresent()) {
                com.mgm.services.booking.room.model.phoenix.RoomProgram program = programOpt.get();

                if (enableNewSegments.equals("false")) {
                    if (StringUtils.isNotBlank(program.getSegmentId())) {
                        return getRoomSegment(program.getSegmentId(), source, false);
                    }
                } else {
                    String ratePlanCode = program.getPromoCode();
                    return getRoomSegment(ratePlanCode, source, true);
                }

            }

        } else {
            String ratePlanCode = acrsStrategy.getRatePlanCodeByProgramId(programId);
            return getRoomSegment(ratePlanCode, source, false);
        }

        return new RoomSegmentResponse();
    }

    @Override
    public void updateValidateResponseForPackagePrograms(RoomProgramValidateRequest validateRequest,
            RoomProgramValidateResponse validateResponse) {
        try {
            com.mgm.services.booking.room.model.content.Program program = programContentDao
                    .getProgramContent(validateRequest.getPropertyId(), validateRequest.getProgramId());
            if (null != program && BooleanUtils.toBoolean(program.getHdePackage())) {
                log.debug("Program ID {} is associated to HDE Package and it's promoCode in Content API is {}",
                        validateRequest.getProgramId(), program.getPromoCode());
                validateResponse.setHdePackage(true);
                
                String ratePlanCode = getRatePlanByProgramId(validateRequest.getProgramId(), validateRequest.getPropertyId());
                log.debug("Rate plan code by program id: {}", ratePlanCode);
                if (StringUtils.isNotEmpty(ratePlanCode)) {
                    PackageConfig[] packageConfigs = packageConfigDao.getPackageConfigs(PackageConfigParam.segmentId,
                            ratePlanCode);
                    if (null == packageConfigs || packageConfigs.length == 0
                            || !BooleanUtils.toBoolean(packageConfigs[0].getActive())) {
                        log.info(
                                "Rate plan code is either not associated to any package tier or associated package tier is not active",
                                ratePlanCode);
                        validateResponse.setValid(false);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get response from Content API", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.mgm.services.booking.room.dao.RoomProgramDAO#getRatePlanByProgramId(java.lang.String, java.lang.String)
     */
    @Override
    public String getRatePlanByProgramId(String programId, String propertyId) {
        RoomProgramDAOStrategy strategy = gseStrategy;
        if (isPropertyManagedByAcrs(propertyId) || ACRSConversionUtil.isAcrsRatePlanGuid(programId)) {
            strategy = acrsStrategy;
        }
        return strategy.getRatePlanCodeByProgramId(programId);
    }

    @Override
    public Map<String, String> getProgramPromoAssociation(RoomProgramPromoAssociationRequest request) {
        RoomProgramDAOStrategy strategy = gseStrategy;
        final List<String> programIds = request.getProgramIds();
        if (isPropertyManagedByAcrs(request.getPropertyId()) ||
                (CollectionUtils.isNotEmpty(programIds) && ACRSConversionUtil.isAcrsRatePlanGuid(programIds.get(0)))) {
            strategy = acrsStrategy;
        }
        return strategy.getProgramPromoAssociation(request);
    }
}
