package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.CustomerDAO;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.ModifyReservationDAOStrategy;
import com.mgm.services.booking.room.dao.PaymentDAO;
import com.mgm.services.booking.room.logging.annotation.LogExecutionTime;
import com.mgm.services.booking.room.model.paymentservice.*;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.EditReservationRequest;
import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.dto.CommitPaymentDTO;
import com.mgm.services.booking.room.model.request.dto.UpdateProfileInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.EditReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.helper.AccertifyInvokeHelper;
import com.mgm.services.booking.room.transformer.CustomerProfileTransformer;
import com.mgm.services.booking.room.transformer.EditReservationRequestTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.Customer;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.util.BaseCommonUtil;
import com.mgmresorts.aurora.common.ItineraryData;
import com.mgmresorts.aurora.common.TripParams;
import com.mgmresorts.aurora.messages.*;
import com.mgmresorts.aurora.service.EAuroraException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation class for ModifyReservationDAO for pre-modify and modify
 * reservation functions.
 * 
 */
@Component
@Log4j2
public class ModifyReservationDAOStrategyGSEImpl extends AuroraBaseDAO implements ModifyReservationDAOStrategy {

    private URLProperties urlProperties;
    private DomainProperties domainProperties;
    private RestTemplate client;
    private AccertifyInvokeHelper accertifyInvokeHelper;
    private CustomerDAO customerDao;
    private ApplicationProperties appProps;
    private RoomProgramCacheService programCacheService;
    private IDMSTokenDAO idmsTokenDAO;
    private PaymentDAO paymentDao;
    
