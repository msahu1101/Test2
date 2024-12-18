package com.mgm.services.booking.room.dao.impl;
import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ReservationDAOStrategy;
import com.mgm.services.booking.room.dao.RoomPriceDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.exception.ACRSErrorUtil;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.request.AuroraPriceRequest;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.request.dto.RoomProgramsResponseDTO;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.FailedReservation;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.transformer.BaseAcrsTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.sql.Array;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation class for ReservationDAO providing functionality of
 * creating/updating room reservation objects.
 *
 */
@Component
@Log4j2
public class ReservationDAOStrategyACRSImpl extends BaseReservationDao implements ReservationDAOStrategy {
    private final RoomPriceDAO roomPriceDAO;
    
    @Autowired
    private ModifyReservationDAOStrategyACRSImpl modifyReservationDAOStrategyACRSImpl;
    
    /**
     * Constructor which also injects all the dependencies. Using constructor
     * based injection since spring's auto-configured WebClient. Builder is not
     * thread-safe and need to get a new instance for each injection point.
     *
     * @param urlProperties
     *            URL Properties
     * @param domainProperties
     *            Domain Properties
     * @param applicationProperties
     *            Application Properties
     * @param builder
     *            Spring's auto-configured rest template builder
     * @throws SSLException
     *             Throws SSL Exception
     */
    @Autowired
    public ReservationDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties,
                                          ApplicationProperties applicationProperties, RestTemplateBuilder builder,
                                          RoomPriceDAO roomPriceDAO, AcrsProperties acrsProperties,
                                          ReferenceDataDAOHelper referenceDataDAOHelper,
                                          ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl,
                                          RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl) throws SSLException {
        super(urlProperties, domainProperties, applicationProperties, acrsProperties,
                CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),
                        applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
                        applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
                        applicationProperties.getConnectionTimeoutACRS(),
                        applicationProperties.getReadTimeOutACRS(),
                        applicationProperties.getSocketTimeOutACRS(),
                        1,
                        applicationProperties.getCrsRestTTL()),
                referenceDataDAOHelper, acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);
        this.roomPriceDAO = roomPriceDAO;        
        this.client.setErrorHandler(new ReservationDAOStrategyACRSImpl.RestTemplateResponseErrorHandler());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ReservationDAO#prereserveRoom(com.mgm.
     * services.booking.room.model.request.PreReserveRequest)
     */
    @Override
    public RoomReservation prepareRoomCartItem(RoomCartRequest prereserveRequest) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for prepareRoomCartItem: {}", CommonUtil.convertObjectToJsonString(prereserveRequest));            
        }
        
        // Get Pricing
        AuroraPriceRequest acrsPriceRequest = AuroraPriceRequest.builder()
                .checkInDate(prereserveRequest.getCheckInDate())
                .checkOutDate(prereserveRequest.getCheckOutDate())
                .roomTypeIds(Arrays.asList(prereserveRequest.getRoomTypeId()))
                .programId(prereserveRequest.getProgramId())
                .numGuests(prereserveRequest.getNumGuests())
                .propertyId(referenceDataDAOHelper.retrieveAcrsPropertyID(prereserveRequest.getPropertyId()))
                .build();

        List<AuroraPriceResponse> acrsPricingResponse = roomPriceDAO.getRoomPrices(acrsPriceRequest);

        // Create Reservation with Pricing invformation
        RoomReservation preparedRoomReservation = new RoomReservation();
        preparedRoomReservation.setBookDate(new Date());
        preparedRoomReservation.setCheckInDate(Date.from(prereserveRequest.getCheckInDate().atStartOfDay().atZone(ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE)).toInstant()));
        preparedRoomReservation.setCheckOutDate(Date.from(prereserveRequest.getCheckOutDate().atStartOfDay().atZone(ZoneId.of(ServiceConstant.DEFAULT_TIME_ZONE)).toInstant()));
        preparedRoomReservation.setPropertyId(prereserveRequest.getPropertyId());
        preparedRoomReservation.setProgramId(prereserveRequest.getProgramId());
        preparedRoomReservation.setNumAdults(prereserveRequest.getNumGuests());
        preparedRoomReservation.setNumRooms(1);
        preparedRoomReservation.setRoomTypeId(prereserveRequest.getRoomTypeId());
        preparedRoomReservation.setBookings(prepareBookingsFromPriceResponse(acrsPricingResponse));

        // Update Charges/Deposit and return room reservation
        return updateRoomReservation(preparedRoomReservation);
    }

    private List<RoomPrice> prepareBookingsFromPriceResponse(List<AuroraPriceResponse> acrsPricingResponse) {
        // All prices available?
        if (acrsPricingResponse.stream().anyMatch(price -> Double.compare(price.getDiscountedPrice(), -1.0) == 0)) {
            throw new BusinessException(ErrorCode.ROOMTYPE_UNAVAILABLE);
        }

        return acrsPricingResponse.stream().map(ReservationDAOStrategyACRSImpl::prepareBookingFromRoomPrice)
                .collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.ReservationDAO#updateRoomReservation(
     * com.mgm.services.booking.room.model.reservation.RoomReservation)
     */
    @Override
    public RoomReservation updateRoomReservation(RoomReservation reservation) {

        log.info("Incoming Request for updateRoomReservation: {}", CommonUtil.convertObjectToJsonString(reservation));

        RoomReservation updatedRoomReservation = getRoomAndComponentCharges(reservation,false);
        if(null != updatedRoomReservation) {
            // Update fields with new values from updatedReservation
            reservation.setDepositPolicyCalc(updatedRoomReservation.getDepositPolicyCalc());
            reservation.setDepositCalc(updatedRoomReservation.getDepositCalc());
            reservation.setChargesAndTaxesCalc(updatedRoomReservation.getChargesAndTaxesCalc());
        }
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
        throw new UnsupportedOperationException("v1 APIs are not supported for ACRS properties.");
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
        if (StringUtils.isEmpty(reservation.getConfirmationNumber())) { // create
                                                                        // pending
            return makePendingRoomReservation(createPendingReservationReq(reservation), reservation.getPropertyId(),
                    reservation.getAgentInfo(), reservation.getRrUpSell(), reservation.getSource());
        } else { // modify pending
            ReservationRetrieveResReservation existingResv = retrievePendingReservationByConfirmationNumber(
                    reservation.getConfirmationNumber(),reservation.getSource());
            return partialModifyPendingRoomReservation(createPendingModifyReq(reservation, existingResv),
                    reservation.getConfirmationNumber(), reservation.getPropertyId(),
                    (null != reservation.getAgentInfo()) ? reservation.getAgentInfo().getAgentId() : null,
                    reservation.getRrUpSell(),reservation.getSource(), reservation.isPerpetualPricing());
        }

    }



    static class RestTemplateResponseErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
            return httpResponse.getStatusCode().isError();
        }

        @Override
        public void handleError(ClientHttpResponse httpResponse) throws IOException {
            String response = StreamUtils.copyToString(httpResponse.getBody(), Charset.defaultCharset());
            log.error("Error received Amadeus: header: {} body: {}", httpResponse.getHeaders().toString(), response);
            ThreadContext.put(ServiceConstant.HTTP_STATUS_CODE, String.valueOf(httpResponse.getStatusCode()));
            try {
                LocalDateTime start = LocalDateTime.parse(ThreadContext.get(ServiceConstant.TIME_TYPE));
                long duration = ChronoUnit.MILLIS.between(start, LocalDateTime.now());
                ThreadContext.put(ServiceConstant.DURATION_TYPE, String.valueOf(duration));
                log.info("Custom Dimensions updated after ACRS call");
            } catch (Exception e) {
                // Do nothing
            }
            //TODO handle errors
            ACRSErrorDetails acrsError = ACRSErrorUtil.getACRSErrorDetailsFromACRSErrorRes(response);
            if ( httpResponse.getStatusCode().value() >= 500 ) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            } else if (response.contains("<UnableToPriceTrip>")) {
                throw new BusinessException(ErrorCode.DATES_UNAVAILABLE);
            } else if (response.contains("<BookingNotFound>")) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION,acrsError.getTitle());
            }

        }
    }

	@Override
    public RoomReservation makeRoomReservationV2(RoomReservation reservation) {

         log.info("Incoming Request for makeRoomReservationV2: {}",
                    CommonUtil.convertObjectToJsonString(reservation));


        // Reset PO flag if promo booking or request does not have any PO rate plan.
        resetPerpetualPricing(reservation);

        if (StringUtils.isNotEmpty(reservation.getConfirmationNumber())) {
            reservation.setPendingReservation(true);
            return modifyReservationDAOStrategyACRSImpl.modifyRoomReservationV2(reservation);
        }

        String originalProgramId = reservation.getProgramId();
        boolean isCashPayment = isCashPayment(reservation.getGuaranteeCode(), reservation.getCreditCardCharges());
        //Deleting the logic for overriding card amount by deposit amount for card amount<deposit amount. ACRS paymentInfo.amount will be = ACRS deposit amount.
        //and actual card amount will be stored into payment history.

        // create acrs pending request without form of payment
        ReservationPendingReq reservationReq = createPendingReservationReq(reservation);
        // if shared reservation
        if (CollectionUtils.isNotEmpty(reservation.getShareWithCustomers())) {
            return makeSharedReservation(reservationReq, reservation, isCashPayment, originalProgramId);
        } else {
            return createReservation(reservationReq, reservation, isCashPayment, originalProgramId);
        }
    }

    @Override
    public PartyRoomReservation makePartyRoomReservation(RoomReservation reservation, boolean splitCreditCardDetails) {

         log.info("Incoming Request for makePartyRoomReservation: {}", CommonUtil.convertObjectToJsonString(reservation));


        PartyRoomReservation partyRoomReservation = new PartyRoomReservation();
        List<RoomReservation> successReservations = new ArrayList<>();
        List<FailedReservation> failedReservations = new ArrayList<>();
        String partyConfNumber = null;
        int requestedRooms = reservation.getNumRooms();
        
        if (CollectionUtils.isNotEmpty(reservation.getCreditCardCharges())) {
            // get(0) - Party doesn't support multiple credit cards.
            double totalCreditCardAmount = reservation.getCreditCardCharges().get(0).getAmount();

                reservation.getCreditCardCharges().get(0).setAmount(totalCreditCardAmount / requestedRooms);
            
        }
        
		for (int i = 1; i <= requestedRooms; i++)
        {
            RoomReservation resv = reservation;
            FailedReservation failedReservation = new FailedReservation();
            boolean failedFlag = false;
            //For 2nd iteration onwards when partyConfNumber is not null
            if (partyConfNumber != null) {
                updatePartyRequestGuidsToACRSFormat(resv, reservation);
                resv.setExtConfirmationNumber(partyConfNumber);
            }
            try{
            	
				if (!splitCreditCardDetails && i > 1) {				
					//Set DD for Child reservations when splitCreditCardDetails false
					resv.setGuaranteeCode(ServiceConstant.DD_CASH_TYPE_STRING);
					resv.setCreditCardCharges(null);
				}

            	resv.setNumRooms(1);
                resv = makeRoomReservationV2(resv);
                
                resv.setPartyConfirmationNumber(partyConfNumber);            

            } catch(BusinessException e){
                failedReservation.setErrorCode(ErrorCode.RESERVATION_NOT_SUCCESSFUL.toString());
                failedReservation.setDescription(e.getMessage());
                failedReservation.setExtInfo(ServiceConstant.PARTY_CONFIRMATION_NUMBER_PREFIX + ServiceConstant.MESSAGE_TYPE_ERROR);
                failedFlag = true;
                failedReservations.add(failedReservation);
                log.error(e.getMessage());                
            }
            
            //Adding confirmation number check, to break the flow if 1st iteration fails.
            if(resv != null && StringUtils.isBlank(resv.getConfirmationNumber())) {
            	break;
            }
            
            //For 1st iteration when partyConfNumber is null            
            if (partyConfNumber == null) {
                try {
                    partyConfNumber = addPartyCnfNumber(resv,reservation.getSource(),resv.isPerpetualPricing());
                } catch (ParseException exception) {
                    failedReservation.setErrorCode(ErrorCode.RESERVATION_NOT_SUCCESSFUL.toString());
                    failedReservation.setDescription(exception.getMessage());
                    failedReservation.setExtInfo(ServiceConstant.PARTY_CONFIRMATION_NUMBER_PREFIX + ServiceConstant.MESSAGE_TYPE_ERROR);
                    failedFlag = true;
                    failedReservations.add(failedReservation);                    
                    log.debug(exception.getMessage());
                }
            }
            if (!failedFlag) {
                successReservations.add(resv);
            }
        }

        partyRoomReservation.setRoomReservations(successReservations);
        partyRoomReservation.setFailedReservations(failedReservations);
        return partyRoomReservation;
    }

    @Override
    public PartyRoomReservation makePartyRoomReservationV4(RoomReservation reservation, boolean splitCreditCardDetails) {
        log.info("Incoming Request for makePartyRoomReservationV4: {}", CommonUtil.convertObjectToJsonString(reservation));
        PartyRoomReservation partyRoomReservation = new PartyRoomReservation();
        List<RoomReservation> successReservations = new ArrayList<>();
        List<FailedReservation> failedReservations = new ArrayList<>();
        int requestedRooms = reservation.getNumRooms();
        List<CreditCardCharge> bills = reservation.getCreditCardCharges();
        if(CollectionUtils.isNotEmpty(bills) && splitCreditCardDetails){
            double billsAmount = bills.get(0).getAmount();
            bills.get(0).setAmount(BigDecimal.valueOf(billsAmount).divide(BigDecimal.valueOf(requestedRooms), 2, RoundingMode.HALF_UP).doubleValue());
        }
        //@TODO set skip payment flags temporary when makeReservationV4 is ready we can remove this
        reservation.setSkipFraudCheck(true);
        reservation.setSkipPaymentProcess(true);
        //create primary reservation
        String authId = getPartyPaymentAuthId(reservation.getCreditCardCharges(),0);
        RoomReservation primaryResv = createReservationForParty(reservation, null, splitCreditCardDetails,authId);
        successReservations.add(primaryResv);
        String partyConfNumber = primaryResv.getPartyConfirmationNumber();
        //create others reservations in async
        if(requestedRooms >1){
            createSecondaryPartyReservations(reservation, partyConfNumber,requestedRooms, splitCreditCardDetails,
                    successReservations,failedReservations);
        }
        partyRoomReservation.setFailedReservations(failedReservations);
        partyRoomReservation.setRoomReservations(successReservations);
        return partyRoomReservation;
    }

    private String getPartyPaymentAuthId(List<CreditCardCharge> creditCardCharges, int i) {
        String authId = null;
        if(CollectionUtils.isNotEmpty(creditCardCharges) && CollectionUtils.isNotEmpty(creditCardCharges.get(0).getAuthIds()) &&
                creditCardCharges.get(0).getAuthIds().size() >= i+1){
            authId = creditCardCharges.get(0).getAuthIds().get(i);
        }
        return authId;
    }

    private RoomReservation createReservationForParty(final RoomReservation reservation,String partyCnfNumber,boolean splitCreditCardDetails, String paymentAuthId){
        RoomReservation resv = new RoomReservation();
        RoomReservation partyResv = null;
        BeanUtils.copyProperties(reservation,resv);
        List<RoomPrice> bookings = reservation.getBookings();
        //copy booking as mutable
        if(CollectionUtils.isNotEmpty(bookings)){
            resv.setBookings(
                    bookings.stream()
                            .map(CommonUtil::copyRoomBooking)
                            .collect(Collectors.toList()));
        }
        resv.setExtConfirmationNumber(partyCnfNumber);
        resv.setNumRooms(1);
        if(!splitCreditCardDetails && StringUtils.isNotBlank(partyCnfNumber)){
            //Set DD for Child reservations when splitCreditCardDetails false
            resv.setGuaranteeCode(ServiceConstant.DD_CASH_TYPE_STRING);
            resv.setCreditCardCharges(null);
        }else if(StringUtils.isNotBlank(paymentAuthId) && CollectionUtils.isNotEmpty(resv.getCreditCardCharges())){
            //set paymentAuthId from authIds
            resv.getCreditCardCharges().get(0).setAuthId(paymentAuthId);
        }else if(CollectionUtils.isNotEmpty(resv.getCreditCardCharges()) && resv.getCreditCardCharges().get(0).getAmount()>0 &&
                StringUtils.isBlank(paymentAuthId)){
            throw new BusinessException(ErrorCode.INVALID_PAYMENT,"Payment authorization id is not provided.");
        }
        try {
             partyResv = makeRoomReservationV2(resv);
            //update partyCnf to the primary reservation
            if(StringUtils.isEmpty(partyCnfNumber)){
                String partyNumber = addPartyCnfNumber(partyResv, resv.getSource(), resv.isPerpetualPricing());
                partyResv.setPartyConfirmationNumber(partyNumber);
            }
        } catch (ParseException e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR,e);
        }
        return partyResv;
    }
    private void createSecondaryPartyReservations(RoomReservation reservation,String primaryCnfNumber, int count, boolean splitCreditCardDetails, List<RoomReservation> successReservations,List<FailedReservation> failedReservations){
        try {
            for(int i =1; i < count; i++) {
            	RoomReservation roomReservationResponse = createReservationForParty(reservation, primaryCnfNumber, splitCreditCardDetails,getPartyPaymentAuthId(reservation.getCreditCardCharges(),i));
            	successReservations.add(roomReservationResponse);
            }
        }catch (Exception ex) {
        	FailedReservation failedReservation = new FailedReservation();
            failedReservation.setErrorCode(ErrorCode.RESERVATION_NOT_SUCCESSFUL.toString());
            failedReservation.setDescription(ex.getMessage());
            failedReservation.setExtInfo(ServiceConstant.PARTY_CONFIRMATION_NUMBER_PREFIX + ServiceConstant.MESSAGE_TYPE_ERROR);
            failedReservations.add(failedReservation);
		}
    }
    private String  addPartyCnfNumber(RoomReservation resv,String source, boolean isPoFlow ) throws ParseException {
        String partyConfNumber = ServiceConstant.PARTY_CONFIRMATION_NUMBER_PREFIX + resv.getConfirmationNumber();
        resv.setPartyConfirmationNumber(partyConfNumber);
        ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
        ModificationChanges modificationChanges = new ModificationChanges();
        modificationChanges.add(createModificationChange(
                ModificationChangesItem.OpEnum.UPSERT, acrsProperties.getModifyPartyConfirmationNumberPath(), resv.getPartyConfirmationNumber()));
        reservationPartialModifyReq.setData(modificationChanges);
        partialModifyPendingRoomReservation(reservationPartialModifyReq,
                resv.getConfirmationNumber(), resv.getPropertyId(),
                (null != resv.getAgentInfo()) ? resv.getAgentInfo().getAgentId() : null,
                resv.getRrUpSell(),source, isPoFlow);
        commitPendingRoomReservation(resv.getConfirmationNumber(),
                resv.getPropertyId(), source, isPoFlow);
        return partyConfNumber;
    }
    private void updatePartyRequestGuidsToACRSFormat(RoomReservation resv , RoomReservation reservation){
            resv.setRoomTypeId(referenceDataDAOHelper.retrieveRoomTypeDetail(resv.getPropertyId(),
                    resv.getRoomTypeId()));
            if (null != resv.getProgramId()) {
                if (ACRSConversionUtil.isAcrsGroupCodeGuid(resv.getProgramId())) {
                    resv.setProgramId(
                            referenceDataDAOHelper.createFormatedGroupCode(resv.getPropertyId(), resv.getProgramId()));
                } else {
                    resv.setProgramId(referenceDataDAOHelper.retrieveRatePlanDetail(resv.getPropertyId(),
                            resv.getProgramId()));
                }
            }
            reservation.getBookings().forEach(booking -> {
                if (ACRSConversionUtil.isAcrsGroupCodeGuid(booking.getProgramId())) {
                    booking.setProgramId(referenceDataDAOHelper.createFormatedGroupCode(reservation.getPropertyId(),
                            booking.getProgramId()));
                } else {
                    booking.setProgramId(referenceDataDAOHelper.retrieveRatePlanDetail(reservation.getPropertyId(),
                            booking.getProgramId()));
                }
            });
    }
   
    private RoomReservation makeSharedReservation(ReservationPendingReq reservationReq,
                                                  RoomReservation reservation, boolean isCashPayment, String originalProgramId) {
        RoomReservation primaryReservation = null;

        String primaryConfirmationNumber = null;
        String linkedId = null;
        boolean isPoFlow = reservation.isPerpetualPricing();
        // 1. create primary reservation with main profile
        // change guest count to request guestCount-shareWithCount
        int primaryAdultCount = reservation.getNumAdults() - reservation.getShareWithCustomers().size();
        primaryAdultCount = primaryAdultCount >0 ? primaryAdultCount : 1;
        // update adultCount for all productUses.
        updateGuestCount(reservationReq,primaryAdultCount);
        primaryReservation = createReservation(reservationReq, reservation, isCashPayment, originalProgramId
        );
        primaryConfirmationNumber = primaryReservation.getConfirmationNumber();

        // override price to 0.0
        ReservationUtil.createOverrideShareWithPricing(reservationReq);

        // 2. create secondary reservations for shareWithCustomers
        List<RoomReservation> sharedReservations = createSecondaryReservations(reservationReq, reservation,
                originalProgramId);
        List<String> shareWiths = sharedReservations.stream().map(RoomReservation::getConfirmationNumber).collect(Collectors.toList());

        // 3. link the shared reservations
        LinkReservationRes linkRes = createSharedReservationLink(primaryConfirmationNumber, shareWiths,
                reservation.getPropertyId(), reservation.getSource(), isPoFlow);
        linkedId = linkRes.getData().getHotelLinkReservation().getLink().getId();
        // set response values
        primaryReservation.setPrimarySharerConfirmationNumber(primaryConfirmationNumber);
          primaryReservation.setShareWiths(shareWiths.toArray(new String[0]));
        primaryReservation.setShareWithReservations(sharedReservations);
        primaryReservation.setShareWithType(ShareWithType.Full);
        primaryReservation.setShareId(linkedId);
        primaryReservation.setShareWithCustomers(reservation.getShareWithCustomers());
        return primaryReservation;
    }

}
