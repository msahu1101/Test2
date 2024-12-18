package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.model.PurchasedComponent;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ProductInventoryDAO;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.model.inventory.BookedItemList;
import com.mgm.services.booking.room.model.inventory.BookedItems;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.ReservationState;
import com.mgm.services.booking.room.service.helper.AccertifyInvokeHelper;
import com.mgm.services.booking.room.validator.RBSTokenScopes;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mgm.services.booking.room.dao.CancelReservationDAO;
import com.mgm.services.booking.room.dao.FindReservationDAO;
import com.mgm.services.booking.room.dao.OCRSDAO;
import com.mgm.services.booking.room.event.ReservationEventType;
import com.mgm.services.booking.room.mapper.RoomReservationResponseMapper;
import com.mgm.services.booking.room.model.ocrs.OcrsReservation;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CancelRoomReservationV2Response;
import com.mgm.services.booking.room.model.response.ItineraryResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.CancelService;
import com.mgm.services.booking.room.service.EventPublisherService;
import com.mgm.services.booking.room.service.IDUtilityService;
import com.mgm.services.booking.room.service.ItineraryService;
import com.mgm.services.booking.room.service.ReservationEmailV2Service;
import com.mgm.services.booking.room.service.helper.FindReservationServiceHelper;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for CancelService
 */
@Component
@Log4j2
public class CancelServiceImpl implements CancelService {

	@Autowired
	private CancelReservationDAO cancelReservationDao;

	@Autowired
	private RoomReservationResponseMapper roomReservationResponseMapper;

    @Autowired
    private RoomReservationRequestMapper requestMapper;
	@Autowired
	private EventPublisherService<RoomReservationV2Response> eventPublisherService;

	@Autowired
	private ItineraryService itineraryService;

	@Autowired
	ApplicationProperties appProperties;

	@Autowired
	private ReservationEmailV2Service emailService;

    @Autowired
    private ReservationServiceHelper reservationServiceHelper;

    @Autowired
    private FindReservationServiceHelper findReservationServiceHelper;

    @Autowired
    private FindReservationDAO findReservationDao;

    @Autowired
    private IDUtilityService idUtilityService;

    @Autowired
    private OCRSDAO ocrsDao;

    @Autowired
    private ProductInventoryDAO productInventoryDAO;

