package com.mgm.services.booking.room.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.dao.*;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.request.dto.CommitPaymentDTO;
import com.mgm.services.booking.room.model.request.dto.UpdateProfileInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.transformer.RoomProgramValidateRequestTransformer;
import com.mgm.services.booking.room.util.ServiceConversionHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.event.ReservationEventType;
import com.mgm.services.booking.room.exception.ErrorTypes;
import com.mgm.services.booking.room.exception.ErrorVo;
import com.mgm.services.booking.room.mapper.PreviewModifyRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.mapper.UserProfileInfoRequestMapper;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.ocrs.OcrsReservation;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.EventPublisherService;
import com.mgm.services.booking.room.service.FindReservationService;
import com.mgm.services.booking.room.service.IDUtilityService;
import com.mgm.services.booking.room.service.ItineraryService;
import com.mgm.services.booking.room.service.ModifyReservationService;
import com.mgm.services.booking.room.service.ReservationEmailV2Service;
import com.mgm.services.booking.room.service.cache.RoomCacheService;
import com.mgm.services.booking.room.service.helper.FindReservationServiceHelper;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.BaseCommonUtil;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for ModifyReservationService
 */
@Component
@Log4j2
public class ModifyReservationServiceImpl implements ModifyReservationService {

    @Autowired
    private ModifyReservationDAO modifyReservationDAO;

    @Autowired
    private RoomReservationRequestMapper requestMapper;

    @Autowired
    private RoomReservationResponseMapper responseMapper;
    
    @Autowired
    private PreviewModifyRequestMapper previewMapper;
    
    @Autowired
    private ItineraryService itineraryService;

    @Autowired
    private EventPublisherService<RoomReservationV2Response> eventPublisherService;

    @Autowired
    private ReservationEmailV2Service emailService;

    @Autowired
    private ApplicationProperties appProperties;

    @Autowired
    private ReservationServiceHelper reservationServiceHelper;

    @Autowired
    private FindReservationDAO findReservationDao;

    @Autowired
    private IDUtilityService idUtilityService;

    @Autowired
    private UserProfileInfoRequestMapper userProfileInfoRequestMapper;

    @Autowired
    private OCRSDAO ocrsDao;
    
    @Autowired
    private FindReservationService findResvService;

    @Autowired
    private FindReservationServiceHelper findReservationServiceHelper;
    
    @Autowired
    private RoomCacheService roomCacheService;

    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private ServiceConversionHelper serviceConversionHelper;

    @Autowired
    private RoomProgramDAO roomProgramDao;

    @Autowired
    private RoomContentDAO roomContentDao;

    @Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;

    @Autowired
    private AcrsProperties acrsProperties;

