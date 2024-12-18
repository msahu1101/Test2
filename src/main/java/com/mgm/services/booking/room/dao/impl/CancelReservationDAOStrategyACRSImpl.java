package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.CancelReservationDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.exception.ACRSErrorUtil;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.loyalty.UpdatedPromotion;
import com.mgm.services.booking.room.model.request.CancelRequest;
import com.mgm.services.booking.room.model.request.CancelV2Request;
import com.mgm.services.booking.room.model.request.ReleaseV2Request;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.Deposit;
import com.mgm.services.booking.room.model.reservation.Payment;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.transformer.BaseAcrsTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
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
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementation class for CancelReservationDAOStrategy for all services related to
 * room reservation cancellation.
 */
@Component
@Log4j2
public class CancelReservationDAOStrategyACRSImpl extends BaseReservationDao implements CancelReservationDAOStrategy {

	private final PropertyConfig propertyConfig;
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
     * @param acrsProperties
     *            Acrs Properties   
     * @param builder
     *            Spring's auto-configured rest template builder
     * @throws SSLException
     *             Throws SSL Exception
     */
    @Autowired
    public CancelReservationDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties,
            ApplicationProperties applicationProperties, AcrsProperties acrsProperties, RestTemplateBuilder builder,
            ReferenceDataDAOHelper referenceDataDAOHelper, ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl,
            RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl, PropertyConfig propertyConfig) {
        super(urlProperties, domainProperties, applicationProperties, acrsProperties, 
        		CommonUtil.getRetryableRestTemplate(builder, applicationProperties.isSslInsecure(), acrsProperties.isLiveCRS(),
                        applicationProperties.getAcrsConnectionPerRouteDaoImpl(),
                        applicationProperties.getAcrsMaxConnectionPerDaoImpl(),
                        applicationProperties.getConnectionTimeoutACRS(),
                        applicationProperties.getReadTimeOutACRS(),
                        applicationProperties.getSocketTimeOutACRS(),
                          1,0,true),
                referenceDataDAOHelper, acrsOAuthTokenDAOImpl, roomPriceDAOStrategyACRSImpl);
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());
		this.propertyConfig = propertyConfig;
    }

    @Autowired
    private ApplicationProperties applicationProperties;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.mgm.services.booking.room.dao.CancelReservationDAO#cancelReservation(
     * com.mgm.services.booking.room.model.request.CancelRequest)
     */
    @Override
    public RoomReservation cancelReservation(CancelRequest cancelRequest, String propertyId) {

        log.info("Incoming Request for cancelReservation: {}", CommonUtil.convertObjectToJsonString(cancelRequest));
        CancelV2Request cancelV2Request = new CancelV2Request();
        cancelV2Request.setConfirmationNumber(cancelRequest.getConfirmationNumber());
        cancelV2Request.setSource(cancelRequest.getSource());
        cancelV2Request.setPropertyId(propertyId);
        return cancelReservation(cancelV2Request);
    }

    @Override
    public RoomReservation cancelReservation(CancelV2Request cancelRequest){
    	final RoomReservation cancelledRoomReservation = processCancelReservation(cancelRequest);

    	// Setting customer/itinerary/id fields from request object
        cancelledRoomReservation.setCustomerId(cancelRequest.getCustomerId());
        cancelledRoomReservation.setItineraryId(cancelRequest.getItineraryId());
        cancelledRoomReservation.setId(cancelRequest.getReservationId());
        // convert acrs to refData guid
        invokeRefDataRoutingInfo(cancelledRoomReservation.getRoutingInstructions(), cancelledRoomReservation.getPropertyId(),false);
        setReferenceData(cancelledRoomReservation);

        return cancelledRoomReservation;
    }

    public RoomReservation cancelReservationV4(CancelV2Request cancelRequest){
        final RoomReservation cancelledRoomReservation = processCancelReservationV4(cancelRequest);

        // Setting customer/itinerary/id fields from request object
        cancelledRoomReservation.setCustomerId(cancelRequest.getCustomerId());
        cancelledRoomReservation.setItineraryId(cancelRequest.getItineraryId());
        cancelledRoomReservation.setId(cancelRequest.getReservationId());
        // convert acrs to refData guid
        invokeRefDataRoutingInfo(cancelledRoomReservation.getRoutingInstructions(), cancelledRoomReservation.getPropertyId(),false);
        setReferenceData(cancelledRoomReservation);

        return cancelledRoomReservation;
    }

	private RoomReservation processCancelReservation(CancelV2Request cancelRequest) {
		if (log.isDebugEnabled()) {
            log.debug("Incoming Request for cancelReservationV2: {}", CommonUtil.convertObjectToJsonString(cancelRequest));
        }

        boolean isOverrideDepositForfeit = cancelRequest.isOverrideDepositForfeit();

        RoomReservation fetchedRoomReservation = cancelRequest.getExistingReservation();
		if (null == fetchedRoomReservation) {
			// Leaving this as v2 requests will not come with existingReservation as that api is deprecated
			ReservationRetrieveResReservation reservationRetrieveRes =
                    retrieveReservationByCnfNumber(cancelRequest.getConfirmationNumber(), cancelRequest.getSource());
			fetchedRoomReservation = RoomReservationTransformer.transform(reservationRetrieveRes, acrsProperties);
		}
        // This flag is required for gaming/non-gaming ACRS routing
        cancelRequest.setPerpetualPricing(fetchedRoomReservation.isPerpetualPricing());
        // Prepare refunds objects
        //Get the out standing amount to be return before processing refund. to handle bellow case
        // refund done but commit failed in first try.
        ReservationRetrieveResReservation pendingResvRes = retrievePendingReservationByConfirmationNumber(fetchedRoomReservation.getConfirmationNumber(),cancelRequest.getSource());
        double outstandingRefund = calculateOutstandingRefund(cancelRequest,pendingResvRes);
        List<CreditCardCharge> refunds = null;
        if (fetchedRoomReservation.getPayments() != null && outstandingRefund > 0) {
            refunds = prepareRefunds(fetchedRoomReservation.getPayments(), fetchedRoomReservation.getDepositCalc(), isOverrideDepositForfeit, fetchedRoomReservation.getPropertyId());
        }
        // Refund the deposit payments
        List<Payment> refundPayments = null;

        if(!cancelRequest.isCancelPending()) {
        // Do A Partial Modify to add the refunds to the reservation in ACRS
        ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
        ModificationChanges modificationChanges = new ModificationChanges();

        if (refunds != null && !cancelRequest.isSkipPaymentProcess()) {
            //check if reservation is modifiable before doing actual refund
            if(isReservationCancellable(fetchedRoomReservation,cancelRequest.getSource())) {
                refundPayments = refundPayments(refunds, cancelRequest.getSource(), cancelRequest.getConfirmationNumber(), fetchedRoomReservation.getPropertyId());
            }
        }else{
            // Create refundPayment from request.billings for payment widget flow
            if(CollectionUtils.isNotEmpty(cancelRequest.getCreditCardCharges())){
                refundPayments =  cancelRequest.getCreditCardCharges().stream()
                        .map(x -> {
                            Payment refundPayment = createPaymentFromCreditCardCharge(x);
                            refundPayment.setTransactionId(x.getAuthId());
                            refundPayment.setDccTransDate(new Date());
                            return refundPayment;
                        }).collect(Collectors.toList());
            }
        }

            List<ModificationChangesItem> modificationChangesList = createModificationChangesForSettledRefunds(refunds, refundPayments);
            modificationChanges.addAll(modificationChangesList);
            // If there is at least 1 refund to add to the reservation then do a partial modify to add the refund
            if ( !modificationChanges.isEmpty()) {
                reservationPartialModifyReq.setData(modificationChanges);

                invokePartialModifyPendingRoomReservation(reservationPartialModifyReq, fetchedRoomReservation.getConfirmationNumber(), fetchedRoomReservation.getPropertyId(), null, null, cancelRequest.getSource());

                // Commit modification
                //temp make commit fail
                //List<Integer> list = new ArrayList<>();
                //list.get(0);
                // temp end
                try {
                    commitPendingRoomReservation(fetchedRoomReservation.getConfirmationNumber(), fetchedRoomReservation.getPropertyId(), cancelRequest.getSource(), cancelRequest.isPerpetualPricing());
                }
                catch (ParseException e) {
                    log.error("Failed to parse ACRS reservation during modify commit while adding refund to reservation.");
                    throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
                }
            }else if(ReservationUtil.isImageInPendingState(pendingResvRes)){
                // Commit modification
                try {
                    commitPendingRoomReservation(fetchedRoomReservation.getConfirmationNumber(), fetchedRoomReservation.getPropertyId(), cancelRequest.getSource(), cancelRequest.isPerpetualPricing());
                }
                catch (ParseException e) {
                    log.error("Failed to parse ACRS reservation during modify commit while adding refund to reservation.");
                    throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
                }
            }
        }

        RoomReservation cancelledRoomReservation;
        // do a cancel pending to cancel the reservation
        try {
            cancelledRoomReservation = cancelPendingRoomReservation(cancelRequest);
        } catch (ParseException e) {
            log.error("Failed to parse ACRS reservation during cancel pending.");
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
        boolean hasRefund = CollectionUtils.isNotEmpty(refunds);
        // If its payment widget flow and there is a refund then skip commit and update promo
        if( !cancelRequest.isCancelPending() || !hasRefund ) {
            try {
                cancelledRoomReservation = commitPendingRoomReservation(cancelRequest.getConfirmationNumber(), fetchedRoomReservation.getPropertyId(), cancelRequest.getSource(), cancelRequest.isPerpetualPricing());
            } catch (ParseException e) {
                log.error("Failed to parse ACRS reservation during confirm cancel. The reservation may have been successfully cancelled.");
                throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
            }

            // Update Patron Promo Status
            int mlifeNo = (null != fetchedRoomReservation.getProfile()) ? fetchedRoomReservation.getProfile().getMlifeNo() : -1;
            if (StringUtils.isNotBlank(fetchedRoomReservation.getPromo()) && mlifeNo > 0) {
                String propertyCode =
                        getPropertyCodeForPromo(referenceDataDAOHelper.retrieveAcrsPropertyID(fetchedRoomReservation.getPropertyId()));
                Optional<UpdatedPromotion> promoToRemove =
                        RoomReservationTransformer.getPatronPromoToRemove(fetchedRoomReservation.getPromo(),
                                propertyCode, mlifeNo,
                                fetchedRoomReservation.getConfirmationNumber());
                if (promoToRemove.isPresent()) {
                    List<UpdatedPromotion> promos = new ArrayList<>();
                    promos.add(promoToRemove.get());
                    updatePatronPromo(promos, cancelRequest.getConfirmationNumber());
                }
            }
        }
		//If payment widget cancelPending =true && hasRefund then ignore the reservation
        if(cancelRequest.isCancelPending() && hasRefund){
            ignorePendingRoomReservation(cancelRequest.getConfirmationNumber(), referenceDataDAOHelper.retrieveAcrsPropertyID(cancelRequest.getPropertyId()), cancelRequest.getSource(), cancelRequest.isPerpetualPricing());
        }
        // Set payments information from fetched before response
		if (null != refundPayments) {
			List<Payment> newPayments = new ArrayList<>(fetchedRoomReservation.getPayments());
			newPayments.addAll(refundPayments);
			cancelledRoomReservation.setPayments(newPayments);
		} else {
			cancelledRoomReservation.setPayments(fetchedRoomReservation.getPayments());
		}
		cancelledRoomReservation.setCreditCardCharges(fetchedRoomReservation.getCreditCardCharges());
		cancelledRoomReservation.setAmountDue(fetchedRoomReservation.getAmountDue());
		return cancelledRoomReservation;
	}

    private double calculateOutstandingRefund(CancelV2Request cancelRequest, ReservationRetrieveResReservation pendingResvRes){
        //Get pending reservation to check if refund already done or not
        //In first fetch we can't this call as it is directly CRS not through PET.
        SegmentRes segments = pendingResvRes.getData().getHotelReservation().getSegments();
        SegmentResItem mainSegment = BaseAcrsTransformer.getMainSegment(segments);
        List<PaymentTransactionRes> depositPayments = mainSegment.getFormOfPayment().getDepositPayments();
        double outstandingRefund = 0.0;
        if(CollectionUtils.isNotEmpty(depositPayments)) {
            List<String> depositList = depositPayments.stream().filter(x -> PaymentIntent.DEPOSIT.equals(x.getPaymentIntent())).map(PaymentTransactionRes::getAmount).collect(Collectors.toList());
            List<String> refundList = depositPayments.stream().filter(x -> PaymentIntent.REFUND.equals(x.getPaymentIntent())).map(PaymentTransactionRes::getAmount).collect(Collectors.toList());
            double depositAmt = depositList.stream().mapToDouble(Double::valueOf).sum();
            double refundAmt = refundList.stream().mapToDouble(Double::valueOf).sum();
            outstandingRefund = depositAmt-refundAmt;
        }
        return outstandingRefund;
    }

    private boolean isReservationCancellable(RoomReservation fetchedRoomReservation, String source) {
        //add Dummy comment
        ReservationPartialModifyReq reservationPartialModifyReqDummy = new ReservationPartialModifyReq();
        ModificationChanges modificationChangesDummy = new ModificationChanges();
        Comment comment = new Comment();
        comment.setText(Collections.singletonList("Cancel request received."));
        comment.setType(TypeOfComments.valueOf("SUPPLEMENTARY_INFO"));
        modificationChangesDummy.add(createModificationChange(ModificationChangesItem.OpEnum.APPEND,
                acrsProperties.getModifyCommentPath(), comment));
        reservationPartialModifyReqDummy.setData(modificationChangesDummy);
        return Optional.ofNullable(invokePartialModifyPendingRoomReservation(reservationPartialModifyReqDummy, fetchedRoomReservation.getConfirmationNumber(), fetchedRoomReservation.getPropertyId(), null, null, source)).isPresent();

    }
    private RoomReservation processCancelReservationV4(CancelV2Request cancelRequest) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for cancelReservationV2: {}", CommonUtil.convertObjectToJsonString(cancelRequest));
        }
        RoomReservation fetchedRoomReservation = cancelRequest.getExistingReservation();
        // This flag is required for gaming/non-gaming ACRS routing
        cancelRequest.setPerpetualPricing(fetchedRoomReservation.isPerpetualPricing());
        // Refund the  payments. This is required to set into response.
        List<Payment> refundPayments = null;
        if(CollectionUtils.isNotEmpty(cancelRequest.getCreditCardCharges())) {
            refundPayments = cancelRequest.getCreditCardCharges().stream()
                    .map(x -> {
                        Payment refundPayment = createPaymentFromCreditCardCharge(x);
                        refundPayment.setTransactionId(x.getAuthId());
                        if (null != x.getTxnDateAndTime()) {
                            refundPayment.setDccTransDate(x.getTxnDateAndTime());
                        } else {
                            refundPayment.setDccTransDate(new Date());
                        }
                        return refundPayment;
                    }).collect(Collectors.toList());
        }
        // Do A Partial Modify to add the refunds to the reservation in ACRS
        ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
        ModificationChanges modificationChanges = new ModificationChanges();
        RoomReservation cancelledRoomReservation;
        try {
            if (CollectionUtils.isNotEmpty(cancelRequest.getCreditCardCharges())) {
                // Create refundPayment from request.billings for payment widget flow
                Map<CreditCardCharge, String> creditCardChargeAuthorizationMap = createCreditCardChargeAuthorizationMap(cancelRequest.getCreditCardCharges());
                //Create ModificationChange for refundPayments.
                List<ModificationChangesItem> modificationChangesForRefundPaymentTxns = getModificationChangesForPaymentTxns(null,
                        creditCardChargeAuthorizationMap);
                modificationChanges.addAll(modificationChangesForRefundPaymentTxns);
                reservationPartialModifyReq.setData(modificationChanges);
                invokePartialModifyPendingRoomReservation(reservationPartialModifyReq, fetchedRoomReservation.getConfirmationNumber(), fetchedRoomReservation.getPropertyId(), null, null, cancelRequest.getSource());

                // Commit modification
                commitPendingRoomReservation(fetchedRoomReservation.getConfirmationNumber(), fetchedRoomReservation.getPropertyId(), cancelRequest.getSource(), cancelRequest.isPerpetualPricing());

            }

            // do a cancel pending to cancel the reservation

            cancelPendingRoomReservation(cancelRequest);
            cancelledRoomReservation = commitPendingRoomReservation(cancelRequest.getConfirmationNumber(), fetchedRoomReservation.getPropertyId(), cancelRequest.getSource(), cancelRequest.isPerpetualPricing());
        } catch (Exception e) {
            log.error("Error while cancelling the reservation {}. Hence ignoring the cancellation", e.getMessage());
            try {
                ignorePendingRoomReservation(cancelRequest.getConfirmationNumber(), fetchedRoomReservation.getPropertyId(), cancelRequest.getSource(), fetchedRoomReservation.isPerpetualPricing());
            }catch(Exception ex){
                log.error("Error during ignoring the reservation {}",ex.getMessage());
            }

            if(e instanceof BusinessException){
                throw new BusinessException(((BusinessException) e).getErrorCode(),e.getMessage());
            }else{
                throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
            }

        }
            // Update Patron Promo Status
            int mlifeNo = (null != fetchedRoomReservation.getProfile()) ? fetchedRoomReservation.getProfile().getMlifeNo() : -1;
            if (StringUtils.isNotBlank(fetchedRoomReservation.getPromo()) && mlifeNo > 0) {
                String propertyCode =
                        getPropertyCodeForPromo(referenceDataDAOHelper.retrieveAcrsPropertyID(fetchedRoomReservation.getPropertyId()));
                Optional<UpdatedPromotion> promoToRemove =
                        RoomReservationTransformer.getPatronPromoToRemove(fetchedRoomReservation.getPromo(),
                                propertyCode, mlifeNo,
                                fetchedRoomReservation.getConfirmationNumber());
                if (promoToRemove.isPresent()) {
                    List<UpdatedPromotion> promos = new ArrayList<>();
                    promos.add(promoToRemove.get());
                    updatePatronPromo(promos, cancelRequest.getConfirmationNumber());
                }
            }
            // Set payments information from fetched before response
            if (CollectionUtils.isNotEmpty(refundPayments)) {
                List<Payment> newPayments = new ArrayList<>(fetchedRoomReservation.getPayments());
                newPayments.addAll(refundPayments);
                cancelledRoomReservation.setPayments(newPayments);
            } else {
                cancelledRoomReservation.setPayments(fetchedRoomReservation.getPayments());
            }
            cancelledRoomReservation.setCreditCardCharges(fetchedRoomReservation.getCreditCardCharges());
            cancelledRoomReservation.setAmountDue(fetchedRoomReservation.getAmountDue());
            return cancelledRoomReservation;


    }

	private String getPropertyCodeForPromo(String propertyCode) {
		List<String> exceptionPropertiesCode = acrsProperties.getPseudoExceptionProperties();
			if(CollectionUtils.isNotEmpty(exceptionPropertiesCode) &&
					exceptionPropertiesCode.contains(propertyCode)){
				PropertyConfig.PropertyValue propertyValue = propertyConfig.getPropertyValuesMap().get(propertyCode);
				return Optional.ofNullable(propertyValue).map(PropertyConfig.PropertyValue::getMasterPropertyCode).orElse(null);
			}else{
				return propertyCode;
			}
	}

	@Override
	public boolean ignoreReservation(ReleaseV2Request cancelRequest) {
		if (log.isDebugEnabled()) {
			log.debug("Incoming Request for ignoreReservation: {}", CommonUtil.convertObjectToJsonString(cancelRequest));
		}
        // For F1 release without confirmation number
        if (cancelRequest.isF1Package() &&
                StringUtils.isEmpty(cancelRequest.getConfirmationNumber())) {
            return true;
        } else {
            return ignorePendingRoomReservation(cancelRequest.getConfirmationNumber(), referenceDataDAOHelper.retrieveAcrsPropertyID(cancelRequest.getPropertyId()), cancelRequest.getSource(), cancelRequest.isPerpetualPricing());
        }
	}

    private List<CreditCardCharge> prepareRefunds(List<Payment> payments, Deposit depositCalc,
                                                  boolean isOverrideDepositForfeit, String propertyId) {
        double forfeitRatio = ServiceConstant.ZERO_DOUBLE_VALUE;

        boolean forfeitDateCheck;
        if(null != depositCalc && null != depositCalc.getForfeitDate()){
            if (StringUtils.isNotEmpty(propertyId) && null != applicationProperties
                    && null != applicationProperties.getTimezone(propertyId)) {
                final String timezone = applicationProperties.getTimezone(propertyId);
                ZoneId propertyZone = ZoneId.of(timezone);
                LocalDateTime propertyDate = LocalDateTime.now(propertyZone);
                forfeitDateCheck = propertyDate.isBefore(
                        depositCalc.getForfeitDate().toInstant().atZone(propertyZone).toLocalDateTime());
            } else {
                forfeitDateCheck = LocalDateTime.now().isBefore(
                        ReservationUtil.convertDateToLocalDateTime(depositCalc.getForfeitDate()));
            }
            // Need to set forfeitRatio if not overridden and now() is after the forfeitDate
            if (!isOverrideDepositForfeit && !forfeitDateCheck) {
                // Need to forfeit the forfeit Amount
                // Sum up all payments so it includes all previous deposits and refunds to determine the ratio
                // of how much of the forfeit is to be applied to each card
                double depositAmount = payments.stream().mapToDouble(Payment::getChargeAmount).sum();
                forfeitRatio = depositCalc.getForfeitAmount() / depositAmount;
            }
        }

        // if forfeitRatio is >= 1.0 then there is no refunds to be processed
        if (forfeitRatio > 1.0 || Math.abs(forfeitRatio - 1.0) <= 0.000001) {
            return new ArrayList<>();
        }

        final double forfeitRatioFinal = forfeitRatio;

        Map<String, Double> paymentMap = payments.stream().filter(x -> StringUtils.isNotEmpty(x.getChargeCardNumber()))
                .collect(Collectors.groupingBy(Payment::getChargeCardNumber,
                        Collectors.summingDouble(Payment::getChargeAmount)));
        // Create new list of refunds
        List<CreditCardCharge> refunds = payments.stream()
                .filter(x -> StringUtils.isNotEmpty(x.getChargeCardNumber()))
                .filter(payment -> payment.getChargeAmount() >= 0.01)
                .filter(payment -> paymentMap.get(payment.getChargeCardNumber()) >= 0.01)
                .filter(distinctByKey(Payment::getChargeCardNumber))
                .map(payment -> createCreditCardChargeFromPayment(payment, paymentMap.get(payment.getChargeCardNumber())))
                .collect(Collectors.toList());

        // Update the amounts on each refund to subtract any forfeit and then negate.
        refunds.forEach(creditCardCharge -> creditCardCharge.setAmount(
                calculateRefundAmountWithForfeit(creditCardCharge.getAmount(), forfeitRatioFinal)));

        return refunds;
    }
    public static <T> Predicate<T> distinctByKey(Function<? super T,Object> keyExtractor) {
        Map<Object,Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private double calculateRefundAmountWithForfeit(double amount, double forfeitRatioFinal) {    	
        BigDecimal amountBD = BigDecimal.valueOf(amount);
        BigDecimal forfeitRatio = BigDecimal.valueOf(forfeitRatioFinal);
        BigDecimal forfeitAmount = amountBD.multiply(forfeitRatio).setScale(2, RoundingMode.HALF_UP);        
        BigDecimal refundAmount = amountBD.subtract(forfeitAmount).negate().setScale(2, RoundingMode.HALF_UP);
        return refundAmount.doubleValue();        
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
           ACRSErrorDetails acrsError = ACRSErrorUtil.getACRSErrorDetailsFromACRSErrorRes(response);
            if ( httpResponse.getStatusCode().value() >= 500 ) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }else if (response.contains("Reservation is already cancelled.") || response.contains("There is no pending image to be committed.")) {
                throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
            }else if (response.contains("No reservation available for the input confirmation number.")) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            }else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION,acrsError.getTitle());
            }            
        }
    }

}
