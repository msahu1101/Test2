package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.PaymentDAO;
import com.mgm.services.booking.room.dao.ReservationDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReservationDAOHelper;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.paymentservice.*;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.FailedReservation;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.helper.AccertifyInvokeHelper;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.util.BaseCommonUtil;
import com.mgm.services.common.util.DateUtil;
import com.mgmresorts.aurora.common.ReservationState;
import com.mgmresorts.aurora.common.RoomPricingType;
import com.mgmresorts.aurora.messages.*;
import com.mgmresorts.aurora.service.EAuroraException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation class for ReservationDAO providing functionality of
 * creating/updating room reservation objects.
 *
 */
@Component
@Log4j2
public class ReservationDAOStrategyGSEImpl extends AuroraBaseDAO implements ReservationDAOStrategy {

    @Autowired
    private ReservationDAOHelper reservationDAOHelper;
    
    @Autowired
    private ApplicationProperties appProps;
    
    @Autowired
	private AccertifyInvokeHelper accertifyInvokeHelper;

    @Autowired
    private IDMSTokenDAO idmsTokenDAO;

    @Autowired
    private PaymentDAO paymentDao;

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.ReservationDAO#prereserveRoom(com.mgm.
     * services.booking.room.model.request.PreReserveRequest)
     */
    @Override
    public RoomReservation prepareRoomCartItem(RoomCartRequest prereserveRequest) {

        // Find latest prices for the selected room and trip dates.
        GetRoomPricingAndAvailabilityExRequest request = MessageFactory.createGetRoomPricingAndAvailabilityExRequest();
        request.setPropertyId(prereserveRequest.getPropertyId());
        request.setCheckInDate(DateUtil.toDate(prereserveRequest.getCheckInDate()));
        request.setCheckOutDate(DateUtil.toDate(prereserveRequest.getCheckOutDate()));
        request.setNumAdults(prereserveRequest.getNumGuests());
        request.setCustomerId(prereserveRequest.getCustomerId());
        request.setPricingType(RoomPricingType.TripPricing);
        request.setRoomTypeIds(new String[] { prereserveRequest.getRoomTypeId() });
        if (!CollectionUtils.isEmpty(prereserveRequest.getAuroraItineraryIds())) {
            request.setItineraryIds(prereserveRequest.getAuroraItineraryIds().stream().toArray(String[]::new));
        }
        if (StringUtils.isNotEmpty(prereserveRequest.getProgramId())) {
            request.setProgramId(prereserveRequest.getProgramId());
            request.setProgramRate(false);
        }

        log.debug("Sent the request to RoomPricingAndAvailability as : {}", request.toJsonString());

        GetRoomPricingAndAvailabilityResponse response = getAuroraClient(prereserveRequest.getSource())
                .getRoomPricingAndAvailabilityEx(request);

        log.debug("Received the response from RoomPricingAndAvailability as : {}", response.toJsonString());

        if (null == response.getPrices()) {
            throw new BusinessException(ErrorCode.ROOMTYPE_UNAVAILABLE);
        }

        // Create room reservation object to invoke aurora API to updated the
        // reservation with all itemized charges and taxes
        List<RoomPrice> bookings = new ArrayList<>();
        for (com.mgmresorts.aurora.common.RoomPrice price : response.getPrices()) {
            RoomPrice roomPrice = new RoomPrice();
            roomPrice.setDate(price.getDate());
            roomPrice.setBasePrice(price.getBasePrice());
            roomPrice.setComp(price.getIsComp());
            roomPrice.setCustomerPrice(price.getPrice());
            roomPrice.setPrice(price.getPrice());
            roomPrice.setOverridePrice(-1);
            roomPrice.setPricingRuleId(price.getPricingRuleId());
            roomPrice.setProgramId(price.getProgramId());
            roomPrice.setProgramIdIsRateTable(price.getProgramIdIsRateTable());

            if (Double.compare(price.getPrice(), -1.0) == 0) {
                throw new BusinessException(ErrorCode.ROOMTYPE_UNAVAILABLE);
            }
            bookings.add(roomPrice);
        }

        // Create reservation object with pricing information received
        RoomReservation reservation = new RoomReservation();
        reservation.setNumRooms(1);
        reservation.setCheckInDate(DateUtil.toDate(prereserveRequest.getCheckInDate()));
        reservation.setCheckOutDate(DateUtil.toDate(prereserveRequest.getCheckOutDate()));
        reservation.setNumAdults(prereserveRequest.getNumGuests());
        reservation.setPropertyId(prereserveRequest.getPropertyId());
        reservation.setProgramId(prereserveRequest.getProgramId());
        // if specific program is not applied, find the dominant program
        if (StringUtils.isEmpty(prereserveRequest.getProgramId())) {
            reservation.setProgramId(reservationDAOHelper.findDominantProgram(bookings));
        }
        ReservationUtil.addSpecialRequests(reservation, appProps);

        reservation.setRoomTypeId(prereserveRequest.getRoomTypeId());
        reservation.setBookings(bookings);

        UpdateRoomReservationRequest updateRoomReservationRequest = MessageFactory.createUpdateRoomReservationRequest();

        com.mgmresorts.aurora.common.RoomReservation auroraRoomReservation = CommonUtil.copyProperties(reservation,
                com.mgmresorts.aurora.common.RoomReservation.class);

        updateRoomReservationRequest.setReservation(auroraRoomReservation);
        updateRoomReservationRequest.setStage(RoomReservationBookingStage.Checkout);

        log.info("Sent the request to updateRoomReservation as : {}", updateRoomReservationRequest.toJsonString());

        final com.mgmresorts.aurora.messages.UpdateRoomReservationResponse reservationResponse = getAuroraClient(
                prereserveRequest.getSource()).updateRoomReservation(updateRoomReservationRequest);

        log.info("Received the response from updateRoomReservation as : {}", reservationResponse.toJsonString());

        reservation = CommonUtil.copyProperties(reservationResponse.getReservation(), RoomReservation.class);

        return reservation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.ReservationDAO#updateRoomReservation(
     * com.mgm.services.booking.room.model.reservation.RoomReservation)
     */
    @Override
    public RoomReservation updateRoomReservation(RoomReservation reservation) {

        ReservationUtil.addSpecialRequests(reservation, appProps);
        UpdateRoomReservationRequest updateRoomReservationRequest = MessageFactory.createUpdateRoomReservationRequest();

        com.mgmresorts.aurora.common.RoomReservation auroraRoomReservation = CommonUtil.copyProperties(reservation,
                com.mgmresorts.aurora.common.RoomReservation.class);

        updateRoomReservationRequest.setReservation(auroraRoomReservation);
        updateRoomReservationRequest.setStage(RoomReservationBookingStage.Checkout);

        log.info("Sent the request to updateRoomReservation as : {}", updateRoomReservationRequest.toJsonString());

        final com.mgmresorts.aurora.messages.UpdateRoomReservationResponse reservationResponse = getAuroraClient(
                reservation.getSource()).updateRoomReservation(updateRoomReservationRequest);

        log.info("Received the response from updateRoomReservation as : {}", reservationResponse.toJsonString());

        reservation = CommonUtil.copyPendingReservation(reservationResponse.getReservation());

        return reservation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ReservationDAO#makeRoomReservation(com.
     * mgm.services.booking.room.model.reservation.RoomReservation)
     */
    @Override
    public RoomReservation makeRoomReservation(RoomReservation reservation) {

        MakeRoomReservationRequest makeRoomReservationRequest = MessageFactory.createMakeRoomReservationRequest();
        com.mgmresorts.aurora.common.RoomReservation auroraRoomReservation = CommonUtil.copyProperties(reservation,
                com.mgmresorts.aurora.common.RoomReservation.class);

        makeRoomReservationRequest.setReservation(auroraRoomReservation);

        long customerId = reservation.getProfile().getId();
        if (customerId <= 0) {
            customerId = createTransientCustomer(reservation);
            auroraRoomReservation.getProfile().setId(customerId);
        }

        String itineraryId = reservation.getItineraryId();
        // Create the itinerary if and only if it is not passed in the request
        if (StringUtils.isBlank(reservation.getItineraryId())) {
            itineraryId = createCustomerItinerary(reservation, customerId);
        }
        makeRoomReservationRequest.setItineraryId(itineraryId);
        auroraRoomReservation.setItineraryId(itineraryId);

        makeRoomReservationRequest.setCustomerId(customerId);

        log.info("Sent the request to makeRoomReservation as : {}", makeRoomReservationRequest.toJsonString());

        final MakeRoomReservationResponse response = getAuroraClient(reservation.getSource())
                .makeRoomReservation(makeRoomReservationRequest);

        log.info("Received the response from makeRoomReservation as : {}", response.toJsonString());

        reservation = CommonUtil.copyProperties(response.getItinerary().getRoomReservations()[0],
                RoomReservation.class);

        return reservation;
    }

    @LogExecutionTime
    @Override
    public RoomReservation makeRoomReservationV2(RoomReservation reservation) {
		AuthorizationTransactionRequest authorizeRequest = null;
        boolean performAFScheck = false;
        if(!reservation.isSkipFraudCheck()) {
            performAFScheck = accertifyInvokeHelper.performAFSCheck(reservation);
        }
        if (performAFScheck && !reservation.isSkipPaymentProcess()) {
            authorizeRequest = accertifyInvokeHelper.createAuthorizeRequest(reservation,
                    reservation.getInSessionReservationId());
            boolean authorized = accertifyInvokeHelper.authorize(authorizeRequest);
            log.info("accertify {} the purchase", authorized ? "authorized" : "not authorized");

            if (!authorized) {
                throw new BusinessException(ErrorCode.TRANSACTION_NOT_AUTHORIZED);
            }
        }
        if(!reservation.isSkipPaymentProcess() && StringUtils.isNotEmpty(reservation.getPropertyId()) && reservation.getPropertyId().equalsIgnoreCase(appProps.getTcolvPropertyId())) {
            boolean isEnableZeroAmountAuthTCOLV = accertifyInvokeHelper.isEnableZeroAmountAuthTCOLVCreate();
            if (isEnableZeroAmountAuthTCOLV) {
                for (CreditCardCharge ccCharge :  reservation.getCreditCardCharges()) {
                    makeZeroDollarAuthPaymentTCOLV(reservation.getCheckInDate(), reservation.getCheckOutDate(),
                            ccCharge, reservation.getPropertyId(), reservation.getSource());
                }
            }
        }
        MakeRoomReservationRequest makeRoomReservationRequest = MessageFactory.createMakeRoomReservationRequest();
        com.mgmresorts.aurora.common.RoomReservation auroraRoomReservation = CommonUtil.copyProperties(reservation,
                com.mgmresorts.aurora.common.RoomReservation.class);

        makeRoomReservationRequest.setReservation(auroraRoomReservation);

        List<ReservationProfile> shareWithCustomersRequest = reservation.getShareWithCustomers();

        if (null != shareWithCustomersRequest) {
            shareWithCustomersRequest.stream()
                    .forEach(reservationRequest -> makeRoomReservationRequest.addShareWithCustomers(CommonUtil
                            .copyProperties(reservationRequest, com.mgmresorts.aurora.common.CustomerProfile.class)));
        }

        long customerId = reservation.getCustomerId();

        String itineraryId = reservation.getItineraryId();
        // Create the itinerary if and only if it is not passed in the request
        if (StringUtils.isBlank(itineraryId)) {
            itineraryId = createCustomerItinerary(reservation, customerId);
        }
        makeRoomReservationRequest.setItineraryId(itineraryId);
        auroraRoomReservation.setItineraryId(itineraryId);

        makeRoomReservationRequest.setCustomerId(customerId);

        log.info("Sent the request to makeRoomReservation as : {}", makeRoomReservationRequest.toJsonString());
        try {
            final MakeRoomReservationResponse response = getAuroraClient(reservation.getSource())
                    .makeRoomReservation(makeRoomReservationRequest);
            log.info("Received the response from makeRoomReservation as : {}", response.toJsonString());
            com.mgmresorts.aurora.common.RoomReservation[] rReservation = response.getItinerary().getRoomReservations();
            String reservationId = reservation.getId();
            // Retrieving the roomReservation object matching the reservationId from the
            // request
            for (com.mgmresorts.aurora.common.RoomReservation roomReservation : rReservation) {
                if (reservationId.equals(roomReservation.getId())) {
                    reservation = CommonUtil.copyProperties(roomReservation, RoomReservation.class);
                    reservation.setCustomerId(response.getItinerary().getCustomerId());
                    break;
                }
            }
            if(null != authorizeRequest && null != authorizeRequest.getTransaction()) {
            	authorizeRequest.getTransaction().setOrderStatus(ServiceConstant.COMPLETED);
            	authorizeRequest.getTransaction().setProcessorResponseText(ServiceConstant.COMPLETED);
            }

        } catch (EAuroraException ex) {
            log.error("Exception trying to make room reservation : ", ex);
            if(null != authorizeRequest && null != authorizeRequest.getTransaction()) {
            	authorizeRequest.getTransaction().setOrderStatus(ServiceConstant.CANCELLED_BY_MERCHANT);
            	authorizeRequest.getTransaction().setProcessorResponseText(ex.getMessage());
            }
            handleAuroraError(ex);
        } finally {
            if(!reservation.isSkipPaymentProcess()) {
                accertifyInvokeHelper.confirmAsyncCall(authorizeRequest, ((null != reservation) ? reservation.getConfirmationNumber() : null));
            }
        }

        return reservation;
    }

    /**
     * Create transient customer profile for non-logged-in guests booking the
     * reservation.
     * 
     * @param reservation Room reservation object
     * @return Customer id of transient profile created
     */
    private long createTransientCustomer(RoomReservation reservation) {

        AddCustomerRequest addCustomerRequest = reservationDAOHelper.prepareAddCustomerRequest(reservation);

        log.info("Sent the request to addCustomer as : {}", addCustomerRequest.toJsonString());
        final AddCustomerResponse addCustomerResponse = getAuroraClient(reservation.getSource())
                .addCustomer(addCustomerRequest);

        log.info("Received the response from addCustomer as : {}", addCustomerResponse.toJsonString());

        return addCustomerResponse.getCustomer().getId();
    }

    /**
     * Create customer itinerary based on trip details.
     * 
     * @param reservation Room reservation object
     * @param customerId  Customer Id
     * @return Itinerary Id
     */
    private String createCustomerItinerary(RoomReservation reservation, long customerId) {
        CreateCustomerItineraryRequest createCustomerItineraryRequest = reservationDAOHelper
                .prepareCreateCustomerItineraryRequest(reservation, customerId);

        log.info("Sent the request to createCustomerItinerary as : {}", createCustomerItineraryRequest.toJsonString());
        final CreateCustomerItineraryResponse response = getAuroraClient(reservation.getSource())
                .createCustomerItinerary(createCustomerItineraryRequest);

        log.info("Received the response from createCustomerItinerary as : {}", response.toJsonString());

        return response.getItinerary().getId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ReservationDAO#saveRoomReservation(com.
     * mgm.services.booking.room.model.reservation.RoomReservation)
     */
    @Override
    public RoomReservation saveRoomReservation(RoomReservation reservation) {

        SaveRoomReservationRequest saveRequest = MessageFactory.createSaveRoomReservationRequest();

        com.mgmresorts.aurora.common.RoomReservation auroraRoomReservation = CommonUtil.copyProperties(reservation,
                com.mgmresorts.aurora.common.RoomReservation.class);
        auroraRoomReservation.setState(ReservationState.AutoSaved);

        long customerId = reservation.getProfile().getId();
        if (customerId <= 0) {
            customerId = createTransientCustomer(reservation);
            auroraRoomReservation.getProfile().setId(customerId);
        }
        if (reservation.getItineraryId() == null) {
            final String itineraryId = createCustomerItinerary(reservation, customerId);
            saveRequest.setItineraryId(itineraryId);
        } else {
            saveRequest.setItineraryId(reservation.getItineraryId());
        }
        saveRequest.setCustomerId(customerId);
        saveRequest.setReservation(auroraRoomReservation);
        
        log.info("Sent the request to saveRoomReservation as : {}", saveRequest.toJsonString());

        final SaveRoomReservationResponse response = getAuroraClient(reservation.getSource())
                .saveRoomReservation(saveRequest);

        log.info("Received the response from saveRoomReservation as : {}", response.toJsonString());

        reservation = CommonUtil.copyProperties(response.getItinerary().getRoomReservations()[0],
                RoomReservation.class);

        return reservation;
    }

    @Override
    @LogExecutionTime
    public PartyRoomReservation makePartyRoomReservation(RoomReservation reservation, boolean splitCreditCardDetails) {
    	AuthorizationTransactionRequest authorizeRequest = null;
    	if (accertifyInvokeHelper.performAFSCheck(reservation)) {
			authorizeRequest = accertifyInvokeHelper.createAuthorizeRequest(reservation,
					reservation.getInSessionReservationId());
			boolean authorized  = accertifyInvokeHelper.authorize(authorizeRequest);
			log.info("accertify {} the purchase", authorized ? "authorized" : "not authorized");
			
			if(!authorized) {
				throw new BusinessException(ErrorCode.TRANSACTION_NOT_AUTHORIZED);
			}
		}

        MakePartyRoomReservationRequest makePartyRoomReservationRequest = MessageFactory
                .createMakePartyRoomReservationRequest();
        com.mgmresorts.aurora.common.RoomReservation auroraRoomReservation = CommonUtil.copyProperties(reservation,
                com.mgmresorts.aurora.common.RoomReservation.class);

        makePartyRoomReservationRequest.setReservation(auroraRoomReservation);
        makePartyRoomReservationRequest.setSplitPaymentDetails(splitCreditCardDetails);

        long customerId = reservation.getCustomerId();

        String itineraryId = reservation.getItineraryId();
        // Create the itinerary if and only if it is not passed in the request
        if (StringUtils.isBlank(itineraryId)) {
            itineraryId = createCustomerItinerary(reservation, customerId);
        }
        makePartyRoomReservationRequest.setItineraryId(itineraryId);
        auroraRoomReservation.setItineraryId(itineraryId);

        makePartyRoomReservationRequest.setCustomerId(customerId);

        log.info("Sent the request to makePartyRoomReservation as : {}",
                makePartyRoomReservationRequest.toJsonString());

        PartyRoomReservation partyRoomReservation = new PartyRoomReservation();
        try {
            final MakePartyRoomReservationResponse response = getAuroraClient(reservation.getSource())
                    .makePartyRoomReservation(makePartyRoomReservationRequest);
            log.info("Received the response from makePartyRoomReservation as : {}", response.toJsonString());

            String reservationId = reservation.getId();

            List<RoomReservation> fullReservationsList = new ArrayList<>();
            Arrays.stream(response.getItineraries()).forEach(itinearary -> {
                List<RoomReservation> reservationsList = new ArrayList<>();
                Arrays.stream(itinearary.getRoomReservations()).forEach(roomReservation -> {
                    if (reservationId.equals(roomReservation.getId())) {
                        RoomReservation pReservation = CommonUtil.copyProperties(roomReservation,
                                RoomReservation.class);
                        pReservation.setCustomerId(itinearary.getCustomerId());
                        reservationsList.add(pReservation);
                    }
                });
                fullReservationsList.addAll(reservationsList);
            });
            partyRoomReservation.setRoomReservations(fullReservationsList);

            List<ErrorContext> errorContextList = new ArrayList<>(response.getErrors().length);
            List<FailedReservation> failedReservationsList = new ArrayList<>(response.getErrors().length);
            org.apache.commons.collections.CollectionUtils.addAll(errorContextList, response.getErrors());
            errorContextList.stream().forEach(errorContext -> failedReservationsList
                    .add(CommonUtil.copyProperties(errorContext, FailedReservation.class)));
            partyRoomReservation.setFailedReservations(failedReservationsList);
        } catch (EAuroraException ex) {
            log.error("Exception trying to makePartyRoomReservation : ", ex);
            handleAuroraError(ex);
        }

        if (null != authorizeRequest) {
            String confirmationNumbers = partyRoomReservation.getRoomReservations().stream()
                    .map(RoomReservation::getConfirmationNumber).collect(Collectors.joining(","));

            accertifyInvokeHelper.confirm(authorizeRequest, confirmationNumbers, null);
        }

        return partyRoomReservation;
    }

    @Override
    public PartyRoomReservation makePartyRoomReservationV4(RoomReservation reservation, boolean splitCreditCardDetails) {
        throw new UnsupportedOperationException("v4 party API is not supported for GSE Properties.");
    }

    private void makeZeroDollarAuthPaymentTCOLV(Date checkIn, Date checkOut, CreditCardCharge charge, String propertyId,
                                      String source) {
        final String authToken = idmsTokenDAO.generateToken().getAccessToken();
        final AuthRequest authReq = ReservationUtil.getAuthRequestTCOLV(checkIn, checkOut, charge,
                referenceDataDAOHelper.retrieveMerchantID(propertyId));

        final HttpEntity<AuthRequest> request = new HttpEntity<>(authReq,
                CommonUtil.createPaymentHeaders(authToken, source));

        if (StringUtils.isEmpty(authReq.getTransactionRefCode())) {
            if (!request.getHeaders().isEmpty() && request.getHeaders().containsKey(ServiceConstant.X_MGM_CORRELATION_ID)) {
                List<String> correlationIDList = request.getHeaders().get(ServiceConstant.X_MGM_CORRELATION_ID);
                if (org.apache.commons.collections.CollectionUtils.isNotEmpty(correlationIDList)) {
                    authReq.setTransactionRefCode(correlationIDList.get(0));
                }
            }
        }

        AuthResponse authResponse = null;
        try {
            authResponse = paymentDao.authorizePayment(request);
        } catch (BusinessException e) {
            log.error("Business Exception during TCOLV zero dollar auth payment call: {}", e.getMessage());
        }

        if (authResponse != null && authResponse.getStatusMessage().equals(ServiceConstant.APPROVED)) {
            log.info("payment authorization successful for property ID {}", propertyId);
        } else {
            throw new BusinessException(ErrorCode.PAYMENT_AUTHORIZATION_FAILED);
        }
    }
}