package com.mgm.services.booking.room.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ProductInventoryDAO;
import com.mgm.services.booking.room.dao.RoomProgramDAO;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.inventory.BookedItemList;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.transformer.RoomProgramValidateRequestTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.dao.ReservationDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.event.ReservationEventType;
import com.mgm.services.booking.room.exception.AuroraError;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.model.reservation.PartyRoomReservation;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.EventPublisherService;
import com.mgm.services.booking.room.service.ItineraryService;
import com.mgm.services.booking.room.service.ReservationEmailV2Service;
import com.mgm.services.booking.room.service.ReservationService;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.util.ServiceConversionHelper;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgmresorts.aurora.service.EAuroraException;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for ReservationService
 */
@Component
@Primary
@Log4j2
public class ReservationServiceImpl implements ReservationService {

	@Autowired
	private ReservationDAO reservationDao;

	@Autowired
	private RoomReservationRequestMapper requestMapper;

	@Autowired
	private RoomReservationResponseMapper responseMapper;

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
    private ServiceConversionHelper serviceConversionHelper;

	@Autowired
	private AcrsProperties acrsProperties;
	
	@Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;

    @Autowired
    private RoomProgramDAO roomProgramDao;

    @Autowired
    private ProductInventoryDAO productInventoryDAO;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mgm.services.booking.room.service.ReservationService#
	 * makeRoomReservation(com.mgm.services.booking.room.model.reservation.
	 * RoomReservation)
	 */
	@Override
	public RoomReservation makeRoomReservation(RoomReservation reservation) {
		return reservationDao.makeRoomReservation(reservation);
	}