    @Autowired
    private AccertifyInvokeHelper accertifyInvokeHelper;


	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.mgm.services.booking.room.service.CancelService#cancelReservation(com
	 * .mgm.services.booking.room.model.request.CancelRequest)
	 */
	@Override
	public RoomReservation cancelReservation(CancelRequest cancelRequest) {
		return cancelReservationDao.cancelReservation(cancelRequest);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.mgm.services.booking.room.service.CancelService#cancelReservation(com
	 * .mgm.services.booking.room.model.request.CancelV2Request)
	 */
	@Override
	public CancelRoomReservationV2Response cancelReservation(CancelV2Request cancelRequest, boolean findItineraryId) {
        RoomReservation roomReservation = cancelReservationDao.cancelReservation(cancelRequest);
        roomReservation.setPaymentWidgetFlow(cancelRequest.isSkipPaymentProcess() || cancelRequest.isCancelPending());
        if(null != roomReservation) {
        	roomReservation.setInSessionReservationId(cancelRequest.getInAuthTransactionId());
        }
        try {
            return updateCancelResponse(findItineraryId, roomReservation, false,
                    cancelRequest.getConfirmationNumber(), cancelRequest.isF1Package(), cancelRequest.isSkipCustomerNotification(),cancelRequest.isCancelPending());
        } finally {
        	log.info(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN+" Header value :"+ThreadContext.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN));
        	log.info(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN+" Header value is present :"+StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN)));
        	if(!roomReservation.isPaymentWidgetFlow() && StringUtils.isNotBlank(ThreadContext.get(ServiceConstant.HEADER_FRAUD_AGENT_TOKEN))) {
        		AuthorizationTransactionRequest authorizeRequest = accertifyInvokeHelper.createAuthorizeRequest(roomReservation, roomReservation.getInSessionReservationId());
        		if (null != authorizeRequest && null != authorizeRequest.getTransaction()) {
        			authorizeRequest.getTransaction().setOrderStatus(ServiceConstant.CANCELLED_BY_CUSTOMER);
        			authorizeRequest.getTransaction().setProcessorResponseText(ServiceConstant.COMPLETED);
        		}
        		if(null != roomReservation) {
        			accertifyInvokeHelper.confirmAsyncCall(authorizeRequest, roomReservation.getConfirmationNumber());
        		}
        	}
        }
	}

	@Override
	public boolean ignoreReservation(ReleaseV2Request cancelRequest) {
		return cancelReservationDao.ignoreReservation(cancelRequest);
	}

    @Override
    public CancelRoomReservationV2Response cancelReservation(CancelV3Request cancelV3Request, String token) {
     RoomReservation findRoomReservation = findReservationDao
                .findRoomReservation(reservationServiceHelper.createFindReservationV2Request(cancelV3Request));

        if (null == findRoomReservation) {
            log.info("No reservation returned neither by aurora nor by ACRS for the given confirmation number {}",
                    cancelV3Request.getConfirmationNumber());
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        // reservation will be cancellable in one of the following cases, otherwise exception will be thrown
        // 1. request has an elevated access (for clients like ICE)
        // 2. firstName and lastName in the request and reservation are matching (fuzzy match)
        // 3. mlife number in the JWT token and reservation are matching
        // 4. mgmId in the JWT token and Ocrs reservation are matching
        if (!isFirstNameLastNameMatching(cancelV3Request, findRoomReservation)
                && !findReservationServiceHelper.validateTokenOrServiceBasedRole(RBSTokenScopes.UPDATE_RESERVATION_ELEVATED.getValue())
                && !isMlifeNumMatching(findRoomReservation) && !isMgmIdMatching(findRoomReservation)) {
            log.info("Does not met any critiera to return the reservation");
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        if (!reservationServiceHelper.validateTokenOrServiceBasedRole(RBSTokenScopes.UPDATE_RESERVATION_ELEVATED.getValue())
                && !reservationServiceHelper.isRequestAllowedToCancelReservation(findRoomReservation)) {
            log.info("Reservation is not allowed to cancel");
            throw new BusinessException(ErrorCode.RESERVATION_CANNOT_BE_CANCELLED);
        }

        if (null != findRoomReservation.getState() &&
                findRoomReservation.getState().equals(ReservationState.Cancelled)) {
            log.info("Reservation is already cancelled");
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
        }

        CancelV2Request cancelV2Req = reservationServiceHelper.createCancelV2Request(cancelV3Request, findRoomReservation);
       // For payment widget refund flow
        if(cancelV3Request.isSkipPaymentProcess() && CollectionUtils.isNotEmpty(cancelV3Request.getBilling())) {
           cancelV2Req.setCreditCardCharges(requestMapper.roomPaymentDetailsRequestListToCreditCardChargeList(cancelV3Request.getBilling()));
       }
        CancelRoomReservationV2Response cancelRoomReservationV2Response = cancelReservation(cancelV2Req , true);

        if (findRoomReservation.isF1Package()) {
            productInventoryDAO.rollBackInventory(ReservationUtil.createRollbackInventoryRequest(findRoomReservation.getConfirmationNumber()));
            ReservationUtil.purchasedComponentsF1Updates(cancelRoomReservationV2Response.getRoomReservation(),
                    appProperties, findRoomReservation.getRatePlanTags(), true);
        }

        return cancelRoomReservationV2Response;
    }

    @Override
    public CancelRoomReservationV2Response cancelPreviewReservation(CancelV4Request cancelV4Request, String token) {
        {
            RoomReservation findRoomReservation = findReservationDao
                    .findRoomReservation(reservationServiceHelper.createFindReservationV2Request(cancelV4Request));

            validateCancel(cancelV4Request,findRoomReservation);
            CancelV2Request cancelV2Req = reservationServiceHelper.createCancelV2Request(cancelV4Request, findRoomReservation);
            return cancelPendingAndIgnore(cancelV2Req);
        }
    }

    private void validateCancel(CancelV4Request cancelV4Request, RoomReservation findRoomReservation){

        if (null == findRoomReservation) {
            log.info("No reservation returned neither by aurora nor by ACRS for the given confirmation number {}",
                    cancelV4Request.getConfirmationNumber());
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        // reservation will be cancellable in one of the following cases, otherwise exception will be thrown
        // 1. request has an elevated access (for clients like ICE)
        // 2. firstName and lastName in the request and reservation are matching (fuzzy match)
        // 3. mlife number in the JWT token and reservation are matching
        // 4. mgmId in the JWT token and Ocrs reservation are matching
        if (!isFirstNameLastNameMatching(cancelV4Request, findRoomReservation)
                && !findReservationServiceHelper.validateTokenOrServiceBasedRole(RBSTokenScopes.UPDATE_RESERVATION_ELEVATED.getValue())
                && !isMlifeNumMatching(findRoomReservation) && !isMgmIdMatching(findRoomReservation)) {
            log.info("Does not met any critiera to return the reservation");
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        if (!reservationServiceHelper.validateTokenOrServiceBasedRole(RBSTokenScopes.UPDATE_RESERVATION_ELEVATED.getValue())
                && !reservationServiceHelper.isRequestAllowedToCancelReservation(findRoomReservation)) {
            log.info("Reservation is not allowed to cancel");
            throw new BusinessException(ErrorCode.RESERVATION_CANNOT_BE_CANCELLED);
        }

        if (null != findRoomReservation.getState() &&
                findRoomReservation.getState().equals(ReservationState.Cancelled)) {
            log.info("Reservation is already cancelled");
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
        }
    }

    private CancelRoomReservationV2Response cancelPendingAndIgnore(CancelV2Request cancelRequest){
        RoomReservation roomReservation = cancelReservationDao.cancelPreviewReservation(cancelRequest);
        CancelRoomReservationV2Response cancelPreviewReservationResponse = new CancelRoomReservationV2Response();
        cancelPreviewReservationResponse
                .setRoomReservation(roomReservationResponseMapper.roomReservationModelToResponse(roomReservation));

        //Set nonEditable and isPkgComponent flag true for pkgComponents
        List<PurchasedComponent> updatedPurchasedComponents = reservationServiceHelper.updatePackageComponentsFlag(
                cancelPreviewReservationResponse.getRoomReservation().getPropertyId(),
                cancelPreviewReservationResponse.getRoomReservation().getPurchasedComponents()
        );
        cancelPreviewReservationResponse.getRoomReservation().setPurchasedComponents(updatedPurchasedComponents);

        // Populate miscellaneous fields
        if(null != cancelPreviewReservationResponse.getRoomReservation().getDepositDetails()) {
            cancelPreviewReservationResponse.getRoomReservation().getDepositDetails()
                    .setRefundAmount(ReservationUtil.getRefundAmount(roomReservation,cancelRequest.isOverrideDepositForfeit(), appProperties));
            cancelPreviewReservationResponse.getRoomReservation().getDepositDetails()
                    .setCalculatedForefeitAmount(ReservationUtil.getForfeitAmount(roomReservation, appProperties));
        }
        return cancelPreviewReservationResponse;
    }

    @Override
    public CancelRoomReservationV2Response cancelCommitReservation(CancelV4Request cancelV4Request, String token) {
        {
            RoomReservation findRoomReservation = findReservationDao
                    .findRoomReservation(reservationServiceHelper.createFindReservationV2Request(cancelV4Request));
            validateCancel(cancelV4Request,findRoomReservation);
            CancelV2Request cancelV2Req = reservationServiceHelper.createCancelV2Request(cancelV4Request, findRoomReservation);
             // For payment widget refund flow
            if(CollectionUtils.isNotEmpty(cancelV4Request.getBilling())) {
                cancelV2Req.setCreditCardCharges(requestMapper.roomPaymentDetailsRequestListToCreditCardChargeList(cancelV4Request.getBilling()));
            }
            RoomReservation roomReservation = cancelReservationDao.cancelCommitReservation(cancelV2Req);
            if(null != roomReservation) {
                roomReservation.setInSessionReservationId(cancelV2Req.getInAuthTransactionId());
            }
            //@TODO do we need to pass findItineraryId = true? As its ACRS flow
            CancelRoomReservationV2Response cancelRoomReservationV2Response = updateCancelResponse(false, roomReservation, false,
                    cancelV2Req.getConfirmationNumber(), cancelV2Req.isF1Package(), cancelV4Request.isSkipCustomerNotification());
            if (findRoomReservation.isF1Package()) {
                productInventoryDAO.rollBackInventory(ReservationUtil.createRollbackInventoryRequest(findRoomReservation.getConfirmationNumber()));
            }

            return cancelRoomReservationV2Response;
        }
    }

    private boolean isMlifeNumMatching(RoomReservation roomReservation) {
        return null != roomReservation && null != roomReservation.getProfile()
                && findReservationServiceHelper.isMlifeNumMatching(roomReservation.getProfile().getMlifeNo());
    }

    private boolean isFirstNameLastNameMatching(CancelV3Request cancelV3Request, RoomReservation roomReservation) {
        return StringUtils.isNotEmpty(cancelV3Request.getFirstName())
                && StringUtils.isNotEmpty(cancelV3Request.getLastName()) && null != roomReservation
                && idUtilityService.isFirstNameLastNameMatching(roomReservation.getProfile(),
                        cancelV3Request.getFirstName(), cancelV3Request.getLastName());
    }
    private boolean isFirstNameLastNameMatching(CancelV4Request cancelV3Request, RoomReservation roomReservation) {
        return StringUtils.isNotEmpty(cancelV3Request.getFirstName())
                && StringUtils.isNotEmpty(cancelV3Request.getLastName()) && null != roomReservation
                && idUtilityService.isFirstNameLastNameMatching(roomReservation.getProfile(),
                cancelV3Request.getFirstName(), cancelV3Request.getLastName());
    }

    private boolean isMgmIdMatching(RoomReservation roomReservation) {
        boolean isMatching = false;
        if (findReservationServiceHelper.isTokenHasMgmId()) {

            OcrsReservation ocrsReservation = ocrsDao.getOCRSReservation(roomReservation.getConfirmationNumber());

            if (null != ocrsReservation && null != ocrsReservation.getMgmProfile()) {
                isMatching = findReservationServiceHelper.isMgmIdMatching(ocrsReservation.getMgmProfile().getMgmId());
            }
        }
        return isMatching;
    }

    @Override
    public boolean ignoreReservationV3(ReleaseV3Request releaseRequest) {
        boolean response = cancelReservationDao.ignoreReservation(ReservationUtil.createReleaseV2Request(releaseRequest));
        if (releaseRequest.isF1Package()) {
            productInventoryDAO.releaseInventory(ReservationUtil.createReleaseInventoryRequest(releaseRequest.getHoldId()));
        }
        return response;
    }

    @Override
    public CancelRoomReservationV2Response cancelReservationF1(CancelV3Request cancelV3Request, String token) {
        RoomReservation findRoomReservation = findReservationDao
                .findRoomReservation(reservationServiceHelper.createFindReservationV2Request(cancelV3Request));

        CancelRoomReservationV2Response response = new CancelRoomReservationV2Response();
        if (null == findRoomReservation) {
            log.info("No reservation returned neither by aurora nor by ACRS for the given confirmation number {}",
                    cancelV3Request.getConfirmationNumber());
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (findRoomReservation.isF1Package()) {
            response = updateCancelResponse(true, findRoomReservation, true,
                            findRoomReservation.getConfirmationNumber(), findRoomReservation.isF1Package(), cancelV3Request.isSkipCustomerNotification());
            response.getRoomReservation().setNotifyCustomer(false);
            eventPublisherService.publishEvent(Collections.singletonList(response.getRoomReservation()),
                    ReservationEventType.CANCEL.toString());
            if (findRoomReservation.isF1Package()) {
                productInventoryDAO.rollBackInventory(ReservationUtil.createRollbackInventoryRequest(findRoomReservation.getConfirmationNumber()));
            }
        }
        return response;
    }

    public CancelRoomReservationV2Response updateCancelResponse (boolean findItineraryId,
                                                                 RoomReservation roomReservation,
                                                                 boolean onlyF1Cancel,
                                                                 String confirmationNumber,
                                                                 boolean isF1Package,
                                                                 boolean skipCustomerNotification,boolean ... isCancelPendingFlow) {
        CancelRoomReservationV2Response cancelReservationResponse = new CancelRoomReservationV2Response();
        cancelReservationResponse
                .setRoomReservation(roomReservationResponseMapper.roomReservationModelToResponse(roomReservation));

        //Set nonEditable and isPkgComponent flag true for pkgComponents
        List<PurchasedComponent> updatedPurchasedComponents = reservationServiceHelper.updatePackageComponentsFlag(
                cancelReservationResponse.getRoomReservation().getPropertyId(),
                cancelReservationResponse.getRoomReservation().getPurchasedComponents()
        );
        cancelReservationResponse.getRoomReservation().setPurchasedComponents(updatedPurchasedComponents);

        // Populate miscellaneous fields
        if(null != cancelReservationResponse.getRoomReservation().getDepositDetails()) {
            cancelReservationResponse.getRoomReservation().getDepositDetails()
                    .setRefundAmount(ReservationUtil.getRefundAmount(roomReservation, appProperties));
            cancelReservationResponse.getRoomReservation().getDepositDetails()
                    .setCalculatedForefeitAmount(ReservationUtil.getForfeitAmount(roomReservation, appProperties));
        }
        boolean isCancelPreview= isCancelPendingFlow.length > 0 && isCancelPendingFlow[0];
        Deposit depositDetails = cancelReservationResponse.getRoomReservation().getDepositDetails();
        double refundAmount = null != depositDetails ? depositDetails.getRefundAmount() : 0;
        boolean isCancelPending = isCancelPreview && refundAmount != 0;
        //isCancelPreview will be always false for TCOLV so isCancelPending will be false.
        if(!isCancelPending) {
            if (findItineraryId && StringUtils.isEmpty(cancelReservationResponse.getRoomReservation().getItineraryId())) {
                ItineraryResponse itineraryResponse = itineraryService
                        .getCustomerItineraryByConfirmationNumber(confirmationNumber);
                String itineraryId = itineraryResponse != null ? itineraryResponse.getItinerary().getItineraryId() : null;
                log.info("ItineraryId {} found in the itinerary service for confirmationNumber {}", itineraryId,
                        confirmationNumber);
                cancelReservationResponse.getRoomReservation().setItineraryId(itineraryId);
            }
            if (ItineraryServiceImpl.isItineraryServiceEnabled()) {
                itineraryService.updateCustomerItinerary(cancelReservationResponse.getRoomReservation());
            }
        }
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        httpRequest.setAttribute("financialImpact",
                ReservationUtil.getCancelFinancialImpact(roomReservation, appProperties));
        String channel = CommonUtil.getChannelHeaderWithFallback(httpRequest);
        boolean isHDEPackageReservation = reservationServiceHelper.isReservationHasHDEProgram(roomReservation);
        // populating notifyCustomer, so it will be available for publishEvent
        boolean channelExcludedFromSendEmail = CommonUtil.isChannelExcludedForEmail(channel,
                appProperties.getExcludeEmailForChannels());
        boolean notifyCustomerViaRTC = reservationServiceHelper.isNotifyCustomerViaRTC(
                cancelReservationResponse.getRoomReservation().getPropertyId(), channelExcludedFromSendEmail,isHDEPackageReservation);
        if (skipCustomerNotification) {
            cancelReservationResponse.getRoomReservation().setNotifyCustomer(false);
        } else {
            cancelReservationResponse.getRoomReservation().setNotifyCustomer(notifyCustomerViaRTC);
        }
        cancelReservationResponse.getRoomReservation().setRatesFormat(ServiceConstant.RTC_RATES_FORMAT);
        cancelReservationResponse.getRoomReservation().setHdePackage(isHDEPackageReservation);

        if (isF1Package) {
            cancelReservationResponse.getRoomReservation().setF1Package(true);
            BookedItemList bookedItemList = productInventoryDAO.
                    getInventoryStatus(confirmationNumber, null);
            if (CollectionUtils.isNotEmpty(bookedItemList)) {
                BookedItems bookedItems = bookedItemList.get(0);
                if (null != bookedItems) {
                    if (StringUtils.isNotEmpty(bookedItems.getHoldId())) {
                        cancelReservationResponse.getRoomReservation().setHoldId(bookedItems.getHoldId());
                    }
                    if (StringUtils.isNotEmpty(bookedItems.getOrderId())) {
                        cancelReservationResponse.getRoomReservation().setOrderId(bookedItems.getOrderId());
                    }
                    if (StringUtils.isNotEmpty(bookedItems.getOrderLineItemId())) {
                        cancelReservationResponse.getRoomReservation().setOrderLineItemId(bookedItems.getOrderLineItemId());
                    }
                }
            }
        }

        if (!onlyF1Cancel && !isCancelPending) {
            eventPublisherService.publishEvent(Collections.singletonList(cancelReservationResponse.getRoomReservation()),
                    ReservationEventType.CANCEL.toString());
            if (notifyCustomerViaRTC) {
                log.info("Email will be sent by RTC");
            } else {
                if (channelExcludedFromSendEmail) {
                    log.info("Email will not be sent for channel:{}", channel);
                } else {
                    emailService.sendCancellationEmail(roomReservation, cancelReservationResponse.getRoomReservation());
                }
            }
        }
        return cancelReservationResponse;
    }
}