     /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.ModifyReservationService#
     * preModifyReservation(com.mgm.services.booking.room.model.request.
     * PreModifyRequest)
     */
    @Override
    public RoomReservation preModifyReservation(PreModifyRequest preModifyRequest) {

        RoomReservation reservation = modifyReservationDAO.preModifyReservation(preModifyRequest);

        Set<String> programIds = new TreeSet<>();
        reservation.getBookings().forEach(booking -> programIds.add(booking.getProgramId()));

        // Since MRD launch, no longer allow modifications on multiple
        // program bookings
        if (programIds.size() > 1) {
            throw new BusinessException(ErrorCode.DATES_UNAVAILABLE);
        }
        return reservation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.ModifyReservationService#
     * modifyReservation(java.lang.String,
     * com.mgm.services.booking.room.model.reservation.RoomReservation)
     */
    @Override
    public RoomReservation modifyReservation(String source, RoomReservation reservation) {
        return modifyReservationDAO.modifyReservation(source, reservation);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.ModifyReservationService#
     * updateProfileInfo(com.mgm.services.booking.room.model.request.
     * UpdateProfileInfoRequest)
     */
    @Override
    public UpdateProfileInfoResponse updateProfileInfo(UpdateProfileInfoRequest request, String token) {
        RoomReservation originalReservation = findReservationDao
                .findRoomReservation(reservationServiceHelper.createFindReservationV2Request(request));
        request.setOriginalReservation(originalReservation);
        if (null != originalReservation) {
            request.setPropertyId(originalReservation.getPropertyId());
        }
        
        
        log.info("Found Reservation: {}", CommonUtil.convertObjectToJsonString(originalReservation));
        log.info("Profile update request: {}", CommonUtil.convertObjectToJsonString(request));

        boolean mlifeChanged = reservationServiceHelper.isMlifeAddedOrChanged(originalReservation, request);
        boolean partnerAcctNoChanged = reservationServiceHelper.isPartnerAccountNoAddedOrChanged(originalReservation,request);
        
        log.info("Mlife changed or updated?: {}", mlifeChanged);
        // OCRS: Update reservation profile if mlife has changed but entire profile isn't changed (matching firstName & lastName)
        //CBSR-1565 Skip OCRS update call for TCOLV, OCRS will get this information from LMS through source system update.
        if ( (mlifeChanged || partnerAcctNoChanged )
                 && originalReservation.getProfile().getFirstName().equalsIgnoreCase(request.getUserProfile().getFirstName())
                && originalReservation.getProfile().getLastName().equalsIgnoreCase(request.getUserProfile().getLastName())
                && !StringUtils.equalsIgnoreCase(appProperties.getTcolvPropertyId(), originalReservation.getPropertyId())){
            log.info("Updating mlife in OCRS");
            UserProfileRequest userProfileRequest = request.getUserProfile();
            if (null != userProfileRequest) {
                UpdateProfileRequest updateProfileRequest = new UpdateProfileRequest();
                updateProfileRequest.setHotelCode(appProperties.getHotelCode(originalReservation.getPropertyId()));
                updateProfileRequest.setOperaConfirmationNumber(originalReservation.getOperaConfirmationNumber());
                // Passing the name from original resv here since OCRS performs name check
                updateProfileRequest.setFirstName(originalReservation.getProfile().getFirstName());
                updateProfileRequest.setLastName(originalReservation.getProfile().getLastName());
                if(null != userProfileRequest.getPartnerAccounts() && partnerAcctNoChanged &&
                        !referenceDataDAOHelper.isPropertyManagedByAcrs(originalReservation.getPropertyId())) {
                    if(CollectionUtils.isNotEmpty(userProfileRequest.getPartnerAccounts())){
                        updateProfileRequest.setPartnerAccountNumber(request.getUserProfile().getPartnerAccounts().get(0).getPartnerAccountNo());
                        updateProfileRequest.setProgramCode(request.getUserProfile().getPartnerAccounts().get(0).getProgramCode());
                        updateProfileRequest.setMembershipLevel(request.getUserProfile().getPartnerAccounts().get(0).getMembershipLevel());
                    }
                }
                //CBSR-982 ICE to start sending MGM Id in the update profile request
                if(StringUtils.isNotEmpty(userProfileRequest.getMgmId())) {
                	updateProfileRequest.setMgmId(userProfileRequest.getMgmId());
                }
                if(mlifeChanged && !referenceDataDAOHelper.isPropertyManagedByAcrs(originalReservation.getPropertyId())) {
                    updateProfileRequest.setMlifeNumber(Integer.toString(userProfileRequest.getMlifeNo()));
                }
                OcrsReservation ocrsUpdatedReservation = ocrsDao.updateProfile(updateProfileRequest);
                if (null != ocrsUpdatedReservation && null != ocrsUpdatedReservation.getMgmProfile()) {
                    log.debug("Completed updating the reservation in OCRS with mlife: {}, and mgmId: {}",
                            ocrsUpdatedReservation.getMgmProfile().getMlifeNumber(), ocrsUpdatedReservation.getMgmProfile().getMgmId());                

                }
            }
        }

        RoomReservation reservation = modifyReservationDAO
                .updateProfileInfo(RoomReservationTransformer.createModifyProfileInfoRequestDTO(request));

        UpdateProfileInfoResponse response = new UpdateProfileInfoResponse();
        response.setRoomReservation(responseMapper.roomReservationModelToResponse(reservation));
        return response;
    }

    @Override
    public ModifyRoomReservationResponse modifyRoomReservationV2(ModifyRoomReservationRequest modifyRoomReservationRequest) {
        RoomReservationRequest reservationRequest = modifyRoomReservationRequest.getRoomReservation();
        RoomReservation roomReservation = requestMapper.roomReservationRequestToModel(reservationRequest);
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        // F1 package check validation
        RoomProgramValidateResponse validateResponse = isF1Package(roomReservation);
        adjustChargeAmount(roomReservation);
        // removing char not supported by Opera
        ReservationUtil.sanitizedComments(roomReservation);

        RoomReservation reservation = modifyReservationDAO.modifyRoomReservationV2(roomReservation);
        // Partner Accounts - Project Max Rewards - CBSR-1635
        if(!ObjectUtils.isEmpty(roomReservation.getProfile()) && CollectionUtils.isNotEmpty(roomReservation.getProfile().getPartnerAccounts())){
            reservation.getProfile().setPartnerAccounts(roomReservation.getProfile().getPartnerAccounts());
        }
        boolean isHDEPackageReservation =reservationServiceHelper.isHDEPackageReservation(reservation);
        ModifyRoomReservationResponse response = updateResponse(reservation, isHDEPackageReservation,validateResponse, roomReservation.getSource());
        // set pkg components flags to true
        List<PurchasedComponent> updatedPurchasedComponents = reservationServiceHelper.updatePackageComponentsFlag(
                response.getRoomReservation().getPropertyId(),
                response.getRoomReservation().getPurchasedComponents()
        );
        response.getRoomReservation().setPurchasedComponents(updatedPurchasedComponents);
        adjustRefundDeposit(reservation,response);
        Double changeInDeposit = 0.0;
        boolean isPaymentWidgetFlow = modifyRoomReservationRequest.getRoomReservation().isSkipPaymentProcess();
        if(null != response.getRoomReservation().getRatesSummary()) {
            changeInDeposit = response.getRoomReservation().getRatesSummary().getChangeInDeposit();
        }
        boolean hasRefund = null != changeInDeposit && changeInDeposit < 0;
        if(!(isPaymentWidgetFlow && hasRefund) || StringUtils.equalsIgnoreCase(appProperties.getTcolvPropertyId(), reservation.getPropertyId())) {
            handleNotification(reservation, response, isHDEPackageReservation, httpRequest, reservationRequest.isSkipCustomerNotification());
        }
        return response;
    }
    @Override
    public ModifyRoomReservationResponse reservationModifyPendingV4(ModifyRoomReservationRequest modifyRoomReservationRequest){
        RoomReservationRequest reservationRequest = modifyRoomReservationRequest.getRoomReservation();
        RoomReservation roomReservation = requestMapper.roomReservationRequestToModel(reservationRequest);
        // F1 package check validation do we need this?
        RoomProgramValidateResponse validateResponse = isF1Package(roomReservation);
       //adjustChargeAmount(roomReservation); is it required here? For web we might need this.
        //For payment widgetFlow commit call won't be there
        //If channel = ice
        RoomReservation reservation = modifyReservationDAO.modifyPendingRoomReservationV2(roomReservation);
        //else
       // reservation =modifyReservationDAO.preModifyReservation(roomReservation);
        //Adjust refund / change In deposit for paymentWidget flow
        boolean isHDEPackageReservation =reservationServiceHelper.isHDEPackageReservation(reservation);
        ModifyRoomReservationResponse response = updateResponse(reservation, isHDEPackageReservation,validateResponse, roomReservation.getSource());
         adjustRefundDeposit(reservation,response);
        // set pkg components flags to true
        List<PurchasedComponent> updatedPurchasedComponents = reservationServiceHelper.updatePackageComponentsFlag(
                response.getRoomReservation().getPropertyId(),
                response.getRoomReservation().getPurchasedComponents()
        );
        response.getRoomReservation().setPurchasedComponents(updatedPurchasedComponents);
        return response;
    }

    private ModifyRoomReservationResponse updateResponse(RoomReservation reservation,  boolean isHDEPackageReservation, RoomProgramValidateResponse validateResponse, String source){
        ModifyRoomReservationResponse response = new ModifyRoomReservationResponse();
        response.setRoomReservation(responseMapper.roomReservationModelToResponse(reservation));

        boolean isDepositForfeit = isHDEPackageReservation || reservationServiceHelper.isDepositForfeit(reservation);
        response.getRoomReservation().setHdePackage(isHDEPackageReservation);
        response.getRoomReservation().setDepositForfeit(isDepositForfeit);
        if (null != validateResponse && validateResponse.isF1Package()) {
            response.getRoomReservation().setF1Package(true);
            reservationServiceHelper.addF1CasinoDefaultComponentPrices(null, response.getRoomReservation() ,
                    validateResponse.getRatePlanTags(), source);
            ReservationUtil.purchasedComponentsF1Updates(response.getRoomReservation(), appProperties,
                    validateResponse.getRatePlanTags(), false);
        }
        return response;
    }
    private void adjustRefundDeposit(RoomReservation reservation, ModifyRoomReservationResponse response){
        if (null != response.getRoomReservation().getRatesSummary()) {
            RatesSummary rates = response.getRoomReservation().getRatesSummary();
            rates.setPreviousDeposit(ReservationUtil.getAmountPaidAgainstDeposit(reservation));
            //change in deposit
            double changeInDeposit = rates.getDepositDue() - rates.getPreviousDeposit();
            rates.setChangeInDeposit(changeInDeposit);
            //refund
            if(null != response.getRoomReservation().getDepositDetails()) {
                if (rates.getDepositDue() < rates.getPreviousDeposit()) {
                    response.getRoomReservation().getDepositDetails().setRefundAmount(changeInDeposit * -1);
                }
            }
        }
    }
    private void handleNotification(RoomReservation reservation, ModifyRoomReservationResponse response,
                                    boolean isHDEPackageReservation,HttpServletRequest httpRequest,
                                    boolean skipCustomerNotification){
        //All notification related calls are moved here
        String channel = CommonUtil.getChannelHeaderWithFallback(httpRequest);
        boolean channelExcludedFromSendEmail = CommonUtil.isChannelExcludedForEmail(channel,
                appProperties.getExcludeEmailForChannels());
        // populating notifyCustomer, so it will be available for publishEvent
        boolean notifyCustomerViaRTC = reservationServiceHelper
                .isNotifyCustomerViaRTC(response.getRoomReservation().getPropertyId(), channelExcludedFromSendEmail, isHDEPackageReservation);
        if (skipCustomerNotification) {
            response.getRoomReservation().setNotifyCustomer(false);
        } else {
            response.getRoomReservation().setNotifyCustomer(notifyCustomerViaRTC);
        }
        response.getRoomReservation().setNotifyCustomer(notifyCustomerViaRTC);
        response.getRoomReservation().setRatesFormat(ServiceConstant.RTC_RATES_FORMAT);
        if (ItineraryServiceImpl.isItineraryServiceEnabled()) {
            itineraryService.createOrUpdateCustomerItinerary(response.getRoomReservation(), reservation.getShareWithReservations());
        }
        httpRequest.setAttribute("financialImpact", ReservationUtil.getReservationFinancialImpact(reservation));

        eventPublisherService.publishEvent(Collections.singletonList(response.getRoomReservation()),
                ReservationEventType.UPDATE.toString());

        if (notifyCustomerViaRTC) {
            log.info("Email will be sent by RTC");
        } else {
            if (channelExcludedFromSendEmail) {
                log.info("Email will not be sent for channel:{}", channel);
            } else {
                emailService.sendConfirmationEmail(reservation, response.getRoomReservation(), isHDEPackageReservation);
            }
        }
    }
    
    private void adjustChargeAmount(RoomReservation roomReservation) {

        if (null != roomReservation.getPayments()) {
            double amountPaid = ServiceConstant.ZERO_DOUBLE_VALUE;
            for (Payment payment : roomReservation.getPayments()) {
                if (ServiceConstant.PAYMENT_STATUS_SETTLED.equalsIgnoreCase(payment.getStatus())) {
                    amountPaid += payment.getChargeAmount();
                }
            }
            
            // Rounding up due to GSE behaviour
            if (CollectionUtils.isNotEmpty(roomReservation.getCreditCardCharges())) {
                double additionalCharge = 0.0;
                if (Math.abs(ReservationUtil.getDepositAmount(roomReservation) - amountPaid) > 0.01) {
                    additionalCharge = BigDecimal.valueOf(ReservationUtil.getDepositAmount(roomReservation)  - amountPaid)
                            .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }

                CreditCardCharge existingCard = roomReservation.getCreditCardCharges().get(0);
                existingCard.setAmount(additionalCharge);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.service.ModifyReservationService#
     * preModifyReservation(com.mgm.services.booking.room.model.request.
     * PreModifyV2Request)
     */
    @Override
    public ModifyRoomReservationResponse preModifyReservation(PreModifyV2Request preModifyV2Request, String token) {
        ModifyRoomReservationResponse response = preModifyReservationWithoutSplReqFilterOut(preModifyV2Request, token);
        //filterOutRoomFeatureNSpecialRequests from response
        filterOutRoomFeatureNSpecialRequests(response.getRoomReservation(),preModifyV2Request.getSource());
        return response;
    }
    public ModifyRoomReservationResponse preModifyReservationWithoutSplReqFilterOut(PreModifyV2Request preModifyV2Request, String token) {
        RoomReservation findRoomReservation = findReservationDao
                .findRoomReservation(reservationServiceHelper.createFindReservationV2Request(preModifyV2Request));

        if (null == findRoomReservation) {
            log.info("No reservation returned neither by aurora nor by ACRS for the given confirmation number {}",
                    preModifyV2Request.getConfirmationNumber());
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        handleComponentForF1(findRoomReservation,preModifyV2Request);
        //Fix to retrieve reservation from ORMS when opera confirmation is sent in request
        if(!StringUtils.equalsIgnoreCase(findRoomReservation.getConfirmationNumber(), preModifyV2Request.getConfirmationNumber())) {
            preModifyV2Request.setConfirmationNumber(findRoomReservation.getConfirmationNumber());
        }

        // Use existing dates on reservation if not supplied
        if (null == preModifyV2Request.getTripDetails()) {
            TripDetailsRequest tripDetails = new TripDetailsRequest();
            tripDetails.setCheckInDate(findRoomReservation.getCheckInDate());
            tripDetails.setCheckOutDate(findRoomReservation.getCheckOutDate());
            preModifyV2Request.setTripDetails(tripDetails);
        }

        if (!reservationServiceHelper.isRequestAllowedToModifyReservation(findRoomReservation, token)) {
            log.info("Reservation is not allowed to modify");
            throw new BusinessException(ErrorCode.RESERVATION_NOT_MODIFIABLE);
        }

        if (!reservationServiceHelper.validateLoggedUserOrServiceToken(token) && !reservationServiceHelper
                .isRequestAllowedToFindReservation(preModifyV2Request, findRoomReservation)) {
            log.info("Request is not allowed to find reservation");
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        // Perform date change eligibility checks
        reservationServiceHelper.isReservationDatesModifiable(findRoomReservation, preModifyV2Request);

        // This will be removed once isPropertyManagedByAcrs switch is removed
        String reservationPropertyId = preModifyV2Request.getPropertyId();
        if (StringUtils.isEmpty(reservationPropertyId)) {
            preModifyV2Request.setPropertyId(findRoomReservation.getPropertyId());
        }
        
        preModifyV2Request.setFindResvResponse(findRoomReservation);
        RoomReservation reservation = modifyReservationDAO.preModifyReservation(preModifyV2Request);

        Set<String> programIds = new HashSet<>();
        Set<String> reservationProgramIds = new HashSet<>();
        reservation.getBookings().forEach(booking -> programIds.add(booking.getProgramId()));
        findRoomReservation.getBookings().forEach(reservationBooking -> reservationProgramIds.add(reservationBooking.getProgramId()));

        // Since MRD launch, no longer allow modifications on multiple
        // program bookings
        // CBSR-589 Return Error If different program Ids are returned during pre-modify flow.
        if (programIds.size() > 1 || (!programIds.containsAll(reservationProgramIds))) {
            throw new BusinessException(ErrorCode.DATES_UNAVAILABLE);
        }

        RoomReservationV2Response roomReservationResponse = responseMapper.roomReservationModelToResponse(reservation);
        if (null != roomReservationResponse.getRatesSummary()) {
            RatesSummary rates = roomReservationResponse.getRatesSummary();
            rates.setPreviousDeposit(ReservationUtil.getAmountPaidAgainstDeposit(findRoomReservation));
        }
        // Add special request and room feature form existing reservation
        updateRoomRequest(roomReservationResponse, findRoomReservation);
        //Set nonEditable and isPkgComponent flag true for pkgComponents
        List<PurchasedComponent> updatedPurchasedComponents = reservationServiceHelper.updatePackageComponentsFlag(
                roomReservationResponse.getPropertyId(),
                roomReservationResponse.getPurchasedComponents()
        );
        roomReservationResponse.setPurchasedComponents(updatedPurchasedComponents);
        ModifyRoomReservationResponse response = new ModifyRoomReservationResponse();
        response.setRoomReservation(roomReservationResponse);
        // populating the customerId, as it is missing for modify preview API
        if (null != roomReservationResponse.getProfile()) {
            response.getRoomReservation().setCustomerId(roomReservationResponse.getProfile().getId());
        }

        if (findRoomReservation.isF1Package()) {

            addF1CasinoDefaultComponentPrices(response, findRoomReservation.getRatePlanTags(),
                    preModifyV2Request.getMlifeNumber(), preModifyV2Request.getSource());
            ReservationUtil.purchasedComponentsF1Updates(response.getRoomReservation(), appProperties,
                    findRoomReservation.getRatePlanTags(), false);
        }
        
        
        return ReservationUtil.setWebSuppresableComponentFlag(response,acrsProperties);
    }

	private void handleComponentForF1(RoomReservation findRoomReservation, PreModifyV2Request preModifyV2Request){
        boolean tcolvF1ComponentFindResv = false;
        if (findRoomReservation.isF1Package()) {
            if (CollectionUtils.isNotEmpty(findRoomReservation.getSpecialRequests())) {
                for (String specialRequestId : findRoomReservation.getSpecialRequests()) {
                    RoomComponent roomComponent = new RoomComponent();
                    if (!ACRSConversionUtil.isAcrsComponentCodeGuid(specialRequestId)) {
                        roomComponent = componentDAO.getRoomComponentById(specialRequestId, findRoomReservation.getPropertyId());
                    } else {
                        roomComponent.setName(ACRSConversionUtil.getComponentNRPlanCode(specialRequestId));
                    }
                    if (StringUtils.isNotEmpty(findRoomReservation.getPropertyId())
                            && StringUtils.equalsIgnoreCase(appProperties.getTcolvPropertyId(), findRoomReservation.getPropertyId())) {
                        String componentCode = ReservationUtil.getTCOLVF1TicketComponentCode(new ArrayList<>(findRoomReservation.getRatePlanTags()));
                        if (StringUtils.isNotEmpty(componentCode) && null != roomComponent && null != roomComponent.getName()
                                && roomComponent.getName().equalsIgnoreCase(componentCode)) {
                            tcolvF1ComponentFindResv = true;
                            if (CollectionUtils.isEmpty(preModifyV2Request.getRoomRequests())) {
                                throw new BusinessException(ErrorCode.F1_NON_EDITABLE_COMPONENTS_NOT_AVAILABLE);
                            } else {
                                if (!preModifyV2Request.getRoomRequests().contains(specialRequestId)) {
                                    throw new BusinessException(ErrorCode.F1_NON_EDITABLE_COMPONENTS_NOT_AVAILABLE);
                                }
                            }
                        }
                    } else {
                        if (null != roomComponent && null != roomComponent.getName() &&
                                roomComponent.getName().startsWith(ServiceConstant.F1_COMPONENT_START_F1)) {
                            if (CollectionUtils.isEmpty(preModifyV2Request.getRoomRequests())) {
                                throw new BusinessException(ErrorCode.F1_NON_EDITABLE_COMPONENTS_NOT_AVAILABLE);
                            } else {
                                if (!preModifyV2Request.getRoomRequests().contains(specialRequestId)) {
                                    throw new BusinessException(ErrorCode.F1_NON_EDITABLE_COMPONENTS_NOT_AVAILABLE);
                                }
                            }
                        }
                    }
                }
            }
            String componentCode = ReservationUtil.getF1DefaultCasinoComponentCode(findRoomReservation.getRatePlanTags());
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(componentCode) && !componentCode.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG)) {
                RoomComponent roomComponent = componentDAO.getRoomComponentByCode(findRoomReservation.getPropertyId(),
                        componentCode, findRoomReservation.getRoomTypeId(), findRoomReservation.getProgramId(),
                        findRoomReservation.getCheckInDate(), findRoomReservation.getCheckOutDate(),
                        preModifyV2Request.getMlifeNumber(), preModifyV2Request.getSource());
                if (null != roomComponent) {
                    if (CollectionUtils.isNotEmpty(preModifyV2Request.getRoomRequests())) {
                        preModifyV2Request.getRoomRequests().add(roomComponent.getId());
                    } else {
                        List<String> roomComponentId = new ArrayList<>();
                        roomComponentId.add(roomComponent.getId());
                        preModifyV2Request.setRoomRequests(roomComponentId);
                    }
                }
            }
            if (StringUtils.isNotEmpty(findRoomReservation.getPropertyId())
                    && StringUtils.equalsIgnoreCase(appProperties.getTcolvPropertyId(), findRoomReservation.getPropertyId())
                    && !tcolvF1ComponentFindResv) {
                throw new BusinessException(ErrorCode.F1_NON_EDITABLE_COMPONENTS_NOT_AVAILABLE);
            }
        }
    }

    private void filterOutRoomFeatureNSpecialRequests( RoomReservationV2Response roomReservationV2Response, String source) {
        if(!ServiceConstant.ICE.equalsIgnoreCase(source)){
            if(CollectionUtils.isNotEmpty(roomReservationV2Response.getPurchasedComponents())) {
                List<PurchasedComponent> existingPurchasedAddons = null;
                List<String> existingAddonsIds = null;
                if(referenceDataDAOHelper.isPropertyManagedByAcrs(roomReservationV2Response.getPropertyId())){
                    //ACRS Addons component will be having ratePlanCode. Room feature and special will not be having ratePlanCode. Based on ratePlanCode we can filter
                    existingPurchasedAddons = roomReservationV2Response.getPurchasedComponents().stream().filter(p -> org.apache.commons.lang3.StringUtils.isNotBlank(p.getRatePlanCode())).collect(Collectors.toList());

                }else{
                    //for GSE
                    existingPurchasedAddons =  roomReservationV2Response.getPurchasedComponents().stream().filter( p->{
                        RoomComponent roomRequest = componentDAO.getRoomComponentById(p.getId(), roomReservationV2Response.getPropertyId());
                        if(null != roomRequest){
                            return ServiceConstant.COMPONENT_STR.equalsIgnoreCase(roomRequest.getComponentType());
                        }else{
                            return false;
                        }
                    }).collect(Collectors.toList());
                }
                existingAddonsIds = existingPurchasedAddons.stream().map(PurchasedComponent::getId).collect(Collectors.toList());
                //Update the response with only Addons
                roomReservationV2Response.setSpecialRequests(existingAddonsIds);
                roomReservationV2Response.setPurchasedComponents(existingPurchasedAddons);
            }
        }
    }

    private void updateRoomRequest(RoomReservationV2Response roomReservationV2Response, RoomReservation existingReservation) {
            if(CollectionUtils.isNotEmpty(existingReservation.getPurchasedComponents())) {
                List<PurchasedComponent> existingSplReqNRoomFeatures;
                List<String> existingSplReqNRoomFeatureIds = null;
                if(referenceDataDAOHelper.isPropertyManagedByAcrs(existingReservation.getPropertyId())){
                    //ACRS Addons component will be having ratePlanCode. Room feature and special will not be having ratePlanCode. Based on ratePlanCode we can filter
                    existingSplReqNRoomFeatures = existingReservation.getPurchasedComponents().stream().filter(p -> org.apache.commons.lang3.StringUtils.isBlank(p.getRatePlanCode())).collect(Collectors.toList());
                    existingSplReqNRoomFeatureIds = existingSplReqNRoomFeatures.stream().map(PurchasedComponent::getId).collect(Collectors.toList());
                }else{
                    //for GSE
                    existingSplReqNRoomFeatures =  existingReservation.getPurchasedComponents().stream().filter( p->{
                        RoomComponent roomRequest = componentDAO.getRoomComponentById(p.getId(), roomReservationV2Response.getPropertyId());
                        if(null != roomRequest){
                            return ServiceConstant.COMPONENT_STR.equalsIgnoreCase(roomRequest.getComponentType());
                        }else{
                            return false;
                        }
                    }).collect(Collectors.toList());
                }
                //Update the response with only Addons
                if(CollectionUtils.isNotEmpty(existingSplReqNRoomFeatureIds)){
                    if(CollectionUtils.isNotEmpty(roomReservationV2Response.getSpecialRequests())){
                        roomReservationV2Response.getSpecialRequests().addAll(existingSplReqNRoomFeatureIds);
                    }else{
                        roomReservationV2Response.setSpecialRequests(existingSplReqNRoomFeatureIds);
                    }
                }
                if(CollectionUtils.isNotEmpty(existingSplReqNRoomFeatures)){
                    if(CollectionUtils.isNotEmpty(roomReservationV2Response.getPurchasedComponents())){
                        roomReservationV2Response.getPurchasedComponents().addAll(existingSplReqNRoomFeatures);
                    }else{
                        roomReservationV2Response.setPurchasedComponents(existingSplReqNRoomFeatures);
                    }
                }
            }
    }

    @Override
    public ModifyRoomReservationResponse commitReservation(PreviewCommitRequest commitRequest, String token) {

        PreModifyV2Request previewRequest = previewMapper.commitToPreviewRequest(commitRequest);

        ModifyRoomReservationResponse resvResponse = preModifyReservationWithoutSplReqFilterOut(previewRequest, token);

        RatesSummary rateSummary = resvResponse.getRoomReservation().getRatesSummary();

        if (!commitRequest.isSkipPaymentProcess()){
            // When there's change in deposit, cvv is mandatory
            if (Double.compare(BaseCommonUtil.round(rateSummary.getPreviousDeposit(), ServiceConstant.TWO_DECIMAL_PLACES),
                    commitRequest.getPreviewReservationDeposit()) < 0 && StringUtils.isEmpty(commitRequest.getCvv())) {
                log.warn("Change in deposit, but cvv is not sent");
                throw new BusinessException(ErrorCode.MODIFY_VIOLATION_NO_CVV);
            }
        }

        // If price change is detected, return an error with updated room
        // reservation object
        if (Double.compare(BaseCommonUtil.round(rateSummary.getReservationTotal(), ServiceConstant.TWO_DECIMAL_PLACES),
                commitRequest.getPreviewReservationTotal()) != 0
                || Double.compare(BaseCommonUtil.round(rateSummary.getDepositDue(), ServiceConstant.TWO_DECIMAL_PLACES),
                        commitRequest.getPreviewReservationDeposit()) != 0) {
            log.warn("Price change is detected");
            resvResponse.setError(getPriceChangeError());
            return resvResponse;
        }
        
        // proceed to commit
        RoomReservationRequest resvRequest = requestMapper.roomReservationResponseToRequest(resvResponse.getRoomReservation());
        resvRequest.setSkipCustomerNotification(commitRequest.isSkipCustomerNotification());
        resvRequest.setSource(commitRequest.getSource());
        resvRequest.setMlifeNumber(commitRequest.getMlifeNumber());
        // CBSR-2054- Add inAuthTransactionId in Commit api request
        resvRequest.setInAuthTransactionId(commitRequest.getInAuthTransactionId());
        ModifyRoomReservationRequest modifyRequest = new ModifyRoomReservationRequest();
        modifyRequest.setRoomReservation(resvRequest);
        //CBSR-2129 - for payment widget integration
        if(commitRequest.isSkipPaymentProcess()){
            resvRequest.setSkipPaymentProcess(true);
            resvRequest.setSkipFraudCheck(true);
        }
        if(StringUtils.isNotBlank(commitRequest.getAuthId()) && CollectionUtils.isNotEmpty(resvRequest.getBilling())){
             resvRequest.getBilling().get(0).getPayment().setAuthId(commitRequest.getAuthId());
        }
        return modifyRoomReservationV2(modifyRequest);

    }

    /**
     * This API + v4 commit will be the replacement of v2/reservation/Commit for payment widget flow
     * Here we will be doing only modify pending.
     * @param commitRequest
     * @param token
     * @return
     */
    @Override
    public ModifyRoomReservationResponse reservationModifyPendingV5(PreviewCommitRequest commitRequest, String token) {

        PreModifyV2Request previewRequest = previewMapper.commitToPreviewRequest(commitRequest);

        ModifyRoomReservationResponse resvResponse = preModifyReservationWithoutSplReqFilterOut(previewRequest, token);
        RatesSummary rateSummary = resvResponse.getRoomReservation().getRatesSummary();
        // If price change is detected, return an error with updated room
        // reservation object
        if (Double.compare(BaseCommonUtil.round(rateSummary.getReservationTotal(), ServiceConstant.TWO_DECIMAL_PLACES),
                commitRequest.getPreviewReservationTotal()) != 0
                || Double.compare(BaseCommonUtil.round(rateSummary.getDepositDue(), ServiceConstant.TWO_DECIMAL_PLACES),
                commitRequest.getPreviewReservationDeposit()) != 0) {
            log.warn("Price change is detected");
            resvResponse.setError(getPriceChangeError());
            return resvResponse;
        }


        RoomReservationRequest resvRequest = requestMapper.roomReservationResponseToRequest(resvResponse.getRoomReservation());
        resvRequest.setSource(commitRequest.getSource());
        resvRequest.setMlifeNumber(commitRequest.getMlifeNumber());
        // CBSR-2054- Add inAuthTransactionId in Commit api request
        resvRequest.setInAuthTransactionId(commitRequest.getInAuthTransactionId());
        ModifyRoomReservationRequest modifyRequest = new ModifyRoomReservationRequest();
        modifyRequest.setRoomReservation(resvRequest);
        return reservationModifyPendingV4(modifyRequest);

    }

    private ErrorVo getPriceChangeError() {
        
        ErrorVo vo = new ErrorVo();
        vo.setCode(String.format("%s-%s-%s", ServiceConstant.ROOM_BOOKING_SERVICE_DOMAIN_CODE,
                ErrorTypes.FUNCTIONAL_ERROR.errorTypeCode(), ErrorCode.MODIFY_VIOLATION_PRICE_CHANGE.getNumericCode()));
        vo.setMessage(ErrorCode.MODIFY_VIOLATION_PRICE_CHANGE.getDescription());
        
        return vo;
    }

    @Override
    public UpdateProfileInfoResponse associateReservation(ReservationAssociateRequest request, String token) {
        RoomReservation originalReservation = findResvService
                .findRoomReservation(reservationServiceHelper.createFindReservationV2Request(request), false);

        Map<String, String> tokenClaims = reservationServiceHelper.getClaims(token);

        int reservationMlifeNo = originalReservation.getProfile().getMlifeNo();
        String mlifeNo = tokenClaims.get(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM);
        int requestMlifeNo = Integer.parseInt(mlifeNo != null ? mlifeNo : "-1");
        boolean isTcolvReservation = StringUtils.equalsIgnoreCase(appProperties.getTcolvPropertyId(), originalReservation.getPropertyId());
        OcrsReservation ocrsUpdatedReservation = null;

        if (reservationMlifeNo > 0) {
            // Scenario 1: reservation contain mlife number but association request does not
            // Scenario 2: reservation and association request contains different mlife number
            if (reservationMlifeNo != requestMlifeNo) {
                log.info("mlife claim of the JWT token is not matching with reservation profile's");
                throw new BusinessException(ErrorCode.ASSOCIATION_VIOLATION_MLIFE_MISMATCH);
            } else {
                // Scenario : reservation and association request contains same mlife number
                log.info("mlife claim of the JWT token is matching with reservation profile's");
                throw new BusinessException(ErrorCode.ASSOCIATION_VIOLATION);
            }
        }
        
        if (!idUtilityService.isFirstNameLastNameMatching(originalReservation.getProfile(),
                tokenClaims.get(ServiceConstant.IDMS_TOKEN_GIVEN_NAME_CLAIM),
                tokenClaims.get(ServiceConstant.IDMS_TOKEN_FAMILY_NAME_CLAIM))) {
            log.info("firstName and lastName of the JWT token are not matching (fuzzy match) with reservation's");
            throw new BusinessException(ErrorCode.ASSOCIATION_VIOLATION_NAME_MISMATCH);
        }
        
        if (StringUtils.isBlank(originalReservation.getOperaConfirmationNumber())) {
            log.info("Reservation is not posted to Opera yet, so avoiding profile modifications");
            throw new BusinessException(ErrorCode.ASSOCIATION_VIOLATION);
        }
        
        // CBSR-1565 Skip OCRS update call for TCOLV unless it is a third party
        // OCRS gets this information from LMS through source system update
        if(!isTcolvReservation || (isTcolvReservation && originalReservation.isThirdParty())) {
        	// OCRS: Update reservation profile
        	UpdateProfileRequest updateProfileRequest = new UpdateProfileRequest();
        	updateProfileRequest.setHotelCode(appProperties.getHotelCode(originalReservation.getPropertyId()));
        	updateProfileRequest.setOperaConfirmationNumber(originalReservation.getOperaConfirmationNumber());
        	updateProfileRequest.setFirstName(tokenClaims.get(ServiceConstant.IDMS_TOKEN_GIVEN_NAME_CLAIM));
        	updateProfileRequest.setLastName(tokenClaims.get(ServiceConstant.IDMS_TOKEN_FAMILY_NAME_CLAIM));
        	updateProfileRequest.setMgmId(tokenClaims.get(ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM));
            // CBSR-2063- if propertyIs in ACRS not send Mlife to OCRS.
            if(!referenceDataDAOHelper.isPropertyManagedByAcrs(originalReservation.getPropertyId())) {
                updateProfileRequest.setMlifeNumber(tokenClaims.get(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM));
            }
        	ocrsUpdatedReservation = ocrsDao.updateProfile(updateProfileRequest);
        	if (null != ocrsUpdatedReservation && null != ocrsUpdatedReservation.getMgmProfile()) {
        		log.debug("Completed updating the reservation in OCRS with mlife: {} and mgmId: {}",
        				ocrsUpdatedReservation.getMgmProfile().getMlifeNumber(),
        				ocrsUpdatedReservation.getMgmProfile().getMgmId());
        	}
        }
        
        if (originalReservation.isThirdParty()) {
            
            if (null == ocrsUpdatedReservation) {
                // Ideally, this will never occur
                throw new BusinessException(ErrorCode.ASSOCIATION_VIOLATION);
            }
            RoomReservation updatedReservation = new RoomReservation();
            String propertyId = appProperties
                    .getPropertyIdFromHotelCode(ocrsUpdatedReservation.getHotelReference().getHotelCode());
            updatedReservation.setPropertyId(propertyId);
            findReservationServiceHelper.updateRoomReservation(ocrsUpdatedReservation, updatedReservation, request.getConfirmationNumber());
            
            UpdateProfileInfoResponse response = new UpdateProfileInfoResponse();
            RoomReservationV2Response roomReservationFromOcrs = responseMapper.roomReservationModelToResponse(updatedReservation);
            //CBSR-1709 - Invoke content API to resolve property id and room type id because incorrect details are being populated 
            //from RBS cache
            com.mgm.services.booking.room.model.content.Room roomDetails = roomContentDao.
            		getRoomContent(ocrsUpdatedReservation.getRoomStays().getRoomStay().get(0).getRoomInventoryCode(), 
            				ocrsUpdatedReservation.getHotelReference().getHotelCode(), true);
            if(null != roomDetails) {
            	roomReservationFromOcrs.setPropertyId(roomDetails.getPropertyId());
            	roomReservationFromOcrs.setRoomTypeId(roomDetails.getId());
            }
            roomReservationFromOcrs.setOperaHotelCode(ocrsUpdatedReservation.getHotelReference().getHotelCode());
            roomReservationFromOcrs.setOperaRoomCode(ocrsUpdatedReservation.getRoomStays().getRoomStay().get(0).getRoomInventoryCode());
            response.setRoomReservation(roomReservationFromOcrs);
            return response;
            
        } else {
            // GSE/ACRS: Update reservation profile, if its not a third party reservation
            UpdateProfileInfoRequest updateProfileInfoRequest = userProfileInfoRequestMapper
                    .roomReservationModelToUpdateProfileInfoRequest(originalReservation);
            updateProfileInfoRequest.setSource(request.getSource());
            updateProfileInfoRequest.setMoveItinerary(true);
            updateProfileInfoRequest.getUserProfile().setMlifeNo(requestMlifeNo);
            updateProfileInfoRequest.setOriginalReservation(originalReservation);

            UpdateProfileInfoRequestDTO updateProfileInfoRequestDTO = RoomReservationTransformer.createModifyProfileInfoRequestDTO(updateProfileInfoRequest);
            updateProfileInfoRequestDTO.setAssociateFlow(true);

            RoomReservation updatedReservation = modifyReservationDAO.updateProfileInfo(updateProfileInfoRequestDTO);

            UpdateProfileInfoResponse response = new UpdateProfileInfoResponse();
            response.setRoomReservation(responseMapper.roomReservationModelToResponse(updatedReservation));
            return response;
        }
        
    }

    @Override
    public ModifyRoomReservationResponse commitPaymentReservation(PaymentRoomReservationRequest request) {
        CommitPaymentDTO commitPaymentDTO = requestMapper.paymentRoomReservationRequestToCommitPaymentDTO(request);
        RoomReservation reservation = modifyReservationDAO.commitPaymentReservation(commitPaymentDTO);
        //Handle F1 use cases
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        // F1 package check validation
        RoomProgramValidateResponse validateResponse = isF1Package(reservation);
        boolean isHDEPackageReservation =reservationServiceHelper.isHDEPackageReservation(reservation);
        ModifyRoomReservationResponse response = updateResponse(reservation, isHDEPackageReservation,validateResponse, request.getSource());
        // set pkg components flags to true
        List<PurchasedComponent> updatedPurchasedComponents = reservationServiceHelper.updatePackageComponentsFlag(
                response.getRoomReservation().getPropertyId(),
                response.getRoomReservation().getPurchasedComponents()
        );
        response.getRoomReservation().setPurchasedComponents(updatedPurchasedComponents);
        handleNotification(reservation, response, isHDEPackageReservation, httpRequest, request.isSkipCustomerNotification());
        return response;
    }

    private void addF1CasinoDefaultComponentPrices(ModifyRoomReservationResponse response,
                                                   List<String> ratePlanTags,
                                                   String mlifeNumber, String source) {
        String componentCode = ReservationUtil.getF1DefaultCasinoComponentCode(ratePlanTags);
        if (StringUtils.isNotEmpty(componentCode) && !componentCode.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG)) {
            RoomReservationV2Response roomReservationV2Response = response.getRoomReservation();
            String propertyId = roomReservationV2Response.getPropertyId();
            Date checkInDate = roomReservationV2Response.getTripDetails().getCheckInDate();
            Date checkOutDate = roomReservationV2Response.getTripDetails().getCheckOutDate();
            RoomComponent roomComponent = componentDAO.getRoomComponentByCode(propertyId, componentCode,
                    roomReservationV2Response.getRoomTypeId(), roomReservationV2Response.getProgramId(),
                    checkInDate, checkOutDate,
                    mlifeNumber, source);
            if (null != roomComponent && StringUtils.isNotEmpty(roomComponent.getId())) {
                Float updatedPrice = ReservationUtil.getRoomComponentPrice(roomComponent,
                        checkInDate, checkOutDate);
                roomReservationV2Response.getBookings().forEach(x -> {
                    if (x.isComp()) {
                        x.setCustomerPrice(x.getPrice());
                        x.setPrice(0.0);
                    }
                    BigDecimal bd = new BigDecimal(x.getPrice() + updatedPrice).setScale(2, RoundingMode.HALF_UP);
                    x.setPrice(bd.doubleValue());
                    BigDecimal bd1 = new BigDecimal(x.getBasePrice() + updatedPrice).setScale(2, RoundingMode.HALF_UP);
                    x.setBasePrice(bd1.doubleValue());
                    x.setComp(false);
                    x.setDiscounted(x.getPrice() < x.getBasePrice());
                });
                // remove the default F1 component
                roomReservationV2Response.getPurchasedComponents().removeIf(x -> x.getId().equalsIgnoreCase(roomComponent.getId()));
                //remove from special request
                roomReservationV2Response.getSpecialRequests().removeIf(x -> x.equalsIgnoreCase(roomComponent.getId()));
                ReservationUtil.updateRatesSummary(roomReservationV2Response, roomComponent, updatedPrice);
            }
        }
    }

    private RoomProgramValidateResponse isF1Package (RoomReservation roomReservation) {
        RoomProgramValidateResponse validateResponse = new RoomProgramValidateResponse();
        if (null != roomReservation) {
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(roomReservation.getProgramId())) {
                validateResponse = isF1Package(roomReservation, null);
            } else {
                if (CollectionUtils.isNotEmpty(roomReservation.getBookings())) {
                    for (RoomPrice booking : roomReservation.getBookings()) {
                        validateResponse = isF1Package(roomReservation, booking.getProgramId());
                        if (validateResponse.isF1Package()) {
                            break;
                        }
                    }
                }
            }
        }
        return validateResponse;
    }

    private RoomProgramValidateResponse isF1Package (RoomReservation roomReservation, String programId) {
        RoomProgramValidateRequest validateRequest = RoomProgramValidateRequestTransformer
                .getRoomProgramValidateRequest(roomReservation, programId);
        serviceConversionHelper.convertGuids(validateRequest);
        RoomProgramValidateResponse validateResponse = roomProgramDao.validateProgramV2(validateRequest);
        if (CollectionUtils.isNotEmpty(validateResponse.getRatePlanTags()) && validateResponse.getRatePlanTags().contains(appProperties.getF1PackageTag())) {
            List<String> validF1ProductCodes = appProperties.getValidF1ProductCodes();
            Optional<String> productCode = validF1ProductCodes.stream().filter(x -> validateResponse.getRatePlanTags().contains(x)).findFirst();
            if (productCode.isPresent()) {
                validateResponse.setF1Package(true);
                reservationServiceHelper.addF1CasinoDefaultComponentPrices(roomReservation, null ,
                        validateResponse.getRatePlanTags(), roomReservation.getSource());
            }
        }
        return validateResponse;
    }
}