    @Override
    public CreateRoomReservationResponse makeRoomReservationV2(CreateRoomReservationRequest createReservationRequest,
            String skipMyVegasConfirm) {

	    RoomReservationRequest reservationRequest = createReservationRequest.getRoomReservation();
		serviceConversionHelper.convertGuids(reservationRequest);

		RoomReservation roomReservation = requestMapper.roomReservationRequestToModel(reservationRequest);

        // F1 package check
        RoomProgramValidateRequest validateRequest = RoomProgramValidateRequestTransformer
                .getRoomProgramValidateRequest(roomReservation, null);
        serviceConversionHelper.convertGuids(validateRequest);
        RoomProgramValidateResponse validateResponse = roomProgramDao.validateProgramV2(validateRequest);
        boolean f1Package = false;
        //Check for F1 packages
        if (CollectionUtils.isNotEmpty(validateResponse.getRatePlanTags()) && validateResponse.getRatePlanTags().contains(appProperties.getF1PackageTag())) {
            Optional<String> productCode = appProperties.getValidF1ProductCodes().stream().
                    filter(x-> validateResponse.getRatePlanTags().contains(x)).findFirst();
            if (productCode.isPresent()) {
                f1Package = true;
                if (StringUtils.isNotEmpty(createReservationRequest.getRoomReservation().getHoldId())) {
                    BookedItemList bookedItemList = productInventoryDAO.
                            getInventoryStatus(null, createReservationRequest.getRoomReservation().getHoldId());
                    reservationServiceHelper.validateF1InventoryStatus(bookedItemList);
                }
                reservationServiceHelper.addF1CasinoDefaultComponentPrices(roomReservation, null ,
                        validateResponse.getRatePlanTags(), roomReservation.getSource());
                if (!validateResponse.getRatePlanTags().contains(ServiceConstant.F1_COMP_TAG)) {
                    reservationServiceHelper.validateTicketCount(roomReservation, validateResponse.getRatePlanTags());
                }
            }
        }

        MyVegasResponse redemptionResponse = null;

        // Call to myVegas validate service only if redemption code present in
        // the request
        if (StringUtils.isNotBlank(reservationRequest.getMyVegasPromoCode())) {
            redemptionResponse = reservationServiceHelper.validateRedemptionCode(
                    reservationRequest.getMyVegasPromoCode(), roomReservation.getCheckInDate(),
                    roomReservation.getPropertyId());
            
            log.info("MyVegas code : {}, validated successfully", reservationRequest.getMyVegasPromoCode());
            reservationServiceHelper.updateProgramId(roomReservation, redemptionResponse);
        }		

        // removing char not supported by Opera
        ReservationUtil.sanitizedComments(roomReservation);
        RoomReservation reservation = reservationDao.makeRoomReservationV2(roomReservation);
        if (reservation.getBookings() != null) {
            reservation.getBookings().forEach(price -> price.setDiscounted(price.getPrice() < price.getBasePrice()));
        }
        // Partner Accounts - Project Max Rewards - CBSR-1634
        if(!ObjectUtils.isEmpty(reservationRequest.getProfile()) && CollectionUtils.isNotEmpty(reservationRequest.getProfile().getPartnerAccounts())){
            reservation.getProfile().setPartnerAccounts(reservationRequest.getProfile().getPartnerAccounts());
        }
        CreateRoomReservationResponse response = new CreateRoomReservationResponse();
        response.setRoomReservation(responseMapper.roomReservationModelToResponse(reservation));
        // set pkg flags to true
        List<PurchasedComponent> updatedPurchasedComponents = reservationServiceHelper.updatePackageComponentsFlag(
                response.getRoomReservation().getPropertyId(),
                response.getRoomReservation().getPurchasedComponents()
        );
        response.getRoomReservation().setPurchasedComponents(updatedPurchasedComponents);
        if (ItineraryServiceImpl.isItineraryServiceEnabled()) {
            itineraryService.createOrUpdateCustomerItinerary(response.getRoomReservation(), reservation.getShareWithReservations());
        }
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        httpRequest.setAttribute("financialImpact", ReservationUtil.getReservationFinancialImpact(reservation));
        String channel = CommonUtil.getChannelHeaderWithFallback(httpRequest);
        boolean isHDEPackageReservation =reservationServiceHelper.isHDEPackageReservation(reservation);
        boolean channelExcludedFromSendEmail = CommonUtil.isChannelExcludedForEmail(channel,
                appProperties.getExcludeEmailForChannels());
        // populating notifyCustomer, so it will be available for publishEvent
        boolean notifyCustomerViaRTC = reservationServiceHelper
                .isNotifyCustomerViaRTC(response.getRoomReservation().getPropertyId(), channelExcludedFromSendEmail, isHDEPackageReservation);
        boolean isDepositForfeit = isHDEPackageReservation || reservationServiceHelper.isDepositForfeit(reservation);
        if (reservationRequest.isSkipCustomerNotification()) {
            response.getRoomReservation().setNotifyCustomer(false);
        } else {
            response.getRoomReservation().setNotifyCustomer(notifyCustomerViaRTC);
        }
        response.getRoomReservation().setRatesFormat(ServiceConstant.RTC_RATES_FORMAT);
        response.getRoomReservation().setHdePackage(isHDEPackageReservation);
        response.getRoomReservation().setDepositForfeit(isDepositForfeit);

        //Invoke Inventory service for F1 packages
        if (f1Package) {
            int ticketCount = ReservationUtil.getTicketCount(validateResponse, response, appProperties);
            int retryCount = 0;
            while (retryCount <= appProperties.getCommitInventoryMaxCount()) {
                String commitInventoryResponse = productInventoryDAO.commitInventory(ReservationUtil.createCommitInventoryRequest(
                        createReservationRequest, response.getRoomReservation().getConfirmationNumber(), ticketCount));
                if (StringUtils.isNotEmpty(commitInventoryResponse)) {
                    break;
                } else {
                    retryCount++;
                }
            }
            if (retryCount > appProperties.getCommitInventoryMaxCount()) {
            	log.error("Failed Commit Inventory Request: {}", CommonUtil.convertObjectToJsonString(ReservationUtil.createCommitInventoryRequest(createReservationRequest, response.getRoomReservation().getConfirmationNumber(), ticketCount)));
                log.error("Commit Inventory call failed even after successful reservation for confirmation number : {}", response.getRoomReservation().getConfirmationNumber());
            }
            response.getRoomReservation().setF1Package(true);
            if (null != response.getRoomReservation() && null != response.getRoomReservation().getPropertyId() && org.apache.commons.lang3.StringUtils.equalsIgnoreCase(appProperties.getTcolvPropertyId(), response.getRoomReservation().getPropertyId())) {
                String componentCode = ReservationUtil.getTCOLVF1TicketComponentCode(new ArrayList<>(validateResponse.getRatePlanTags()));
                if (org.apache.commons.lang3.StringUtils.isNotEmpty(componentCode)) {
                    for (PurchasedComponent purchasedComponent : response.getRoomReservation().getPurchasedComponents()) {
                        if (null != purchasedComponent.getCode() && purchasedComponent.getCode().equalsIgnoreCase(componentCode)) {
                            String f1ComponentCode = ReservationUtil.getF1DefaultPublicTicketComponentCode(validateResponse.getRatePlanTags());
                            purchasedComponent.setCode(f1ComponentCode);
                        }
                    }
                }
            }
            reservationServiceHelper.addF1CasinoDefaultComponentPrices(null, response.getRoomReservation(),
                    validateResponse.getRatePlanTags(), roomReservation.getSource());
        }

        eventPublisherService.publishEvent(Collections.singletonList(response.getRoomReservation()),
                ReservationEventType.CREATE.toString());

        if (null != reservation && null != redemptionResponse && !BooleanUtils.toBoolean(skipMyVegasConfirm)) {

            log.info("Calling MyVegas redemption for code: {}", redemptionResponse.getRedemptionCode());
            reservationServiceHelper.confirmRedemptionCode(reservationRequest, redemptionResponse,
                    reservation.getConfirmationNumber(), reservation.getCheckInDate(),
                    response.getRoomReservation().getPropertyId());
        }

        if (notifyCustomerViaRTC) {
            log.info("Email will be sent by RTC");
        } else {
            if (channelExcludedFromSendEmail) {
                log.info("Email will not be sent for channel:{}", channel);
            } else {
                emailService.sendConfirmationEmail(reservation, response.getRoomReservation(),isHDEPackageReservation);
            }
        }

        return response;
	}