    /**
     * Constructor which also injects all the dependencies. Using constructor based
     * injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     * 
     * @param urlProperties         URL Properties
     * @param domainProperties      Domain Properties
     * @param applicationProperties Application Properties
     * @param builder               Spring's auto-configured rest template builder
     * @param accertifyInvokeHelper Accertify Helper
     * @param customerDao           Customer Dao interface
     * @param appProps              Application properties
     * @throws SSLException Throws SSL Exception
     */
    public ModifyReservationDAOStrategyGSEImpl(URLProperties urlProperties, DomainProperties domainProperties,
                                               ApplicationProperties applicationProperties, RestTemplateBuilder builder,
                                               AccertifyInvokeHelper accertifyInvokeHelper, CustomerDAO customerDao,
                                               ApplicationProperties appProps, RoomProgramCacheService programCacheService,
                                               IDMSTokenDAO idmsTokenDAO, PaymentDAO paymentDao) throws SSLException {
        super();
        this.urlProperties = urlProperties;
        this.domainProperties = domainProperties;
        this.client = CommonUtil.getRetryableRestTemplate(builder, true, true,applicationProperties.getConnectionPerRouteDaoImpl(),
                applicationProperties.getMaxConnectionPerDaoImpl(),
                applicationProperties.getConnectionTimeout(),
                applicationProperties.getReadTimeOut(),
                applicationProperties.getSocketTimeOut(),1,applicationProperties.getCrsRestTTL());

        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
        this.accertifyInvokeHelper = accertifyInvokeHelper;
        this.customerDao = customerDao;
        this.appProps = appProps;
        this.programCacheService = programCacheService;
        this.idmsTokenDAO = idmsTokenDAO;
        this.paymentDao = paymentDao;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.mgm.services.booking.room.dao.ModifyReservationDAO#
     * preModifyReservation(
     * com.mgm.services.booking.room.model.request.PreModifyRequest)
     */
    @Override
    public RoomReservation preModifyReservation(PreModifyRequest preModifyRequest) {

        final EditReservationRequest request = EditReservationRequestTransformer
                .getEditReservationRequest(preModifyRequest);

        log.info("Sent the request to getModifiedReservation as : {}", CommonUtil.objectToJson(request));

        EditReservationResponse response = client
                .postForEntity(domainProperties.getOrms().concat(urlProperties.getRoomReservationModify()), request,
                        EditReservationResponse.class)
                .getBody();

        RoomReservation reservation = response.getModifiedReservation();
        log.info("Received the response to getModifiedReservation as : {}", CommonUtil.objectToJson(reservation));

        if (preModifyRequest.getFirstName().equalsIgnoreCase(reservation.getProfile().getFirstName())
                && preModifyRequest.getLastName().equalsIgnoreCase(reservation.getProfile().getLastName())) {
            replaceCCToken(reservation);
            return reservation;
        } else {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ModifyReservationDAO#modifyReservation(
     * java.lang.String,
     * com.mgm.services.booking.room.model.reservation.RoomReservation)
     */
    @Override
    public RoomReservation modifyReservation(String source, RoomReservation reservation) {

        final ModifyRoomReservationRequest auroraRequest = MessageFactory.createModifyRoomReservationRequest();

        com.mgmresorts.aurora.common.RoomReservation auroraRoomReservation = CommonUtil.copyProperties(reservation,
                com.mgmresorts.aurora.common.RoomReservation.class);

        auroraRequest.setReservation(auroraRoomReservation);
        auroraRequest.setItineraryId(reservation.getItineraryId());
        auroraRequest.setCustomerId(reservation.getProfile().getId());

        log.info("Sent the request to modifyRoomReservation as : {}", auroraRequest.toJsonString());
        final ModifyRoomReservationResponse response = getAuroraClient(source).modifyRoomReservation(auroraRequest);
        log.info("Received the response to modifyRoomReservation as : {}", response.toJsonString());

        for (com.mgmresorts.aurora.common.RoomReservation roomReservation : response.getItinerary()
                .getRoomReservations()) {
            if (roomReservation.getId().equals(reservation.getId())) {

                reservation = CommonUtil.copyProperties(roomReservation, RoomReservation.class);
                return reservation;
            }
        }
        log.info("Reservation not found after modification");
        throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ModifyReservationDAO#updateProfileInfo(com.
     * mgm. services.booking.room.model.request.dto.UpdateProfileInfoRequestDTO)
     */
    @Override
    @LogExecutionTime
    public RoomReservation updateProfileInfo(UpdateProfileInfoRequestDTO requestDTO) {

        // Retrieve Reservation (provided in requestDTO.getOriginalReservation()
        RoomReservation originalReservation = requestDTO.getOriginalReservation();
        String operaConfirmationNumber = originalReservation.getOperaConfirmationNumber();

        // If move itinerary required - Mlife associate flow only
        if (requestDTO.isMoveItinerary()) {
            // if mlife has changed from original reservation, retrieve
            // customer id from profile
            if (isMlifeChanged(requestDTO, originalReservation)) {

                GetCustomerByMlifeNoRequest getCustomerByMlifeNoRequest = MessageFactory
                        .createGetCustomerByMlifeNoRequest();
                getCustomerByMlifeNoRequest.setMlifeNo(requestDTO.getUserProfile()
                        .getMlifeNo());

                Customer customer = customerDao.getCustomer(String.valueOf(requestDTO.getUserProfile()
                        .getMlifeNo()));
                requestDTO = requestDTO.toBuilder()
                        .userProfile(CustomerProfileTransformer.convert(customer))
                        .build();

            }

            // If customerId has changed, add Retrieved Room Reservation
            // to new Customer Profile's Itineraries
            if (isCustomerIdChanged(requestDTO, originalReservation)) {

                AddCustomerItineraryRequest addCustomerItineraryRequest = MessageFactory
                        .createAddCustomerItineraryRequest();
                addCustomerItineraryRequest.setCustomerId(requestDTO.getUserProfile()
                        .getId());

                final TripParams tripParams = TripParams.create();
                tripParams.setArrivalDate(originalReservation.getCheckInDate());
                tripParams.setDepartureDate(originalReservation.getCheckOutDate());
                tripParams.setNumAdults(originalReservation.getNumAdults());
                tripParams.setNumChildren(originalReservation.getNumChildren());

                ItineraryData itineraryData = new ItineraryData();
                originalReservation
                        .setProfile(CommonUtil.copyProperties(requestDTO.getUserProfile(), ReservationProfile.class));
                itineraryData.addRoomReservations(CommonUtil.copyProperties(originalReservation,
                        com.mgmresorts.aurora.common.RoomReservation.class));
                itineraryData.setTripParams(tripParams);
                addCustomerItineraryRequest.setItinerary(itineraryData);
                return addCustomerItinerary(addCustomerItineraryRequest, requestDTO.getSource(),
                        operaConfirmationNumber);

            } else {
                // If there's no customerId change, simply update the
                // profile on existing reservation
                ModifyProfileRoomReservationRequest modifyProfileReservationRequest = RoomReservationTransformer
                        .createAuroraModifyProfileRoomReservationRequest(requestDTO);

                return modifyProfileRoomReservation(modifyProfileReservationRequest, requestDTO.getSource(),
                        operaConfirmationNumber);
            }
        } else {
            // If there's no itinerary move (ICE update profile flow) - simply update the profile attributes
            ModifyProfileRoomReservationRequest modifyProfileReservationRequest = RoomReservationTransformer
                    .createAuroraModifyProfileRoomReservationRequest(requestDTO);

            return modifyProfileRoomReservation(modifyProfileReservationRequest, requestDTO.getSource(),
                    operaConfirmationNumber);
        }

    }
    
    private boolean isMlifeChanged(UpdateProfileInfoRequestDTO requestDTO, RoomReservation originalReservation) {
        return requestDTO.getUserProfile()
                .getMlifeNo() != originalReservation.getProfile()
                        .getMlifeNo()
                && requestDTO.getUserProfile()
                        .getMlifeNo() > 0;
    }
    
    private boolean isCustomerIdChanged(UpdateProfileInfoRequestDTO requestDTO, RoomReservation originalReservation) {
        return requestDTO.getUserProfile()
                .getId() != originalReservation.getProfile()
                        .getId();
    }

    private RoomReservation addCustomerItinerary(AddCustomerItineraryRequest request, String source, String operaConfirmationNumber){
        RoomReservation roomReservation = null;
        try{
            log.info("Sent the request to addCustomerItinerary as : {}",
                     request.toJsonString());
            AddCustomerItineraryResponse response = getAuroraClient(source)
                    .addCustomerItinerary(request);
            log.info("Received the response from addCustomerItinerary as : {}", response.toJsonString());
            roomReservation = getDesiredRoomReservationByOperaConfirmationNumber(response.getItinerary().getRoomReservations(), operaConfirmationNumber);
        } catch (EAuroraException eAuroraException){
            log.error("Exception during ModifyProfile flow from addCustomerItinerary API: ", eAuroraException);
            handleAuroraError(eAuroraException);
        }
        return roomReservation;
    }

    private RoomReservation modifyProfileRoomReservation(ModifyProfileRoomReservationRequest request, String source, String operaConfirmationNumber){
        RoomReservation updatedReservation = null;
        try {
            log.info("Sent the request to modifyProfileRoomReservation as : {}",
                     request.toJsonString());
            ModifyProfileRoomReservationResponse response = getAuroraClient(source)
                    .modifyProfileRoomReservation(request);
            log.info("Received the response from modifyProfileRoomReservation as : {}", response.toJsonString());
            if (null != response.getItinerary() && response.getItinerary().getRoomReservations().length != 0) {
                updatedReservation = getDesiredRoomReservationByOperaConfirmationNumber(response.getItinerary().getRoomReservations(),
                                                               operaConfirmationNumber);
            }
        } catch (EAuroraException ex) {
            log.error("Exception trying to modify Profile in RoomReservation : ", ex);
            handleAuroraError(ex);
        }
        return updatedReservation;
    }

    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {

            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {

            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.info("Error: Status Code: {}, Response: {}", httpResponse.getStatusCode(), response);
            if (response.contains("<UnableToPriceTrip>")) {
                throw new BusinessException(ErrorCode.DATES_UNAVAILABLE);
            } else if (response.contains("<BusinessRuleViolation>")) {
                if (response.contains("component ids are not applicable for this modification")) {
                    throw new BusinessException(ErrorCode.UNSUPPORTED_COMPONENTS);
                } else {
                    throw new BusinessException(ErrorCode.MODIFY_VIOLATION);
                }
            } else if (response.contains("<InvalidReservationState>")) {
                throw new BusinessException(ErrorCode.UNMODIFIABLE_RESV_STATUS);
            } else if (httpResponse.getStatusCode().equals(HttpStatus.NOT_FOUND)
                    || response.contains("<BookingNotFound>")) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else {
                throw new SystemException(ErrorCode.UNABLE_TO_PRICE, null);
            }

        }
    }

    @Override
    @LogExecutionTime
    public RoomReservation modifyRoomReservationV2(RoomReservation reservation) {
    	AuthorizationTransactionRequest authorizeRequest = null;
        boolean performAFScheck = false;
        if(!reservation.isSkipFraudCheck()) {
            performAFScheck = accertifyInvokeHelper.performAFSCheck(reservation);
        }
        if (performAFScheck && !reservation.isSkipPaymentProcess()) {
			authorizeRequest = accertifyInvokeHelper.createAuthorizeRequest(reservation,
					reservation.getInSessionReservationId());
			boolean authorized  = accertifyInvokeHelper.authorize(authorizeRequest);
			log.info("accertify {} the purchase", authorized ? "authorized" : "not authorized");
			
			if(!authorized) {
				throw new BusinessException(ErrorCode.TRANSACTION_NOT_AUTHORIZED);
			}
		}

        if (!reservation.isSkipPaymentProcess() && null != reservation && StringUtils.isNotEmpty(reservation.getPropertyId()) &&
                reservation.getPropertyId().equalsIgnoreCase(appProps.getTcolvPropertyId())) {
            boolean isEnableZeroAmountAuthTCOLV = accertifyInvokeHelper.isEnableZeroAmountAuthTCOLVModify();
            if (isEnableZeroAmountAuthTCOLV) {
                for (CreditCardCharge creditCardCharge : reservation.getCreditCardCharges()) {
                    makeZeroDollarAuthPaymentTCOLV(reservation.getCheckInDate(), reservation.getCheckOutDate(),
                            creditCardCharge, reservation.getPropertyId(), reservation.getSource());
                }
            }
        }

        ModifyRoomReservationRequest modifyRoomReservationRequest = MessageFactory.createModifyRoomReservationRequest();

        com.mgmresorts.aurora.common.RoomReservation auroraRoomReservation = CommonUtil.copyProperties(reservation,
                com.mgmresorts.aurora.common.RoomReservation.class);

        modifyRoomReservationRequest.setReservation(auroraRoomReservation);
        modifyRoomReservationRequest.setItineraryId(reservation.getItineraryId());
        modifyRoomReservationRequest.setCustomerId(reservation.getProfile().getId());

        List<ReservationProfile> shareWithCustomersRequest = reservation.getShareWithCustomers();

        if (null != shareWithCustomersRequest) {
            shareWithCustomersRequest.stream()
                    .forEach(reservationRequest -> modifyRoomReservationRequest.addShareWithCustomers(CommonUtil
                            .copyProperties(reservationRequest, com.mgmresorts.aurora.common.CustomerProfile.class)));
        }

        log.info("Sent the request to modifyRoomReservation as : {}", modifyRoomReservationRequest.toJsonString());
        try {
            final ModifyRoomReservationResponse response = getAuroraClient(reservation.getSource())
                    .modifyRoomReservation(modifyRoomReservationRequest);
            log.info("Received the response to modifyRoomReservation as : {}", response.toJsonString());

            for (com.mgmresorts.aurora.common.RoomReservation roomReservation : response.getItinerary()
                    .getRoomReservations()) {
                if (roomReservation.getId().equals(reservation.getId())) {
                    reservation = CommonUtil.copyProperties(roomReservation, RoomReservation.class);
                    reservation.setCustomerId(response.getItinerary().getCustomerId());
                    if(null != authorizeRequest && null != authorizeRequest.getTransaction()) {
                        authorizeRequest.getTransaction().setOrderStatus(ServiceConstant.COMPLETED);
                        authorizeRequest.getTransaction().setProcessorResponseText(ServiceConstant.COMPLETED);
                    }
                    return reservation;
                }
            }
        } catch (EAuroraException ex) {
            log.error("Exception trying to modify room reservation : ", ex);
            if(null != authorizeRequest && null != authorizeRequest.getTransaction()){
                authorizeRequest.getTransaction().setOrderStatus(ServiceConstant.CANCELLED_BY_MERCHANT);
                authorizeRequest.getTransaction().setProcessorResponseText(ex.getMessage());
            }
            handleAuroraError(ex);
        } finally {
        	if(!reservation.isSkipPaymentProcess() && null != reservation) {
        		accertifyInvokeHelper.confirmAsyncCall(authorizeRequest, reservation.getConfirmationNumber());
        	}
        }
        log.info("Reservation not found after modification");
        throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ModifyReservationDAO#preModifyReservation(
     * com. mgm.services.booking.room.model.request.PreModifyV2Request)
     */
    @Override
    public RoomReservation preModifyReservation(PreModifyV2Request preModifyRequest) {

        final EditReservationRequest request = EditReservationRequestTransformer
                .getEditReservationRequest(preModifyRequest, appProps);

        log.info("Sent the request to Preview - getModifiedReservation as : {}", CommonUtil.objectToJson(request));

        EditReservationResponse response = client
                .postForEntity(domainProperties.getOrms().concat(urlProperties.getRoomReservationModify()), request,
                        EditReservationResponse.class)
                .getBody();

        RoomReservation reservation = response.getModifiedReservation();
        log.info("Received the response to Preview - getModifiedReservation as : {}",
                CommonUtil.objectToJson(reservation));
        //ORMS API does not handle Min Nights Validation, handling it here as a workaround
        Set<String> programIds = new HashSet<>();
        reservation.getBookings().forEach(booking -> programIds.add(booking.getProgramId()));
        if(programIds.size() == 1 && !programIds.contains(null)) {
        	RoomProgram program = programCacheService.getRoomProgram(programIds.iterator().next());
        	int numNights = Days.daysBetween(new DateTime(reservation.getCheckInDate()), new DateTime(reservation.getCheckOutDate())).getDays();
        	if(program !=null  && numNights < program.getMinNights()) {
        		throw new BusinessException(ErrorCode.DATES_UNAVAILABLE);
        	}
        }
        replaceCCToken(reservation);
        return reservation;
    }
    @Override
    public RoomReservation commitPaymentReservation(CommitPaymentDTO refundRoomReservationRequest){
        throw new SystemException(ErrorCode.SYSTEM_ERROR,new Throwable("GSE does not support this API"));
    }

    @Override
    public RoomReservation modifyPendingRoomReservationV2(RoomReservation reservation) {
        throw new SystemException(ErrorCode.SYSTEM_ERROR,new Throwable("GSE does not support this API"));
    }

    private void makeZeroDollarAuthPaymentTCOLV(Date checkInDate, Date checkOutDate, CreditCardCharge charge, String propertyId,
                                      String source) {
        final String idmsAuthToken = idmsTokenDAO.generateToken().getAccessToken();
        final AuthRequest paymentAuthReq = ReservationUtil.getAuthRequestTCOLV(checkInDate, checkOutDate, charge,
                referenceDataDAOHelper.retrieveMerchantID(propertyId));

        final HttpEntity<AuthRequest> httpRequest = new HttpEntity<>(paymentAuthReq,
                CommonUtil.createPaymentHeaders(idmsAuthToken, source));

        if (StringUtils.isEmpty(paymentAuthReq.getTransactionRefCode())) {
            if (!httpRequest.getHeaders().isEmpty() && httpRequest.getHeaders().containsKey(ServiceConstant.X_MGM_CORRELATION_ID)) {
                List<String> correlationIDList = httpRequest.getHeaders().get(ServiceConstant.X_MGM_CORRELATION_ID);
                if (org.apache.commons.collections.CollectionUtils.isNotEmpty(correlationIDList)) {
                    paymentAuthReq.setTransactionRefCode(correlationIDList.get(0));
                }
            }
        }

        AuthResponse response = null;
        try {
            response = paymentDao.authorizePayment(httpRequest);
        } catch (BusinessException e) {
            log.error("Business Exception during TCOLV zero dollar auth payment call: {}", e.getMessage());
        }

        if (response != null && response.getStatusMessage().equals(ServiceConstant.APPROVED)) {
            log.info("payment authorization successful for property ID {}", propertyId);
        } else {
            throw new BusinessException(ErrorCode.PAYMENT_AUTHORIZATION_FAILED);
        }
    }
}