    @Override
    public CreatePartyRoomReservationResponse makePartyRoomReservation(
            CreatePartyRoomReservationRequest createPartyRoomReservationRequest, String skipMyVegasConfirm) {
		RoomReservationRequest reservationRequest = createPartyRoomReservationRequest.getRoomReservation();
		RoomReservation roomReservation = requestMapper.roomReservationRequestToModel(reservationRequest);
        // Call to myVegas validate service only if redemption code present in
        // the request
        MyVegasResponse redemptionResponse = validateAndUpdateMyVegasProgram(reservationRequest,roomReservation);
        // removing char not supported by Opera
        ReservationUtil.sanitizedComments(roomReservation);
        PartyRoomReservation reservation = reservationDao.makePartyRoomReservation(roomReservation,
                createPartyRoomReservationRequest.isSplitCreditCardDetails());
        CreatePartyRoomReservationResponse response = responseMapper.partyRoomReservationsModelToResponse(reservation);
        invokePostPartyBookingOperations(response,reservation,redemptionResponse,reservationRequest,skipMyVegasConfirm);
        return response;
	}

    @Override
    public CreatePartyRoomReservationResponse makePartyRoomReservationV4(CreatePartyRoomReservationRequest createPartyRoomReservationRequest, String skipMyVegasConfirm) {
        RoomReservationRequest reservationRequest = createPartyRoomReservationRequest.getRoomReservation();
        RoomReservation roomReservation = requestMapper.roomReservationRequestToModel(reservationRequest);
        // Call to myVegas validate service only if redemption code present in
        // the request
        MyVegasResponse redemptionResponse = validateAndUpdateMyVegasProgram(reservationRequest,roomReservation);
        // removing char not supported by Opera
        ReservationUtil.sanitizedComments(roomReservation);
        PartyRoomReservation reservation = reservationDao.makePartyRoomReservationV4(roomReservation,
                createPartyRoomReservationRequest.isSplitCreditCardDetails());
        CreatePartyRoomReservationResponse response = responseMapper.partyRoomReservationsModelToResponse(reservation);
        invokePostPartyBookingOperations(response,reservation,redemptionResponse,reservationRequest,skipMyVegasConfirm);
        return response;
    }

    private void invokePostPartyBookingOperations(CreatePartyRoomReservationResponse response,PartyRoomReservation reservation,
                                                  MyVegasResponse redemptionResponse,RoomReservationRequest reservationRequest,
                                                  String skipMyVegasConfirm){
        if (ItineraryServiceImpl.isItineraryServiceEnabled()
                && CollectionUtils.isNotEmpty(response.getRoomReservations())) {
            response.getRoomReservations().stream().skip(1)
                    .filter(roomReservationResponse -> referenceDataDAOHelper.isPropertyManagedByAcrs(roomReservationResponse.getPropertyId()))
                    .forEach(roomReservationResponse -> roomReservationResponse
                            .setItineraryId(itineraryService.createCustomerItinerary(roomReservationResponse)));
            response.getRoomReservations().stream().forEach(
                    roomReservationResponse -> itineraryService.updateCustomerItinerary(roomReservationResponse));
        }

        String financialImpact = ReservationUtil.getPartyReservationFinancialImpact(reservation);
        if (StringUtils.isNotEmpty(financialImpact)) {
            HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
            httpRequest.setAttribute("financialImpact", financialImpact);
        }
        eventPublisherService.publishEvent(response.getRoomReservations(), ReservationEventType.CREATE.toString());
        if (null != reservation) {
            String confirmationNumbers = response.getRoomReservations().stream()
                    .map(RoomReservationV2Response::getConfirmationNumber).collect(Collectors.joining(","));

            if (null != redemptionResponse && !BooleanUtils.toBoolean(skipMyVegasConfirm)) {
                log.info("Calling MyVegas redemption for code: {}", redemptionResponse.getRedemptionCode());
                reservationServiceHelper.confirmRedemptionCode(reservationRequest, redemptionResponse,
                        confirmationNumbers, reservation.getRoomReservations().get(0).getCheckInDate(),
                        response.getRoomReservations().get(0).getPropertyId());
            }
        }

    }

    private MyVegasResponse validateAndUpdateMyVegasProgram(RoomReservationRequest reservationRequest, RoomReservation roomReservation){
        MyVegasResponse redemptionResponse = null;
        if (StringUtils.isNotBlank(reservationRequest.getMyVegasPromoCode())) {
            redemptionResponse = reservationServiceHelper.validateRedemptionCode(
                    reservationRequest.getMyVegasPromoCode(), roomReservation.getCheckInDate(),
                    roomReservation.getPropertyId());

            log.info("MyVegas code : {}, validated successfully", reservationRequest.getMyVegasPromoCode());
            reservationServiceHelper.updateProgramId(roomReservation, redemptionResponse);
        }
        return redemptionResponse;
    }

	@Override
	public SaveReservationResponse saveRoomReservation(SaveReservationRequest saveReservationRequest) {
		RoomReservation roomReservation = requestMapper
				.roomReservationRequestToModel(saveReservationRequest.getRoomReservation());
		SaveReservationResponse response = new SaveReservationResponse();
		try {
            RoomReservation roomReservationResponse = reservationDao.saveRoomReservation(roomReservation);
            // Special handling for borgata
            ReservationUtil.removeSpecialRequests(roomReservationResponse, appProperties);
            response.setRoomReservation(reservationDao.saveRoomReservation(roomReservationResponse));
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
		return response;
	}
}
