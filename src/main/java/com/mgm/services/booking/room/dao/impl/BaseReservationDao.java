package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.*;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.dao.helper.ReservationDAOHelper;
import com.mgm.services.booking.room.mapper.RoomReservationPendingResMapper;
import com.mgm.services.booking.room.model.*;
import com.mgm.services.booking.room.model.crs.reservation.Email;
import com.mgm.services.booking.room.model.crs.reservation.Period;
import com.mgm.services.booking.room.model.crs.reservation.Warning;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.crs.searchoffers.*;
import com.mgm.services.booking.room.model.loyalty.UpdatedPromotion;
import com.mgm.services.booking.room.model.paymentservice.*;
import com.mgm.services.booking.room.model.profile.Profile;
import com.mgm.services.booking.room.model.refdata.*;
import com.mgm.services.booking.room.model.request.RoomRequest;
import com.mgm.services.booking.room.model.request.TripDetail;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.ACRSAuthTokenResponse;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.model.response.AuroraPricesResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.service.helper.AccertifyInvokeHelper;
import com.mgm.services.booking.room.transformer.*;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;
import com.mgm.services.common.util.BaseCommonUtil;
import com.mgm.services.common.util.DateUtil;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.NumberUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.abs;

@Log4j2
public abstract class BaseReservationDao extends BaseAcrsDAO {

	@Autowired
	@Setter
	private PaymentDAO paymentDao;
	@Autowired
	@Setter
	IDMSTokenDAO idmsTokenDAO;
	@Autowired
	@Setter
	private RoomReservationPendingResMapper pendingResMapper;
	@Autowired
	@Setter
	private RefDataDAO refDataDAO;
    @Autowired
    @Setter
    private OCRSDAO ocrsDao;
	@Autowired
	@Setter
	private LoyaltyDao loyaltyDao;
	@Autowired
	@Setter
  private RoomProgramDAO roomProgramDao;

	private final RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl;

	@Autowired
	protected AccertifyInvokeHelper accertifyInvokeHelper;
	@Autowired
	private ReservationDAOHelper reservationDAOHelper;
	private final ExecutorService executor = Executors.newCachedThreadPool();

	private static final String REQUEST_TOKEN_PATH = "$.data[%s].value.paymentCard.cardNumber.token";

	protected BaseReservationDao(URLProperties urlProperties, DomainProperties domainProperties,
			ApplicationProperties applicationProperties, AcrsProperties acrsProperties, RestTemplate restTemplate, ReferenceDataDAOHelper referenceDataDAOHelper,
								 ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl, RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl) {
		super(urlProperties, domainProperties, applicationProperties, acrsProperties, restTemplate, referenceDataDAOHelper, acrsOAuthTokenDAOImpl);
		this.roomPriceDAOStrategyACRSImpl = roomPriceDAOStrategyACRSImpl;
	}

	private void makeAFSAuthorize(AuthorizationTransactionRequest authorizeRequest, String source) {
		final String authToken = idmsTokenDAO.generateToken().getAccessToken();
		HttpHeaders headers = CommonUtil.createPaymentHeaders(authToken, source);


		final HttpEntity<AuthorizationTransactionRequest> request = new HttpEntity<>(authorizeRequest,
				headers);
		final AuthorizationTransactionResponse authResponse = paymentDao.afsAuthorize(request);
		if(authResponse == null || !authResponse.isAuthorized()){
			throw new BusinessException(ErrorCode.TRANSACTION_NOT_AUTHORIZED);
		}
	}

	protected String makeAuthorizePayment(Date checkIn, Date checkOut, CreditCardCharge charge, String propertyId, String confirmationNumber,
			String source) {
		final String authToken = idmsTokenDAO.generateToken().getAccessToken();
		final AuthRequest authReq = new AuthRequest();
		authReq.setTransactionRefCode(confirmationNumber);
		authReq.setMerchantID(referenceDataDAOHelper.retrieveMerchantID(propertyId));
		String amount = String.valueOf(BaseCommonUtil.round(charge.getAmount(),ServiceConstant.TWO_DECIMAL_PLACES));
		//if amount = 0.0 and card type is amex then change amount to 0.01
		if(ServiceConstant.AMEX_TYPE.equalsIgnoreCase(charge.getType()) && charge.getAmount() ==0.0){
			authReq.setAmount("0.01");
		}else {
			authReq.setAmount(amount);
		}
		final PaymentMethods paymentMethods = new PaymentMethods();
		final PaymentMethodsCard card = new PaymentMethodsCard();
		card.setPaymentToken(charge.getNumber());
        card.setNameOnCard(charge.getHolder());
       	paymentMethods.setCard(card);
		authReq.setPaymentMethods(paymentMethods);
		authReq.setBillTo(new BillTo());
       	authReq.setHotelData(getHotelData(checkIn, checkOut, amount));

		final HttpEntity<AuthRequest> request = new HttpEntity<>(authReq,
				CommonUtil.createPaymentHeaders(authToken, source));

		final AuthResponse authResponse = paymentDao.authorizePayment(request);

		if (authResponse != null && authResponse.getStatusMessage().equals(ServiceConstant.APPROVED)) {
			log.info("payment authorization Successful");
			if(charge.getAmount() >= 0.01) {
				return authResponse.getAuthRequestId();
			}else{
				return ServiceConstant.COMP;
			}
		} else {
			throw new BusinessException(ErrorCode.PAYMENT_AUTHORIZATION_FAILED);
		}
	}

	protected void capturePayment(Date checkIn, Date checkOut, Entry<CreditCardCharge, String> entry, String propertyId, String confirmationNumber,
			String source) {
		final String authToken = idmsTokenDAO.generateToken().getAccessToken();
		final CaptureRequest capReq = new CaptureRequest();
		capReq.setTransactionRefCode(confirmationNumber);
		capReq.setMerchantID(referenceDataDAOHelper.retrieveMerchantID(propertyId));
		capReq.setAuthRequestId(entry.getValue());
        CreditCardCharge creditCardCharge = entry.getKey();
        String amount = String.valueOf(BaseCommonUtil.round(creditCardCharge.getAmount(),ServiceConstant.TWO_DECIMAL_PLACES));
		capReq.setAmount(amount);
        capReq.setHotelData(getHotelData(checkIn, checkOut, amount));
        PaymentMethodsNameOnCardOnly paymentMethodsNameOnCardOnly = new PaymentMethodsNameOnCardOnly();
        PaymentMethodsNameOnCardOnlyCard card = new PaymentMethodsNameOnCardOnlyCard();
        card.setNameOnCard(creditCardCharge.getHolder());
        paymentMethodsNameOnCardOnly.setCard(card);
        capReq.setPaymentMethodsNameOnCardOnly(paymentMethodsNameOnCardOnly);
		final HttpEntity<CaptureRequest> request = new HttpEntity<>(capReq,
				CommonUtil.createPaymentHeaders(authToken, source));

		final CaptureResponse captureResponse = paymentDao.capturePayment(request);
		if (captureResponse.getStatusMessage().equals(ServiceConstant.APPROVED)) {
			log.info("payment capture Successful");
		} else {
			throw new BusinessException(ErrorCode.PAYMENT_CAPTURE_FAILED);
		}
	}

	private HotelData getHotelData(Date checkIn, Date checkOut, String amount) {
	    HotelData hotelData = new HotelData();
        hotelData.setRoomRate(amount);
        hotelData.setCheckinDate(BaseCommonUtil.getDateStr(checkIn, ServiceConstant.ISO_8601_DATE_FORMAT));
        hotelData.setCheckoutDate(BaseCommonUtil.getDateStr(checkOut, ServiceConstant.ISO_8601_DATE_FORMAT));
       return  hotelData;
	}

	protected List<Payment> refundPayments(List<CreditCardCharge> refunds, String source, String confirmationNumber, String propertyId) {
		final String authToken = idmsTokenDAO.generateToken().getAccessToken();
		final String merchantId = referenceDataDAOHelper.retrieveMerchantID(propertyId);
		return refunds.stream()
				.filter(refund -> refund.getAmount() != 0)
				.map(charge -> refundCreditCardCharge(charge, authToken, confirmationNumber, merchantId, source))
				.collect(Collectors.toList());
	}

	private Payment refundCreditCardCharge(CreditCardCharge creditCardCharge, String authToken, String confirmationNumber, String merchantId, String source) {
		final RefundRequest refReq = new RefundRequest();
		refReq.setTransactionRefCode(confirmationNumber);
		refReq.setMerchantID(merchantId);
		// Note: used absolute value as refund API wants positive values for refunds.
		refReq.setAmount(String.valueOf(BaseCommonUtil.round(abs(creditCardCharge.getAmount()),ServiceConstant.THREE_DECIMAL_PLACES)));
		final PaymentMethods paymentMethods = new PaymentMethods();
		final PaymentMethodsCard card = new PaymentMethodsCard();

		card.setPaymentToken(creditCardCharge.getNumber());
		if ( StringUtils.isNotBlank(creditCardCharge.getCvv())) {
			card.setCvv(Integer.parseInt(creditCardCharge.getCvv()));
		}
		LocalDate expiryLocalDate = ReservationUtil.convertDateToLocalDate(creditCardCharge.getExpiry());
		card.setExpirationMonth(expiryLocalDate.getMonthValue());
		card.setExpirationYear(expiryLocalDate.getYear());
		paymentMethods.setCard(card);
		refReq.setPaymentMethods(paymentMethods);

		final HttpEntity<RefundRequest> request = new HttpEntity<>(refReq, CommonUtil.createPaymentHeaders(authToken, source));

		final RefundResponse refundResponse = paymentDao.refundPayment(request);

		if (refundResponse.getStatusMessage().equals(ServiceConstant.APPROVED)) {
			log.info("Payment refund successful for card '{}' and amount '{}'", creditCardCharge.getMaskedNumber(), creditCardCharge.getAmount());
		} else {
			log.error("Payment refund failed for card '{}' and amount '{}'", creditCardCharge.getMaskedNumber(), creditCardCharge.getAmount());
			throw new BusinessException(ErrorCode.PAYMENT_REFUND_FAILED);
		}

		// If refund is successful create payment object from the creditCardCharge object
		Payment payment = createPaymentFromCreditCardCharge(creditCardCharge);
		// Set DCC transDate to now as ICE reads this as transDate even if DCC not completed.
		payment.setDccTransDate(new Date());
		// set refund payment id
		payment.setTransactionId(refundResponse.getRequestID());
		return payment;
	}

	public static Payment createPaymentFromCreditCardCharge(CreditCardCharge creditCardCharge) {
		Payment payment = new Payment();

		// fields from creditCardCharge
		payment.setChargeCardExpiry(creditCardCharge.getExpiry());
		payment.setChargeCardHolder(creditCardCharge.getHolder());
		payment.setChargeCardMaskedNumber(creditCardCharge.getMaskedNumber());
		payment.setChargeCardNumber(creditCardCharge.getNumber());
		payment.setChargeCardType(creditCardCharge.getType());
		payment.setChargeCurrencyCode(creditCardCharge.getCurrencyCode());
		payment.setChargeAmount(creditCardCharge.getAmount());

		// DCC fields from creditCardCharge
		payment.setDccAmount(creditCardCharge.getFxAmount());
		payment.setDccRate(creditCardCharge.getFxExchangeRate());
		payment.setDccCurrencyCode(creditCardCharge.getFxCurrencyISOCode());
		// Set PaymentTxnId
		payment.setPaymentTxnId(creditCardCharge.getAuthId());
		return payment;
	}

	public static CreditCardCharge createCreditCardChargeFromPayment(Payment payment, double amount) {
		CreditCardCharge creditCardCharge = new CreditCardCharge();

		// fields from payment
		creditCardCharge.setExpiry(payment.getChargeCardExpiry());
		creditCardCharge.setHolder(payment.getChargeCardHolder());
		creditCardCharge.setMaskedNumber(payment.getChargeCardMaskedNumber());
		creditCardCharge.setNumber(payment.getChargeCardNumber());
		creditCardCharge.setCcToken(payment.getChargeCardNumber());
		creditCardCharge.setType(payment.getChargeCardType());
		creditCardCharge.setCurrencyCode(payment.getChargeCurrencyCode());
		creditCardCharge.setAmount(amount);

		// DCC Fields
		creditCardCharge.setFxAmount(payment.getDccAmount());
		creditCardCharge.setFxExchangeRate(payment.getDccRate());
		creditCardCharge.setFxCurrencyISOCode(payment.getDccCurrencyCode());

		return creditCardCharge;
	}

	/**
	 * Create Pending Reservation Request with ACRS
	 *
	 * @param reservationPartialModifyReq
	 *            ACRS Room Reservation Request object
	 * @return confirmationNumber of pending hotel reservation request
	 */

	protected RoomReservation partialModifyPendingRoomReservation(ReservationPartialModifyReq reservationPartialModifyReq,
																  String confirmationNumber, String propertyCode,
																  String iATAId, String rRUpSell,String source, boolean isPoFlow) {

			ReservationModifyPendingRes reservationRes = invokePartialModifyPendingRoomReservation(reservationPartialModifyReq,
					confirmationNumber, propertyCode, iATAId, rRUpSell, source, isPoFlow);

			return transformReservationModifyPendingRes(reservationRes);
    	}

    protected RoomReservation transformReservationModifyPendingRes(ReservationModifyPendingRes reservationRes) {
        try {
            HotelReservationRes hotelReservationRes = pendingResMapper
                    .pendingResvToHotelReservationRes(reservationRes.getData().getHotelReservation());
			// merge subTotalServiceCharges into taxList
			SegmentResItem mainSegment = BaseAcrsTransformer.getMainSegment(hotelReservationRes.getSegments());
			RoomReservationTransformer.mergeSubTotalServCrgIntoTaxList(mainSegment);
            RoomReservation roomReservation = BaseAcrsTransformer
                    .createRoomReservationFromHotelReservationRes(hotelReservationRes, acrsProperties);
            referenceDataDAOHelper.updateAcrsReferencesToGse(roomReservation);
            roomReservation.setCrsWarnings(reservationRes.getWarnings());
            return roomReservation;
        } catch (ParseException ex) {
            log.error("Error message {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, ex.getMessage());
        }
    }

	protected RoomReservation invokePartialModifyPendingRoomReservation(ReservationPartialModifyReq reservationPartialModifyReq,
																		String confirmationNumber, String propertyCode,
																		String iATAId, String rRUpSell,
																		String source) {

        ReservationModifyPendingRes reservationRes = invokePartialModifyPendingRoomReservation(reservationPartialModifyReq,
				confirmationNumber, propertyCode, iATAId, rRUpSell, source, false);
		return reservationModifyPendingResToRoomReservation(reservationRes);
    }

	protected RoomReservation partialModifyPendingRoomReservationFromPaymentPO(ReservationPartialModifyReq reservationPartialModifyReq,
																			   String confirmationNumber, String propertyCode,
																			   String iATAId, String rRUpSell, String source) {

		ReservationModifyPendingRes reservationRes = invokePartialModifyPendingRoomReservation(reservationPartialModifyReq,
				confirmationNumber, propertyCode, iATAId, rRUpSell, source, true);
		return reservationModifyPendingResToRoomReservation(reservationRes);
	}


	protected RoomReservation reservationModifyPendingResToRoomReservation(ReservationModifyPendingRes reservationRes) {
		try {
			HotelReservationRes hotelReservationRes = pendingResMapper
					.pendingResvToHotelReservationRes(reservationRes.getData().getHotelReservation());
			RoomReservation roomReservation = BaseAcrsTransformer
					.createRoomReservationFromHotelReservationRes(hotelReservationRes, acrsProperties);
			referenceDataDAOHelper.updateAcrsReferencesToGse(roomReservation);
			roomReservation.setCrsWarnings(reservationRes.getWarnings());
			return roomReservation;
		} catch (ParseException ex) {
			log.error("Error message {}", ex.getMessage());
			throw new SystemException(ErrorCode.SYSTEM_ERROR, ex);
		}
	}

	protected ReservationModifyPendingRes invokePartialModifyPendingRoomReservation(ReservationPartialModifyReq reservationPartialModifyReq,
																					String confirmationNumber,
																					String propertyCode,
																					String iATAId,
																					String rRUpSell,
																					String source,
																					boolean isPoFlow) {
	if(referenceDataDAOHelper.isPETDisabled()){
		return invokeACRSModifyPending(reservationPartialModifyReq,confirmationNumber,propertyCode,iATAId,rRUpSell,source,isPoFlow);
	}else{
		return modifyPendingThroughPET(reservationPartialModifyReq,confirmationNumber,propertyCode,iATAId,rRUpSell,source,isPoFlow);
	}
	}


	private ReservationModifyPendingRes modifyPendingThroughPET(ReservationPartialModifyReq reservationPartialModifyReq,
																String confirmationNumber,
																String propertyCode,
																String iATAId,
																String rRUpSell,
																String source,
																boolean isPoFlow){

			final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);
			final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
			final String tokenPath = retrieveCardTokenPath(reservationPartialModifyReq);

			// TODO when PET contract finalized on po endpoints utilize isPoFlow flag to conditionally send to gaming endpoint
			final DestinationHeader destinationHeader = CommonUtil.createDestinationHeader(
					referenceDataDAOHelper.retrieveAcrsPropertyID(propertyCode), acrsVendor,ServiceConstant.HTTP_METHOD_PATCH, iATAId, rRUpSell);
			destinationHeader.setXAuthorization(
					ServiceConstant.HEADER_AUTH_BEARER + acrsAuthTokenResponseMap.get(acrsVendor).getToken());

			// Note that logging of request/response is handled in paymentDao.sendRequestToPaymentExchangeToken
			return paymentDao.sendRequestToPaymentExchangeToken(
					reservationPartialModifyReq, tokenPath, destinationHeader, confirmationNumber, isPoFlow);

	}

	private String retrieveCardTokenPath(ReservationPartialModifyReq reservationPartialModifyReq) {
		int count = 0;
		boolean firstTokenReceived = false;
		StringBuilder destinationTokenPathBuilder = new StringBuilder();
		for (ModificationChangesItem modificationChange : reservationPartialModifyReq.getData()) {
			if(modificationChange.getValue() !=null ) {
				PaymentCard paymentCard = null;
				if(modificationChange.getValue() instanceof PaymentInfoReq) {
					PaymentInfoReq paymentInfoReq = (PaymentInfoReq) modificationChange.getValue();
					paymentCard = paymentInfoReq.getPaymentCard();
				}else if(modificationChange.getValue() instanceof PaymentTransactionReq) {
					PaymentTransactionReq paymentTransactionReq = (PaymentTransactionReq) modificationChange.getValue();
					paymentCard = paymentTransactionReq.getPaymentCard();
				}

				if(paymentCard != null && paymentCard.getCardNumber() !=null && paymentCard.getCardNumber().getToken() != null) {
					if(firstTokenReceived) {
						destinationTokenPathBuilder.append(ServiceConstant.SEMICOLON);
					}
					destinationTokenPathBuilder.append(String.format(REQUEST_TOKEN_PATH, count));
					firstTokenReceived = true;

				}
				count++;
			}

		}
		return destinationTokenPathBuilder.toString();
	}

	protected List<ModificationChangesItem> createModificationChangesForSettledRefunds(List<CreditCardCharge> creditCardCharges,
																					   List<Payment> refundPayments) {
		List<ModificationChangesItem> depositModificationChanges = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(creditCardCharges)) {
			List<PaymentTransactionReq> paymentTransactionReqList =
					RoomReservationTransformer.createACRSPaymentTransactionReqFromCreditCardCharges(creditCardCharges,
							refundPayments);
			paymentTransactionReqList.forEach(req -> req.setPaymentStatus(PaymentStatus.PAYMENT_RECEIVED));

			depositModificationChanges.addAll(paymentTransactionReqList.stream()
					.map(deposit -> createModificationChange(ModificationChangesItem.OpEnum.APPEND,
							acrsProperties.getModifyDepositPaymentsPath(), deposit))
					.collect(Collectors.toList()));
		}

		return depositModificationChanges;
	}

	protected ModificationChangesItem createModificationChange(ModificationChangesItem.OpEnum op, String path, Object value) {
		ModificationChangesItem modificationChange = new ModificationChangesItem();
		modificationChange.setPath(path);
		modificationChange.setOp(op);
		modificationChange.setValue(value);
		return modificationChange;
	}

    protected ModificationChangesItem createRemoveModificationChange(String path) {
		ModificationChangesItem modificationChange = new ModificationChangesItem();
        modificationChange.setPath(path);
        modificationChange.setOp(ModificationChangesItem.OpEnum.REMOVE);
        return modificationChange;
    }

	/**
	 *
	 * @param reservation
	 * @param existingResv
	 * @return
	 */
	protected ReservationPartialModifyReq createPendingModifyReq(RoomReservation reservation,
			ReservationRetrieveResReservation existingResv) {

		ModificationChangesRequest modificationChangesRequest = populatePreModifyRequest(reservation,
				RoomReservationTransformer.transform(existingResv, acrsProperties));
		ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
		ModificationChanges modificationChanges = createModificationChangesFromModificationChangesRequest(
				modificationChangesRequest, existingResv, false);

		// add changes for addOns
		modificationChanges.addAll(createModificationChangesForAddOns(modificationChangesRequest, existingResv));
		reservationPartialModifyReq.setData(modificationChanges);
		return reservationPartialModifyReq;
	}

	protected ModificationChangesRequest populatePreModifyRequest(RoomReservation reservation) {
		return populatePreModifyRequest(reservation, null);
	}

	/**
	 * populatePreModifyRequest will populate PreModify Request
	 *
	 * @param reservation
	 * @param existingReservation
	 * @return
	 */
	protected ModificationChangesRequest populatePreModifyRequest(RoomReservation reservation, RoomReservation existingReservation) {
        ModificationChangesRequest modificationChangesRequest = new ModificationChangesRequest();
        modificationChangesRequest.setSource(reservation.getSource());
        modificationChangesRequest.setGroupCode(reservation.getIsGroupCode());
        modificationChangesRequest.setConfirmationNumber(reservation.getConfirmationNumber());
        modificationChangesRequest.setPartyConfirmationNumber(reservation.getPartyConfirmationNumber());
        TripDetail tripDetail = new TripDetail();
        modificationChangesRequest.setTripDetails(tripDetail);
        tripDetail.setCheckInDate(reservation.getCheckInDate());
        tripDetail.setCheckOutDate(reservation.getCheckOutDate());
        tripDetail.setNumAdults(reservation.getNumAdults());
        tripDetail.setNumChildren(reservation.getNumChildren());

		List<String> existingSpecialRequests = Optional.ofNullable(existingReservation)
												.map(RoomReservation::getSpecialRequests)
												.orElse(null);
        modificationChangesRequest.setRoomRequests(populateRoomRequests(reservation.getSpecialRequests(), existingSpecialRequests));
        if (CollectionUtils.isNotEmpty(reservation.getAdditionalComments())) {
            modificationChangesRequest.setComments(reservation.getAdditionalComments());
        }
        if (StringUtils.isNotEmpty(reservation.getComments())) {
            if (CollectionUtils.isEmpty(modificationChangesRequest.getComments())) {
                modificationChangesRequest.setComments(Collections.singletonList(reservation.getComments()));
            } else {
                modificationChangesRequest.getComments().add(reservation.getComments());
            }
        }
        if (null != reservation.getPropertyId()) {
            modificationChangesRequest.setPropertyId(reservation.getPropertyId());
        }
        if (null != reservation.getRoomTypeId()) {
            modificationChangesRequest.setRoomTypeId(reservation.getRoomTypeId());
        }
        modificationChangesRequest.setProgramId(reservation.getProgramId());
        if(CollectionUtils.isNotEmpty(reservation.getBookings())){
			//set copy of bookings
			modificationChangesRequest.setBookings(
					reservation.getBookings().stream()
							.map(CommonUtil::copyRoomBooking)
							.collect(Collectors.toList()));
		}

        if (null != reservation.getGuaranteeCode()) {
            modificationChangesRequest.setGuaranteeCode(reservation.getGuaranteeCode());
        }
        //INC-3
        if (null != reservation.getCustomerDominantPlay()) {
            modificationChangesRequest.setDominantPlay(reservation.getCustomerDominantPlay());
        }
        if (reservation.getCustomerRank() > 0) {
            modificationChangesRequest.setRankOrSegment(reservation.getCustomerRank());
        } else if (reservation.getCustomerSegment() > 0) {
            modificationChangesRequest.setRankOrSegment(reservation.getCustomerSegment());
        }

        if (CollectionUtils.isNotEmpty(reservation.getRoutingInstructions())) {
            // routingAuthGuid to appUserId from ref data
            invokeRefDataRoutingInfo(reservation.getRoutingInstructions(), reservation.getPropertyId(), true);
        }
        modificationChangesRequest.setRoutingInstructions(reservation.getRoutingInstructions());

        modificationChangesRequest.setPerpetualOffer(reservation.isPerpetualPricing());
		//INC-4 trace and alerts - If we set traces and alerts from existing reservations, it will cause issue in ref data call as we have only code and not id
		// So we will take whatever we get from the incoming request.
		// For ICE, we will remove if incoming request is null and for web we will ignore
		modificationChangesRequest.setTraces(reservation.getTraces());
		modificationChangesRequest.setAlerts(reservation.getAlerts());

        modificationChangesRequest.setPromo(reservation.getPromo());

		// mlife needed from fetch reservation
		modificationChangesRequest.setMlifeNumber(String.valueOf((null != reservation.getProfile()) ? reservation.getProfile().getMlifeNo() : -1));

		// CBSR 2411: email address
		if (null != reservation.getProfile()) {
			if (null != modificationChangesRequest.getProfile()) {
				if (StringUtils.isNotEmpty(reservation.getProfile().getFirstName())) {
					modificationChangesRequest.getProfile().setFirstName(reservation.getProfile().getFirstName());
				}
				if (StringUtils.isNotEmpty(reservation.getProfile().getLastName())) {
					modificationChangesRequest.getProfile().setLastName(reservation.getProfile().getLastName());
				}
				if (StringUtils.isNotEmpty(reservation.getProfile().getEmailAddress1())) {
					modificationChangesRequest.getProfile().setEmail(reservation.getProfile().getEmailAddress1());
				}
				if (StringUtils.isNotEmpty(reservation.getProfile().getEmailAddress2())) {
					modificationChangesRequest.getProfile().setSecondEmail(reservation.getProfile().getEmailAddress2());
				}
			} else {
				Profile profile = new Profile();
				if (StringUtils.isNotEmpty(reservation.getProfile().getEmailAddress1())) {
					profile.setEmail(reservation.getProfile().getEmailAddress1());
				}
				if (StringUtils.isNotEmpty(reservation.getProfile().getEmailAddress2())) {
					profile.setSecondEmail(reservation.getProfile().getEmailAddress2());
				}
				if (StringUtils.isNotEmpty(reservation.getProfile().getFirstName())) {
					profile.setFirstName(reservation.getProfile().getFirstName());
				}
				if (StringUtils.isNotEmpty(reservation.getProfile().getLastName())) {
					profile.setLastName(reservation.getProfile().getLastName());
				}
				modificationChangesRequest.setProfile(profile);
			}
		}

        return modificationChangesRequest;
    }

	protected List<RoomRequest> populateRoomRequests(List<String> specialRequests, List<String> existingSpecialRequests){
		return populateListWithExistingIfNull(specialRequests, existingSpecialRequests).stream()
				.map(ModifyReservationDAOStrategyACRSImpl::createRoomRequest)
				.collect(Collectors.toList());
	}

	private <T> List<T> populateListWithExistingIfNull(List<T> requestCollection, List<T> existingCollection) {
		if (CollectionUtils.isNotEmpty(requestCollection)) {
			return requestCollection;
		} else if (null == requestCollection && null != existingCollection) {
			return existingCollection;
		}
		// In case of non-null but empty set we want to remove all existing objects in collection so return empty set
		return new ArrayList<>();
	}

	protected ModificationChanges createModificationChangesFromModificationChangesRequest(
            ModificationChangesRequest modificationChangesRequest, ReservationRetrieveResReservation existingResv, boolean updateProfile) {
        RoomReservation fetchedReservation = RoomReservationTransformer.transform(existingResv, acrsProperties);

		// update reference data on modificationChangesRequest
		modificationChangesRequest = referenceDataDAOHelper.updateAcrsReferencesFromGse(modificationChangesRequest);

        ModificationChanges modificationChanges = new ModificationChanges();
		SegmentResItem mainSegment = BaseAcrsTransformer.getMainSegment(existingResv.getData().getHotelReservation().getSegments());
		//modify changes for product uses (room, dates, rateplan, guest count)
		if(isProductUsesModified(modificationChangesRequest, fetchedReservation)) {
        	modificationChanges.addAll(createAcrsProductUseModificationChanges(modificationChangesRequest, fetchedReservation, mainSegment));
        }

        // comments
        if(hasModifiedComments(modificationChangesRequest.getComments(),fetchedReservation.getAdditionalComments())
				&& !updateProfile) {
			List<Comment> comments = RoomReservationTransformer.createACRSComments(modificationChangesRequest.getComments(),null,acrsProperties.getMaxAcrsCommentLength());
            modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                    acrsProperties.getModifyCommentPath(), comments));
        }

        // custom data changes
        if (hasModifiedCustomData(modificationChangesRequest, fetchedReservation)) {
            ReservationCustomData modifiedCustomData = RoomReservationTransformer.buildACRSReservationCustomData(
                    modificationChangesRequest.getProgramId(), modificationChangesRequest.getBookings());
                // in existing reservation customData there but in modified
                // custom data is not there.
		if (null == modifiedCustomData
                    && RoomReservationTransformer.hasOverriddenProgram(fetchedReservation.getBookings())) {
                modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                        acrsProperties.getModifyCustomDataPath(), new CustomData()));
            } else if (null != modifiedCustomData) {
                // if custom data added/modified
                modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                        acrsProperties.getModifyCustomDataPath(), modifiedCustomData));
            }
        }

        // Guarantee Code changes
        if (null != modificationChangesRequest.getGuaranteeCode()) {
            if (!(modificationChangesRequest.getGuaranteeCode())
                    .equalsIgnoreCase(fetchedReservation.getGuaranteeCode())) {
                modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                        acrsProperties.getModifyGuaranteeCodePath(), modificationChangesRequest.getGuaranteeCode()));
            }
        }

        // Profile changes
		int existingProfileIndex = existingResv.getData().getHotelReservation().getUserProfiles().get(0).getId();
        if (null != modificationChangesRequest.getProfile()) {
			modificationChanges.addAll(createModificationChangesForProfile(modificationChangesRequest.getProfile(), fetchedReservation.getProfile(), existingProfileIndex));

        }
		// INC-3 customer rank, segment and dominentplay

		if (modificationChangesRequest.getRankOrSegment() > 0) {
			modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format(acrsProperties.getModifyProfileTierPath(), existingProfileIndex), Integer.toString( modificationChangesRequest.getRankOrSegment())));

		}
		if (null != modificationChangesRequest.getDominantPlay()) {
			modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format(acrsProperties.getModifyProfileDominancePath(), existingProfileIndex), BaseAcrsTransformer.transFormDominentPlayForACRS(modificationChangesRequest.getDominantPlay())));

		}

        //billings are handled inside modifyRoomReservationV2() method.
        //partyConfirmationNumber added to link the current reservation being modified to PartyReservation
        if (null != modificationChangesRequest.getPartyConfirmationNumber()
                && !StringUtils.equalsIgnoreCase(modificationChangesRequest.getPartyConfirmationNumber(),
                fetchedReservation.getPartyConfirmationNumber())) {
            modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                    acrsProperties.getModifyPartyConfirmationNumberPath(),
                    modificationChangesRequest.getPartyConfirmationNumber()));
        }

        // inc-4 traces , alerts , special req, room features
        List<ReservationSplRequest> splReqObjList = null;
        if (CollectionUtils.isNotEmpty(modificationChangesRequest.getRoomRequests())) {
            List<String> specialRequestsList = modificationChangesRequest.getRoomRequests().stream()
                    .filter(roomReq -> !ServiceConstant.COMPONENT_FORMAT
                            .equalsIgnoreCase(ACRSConversionUtil.getComponentType(roomReq.getId())))
                    .map(RoomRequest::getId).collect(Collectors.toList());
             if(CollectionUtils.isNotEmpty(specialRequestsList)) {
                 splReqObjList = buildSpecialReqObjList(specialRequestsList);
             }

        }
        if (CollectionUtils.isNotEmpty(modificationChangesRequest.getAlerts())
                || CollectionUtils.isNotEmpty(modificationChangesRequest.getTraces())
                || CollectionUtils.isNotEmpty(splReqObjList)) {
            invokeRefDataEntities(modificationChangesRequest.getPropertyId(),
                    modificationChangesRequest.getAlerts(), modificationChangesRequest.getTraces(), splReqObjList, true);
        }
        // Alerts changes
        if (CollectionUtils.isNotEmpty(modificationChangesRequest.getAlerts())) {
            modificationChanges.add(
                    createModificationChange(ModificationChangesItem.OpEnum.UPSERT, acrsProperties.getModifyAlertsPath(),
                            RoomReservationTransformer.createACRSAlerts(modificationChangesRequest.getAlerts())));
        } else if (CollectionUtils.isNotEmpty(fetchedReservation.getAlerts())
				&& ServiceConstant.ICE.equalsIgnoreCase(modificationChangesRequest.getSource()) && !updateProfile) {
            modificationChanges.add(createRemoveModificationChange(acrsProperties.getModifyAlertsPath()));

        }
        //Traces changes
        if (CollectionUtils.isNotEmpty(modificationChangesRequest.getTraces())) {
            modificationChanges.add(
                    createModificationChange(ModificationChangesItem.OpEnum.UPSERT, acrsProperties.getModifyTracesPath(),
                            RoomReservationTransformer.createACRSTraces(modificationChangesRequest.getTraces())));

        } else if (CollectionUtils.isNotEmpty(fetchedReservation.getTraces())
				&& ServiceConstant.ICE.equalsIgnoreCase(modificationChangesRequest.getSource()) && !updateProfile) {
            modificationChanges.add(createRemoveModificationChange(acrsProperties.getModifyTracesPath()));
        }
        // splReq and room features changes
        if(CollectionUtils.isNotEmpty(splReqObjList)) {
            modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                    acrsProperties.getModifySpecialRequestPath(), RoomReservationTransformer
                            .buildACRSServiceRequestsFromSpecialRequestsLists(splReqObjList)));
        }else if(CollectionUtils.isNotEmpty(fetchedReservation.getSpecialRequestObjList()) && !updateProfile) {
			// delete all serviceRequest
            modificationChanges.add(createRemoveModificationChange(acrsProperties.getModifySpecialRequestPath()));
        }

        return modificationChanges;
    }

	private boolean hasModifiedComments(List<String> modifiedComments, List<String> exisitingCommments) {
		if(CollectionUtils.isEmpty(exisitingCommments) && CollectionUtils.isNotEmpty(modifiedComments)) {
			return true;
		}else if(CollectionUtils.isNotEmpty(exisitingCommments) && CollectionUtils.isNotEmpty(modifiedComments)
				 &&!(modifiedComments.equals(exisitingCommments))) {
			 return true;
	    }else return CollectionUtils.isNotEmpty(exisitingCommments) && CollectionUtils.isEmpty(modifiedComments);
	}

	private List<ModificationChangesItem> createModificationChangesForProfile(Profile modifiedProfile, ReservationProfile existingProfile, int existingProfileIndex) {
		List<ModificationChangesItem> modificationChanges = new ArrayList<>();
		// Title
		String title = modifiedProfile.getTitle();
		if (null == title && null != existingProfile.getTitle()) {
			//ACRS is not expecting null/empty to unset the title.
			title = ServiceConstant.WHITESPACE_STRING;
		}
		if (!StringUtils.equalsIgnoreCase(title, existingProfile.getTitle())) {
			modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
															 String.format(acrsProperties.getModifyProfileTitlePath(),existingProfileIndex),
															 title));
		}
		// FirstName
		if (!StringUtils.equalsIgnoreCase(modifiedProfile.getFirstName(),
										  existingProfile.getFirstName())) {
			modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format(acrsProperties.getModifyProfileFirstNamePath(),existingProfileIndex),
															 modifiedProfile.getFirstName()));
		}
		// LastName
		if (!StringUtils.equalsIgnoreCase(modifiedProfile.getLastName(),
										  existingProfile.getLastName())) {
			modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format( acrsProperties.getModifyProfileLastNamePath(),existingProfileIndex),
															 modifiedProfile.getLastName()));
		}
		// PhoneNumbers
		if (CollectionUtils.isNotEmpty(modifiedProfile.getPhoneNumbers())
				&& CollectionUtils.isNotEmpty(existingProfile.getPhoneNumbers())) {
			// Phone Type
			if (!StringUtils.equalsIgnoreCase(
					modifiedProfile.getPhoneNumbers().get(0).getType(),
					existingProfile.getPhoneNumbers().get(0).getType())) {
				modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
						String.format(acrsProperties.getModifyProfilePhoneTypePath(),existingProfileIndex),
																 modifiedProfile.getPhoneNumbers().get(0).getType()));
			}
			// Phone Number
			if (!StringUtils.equalsIgnoreCase(
					modifiedProfile.getPhoneNumbers().get(0).getNumber(),
					existingProfile.getPhoneNumbers().get(0).getNumber())) {
				modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
						String.format(acrsProperties.getModifyProfilePhoneNumberPath(),existingProfileIndex),
																 modifiedProfile.getPhoneNumbers().get(0).getNumber()));
			}
		}

		boolean isEmailChanged = false;
		List<Email> emails = new ArrayList<>();
		// EmailAddress1
		if (StringUtils.isNotEmpty(modifiedProfile.getEmail())
				&& !StringUtils.equalsIgnoreCase(modifiedProfile.getEmail(),
												 existingProfile.getEmailAddress1())) {
			Email email = new Email();
			email.setAddress(modifiedProfile.getEmail());
			email.setType(EmailType.NUMBER_1);
			emails.add(email);
			isEmailChanged = true;
		} else if (StringUtils.isNotEmpty(existingProfile.getEmailAddress1())) {
			Email email = new Email();
			email.setAddress(existingProfile.getEmailAddress1());
			email.setType(EmailType.NUMBER_1);
			emails.add(email);
		}

		// EmailAddress2
		if (StringUtils.isNotEmpty(modifiedProfile.getSecondEmail())
				&& !StringUtils.equalsIgnoreCase(modifiedProfile.getSecondEmail(),
												 existingProfile.getEmailAddress2())) {
			Email email = new Email();
			email.setAddress(modifiedProfile.getSecondEmail());
			email.setType(EmailType.NUMBER_1);
			emails.add(email);
			isEmailChanged = true;
		} else if (StringUtils.isNotEmpty(existingProfile.getEmailAddress2())) {
			Email email = new Email();
			email.setAddress(existingProfile.getEmailAddress2());
			email.setType(EmailType.NUMBER_1);
			emails.add(email);
		}

		if (isEmailChanged) {
			modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format(acrsProperties.getModifyProfileEmailPath(),existingProfileIndex), emails));
		}

		// MlifeNo
		if (null != modifiedProfile.getMlifeNumber()
				&& !StringUtils.equalsIgnoreCase(modifiedProfile.getMlifeNumber(),
												 Integer.toString(existingProfile.getMlifeNo()))) {
			LoyaltyProgram loyaltyProgram = new LoyaltyProgram();
			loyaltyProgram.setLoyaltyId(modifiedProfile.getMlifeNumber());
			List<LoyaltyProgramProgramsItem> programs = new ArrayList<>();
			LoyaltyProgramProgramsItem program = new LoyaltyProgramProgramsItem();
			program.setProgramName(ServiceConstant.ACRS_MLIFE_PROGRAM_NAME);
			programs.add(program);
			loyaltyProgram.setPrograms(programs);
			modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format(acrsProperties.getModifyProfileMlifeNoPath(),existingProfileIndex),
															 loyaltyProgram));
		}
		//Additional account number i.e. MI Addition
		if (CollectionUtils.isNotEmpty(modifiedProfile.getPartnerAccounts()) &&
				CollectionUtils.isEmpty(existingProfile.getPartnerAccounts())
		){
			PartnerAccounts modifiedPartnerInfo = modifiedProfile.getPartnerAccounts().get(0);
			AdditionalMembershipProgram additionalMembershipProgram = new AdditionalMembershipProgram();
			additionalMembershipProgram.setId(modifiedPartnerInfo.getPartnerAccountNo());
			additionalMembershipProgram.setProgramCode(modifiedPartnerInfo.getProgramCode());
			additionalMembershipProgram.setLevel(modifiedPartnerInfo.getMembershipLevel());
			modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.APPEND,
					String.format(acrsProperties.getAddProfileAdditionalMembershipPath(),existingProfileIndex),
					additionalMembershipProgram));

		}

		// Profile Address
		if (CollectionUtils.isNotEmpty(modifiedProfile.getAddresses())
				&& CollectionUtils.isNotEmpty(existingProfile.getAddresses())) {

			final List<String> streets = new ArrayList<>();
			boolean updatedStreet = false;

			// TODO this code seems to only look at the first address is that correct?
			//  I extracted this code into this method as part of a refactor - mhedden 2022-03-03

			ProfileAddress existingFirstAddress = existingProfile.getAddresses().get(0);
			ProfileAddress modifiedFirstAddress = modifiedProfile.getAddresses().get(0);
			// Street1 - always not null
			streets.add(0, existingFirstAddress.getStreet1());

			if (!StringUtils.equalsIgnoreCase(modifiedFirstAddress.getStreet1(), existingFirstAddress.getStreet1())) {
				streets.set(0, modifiedFirstAddress.getStreet1());
				updatedStreet = true;
			}

			// Street2
			if (!StringUtils.equalsIgnoreCase(modifiedFirstAddress.getStreet2(), existingFirstAddress.getStreet2())) {
				// if modified street2 is null, call update profile with no
				// street2 values
				updatedStreet = true;
				if (modifiedFirstAddress.getStreet2() != null) {
					streets.add(1, modifiedFirstAddress.getStreet2());
				}
			} else if (existingFirstAddress.getStreet2() != null) {
				streets.add(1, existingFirstAddress.getStreet2());
			}

			if (updatedStreet) {
				modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
						String.format(acrsProperties.getModifyProfileStreetPath(),existingProfileIndex), streets));
			}

			// City
			if (!StringUtils.equalsIgnoreCase(
					modifiedFirstAddress.getCity(),
					existingFirstAddress.getCity())) {
				modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
						String.format(acrsProperties.getModifyProfileCityPath(),existingProfileIndex),
																 modifiedFirstAddress.getCity()));
			}

			// State
			if (!StringUtils.equalsIgnoreCase(
					modifiedFirstAddress.getState(),
					existingFirstAddress.getState())) {
				modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
						String.format(acrsProperties.getModifyProfileStatePath(),existingProfileIndex),
																 modifiedFirstAddress.getState()));
			}
			// PostalCode
			if (!StringUtils.equalsIgnoreCase(
					modifiedFirstAddress.getPostalCode(),
					existingFirstAddress.getPostalCode())) {
				modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
						String.format(acrsProperties.getModifyProfilePostalCodePath(),existingProfileIndex),
																 modifiedFirstAddress.getPostalCode()));
			}
			// Country
			if (!StringUtils.equalsIgnoreCase(
					modifiedFirstAddress.getCountry(),
					existingFirstAddress.getCountry())) {
				modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
						String.format(acrsProperties.getModifyProfileCountryPath(),existingProfileIndex),
																 modifiedFirstAddress.getCountry()));
			}
		}

		return modificationChanges;
	}

	public boolean isProductUsesModified(ModificationChangesRequest modificationChangesRequest, RoomReservation fetchedReservation) {
		// Check tripDetails fields for changes
		if (null != modificationChangesRequest.getTripDetails()) {
			TripDetail tripdetail = modificationChangesRequest.getTripDetails();
			// checkInDate changed
			if (!ReservationUtil.areDatesEqualExcludingTime(fetchedReservation.getCheckInDate(), tripdetail.getCheckInDate())) {
				return true;
			}

			// checkOutDate changed
			if (!ReservationUtil.areDatesEqualExcludingTime(fetchedReservation.getCheckOutDate(), tripdetail.getCheckOutDate())) {
				return true;
			}

			// number of Guests change
			if (fetchedReservation.getNumAdults() != tripdetail.getNumAdults()
				|| fetchedReservation.getNumChildren() != tripdetail.getNumChildren()) {
				return true;
			}
		} else {
			// In the scenario where tripDetails is null we must return false to avoid an NPE in createAcrsProductUseModificationChanges()
			return false;
		}

		// roomType changed
		if (!fetchedReservation.getRoomTypeId().equalsIgnoreCase(modificationChangesRequest.getRoomTypeId())){
			return true;
		}

		// bookings change (programId, overrideProgramId, overridePrice)
		// if !haveBookingsChanged() then productUses not modified, returning false
		return haveBookingsChanged(modificationChangesRequest.getBookings(), fetchedReservation.getBookings());
	}

	/**
     * this will add changes for product uses in modify reservation.
     * @param modificationChangesRequest
     * @param fetchedReservation
	 * @param mainSegment
     */
    public ModificationChanges createAcrsProductUseModificationChanges(ModificationChangesRequest modificationChangesRequest,
            RoomReservation fetchedReservation, SegmentResItem mainSegment) {
        ModificationChanges modificationChanges = new ModificationChanges();
        // If below mandatory data are not there in the request then those data
        // will be set from existing data. Some case like preview only trip will
        // be there.
        TripDetail modifiedTrip = modificationChangesRequest.getTripDetails();
        String inventoryCode = null != modificationChangesRequest.getRoomTypeId()
                ? modificationChangesRequest.getRoomTypeId()
                : fetchedReservation.getRoomTypeId();
        if (null == modificationChangesRequest.getProgramId()
                && CollectionUtils.isEmpty(modificationChangesRequest.getBookings())) {
            modificationChangesRequest.setGroupCode(fetchedReservation.getIsGroupCode());
        }
        String ratePlan = null != modificationChangesRequest.getProgramId() ? modificationChangesRequest.getProgramId()
                : fetchedReservation.getProgramId();

        List<RoomPrice> bookings = CollectionUtils.isNotEmpty(modificationChangesRequest.getBookings())
                ? modificationChangesRequest.getBookings()
                : fetchedReservation.getBookings();

        int numChildren = modifiedTrip.getNumChildren();
		int numAdults = modifiedTrip.getNumAdults();
		boolean guestCountChanged = fetchedReservation.getNumAdults() != numAdults || fetchedReservation.getNumChildren() != numChildren;

		List<GuestCountReq> modifiedGuestCount = RoomReservationTransformer.createACRSGuestCounts(numAdults, numChildren);

		// check if no overlapping dates from modified trip to original trip
		// or if we have changed room type
		// or if we have a date extension combined with a guestCountChange
		if ( !isOverlappingDatesOnModification(modifiedTrip, fetchedReservation)
				|| !fetchedReservation.getRoomTypeId().equalsIgnoreCase(inventoryCode)
				|| (isShoulderExtension(modifiedTrip.getCheckInDate(), modifiedTrip.getCheckOutDate(), fetchedReservation) && guestCountChanged)) {
			// Remove all existing productUse objects
			modificationChanges.addAll(mainSegment.getOffer().getProductUses().stream()
					.map(productUse -> createRemoveModificationChange(String.format(acrsProperties.getDeleteProductUsePath(), productUse.getId())))
					.collect(Collectors.toList()));

			// Append 1 or more productUses based on ratePlanCodes on bookings
			ProductUseReq modifiedProductUses = RoomReservationTransformer.buildACRSProductUseReq(
					modifiedTrip.getCheckInDate(), modifiedTrip.getCheckOutDate(), inventoryCode, 1, numChildren, numAdults,
					modificationChangesRequest.isGroupCode(), ratePlan, bookings, modificationChangesRequest.getRoutingInstructions());

			modificationChanges.addAll(modifiedProductUses.stream()
											.map(productUse ->createModificationChange(ModificationChangesItem.OpEnum.APPEND,
															 acrsProperties.getAddProductUsePath(), productUse))
											.collect(Collectors.toList()));
		} else {
			// New trip dates overlap with original trip dates and roomType unchanged, create modifications accordingly:
			if (modificationChangesRequest.isPerpetualOffer()) {
				// This is needed to carry over any override price from inputs to the existing dates which will
				// otherwise be unchanged.
				bookings = combinePerpetualOfferBookings(bookings, fetchedReservation.getBookings(), modifiedTrip);
			}

			ProductUseReq modifiedProductUses = RoomReservationTransformer.buildACRSProductUseReq(
					modifiedTrip.getCheckInDate(), modifiedTrip.getCheckOutDate(), inventoryCode, 1, numChildren, numAdults,
					modificationChangesRequest.isGroupCode(), ratePlan, bookings, modificationChangesRequest.getRoutingInstructions());

			// updated product uses with modifications
			if (haveBookingsChanged(bookings, fetchedReservation.getBookings()) || guestCountChanged) {
				Date overlapStartDate = (ReservationUtil.isFirstDateAfterSecondDateExcludingTime(modifiedTrip.getCheckInDate(),
						fetchedReservation.getCheckInDate())) ? modifiedTrip.getCheckInDate() : fetchedReservation.getCheckInDate();
				Date overlapEndDate = (ReservationUtil.isFirstDateBeforeSecondDateExcludingTime(modifiedTrip.getCheckOutDate(),
						fetchedReservation.getCheckOutDate()) ? modifiedTrip.getCheckOutDate() : fetchedReservation.getCheckOutDate());

				ProductUseRes originalProductUses = mainSegment.getOffer().getProductUses();
				List<Integer> overlapProductUseIds = originalProductUses.stream()
						.filter(productUse -> Boolean.TRUE.equals(productUse.getIsMainProduct()))
										.filter(productUse -> ReservationUtil.isOverlappingDatesOnModification(overlapStartDate,
												overlapEndDate, ReservationUtil.convertLocalDateToDate(productUse.getPeriod().getStart()),
												ReservationUtil.convertLocalDateToDate(productUse.getPeriod().getEnd())))
										.map(ProductUseResItem::getId)
										.collect(Collectors.toList());

				modificationChanges.addAll(createModificationChangesForUpdatedProductUses(modifiedProductUses, originalProductUses,
						overlapProductUseIds, (guestCountChanged) ? modifiedGuestCount : null ));
			}

			// Need to understand if shoulder productUses can be upserted
			boolean checkInProductUseAlreadyRemoved = hasProgramChangedForDateInModifiedBookings(modificationChangesRequest.getBookings(),
					fetchedReservation.getBookings(), fetchedReservation.getCheckInDate());
			Calendar cal = Calendar.getInstance();
			cal.setTime(fetchedReservation.getCheckOutDate());
			cal.add(Calendar.DAY_OF_YEAR, -1);
			boolean checkOutProductUseAlreadyRemoved = hasProgramChangedForDateInModifiedBookings(modificationChangesRequest.getBookings(),
					fetchedReservation.getBookings(), cal.getTime());

			// create shoulder modification changes
			if (isShoulderExtension(modifiedTrip.getCheckInDate(), modifiedTrip.getCheckOutDate(), fetchedReservation)) {
				boolean excludeRatePlanCode = modificationChangesRequest.isPerpetualOffer();
				String roomTypeId = modificationChangesRequest.getRoomTypeId();
				Date checkInDate = modifiedTrip.getCheckInDate();
				Date checkOutDate = modifiedTrip.getCheckOutDate();

				//Change order of ModificationChanges to do extension first
				// if upsert from modified productUse (meaning override price change) present
				boolean addToFront = modificationChanges.stream()
						.map(ModificationChangesItem::getOp)
						.anyMatch(ModificationChangesItem.OpEnum.UPSERT::equals);

				// checkInDate extension
				List<ModificationChangesItem> modificationChangesForCheckInExtension = createModificationChangesForCheckInExtension(checkInDate, roomTypeId,
						modifiedGuestCount, fetchedReservation, excludeRatePlanCode, mainSegment,
						checkInProductUseAlreadyRemoved, modifiedProductUses);
				if (addToFront) {
					modificationChanges.addAll(0, modificationChangesForCheckInExtension);
				} else {
					modificationChanges.addAll(modificationChangesForCheckInExtension);
				}
				// checkOutDate extension
				List<ModificationChangesItem> modificationChangesForCheckOutExtension = createModificationChangesForCheckOutExtension(checkOutDate,
						roomTypeId, modifiedGuestCount, fetchedReservation, excludeRatePlanCode, mainSegment,
						checkOutProductUseAlreadyRemoved, modifiedProductUses);
				if (addToFront) {
					modificationChanges.addAll(0, modificationChangesForCheckOutExtension);
				} else {
					modificationChanges.addAll(modificationChangesForCheckOutExtension);
				}
			}

			// remove dates if trip has been shortened
			if (isDatesRemovedDuringModify(modifiedTrip.getCheckInDate(), modifiedTrip.getCheckOutDate(), fetchedReservation)) {
				modificationChanges.addAll(createModificationChangesForDateRemoval(modifiedTrip.getCheckInDate(),
						modifiedTrip.getCheckOutDate(), fetchedReservation, mainSegment, checkInProductUseAlreadyRemoved,
						checkOutProductUseAlreadyRemoved));
			}
		}

        //group code
		String existingGroupCode = mainSegment.getOffer().getGroupCode();
        if(modificationChangesRequest.isGroupCode()) {
            modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                    acrsProperties.getModifyGroupCodePath(), ratePlan));
        }else if(StringUtils.isNotEmpty(existingGroupCode)){
			//Modify groupCode to RatePlan
			// remove the existing group code from segment.
			modificationChanges.add(
					createRemoveModificationChange(acrsProperties.getModifyGroupCodePath())
			);
		}

        // update PerpetualOffer
        if (modificationChangesRequest.isPerpetualOffer() != fetchedReservation.isPerpetualPricing()) {
            modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                    acrsProperties.getModifyPerpetualOfferPath(), modificationChangesRequest.isPerpetualOffer()));

        }
		// overridden price with 0 amount
		if(RoomReservationTransformer.hasZeroOverriddenPrice(modificationChangesRequest.getBookings())) {
			modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					acrsProperties.getModifyForcedSellPath(), true));
		}
		return modificationChanges;
    }

	private List<RoomPrice> combinePerpetualOfferBookings(List<RoomPrice> newBookings,
														  List<RoomPrice> originalBookings,
														  TripDetail modifiedTrip) {
		LocalDate checkInDate = ReservationUtil.convertDateToLocalDate(modifiedTrip.getCheckInDate());
		LocalDate checkOutDate = ReservationUtil.convertDateToLocalDate(modifiedTrip.getCheckOutDate());
		List<LocalDate> datesOnReservation = Stream.iterate(checkInDate, date -> date.plusDays(1))
				.limit(ChronoUnit.DAYS.between(checkInDate, checkOutDate))
				.collect(Collectors.toList());

		return datesOnReservation.stream()
				.map(date -> combinePerpetualOfferBookingForDate(newBookings, originalBookings, date))
				.collect(Collectors.toList());
	}

	private RoomPrice combinePerpetualOfferBookingForDate(List<RoomPrice> newBookings, List<RoomPrice> originalBookings,
														  LocalDate date) {
		Optional<RoomPrice> newBookingForDateOptional = newBookings.stream()
				.filter(newBooking -> date.isEqual(ReservationUtil.convertDateToLocalDate(newBooking.getDate())))
				.findFirst();

		Optional<RoomPrice> originalBookingForDateOptional = originalBookings.stream()
				.filter(originalBooking -> date.isEqual(ReservationUtil.convertDateToLocalDate(originalBooking.getDate())))
				.findFirst();

		RoomPrice newRoomPriceForDate = newBookingForDateOptional.orElse(null);

		if(originalBookingForDateOptional.isPresent()) {
			RoomPrice originalBookingForDate = CommonUtil.copyRoomBooking(originalBookingForDateOptional.get());
			if (null != newRoomPriceForDate) {
				originalBookingForDate.setOverridePrice(newRoomPriceForDate.getOverridePrice());
				originalBookingForDate.setOverrideProgramId(newRoomPriceForDate.getOverrideProgramId());
			}
			return originalBookingForDate;
		} else {
			return newRoomPriceForDate;
		}
	}

	private boolean hasProgramChangedForDateInModifiedBookings(List<RoomPrice> modifiedBookings,
															   List<RoomPrice> fetchedBookings, Date targetDate) {
		Optional<RoomPrice> modifiedBookingOptional = modifiedBookings.stream()
				.filter(booking -> ReservationUtil.areDatesEqualExcludingTime(booking.getDate(), targetDate))
				.findFirst();

		Optional<RoomPrice> fetchedBookingOptional = fetchedBookings.stream()
				.filter(booking -> ReservationUtil.areDatesEqualExcludingTime(booking.getDate(), targetDate))
				.findFirst();

		if (!fetchedBookingOptional.isPresent()) {
			log.error("Unable to find booking containing original check-in date in fetched booking, which shouldn't be possible.");
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}

		if (modifiedBookingOptional.isPresent()) {
			RoomPrice fetchedBooking = fetchedBookingOptional.get();
			RoomPrice modifiedBooking = modifiedBookingOptional.get();
			if (null == fetchedBooking.getOverrideProgramId()) {
				if (null != modifiedBooking.getOverrideProgramId()) {
					return !modifiedBooking.getOverrideProgramId().equalsIgnoreCase(fetchedBooking.getProgramId());
				} else {
					return !fetchedBooking.getProgramId().equalsIgnoreCase(modifiedBooking.getProgramId());
				}
			} else {
				return !fetchedBooking.getOverrideProgramId().equalsIgnoreCase(modifiedBooking.getOverrideProgramId());
			}
		} else {
			// We return false if the new date has been removed as that will be handled during shoulder changes
			return false;
		}
	}

	/**
	 * Currently 3 possible scenarios for modifying existing product uses:
	 * 	1. Program changed (remove old product use and replace with new product use)
	 * 	2. Guest Count changed (retain old product use and upsert guestCount)
	 * 	3. Override Price changed but program not changed.
	 *
	 * @param modifiedProductUses
	 * @param originalProductUses
	 * @param overlapProductUseIds
	 * @param modifiedGuestCount
	 * @return List of ModificationChanges required to modify productUses to match desired modifications
	 */
	private List<ModificationChangesItem> createModificationChangesForUpdatedProductUses(ProductUseReq modifiedProductUses,
																					ProductUseRes originalProductUses,
																					List<Integer> overlapProductUseIds,
																					List<GuestCountReq> modifiedGuestCount) {
		return originalProductUses.stream()
				.filter(productUse -> overlapProductUseIds.contains(productUse.getId()))
				.flatMap(productUse -> createModificationChangeForUpdateProductUse(productUse, modifiedProductUses,
						modifiedGuestCount).stream())
				.collect(Collectors.toList());
	}

	private List<ModificationChangesItem> createModificationChangeForUpdateProductUse(ProductUseResItem originalProductUse,
																				 ProductUseReq modifiedProductUses,
																				 List<GuestCountReq> modifiedGuestCount) {
		List<ModificationChangesItem> modificationChangeList = new ArrayList<>();

		LocalDate originalProductUseStartDate = originalProductUse.getPeriod().getStart();
		LocalDate originalProductUseEndDate = originalProductUse.getPeriod().getEnd();
		List<ProductUseReqItem> modifiedProductUsesMatchingDate = modifiedProductUses.stream()
				.filter(modifiedProductUse -> modifiedProductUse.getPeriod().getEnd().isAfter(originalProductUseStartDate))
				.filter(modifiedProductUse -> modifiedProductUse.getPeriod().getStart().isBefore(originalProductUseEndDate))
				.collect(Collectors.toList());

		// Check if program (AKA ratePlanCode) changed
		Optional<ProductUseReqItem> programChanged = modifiedProductUsesMatchingDate.stream()
				.filter(productUse -> StringUtils.isNotEmpty(productUse.getRatePlanCode()))
				.filter(productUse -> !productUse.getRatePlanCode().equalsIgnoreCase(originalProductUse.getRatePlanCode()))
				.findAny();

		// If program (AKA ratePlanCode) changed then remove and replace entire ProductUse
		if (programChanged.isPresent()) {
			// remove original
			modificationChangeList.add(createRemoveModificationChange(String.format(acrsProperties.getDeleteProductUsePath(), originalProductUse.getId())));

			// add new productUse replacing original
			// could be 1 or more
			modificationChangeList.addAll(modifiedProductUsesMatchingDate.stream()
												  .map(newProductUse -> createModificationChange(ModificationChangesItem.OpEnum.APPEND, acrsProperties.getAddProductUsePath(), newProductUse))
												  .collect(Collectors.toList()));
		} else {
			if (null != modifiedGuestCount) {
				// Add modification change for guest count
				modificationChangeList.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
						String.format(acrsProperties.getModifyGuestCountsPath(), originalProductUse.getId()),
						modifiedGuestCount));
			}

			// Add modification change for override price changes
			Optional.ofNullable(getModificationChangesItemForOverridePriceChanges(originalProductUse, modifiedProductUsesMatchingDate))
					.map(modificationChangeList::add);
		}
		return modificationChangeList;
	}

	private ModificationChangesItem getModificationChangesItemForOverridePriceChanges(ProductUseResItem originalProductUse, List<ProductUseReqItem> modifiedProductUsesMatchingDate) {
		if (modifiedProductUsesMatchingDate.isEmpty() || !hasOverridePriceChanged(originalProductUse, modifiedProductUsesMatchingDate)) {
			return null;
		}

		if (modifiedProductUsesMatchingDate.size() > 1) {
			// this should only be possible if there was a program change on one of the nights in this productUse
			// and if that was the case we wouldn't have dropped into this else block
			log.error("Modifying OverridePrice error: Unexpected multiple modifiedProductUses matching " +
							"single productUse with dates- start:{}, end:{}.", originalProductUse.getPeriod().getStart(),
					originalProductUse.getPeriod().getEnd());
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}
		List<RateReq> modifiedRequestedRates = modifiedProductUsesMatchingDate.get(0).getRequestedProductRates();

		// add reverting override rate to modifiedRequestedRates if necessary
		modifiedRequestedRates.addAll(getRateReqListForOverrideRemoval(originalProductUse));
		if (modifiedRequestedRates.isEmpty()) {
			return null;
		}
		// Create Modification Change and add to List
		String modificationPath = String.format(acrsProperties.getModifyRequestedRatesPath(),
				originalProductUse.getId());
		return createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
				modificationPath, modifiedRequestedRates);
	}

	private List<RateReq> getRateReqListForOverrideRemoval(ProductUseResItem originalProductUse) {
		List<RateReq> resetOverrideRateReqs = new ArrayList<>();
		if (null == originalProductUse) {
			return resetOverrideRateReqs;
		}
		List<RateRes> existingRequestedRates = originalProductUse.getProductRates().getRequestedRates();
		if (null != originalProductUse.getPackageRates()) {
			existingRequestedRates = originalProductUse.getPackageRates().getRequestedRates();
		}
		List<RateRes> existingOverriddenRates = existingRequestedRates.stream()
				.filter(rateReq -> null != rateReq.getBase())
				.filter(rateReq -> Boolean.TRUE.equals(rateReq.getBase().getOverrideInd()))
				.collect(Collectors.toList());
		final List<RateRes> existingOverriddenRatesFinal = existingOverriddenRates;
		resetOverrideRateReqs = RoomReservationTransformer.transform(existingOverriddenRates);
		resetOverrideRateReqs.forEach(rateReq -> {
			BaseReq base = rateReq.getBase();
			base.setOverrideInd(false);
			base.setAmount(getOriginalAmount(existingOverriddenRatesFinal, rateReq.getStart()));
		});
		return resetOverrideRateReqs;
	}

	private String getOriginalAmount(List<RateRes> existingRequestedRates, LocalDate startDate) {
		if (null == startDate) return null;
		return existingRequestedRates.stream()
				.filter(rateRes -> startDate.isEqual(rateRes.getStart()))
				.map(RateRes::getBase)
				.map(BaseRes::getOriginalBaseRates)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.filter(originalBaseRate -> startDate.isEqual(originalBaseRate.getStart()))
				.map(OriginalBaseRate::getBsAmt)
				.findFirst()
				.orElse(null);
	}

	private boolean hasOverridePriceChanged(ProductUseResItem originalProductUse, List<ProductUseReqItem> modifiedProductUsesMatchingDate) {
		ProductUseRates productUseRates = originalProductUse.getProductRates();

		if (null != originalProductUse.getPackageRates()) {
			// if packageRates are present then it is a package and we need to observe pricing from packageRates
			productUseRates = BaseAcrsTransformer.getProductUseRatesFromProductUse(originalProductUse);
		}

		if (null == productUseRates || null == productUseRates.getRequestedRates()) {
			return false;
		}
		boolean hasOverridePriceChanged = false;
		// find all nights with overridden prices
		List<RateRes> rateResList = productUseRates.getRequestedRates().stream()
				.filter(rateRes -> null != rateRes.getBase())
				.filter(rateRes -> Boolean.TRUE == rateRes.getBase().getOverrideInd())
				.collect(Collectors.toList());
		if (!rateResList.isEmpty()) {
			// override present in original find and compare prices in modified product use
			hasOverridePriceChanged = rateResList.stream()
					.anyMatch(rateRes -> hasOverridePriceChanged(rateRes, modifiedProductUsesMatchingDate));
			// or new overridden price is added with existing overridden price list
			if (!hasOverridePriceChanged) {
				hasOverridePriceChanged = hasOverridePriceChangedOnNewDates(modifiedProductUsesMatchingDate, rateResList);
			}
		} else {
			// No override exist in original product use, so return true if override exists in modified product use
			Optional<RateReq> optionalModifiedRateRes = modifiedProductUsesMatchingDate.stream()
					.flatMap(productUse -> productUse.getRequestedProductRates().stream())
					.filter(rateReq -> null != rateReq.getBase())
					.filter(rateReq -> rateReq.getBase().getOverrideInd())
					.findAny();
			hasOverridePriceChanged = optionalModifiedRateRes.isPresent();
		}
		return hasOverridePriceChanged;
	}

	private static boolean hasOverridePriceChangedOnNewDates(List<ProductUseReqItem> modifiedProductUsesMatchingDate, List<RateRes> rateResList) {
		List<RateReq> modifiedOverriddenPriceList = modifiedProductUsesMatchingDate.stream()
				.flatMap(productUse -> productUse.getRequestedProductRates().stream())
				.filter(rateRes -> null != rateRes.getBase())
				.filter(rateRes -> Boolean.TRUE == rateRes.getBase().getOverrideInd())
				.collect(Collectors.toList());
		Optional<RateReq> missingRateReqInOriginal = modifiedOverriddenPriceList.stream()
				.filter(newRateReq -> !isRateReqDatesInRateResList(rateResList, newRateReq))
				.findAny();
		return missingRateReqInOriginal.isPresent();
	}

	private static boolean isRateReqDatesInRateResList(List<RateRes> rateResList, RateReq newRateReq) {
		LocalDate newRateReqStartDate = newRateReq.getStart();
		LocalDate newRateReqEndDate = newRateReq.getEnd();
		LocalDate dateIterator = newRateReqStartDate;
		while (dateIterator.isBefore(newRateReqEndDate)) {
			final LocalDate finalDateIterator = dateIterator;
			Optional<RateRes> foundOriginalMatch = rateResList.stream()
					.filter(originalRateReq -> !finalDateIterator.isBefore(originalRateReq.getStart()))
					.filter(originalRateReq -> finalDateIterator.isBefore(originalRateReq.getEnd()))
					.findAny();
			if (foundOriginalMatch.isPresent()) {
				dateIterator = dateIterator.plusDays(1);
			} else {
				// DateIterator date not found return false
				return false;
			}

		}
		// All dates found
		return true;
	}

	private boolean hasOverridePriceChanged(RateRes rateRes, List<ProductUseReqItem> modifiedProductUses) {
		// loop through each night in rateRes
		LocalDate dateIterator = rateRes.getStart();
		while (dateIterator.isBefore(rateRes.getEnd())) {
			final LocalDate currentDate = dateIterator;
			// find matching night
			List<BigDecimal> modifiedOverridePrice = modifiedProductUses.stream()
					.flatMap(productUse -> productUse.getRequestedProductRates().stream())
					.filter(rateReq -> !currentDate.isBefore(rateReq.getStart())) // currentDate equal to or after start
					.filter(rateReq -> currentDate.isBefore(rateReq.getEnd())) // currentDate is before end
					.map(RateReq::getBase)
					.map(BaseReq::getAmount)
					.map(amount -> NumberUtils.parseNumber(amount, BigDecimal.class))
					.collect(Collectors.toList());

			if (modifiedOverridePrice.size() > 1) {
				log.error("Data error in hasOverridePriceChanged. modifiedProductUse has multiple RequestedProductRates" +
						" for the night of {}.", dateIterator);
				throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
			}

			BigDecimal originalOverrideBigDecimal = BigDecimal.valueOf(-1.0);
			if (null != rateRes.getBase()) {
				originalOverrideBigDecimal = NumberUtils.parseNumber(rateRes.getBase().getAmount(), BigDecimal.class);
			}

			// compare override price
			if (CollectionUtils.isNotEmpty(modifiedOverridePrice)) {
				BigDecimal modifiedOverrideBigDecimal = modifiedOverridePrice.get(0);

				if (modifiedOverrideBigDecimal.compareTo(originalOverrideBigDecimal) != 0) {
					return true;
				}
			} else if (BigDecimal.valueOf(-1.0).compareTo(originalOverrideBigDecimal) != 0) {
				// There is no override in modified but there was one in original
				return true;
			}
			dateIterator = dateIterator.plusDays(1);
		}
		return false;
	}

	private boolean isOverlappingDatesOnModification(TripDetail modifiedTrip, RoomReservation fetchedReservation) {
		return ReservationUtil.isOverlappingDatesOnModification(modifiedTrip.getCheckInDate(), modifiedTrip.getCheckOutDate(), fetchedReservation.getCheckInDate(), fetchedReservation.getCheckOutDate());
	}

	private boolean haveBookingsChanged(List<RoomPrice> modifiedBookings, List<RoomPrice> existingBookings) {
		return null != modifiedBookings && modifiedBookings.stream()
				.anyMatch(booking -> !isBookingUnModified(booking, existingBookings));
	}

	private boolean isBookingUnModified(RoomPrice targetBooking, List<RoomPrice> bookings) {
		Optional<RoomPrice> matchingBooking = bookings.stream()
				.filter(booking -> ReservationUtil.areDatesEqualExcludingTime(targetBooking.getDate(), booking.getDate()))
				.findFirst();

		if (!matchingBooking.isPresent()) {
			if (-1.0 == targetBooking.getOverridePrice()) {
				// If no override then product use is not modified
				return true;
			}

			return willExtensionLeaveProductUseOverrideUnModified(targetBooking, bookings);
		}
		RoomPrice booking = matchingBooking.get();
		// Removing price check because we don't care if the product use price has changed (for same program)
		// after the fact, original booked price should be honored in that case and productUse not modified.
		if (booking.getOverridePrice() != targetBooking.getOverridePrice()) {
			return false;
		} else if (!booking.getProgramId().equalsIgnoreCase(targetBooking.getProgramId())) {
			return false;
		} else
			return (null == targetBooking.getOverrideProgramId() && null == booking.getOverrideProgramId()) || (null != booking.getOverrideProgramId() && booking.getOverrideProgramId().equalsIgnoreCase(targetBooking.getOverrideProgramId()));
	}

	private static boolean willExtensionLeaveProductUseOverrideUnModified(RoomPrice targetBooking, List<RoomPrice> bookings) {
		// Compare with Check-in booking object
		Optional<RoomPrice> checkInBookingOptional = bookings.stream()
				.min(Comparator.comparing(RoomPrice::getDate));
		if (!checkInBookingOptional.isPresent()) {
			// No existing bookings
			return true;
		}
		RoomPrice checkInBooking = checkInBookingOptional.get();
		String targetProgramId = Optional.ofNullable(targetBooking.getOverrideProgramId()).orElse(targetBooking.getProgramId());
		String checkInProgramId = Optional.ofNullable(checkInBooking.getOverrideProgramId()).orElse(checkInBooking.getProgramId());

		if (ReservationUtil.isFirstDateBeforeSecondDateExcludingTime(targetBooking.getDate(), checkInBooking.getDate())
				&& checkInProgramId.equalsIgnoreCase(targetProgramId)) {
			// targetBooking before checkInDate and on same programId (same product use)
			return false;
		}

		RoomPrice lastNightBooking = checkInBooking;
		String lastNightProgramId = checkInProgramId;
		if (bookings.size() > 1) {
			// find last night booking object
			lastNightBooking = bookings.stream()
					.max(Comparator.comparing(RoomPrice::getDate))
					.get();
			lastNightProgramId = Optional.ofNullable(lastNightBooking.getOverrideProgramId()).orElse(lastNightBooking.getProgramId());
		}

		// if targetBooking before lastNight AND on same programId (same product use) return false, otherwise return true
		return !ReservationUtil.isFirstDateAfterSecondDateExcludingTime(targetBooking.getDate(), lastNightBooking.getDate())
				|| !lastNightProgramId.equalsIgnoreCase(targetProgramId);
	}

	/**
	 *
	 * @param checkInDate
	 * @param checkOutDate
	 * @param fetchedReservation
	 * @param mainSegment
	 * @return
	 */
	private List<ModificationChangesItem> createModificationChangesForDateRemoval(Date checkInDate, Date checkOutDate,
																			 RoomReservation fetchedReservation, SegmentResItem mainSegment, boolean checkInProductUseAlreadyRemoved, boolean checkOutProductUseAlreadyRemoved) {
		List<ModificationChangesItem> modificationChangeList = new ArrayList<>();

		// checkInDate removed dates
		if (ReservationUtil.isFirstDateAfterSecondDateExcludingTime(checkInDate, fetchedReservation.getCheckInDate())) {
			// Product Uses to remove entirely
			LocalDate newCheckInDate = ReservationUtil.convertDateToLocalDate(checkInDate);

			List<ProductUseResItem> productUsesToRemove = mainSegment.getOffer().getProductUses().stream()
					.filter(productUse -> newCheckInDate.isAfter(productUse.getPeriod().getEnd().minusDays(1)))
					.collect(Collectors.toList());

			if (checkInProductUseAlreadyRemoved && CollectionUtils.isNotEmpty(productUsesToRemove)) {
				// identify checkInProductUse
				LocalDate originalCheckInDate = ReservationUtil.convertDateToLocalDate(fetchedReservation.getCheckInDate());
				ProductUseResItem checkInProductUse = getCheckInProductUse(mainSegment.getOffer().getProductUses(), originalCheckInDate);

				// remove checkInProductUse from productUsesToRemove because it has already been removed.
				productUsesToRemove.removeIf(productUse -> Objects.equals(productUse.getId(), checkInProductUse.getId()));
			}

			if (CollectionUtils.isNotEmpty(productUsesToRemove)) {
				List<ModificationChangesItem> modificationChangesRemoveProductUses = productUsesToRemove.stream()
								.map(productUse -> createRemoveModificationChange(String.format(acrsProperties.getDeleteProductUsePath(), productUse.getId())))
								.collect(Collectors.toList());
				modificationChangeList.addAll(modificationChangesRemoveProductUses);
			}

			if (!checkInProductUseAlreadyRemoved) {
				// Product Use to modify the start date
				Optional<ProductUseResItem> productUseToModify = mainSegment.getOffer().getProductUses().stream()
						.filter(productUse -> newCheckInDate.isAfter(productUse.getPeriod().getStart()))
						.filter(productUse -> newCheckInDate.isBefore(productUse.getPeriod().getEnd()))
						.findFirst();

				String checkInDateString = DateUtil.convertDateToString(ServiceConstant.ISO_8601_DATE_FORMAT, checkInDate, TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE));
				productUseToModify.ifPresent(productUse1 -> modificationChangeList.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT, String.format(acrsProperties.getModifyDateStartPath(), productUse1.getId()), checkInDateString)));
			}
		}

		// checkOutDate removed dates
		if (ReservationUtil.isFirstDateBeforeSecondDateExcludingTime(checkOutDate, fetchedReservation.getCheckOutDate())) {
			// product uses to remove entirely
			LocalDate newCheckOutDate = ReservationUtil.convertDateToLocalDate(checkOutDate);
			List<ProductUseResItem> productUsesToRemove = mainSegment.getOffer().getProductUses().stream()
					.filter(productUse -> !newCheckOutDate.isAfter(productUse.getPeriod().getStart()))
					.collect(Collectors.toList());

			if (checkOutProductUseAlreadyRemoved && CollectionUtils.isNotEmpty(productUsesToRemove)) {
				// identify checkOutProductUse
				LocalDate originalCheckOutDate = ReservationUtil.convertDateToLocalDate(fetchedReservation.getCheckOutDate());
				ProductUseResItem checkOutProductUse = getCheckOutProductUse(mainSegment.getOffer().getProductUses(), originalCheckOutDate);

				// remove checkOutProductUse from productUsesToRemove because it has already been removed.
				productUsesToRemove.removeIf(productUse -> Objects.equals(productUse.getId(), checkOutProductUse.getId()));
			}

			if (CollectionUtils.isNotEmpty(productUsesToRemove)) {
				List<ModificationChangesItem> modificationChangesRemoveProductUses = productUsesToRemove.stream()
						.map(productUse -> createRemoveModificationChange(String.format(acrsProperties.getDeleteProductUsePath(), productUse.getId())))
						.collect(Collectors.toList());
				modificationChangeList.addAll(modificationChangesRemoveProductUses);
			}

			if (!checkOutProductUseAlreadyRemoved) {
				// product use to modify end date
				Optional<ProductUseResItem> productUseToModify = mainSegment.getOffer().getProductUses().stream()
						.filter(productUse -> newCheckOutDate.isAfter(productUse.getPeriod().getStart()))
						.filter(productUse -> newCheckOutDate.isBefore(productUse.getPeriod().getEnd()))
						.findFirst();

				String checkOutDateString = DateUtil.convertDateToString(ServiceConstant.ISO_8601_DATE_FORMAT, checkOutDate, TimeZone.getTimeZone(ServiceConstant.DEFAULT_TIME_ZONE));
				productUseToModify.ifPresent(productUse -> modificationChangeList.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT, String.format(acrsProperties.getModifyDateEndPath(), productUse.getId()), checkOutDateString)));
			}
		}

		return modificationChangeList;
	}

	private ProductUseResItem getCheckOutProductUse(ProductUseRes productUses, LocalDate originalCheckOutDate) {
		Optional<ProductUseResItem> checkOutProductUseOptional = productUses.stream()
				.filter(productUse -> productUse.getPeriod().getEnd().equals(originalCheckOutDate))
				.findFirst();
		if(!checkOutProductUseOptional.isPresent()) {
			log.error("Unable to find checkOutProductUse for shoulder date extension for checkOutDate: {}.", originalCheckOutDate);
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}
		return checkOutProductUseOptional.get();
	}

	private List<ModificationChangesItem> createModificationChangesForCheckOutExtension(Date checkOutDate, String roomTypeId,
																				   List<GuestCountReq> modifiedGuestCount,
																				   RoomReservation fetchedReservation,
																				   boolean excludeRatePlanCode,
																				   SegmentResItem mainSegment,
																				   boolean checkOutProductUseRemoved,
																				   ProductUseReq modifiedProductUses) {
		List<ModificationChangesItem> modificationChangeList = new ArrayList<>();
		if (ReservationUtil.isFirstDateAfterSecondDateExcludingTime(checkOutDate, fetchedReservation.getCheckOutDate())) {
			if (excludeRatePlanCode) {
				modificationChangeList.add(createModificationChangeForCheckOutExtensionExcludeRatePlanCode(checkOutDate,
						roomTypeId, modifiedGuestCount, fetchedReservation, modifiedProductUses));
			} else {
				modificationChangeList.addAll(createModificationChangesForCheckOutExtension(
						fetchedReservation, mainSegment, checkOutProductUseRemoved,
						modifiedProductUses));
			}
		}
		return modificationChangeList;
	}

	private List<ModificationChangesItem> createModificationChangesForCheckInExtension(Date checkInDate, String roomTypeId,
																				  List<GuestCountReq> modifiedGuestCount,
																				  RoomReservation fetchedReservation,
																				  boolean excludeRatePlanCode,
																				  SegmentResItem mainSegment,
																				  boolean checkInProductUseRemoved,
																				  ProductUseReq modifiedProductUses) {
		List<ModificationChangesItem> modificationChangeList = new ArrayList<>();
		if (ReservationUtil.isFirstDateBeforeSecondDateExcludingTime(checkInDate, fetchedReservation.getCheckInDate())) {
			if (excludeRatePlanCode) {
				modificationChangeList.add(createModificationChangeForCheckInExtensionExcludeRatePlanCode(checkInDate,
						roomTypeId, modifiedGuestCount, fetchedReservation, modifiedProductUses));
			} else {
				modificationChangeList.addAll(createModificationChangesForCheckInExtension(
						fetchedReservation, mainSegment,
						checkInProductUseRemoved, modifiedProductUses));
			}
		}
		return modificationChangeList;
	}

	private List<ModificationChangesItem> createModificationChangesForCheckOutExtension(RoomReservation fetchedReservation,
																				   SegmentResItem mainSegment,
																				   boolean checkOutProductUseRemoved,
																				   ProductUseReq modifiedProductUses) {
		List<ModificationChangesItem> modificationChangeList = new ArrayList<>();
		// create 1 or more productUses based on ratePlanCodes on bookings for non-perpetual offer modification
		LocalDate originalCheckOutDate = ReservationUtil.convertDateToLocalDate(fetchedReservation.getCheckOutDate());
		// find new end date for last existing productUse
		LocalDate newEndDate = findNewEndDateForCheckOutProductUse(modifiedProductUses, originalCheckOutDate);

		boolean endDateChanged = !newEndDate.isEqual(originalCheckOutDate);

		// check if upsert of end date on original checkOut productUse required
		if (endDateChanged && !checkOutProductUseRemoved) {
			// identify original checkOutProductUse
			ProductUseResItem checkOutProductUse = getCheckOutProductUse(mainSegment.getOffer().getProductUses(), originalCheckOutDate);

			// Create upsert modification change
			modificationChangeList.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format(acrsProperties.getModifyDateEndPath(), checkOutProductUse.getId()), newEndDate));
		}

		modificationChangeList.addAll(modifiedProductUses.stream()
				.filter(productUse -> productUse.getPeriod().getEnd().isAfter(newEndDate))
				.map(productUse -> createModificationChange(ModificationChangesItem.OpEnum.APPEND,
						acrsProperties.getAddProductUsePath(),
						productUse))
				.collect(Collectors.toList()));
		return modificationChangeList;
	}

	private ModificationChangesItem createModificationChangeForCheckOutExtensionExcludeRatePlanCode(Date checkOutDate,
																							   String roomTypeId,
																							   List<GuestCountReq> modifiedGuestCount,
																							   RoomReservation fetchedReservation,
																							   ProductUseReq modifiedProductUses) {
		Date start = fetchedReservation.getCheckOutDate();
		Period period = RoomReservationTransformer.createACRSPeriod(start, checkOutDate);
		ProductUseReqItem checkOutExtensionProductUse = RoomReservationTransformer.createProductUse(period, roomTypeId,
				1, modifiedGuestCount);
		// Need to add any override rates if present for the dates covered by this productUse
		List<RateReq> overrideRateReq = modifiedProductUses.stream()
				.filter(modifiedProductUse -> !modifiedProductUse.getPeriod().getStart().isBefore(ReservationUtil.convertDateToLocalDate(start)))
				.filter(modifiedProductUse -> modifiedProductUse.getPeriod().getStart().isBefore(ReservationUtil.convertDateToLocalDate(checkOutDate)))
				.flatMap((ProductUseReqItem productUse) -> productUse.getRequestedProductRates().stream())
				.collect(Collectors.toList());
		if(CollectionUtils.isNotEmpty(overrideRateReq)) {
			checkOutExtensionProductUse.setRequestedProductRates(overrideRateReq);
		}
		return createModificationChange(ModificationChangesItem.OpEnum.APPEND,
				acrsProperties.getAddProductUsePath(), checkOutExtensionProductUse);
	}

	private List<ModificationChangesItem> createModificationChangesForCheckInExtension(RoomReservation fetchedReservation,
																				  SegmentResItem mainSegment,
																				  boolean checkInProductUseRemoved,
																				  ProductUseReq modifiedProductUses) {
		List<ModificationChangesItem> modificationChangeList = new ArrayList<>();
		// create 1 or more productUses based on ratePlanCodes on bookings for non-perpetual offer modification
		LocalDate originalCheckInDate = ReservationUtil.convertDateToLocalDate(fetchedReservation.getCheckInDate());
		// find new start date for first existing productUse
		LocalDate newStartDate = findNewStartDateForCheckInProductUse(modifiedProductUses, originalCheckInDate);

		boolean startDateChanged = !newStartDate.isEqual(originalCheckInDate);

		// If new start date is not equal to original and the original check in productUse hasn't been replaced
		// then we will update the start date on the original check in productUse with an upsert
		if (startDateChanged && !checkInProductUseRemoved) {
			// Identify original checkIn Product Use for modificationChange
			ProductUseResItem checkInProductUse = getCheckInProductUse(mainSegment.getOffer().getProductUses(), originalCheckInDate);

			// Create upsert modificationChange to update the start date on the checkIn productUse
			modificationChangeList.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format(acrsProperties.getModifyDateStartPath(), checkInProductUse.getId()), newStartDate));
		}

		// Create modificationChanges to append any remaining new productUses before newStartDate
		modificationChangeList.addAll(modifiedProductUses.stream()
				.filter(productUse -> !productUse.getPeriod().getEnd().isAfter(originalCheckInDate))
				.map(productUse -> createModificationChange(ModificationChangesItem.OpEnum.APPEND,
						acrsProperties.getAddProductUsePath(), productUse))
				.collect(Collectors.toList()));

		return modificationChangeList;
	}

	private ModificationChangesItem createModificationChangeForCheckInExtensionExcludeRatePlanCode(Date checkInDate,
																							  String roomTypeId,
																							  List<GuestCountReq> modifiedGuestCount,
																							  RoomReservation fetchedReservation,
																							  ProductUseReq modifiedProductUses) {
		Date end = fetchedReservation.getCheckInDate();
		Period period = RoomReservationTransformer.createACRSPeriod(checkInDate, end);
		ProductUseReqItem checkInExtensionProductUse = RoomReservationTransformer.createProductUse(period, roomTypeId, 1, modifiedGuestCount);
		// Need to add any override rates if present for the dates covered by this productUse
		List<RateReq> overrideRateReq = modifiedProductUses.stream()
				.filter(modifiedProductUse -> !modifiedProductUse.getPeriod().getStart().isBefore(ReservationUtil.convertDateToLocalDate(checkInDate)))
				.filter(modifiedProductUse -> modifiedProductUse.getPeriod().getStart().isBefore(ReservationUtil.convertDateToLocalDate(end)))
				.flatMap((ProductUseReqItem productUse) -> productUse.getRequestedProductRates().stream())
				.collect(Collectors.toList());
		if(CollectionUtils.isNotEmpty(overrideRateReq)) {
			checkInExtensionProductUse.setRequestedProductRates(overrideRateReq);
		}
		return createModificationChange(ModificationChangesItem.OpEnum.APPEND,
				acrsProperties.getAddProductUsePath(), checkInExtensionProductUse);
	}

	private ProductUseResItem getCheckInProductUse(ProductUseRes productUses, LocalDate originalCheckInDate) {
		Optional<ProductUseResItem> checkInProductUse = productUses.stream()
				.filter(productUse1 -> originalCheckInDate.isEqual(productUse1.getPeriod().getStart()))
				.findFirst();

		if (!checkInProductUse.isPresent()) {
			log.error("Unable to find checkInProductUse for shoulder date extension for checkInDate: {}.", originalCheckInDate);
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}

		return checkInProductUse.get();
	}

	private LocalDate findNewEndDateForCheckOutProductUse(ProductUseReq modifiedProductUses, LocalDate originalCheckOutDate) {

		Optional<LocalDate> newEndDateOptional = modifiedProductUses.stream()
				.map(ProductUseReqItem::getPeriod)
				.filter(period -> !period.getStart().isAfter(originalCheckOutDate))
				.map(Period::getEnd)
				.filter(end -> !end.isBefore(originalCheckOutDate))
				.findFirst();

		if (!newEndDateOptional.isPresent()){
			log.error("Unable to find new end date for productUse when adding shoulder dates. OriginalCheckOutDate: " +
					"{}, modifiedProductUses: {}", originalCheckOutDate.toString(), modifiedProductUses.toString());
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}

		return newEndDateOptional.get();
	}

	private LocalDate findNewStartDateForCheckInProductUse(ProductUseReq modifiedProductUses, LocalDate originalCheckInDate) {
		// Identify modified productUse which contains original check-in date and return it's start date
		Optional<LocalDate> newStartDateOptional = modifiedProductUses.stream()
				.map(ProductUseReqItem::getPeriod)
				.filter(period -> !period.getStart().isAfter(originalCheckInDate))
				.filter(period -> period.getEnd().isAfter(originalCheckInDate))
				.map(Period::getStart)
				.findFirst();

		if (!newStartDateOptional.isPresent()) {
			log.error("Unable to find new start date for productUse when adding shoulder dates.");
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}

		return newStartDateOptional.get();
	}

	/**
	 * check modified check in/out dates versus current check in/out dates on reservation being modified
	 * @param checkInDate modified checkInDate
	 * @param checkOutDate modified checkOutDate
	 * @param fetchedReservation currently committed reservation attempting to be modified
	 * @return true if either:
	 * 			checkInDate is after fetchedReservation.getCheckInDate
	 * 			or
	 * 			checkOutDate is before fetchedReservation.getCheckOutDate
	 */
	private boolean isDatesRemovedDuringModify(Date checkInDate, Date checkOutDate, RoomReservation fetchedReservation) {
		return (ReservationUtil.isFirstDateAfterSecondDateExcludingTime(checkInDate, fetchedReservation.getCheckInDate()))
				||
				(ReservationUtil.isFirstDateBeforeSecondDateExcludingTime(checkOutDate, fetchedReservation.getCheckOutDate()));
	}

	/**
	 * check modified check in/out dates versus current check in/out dates on reservation being modified
	 * @param checkInDate modified checkInDate
	 * @param checkOutDate modified checkOutDate
	 * @param fetchedReservation currently committed reservation attempting to be modified
	 * @return true if either:
	 * 		checkInDate is before fetchedReservation.getCheckInDate()
	 * 		or
	 * 		checkOutDate is after fetchedReservation.getCheckOutDate()
	 */
	private boolean isShoulderExtension(Date checkInDate, Date checkOutDate, RoomReservation fetchedReservation) {
		return (ReservationUtil.isFirstDateBeforeSecondDateExcludingTime(checkInDate, fetchedReservation.getCheckInDate()))
				||
				(ReservationUtil.isFirstDateAfterSecondDateExcludingTime(checkOutDate, fetchedReservation.getCheckOutDate()));
	}

	/**
     *
     * @param existingAddOnComponents
     * @param ratePlanCode
     * @param inventoryTypeCode
     * @return
     */
    public SegmentResItem getExistingAddOnComponent(List<SegmentResItem> existingAddOnComponents, String ratePlanCode,
											  String inventoryTypeCode) {
        Optional<SegmentResItem> existingAddOn = existingAddOnComponents.stream()
                .filter(component -> component.getOffer().getProductUses().get(0).getRatePlanCode()
                        .equalsIgnoreCase(ratePlanCode))
                .filter(component -> component.getOffer().getProductUses().get(0).getInventoryTypeCode()
                        .equalsIgnoreCase(inventoryTypeCode))
                .findFirst();

        return existingAddOn.orElse(null);

    }

    /**
     *
     * @param modificationChangesRequest
     * @param existingResv
     */
    protected List<ModificationChangesItem> createModificationChangesForAddOns(
			ModificationChangesRequest modificationChangesRequest, ReservationRetrieveResReservation existingResv) {

        List<ModificationChangesItem> addOnModificationChanges = new ArrayList<>();
        SegmentRes existingSegments = existingResv.getData().getHotelReservation().getSegments();
        List<SegmentResItem> existingAddOnComponents = BaseAcrsTransformer.getComponentSegments(existingSegments);
    	//Get propertyCode from existing Reservation
		String acrsExistingPropertyCode = existingResv.getData().getHotelReservation().getHotels().get(0).getPropertyCode();

		List<String> targetComponentIds = modificationChangesRequest.getRoomRequests().stream()
				.map(RoomRequest::getId)
				.filter(ACRSConversionUtil::isAcrsComponentCodeGuid)
				.filter(component -> ServiceConstant.COMPONENT_FORMAT
						.equalsIgnoreCase(ACRSConversionUtil.getComponentType(component)))
				.collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(targetComponentIds)) {
			// There are Add ons of component type in the modificationChangesRequest
			addOnModificationChanges.addAll(targetComponentIds.stream()
							.filter(addons->!isPkgComponent(addons,acrsExistingPropertyCode))
					.flatMap(componentId -> getModificationChangeForAddOnUpsert(existingAddOnComponents, existingSegments, modificationChangesRequest, acrsExistingPropertyCode, componentId).stream())
									.collect(Collectors.toList()));
			// delete any existing non-pkg addons that are not in modificationChangesRequest
			if (CollectionUtils.isNotEmpty(existingAddOnComponents)) {
				addOnModificationChanges.addAll(existingAddOnComponents.stream()
								.filter(existingComponent -> !isPkgComponent(existingComponent))
						.filter(existingComponent -> !existingComponentIsInTargetComponents(existingComponent, targetComponentIds))
										.map(SegmentResItem::getId)
												.flatMap(id -> createModificationChangeToRemoveAddOnById(id).stream())
														.collect(Collectors.toList()));
			}
		} else {
			// delete all non-pkg component since no component type add on present in modificationChangesRequest
			List<Integer> existingAddOnIds = existingAddOnComponents.stream()
					.filter(existingComponent -> !isPkgComponent(existingComponent))
					.map(SegmentResItem::getId)
												.collect(Collectors.toList());
			List<ModificationChangesItem> removeAllModificationChanges = existingAddOnIds.stream()
					.flatMap(id -> createModificationChangeToRemoveAddOnById(id).stream())
							.collect(Collectors.toList());

			addOnModificationChanges.addAll(removeAllModificationChanges);
        }
        return addOnModificationChanges;

    }

	private boolean isPkgComponent(SegmentResItem existingComponent) {
		boolean isPkgComponent = false;
		String propertyId = referenceDataDAOHelper.retrieveGsePropertyID(existingComponent.getPropertyCode());
		List<String> pkgComponentCodes = reservationDAOHelper.getPkgComponentCodesByPropertyId(propertyId);
		if(CollectionUtils.isNotEmpty(pkgComponentCodes)){
			String invCode = existingComponent.getOffer().getProductUses().get(0).getInventoryTypeCode();
			isPkgComponent= pkgComponentCodes.contains(invCode);
		}
		return isPkgComponent;
	}
	private boolean isPkgComponent(String componentId,String propertyCode) {
		String propertyId = referenceDataDAOHelper.retrieveGsePropertyID(propertyCode);
		List<String> pkgComponentCodes = reservationDAOHelper.getPkgComponentCodesByPropertyId(propertyId);
		return ReservationUtil.isPkgComponent(componentId, pkgComponentCodes);
	}

	private List<ModificationChangesItem> getModificationChangeForAddOnUpsert(List<SegmentResItem> existingAddOnComponents,
																		 SegmentRes existingSegments,
																		 ModificationChangesRequest modificationChangesRequest,
																		 String acrsExistingPropertyCode,
																		 String componentId) {
		String ratePlanCode = ACRSConversionUtil.getComponentNRPlanCode(componentId);
		String inventoryTypeCode = ACRSConversionUtil.getComponentCode(componentId);
		SegmentResItem existingAddOnComponent = getExistingAddOnComponent(existingAddOnComponents, ratePlanCode,
				inventoryTypeCode);
		if (null == existingAddOnComponent) {
			// add new addOn
			SegmentResItem roomSegment = BaseAcrsTransformer.getMainSegment(existingSegments);
			return Collections.singletonList(createModificationChangeForNewAddOn(modificationChangesRequest, ratePlanCode,
					inventoryTypeCode, roomSegment, acrsExistingPropertyCode));
		} else {
			// update checkin and check out date
			return createModificationChangesForUpdatedCheckInCheckOutDates(existingAddOnComponent, modificationChangesRequest);
		}
	}

	private ModificationChangesItem createModificationChangeForNewAddOn(ModificationChangesRequest modificationChangesRequest,
																   String ratePlanCode, String inventoryTypeCode,
																   SegmentResItem roomSegment, String acrsExistingPropertyCode) {
		Integer roomSegmentId = roomSegment.getId();
		Integer roomSegmentHolderId = roomSegment.getSegmentHolderId();
		Integer roomSegmentBookerId = roomSegment.getBookerId();
		TripDetail tripDetail = modificationChangesRequest.getTripDetails();

		Period acrsPeriod = RoomReservationTransformer.createACRSPeriod(tripDetail.getCheckInDate(),
				tripDetail.getCheckOutDate());

		List<GuestCountReq> acrsGuestCounts = RoomReservationTransformer.createACRSGuestCounts(tripDetail.getNumAdults(),
				tripDetail.getNumChildren());

		SegmentReqItem componentSegmentReq = RoomReservationTransformer.createComponentSegmentReq(
				acrsExistingPropertyCode,
				acrsPeriod,
				acrsGuestCounts,
				ratePlanCode, inventoryTypeCode, roomSegmentHolderId, roomSegmentBookerId, roomSegmentId);

		return createModificationChange(ModificationChangesItem.OpEnum.APPEND,
				acrsProperties.getAddComponentPath(),
				componentSegmentReq);
	}

	private boolean existingComponentIsInTargetComponents(SegmentResItem existingComponent, List<String> addOnComponentReqs) {
		String ratePlanCode = existingComponent.getOffer().getProductUses().get(0)
				.getRatePlanCode();
		String inventoryTypeCode = existingComponent.getOffer().getProductUses().get(0)
				.getInventoryTypeCode();
		return addOnComponentReqs.stream()
				.filter(reqAddon -> ACRSConversionUtil.getComponentNRPlanCode(reqAddon)
						.equalsIgnoreCase(ratePlanCode))
				.anyMatch(reqAddon -> ACRSConversionUtil.getComponentCode(reqAddon)
						.equalsIgnoreCase(inventoryTypeCode));
	}

	private List<ModificationChangesItem> createModificationChangesForUpdatedCheckInCheckOutDates(SegmentResItem existingAddOnComponent,
																		ModificationChangesRequest modificationChangesRequest) {
		List<ModificationChangesItem> modificationChangeList = new ArrayList<>();
		Integer productId = existingAddOnComponent.getOffer().getProductUses().get(0).getId();
		TripDetail tripDetail = modificationChangesRequest.getTripDetails();

		LocalDate existingCheckInDate = existingAddOnComponent.getStart();
		LocalDate newCheckInDate = ReservationUtil.convertDateToLocalDate(tripDetail.getCheckInDate());
		LocalDate existingCheckOutDate = existingAddOnComponent.getEnd();
		LocalDate newCheckOutDate = ReservationUtil.convertDateToLocalDate(tripDetail.getCheckOutDate());

		if (!existingCheckInDate.equals(newCheckInDate)) {
			modificationChangeList.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format(acrsProperties.getUpdateComponentCheckedInPath(),
							existingAddOnComponent.getId(), productId),
					BaseCommonUtil.getDateStr(tripDetail.getCheckInDate(), ServiceConstant.ISO_8601_DATE_FORMAT)));
		}

		if (!existingCheckOutDate.equals(newCheckOutDate)) {
			modificationChangeList.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
					String.format(acrsProperties.getUpdateComponentCheckedOutPath(),
							existingAddOnComponent.getId(), productId),
					BaseCommonUtil.getDateStr(tripDetail.getCheckOutDate(), ServiceConstant.ISO_8601_DATE_FORMAT)));
		}

		return modificationChangeList;
	}

	private List<ModificationChangesItem> createModificationChangeToRemoveAddOnById(Integer id) {
		List<ModificationChangesItem> modificationChangeList = new ArrayList<>();

		modificationChangeList.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT, String
						.format(acrsProperties.getUpdateComponentStatusPath(), id),
				ServiceConstant.SEGMENT_CANCEL));

		CancellationReason reason = new CancellationReason();
		reason.setCode(ServiceConstant.SEGMENT_DELETED_ON_MODIFY);
		modificationChangeList.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
				String.format(acrsProperties.getUpdateComponentCancelReasonPath(),
						id),
				Collections.singletonList(reason)));

		return modificationChangeList;
	}

	/**
     * this will return reservation primary profile from ocrs reservation
     * @param cnfNumber
     * @return
     */
    protected UserProfile getOcrsResvPrimaryProfile(String cnfNumber) {
        final String ocrsToken = idmsTokenDAO.generateToken().getAccessToken();
        HttpEntity<?> request = new HttpEntity<>(CommonUtil.createOcrsHeaders(ocrsToken));
        return ocrsDao.getOCRSResvPrimaryProfile(request, cnfNumber);

    }

    public ReservationPendingReq createPendingReservationReq(RoomReservation reservation) {
        //TODO: Handle multiple cards

        List<CreditCardCharge> creditCardChargeList = reservation.getCreditCardCharges();

        // Map GSE property/room GUID to ACRS code

        if (reservation.getProgramId() != null) {
            if (ACRSConversionUtil.isAcrsGroupCodeGuid(reservation.getProgramId())) {
                reservation.setProgramId(referenceDataDAOHelper.createFormatedGroupCode(reservation.getPropertyId(),
                        reservation.getProgramId()));
                reservation.setIsGroupCode(true);
            } else {
                reservation.setProgramId(referenceDataDAOHelper.retrieveRatePlanDetail(reservation.getPropertyId(),
                        reservation.getProgramId()));
            }
        }
        //for hold api bookings will be empty only programId will be there.
        if(CollectionUtils.isNotEmpty(reservation.getBookings())){
        	reservation.getBookings().forEach(booking -> {
                if (ACRSConversionUtil.isAcrsGroupCodeGuid(booking.getProgramId())) {
                    booking.setProgramId(referenceDataDAOHelper.createFormatedGroupCode(reservation.getPropertyId(),
                            booking.getProgramId()));
                } else {
                    booking.setProgramId(referenceDataDAOHelper.retrieveRatePlanDetail(reservation.getPropertyId(),
                            booking.getProgramId()));
                }
                if (StringUtils.isNotEmpty(booking.getOverrideProgramId())) {
                    booking.setOverrideProgramId(referenceDataDAOHelper
                            .retrieveRatePlanDetail(reservation.getPropertyId(), booking.getOverrideProgramId()));
                }
        	});
        }

        reservation.setRoomTypeId(referenceDataDAOHelper.retrieveRoomTypeDetail(reservation.getPropertyId(),
                reservation.getRoomTypeId()));
        // routingAuthGuid to appUserId from ref data
        if(CollectionUtils.isNotEmpty(reservation.getRoutingInstructions())) {
            invokeRefDataRoutingInfo(reservation.getRoutingInstructions(), reservation.getPropertyId(), true);
        }
        //INC-4
        // convert id to code for alerts , traces , splReq , room features from refData.
        List<String> splReqs = null;
        if (CollectionUtils.isNotEmpty(reservation.getSpecialRequests())) {
            splReqs = reservation.getSpecialRequests().stream()
                    .filter(component -> !ServiceConstant.COMPONENT_FORMAT
                            .equalsIgnoreCase(ACRSConversionUtil.getComponentType(component)))
                    .collect(Collectors.toList());
        }

        if (CollectionUtils.isNotEmpty(reservation.getAlerts()) || CollectionUtils.isNotEmpty(reservation.getTraces())
                || CollectionUtils.isNotEmpty(splReqs)) {
            // add only spl req and room feature in specialRequestObjList with
            // type
            reservation.setSpecialRequestObjList(buildSpecialReqObjList(splReqs));
            invokeRefDataEntities(reservation.getPropertyId(), reservation.getAlerts(), reservation.getTraces(),
                    reservation.getSpecialRequestObjList(), true);
        }

        reservation.setPropertyId(referenceDataDAOHelper.retrieveAcrsPropertyID(reservation.getPropertyId()));

        if (CollectionUtils.isNotEmpty(creditCardChargeList)) {
            DepositPolicy depositPolicy = new DepositPolicy();
            depositPolicy.setDepositRequired(creditCardChargeList.stream()
							.anyMatch(credit -> credit.getAmount() > 0));
            reservation.setDepositPolicyCalc(depositPolicy);
            reservation.setAmountDue(creditCardChargeList.stream().mapToDouble(CreditCardCharge::getAmount).sum());
        }
		return RoomReservationTransformer.transformACRReservationReq(reservation,acrsProperties.getMaxAcrsCommentLength());
    }

    private List<ReservationSplRequest> buildSpecialReqObjList(List<String> splReqs) {
    	 List<ReservationSplRequest> splReqObjList = new ArrayList<>();
    	if(CollectionUtils.isEmpty(splReqs)) return splReqObjList;
        splReqs.forEach(splReqStr -> {
            ReservationSplRequest splReqObj = new ReservationSplRequest();
            splReqObj.setId(splReqStr);
            splReqObj.setType(ServiceConstant.ROOM_FEATURE_SPECIAL_REQUEST);
            splReqObjList.add(splReqObj);
        });
        return splReqObjList;
    }

    /**
     * This will return room and component charges from ACRS retrieve pricing api.
     * @param request
     * @return
     */
    protected RoomReservation getRoomAndComponentCharges(RoomReservation request, boolean isPOFlow) {
		try {
			SuccessfulPricing searchOffersResponse = getAcrsDataForRoomAndComponentCharge(request,isPOFlow);
			RoomReservation roomReservation = RoomAndComponentChargesTranformer.getRoomAndComponentChargesResponse(searchOffersResponse
					, request.getNumRooms(), acrsProperties);
			if(null != roomReservation.getDepositCalc()) {
				roomReservation.getDepositCalc().setOverrideAmount(null != request.getDepositCalc() ? request.getDepositCalc().getOverrideAmount() : -1);
			}
			roomReservation.setIsGroupCode(request.getIsGroupCode());
			referenceDataDAOHelper.updateAcrsReferencesToGse(roomReservation);
			return roomReservation;
		}catch (BusinessException be){
			log.error("error while retrieve pricing API -{}",be.getMessage());
		}
		return null;
    }

	protected SuccessfulPricing getAcrsDataForRoomAndComponentCharge(RoomReservation request, boolean isPOFlow){
		String ratePlanCode = null;
		if (ACRSConversionUtil.isAcrsGroupCodeGuid(request.getProgramId())) {
			request.setIsGroupCode(true);
			ratePlanCode = referenceDataDAOHelper.createFormatedGroupCode(request.getPropertyId(), request.getProgramId());
		} else {
			ratePlanCode = referenceDataDAOHelper.retrieveRatePlanDetail(request.getPropertyId(),
					request.getProgramId());
		}
		final String acrsPropertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(request.getPropertyId());

		if(ACRSConversionUtil.isPORatePlan(ratePlanCode) && isPOFlow) {
			ratePlanCode = acrsProperties.getBaseRatePlan(acrsPropertyCode.toUpperCase());
		}else if(CollectionUtils.isNotEmpty(request.getBookings())){
			String dominantProgramId = reservationDAOHelper.findDominantProgram(request.getBookings());
			ratePlanCode = referenceDataDAOHelper.retrieveRatePlanDetail(request.getPropertyId(),
					dominantProgramId);
		}
		//temp fix for group code. If rate plan is group code the use bar price
		//Once ACRS support pricing by group code then remove this
		if(request.getIsGroupCode()) {
			ratePlanCode = acrsProperties.getBaseRatePlan(acrsPropertyCode.toUpperCase());
		}

		final String acrsInventoryCode = referenceDataDAOHelper.retrieveRoomTypeDetail(acrsPropertyCode,
				request.getRoomTypeId());

		BodyParameterPricing bodyParameterPricing = new BodyParameterPricing();
		DataRqPricing dataRqPricing = new DataRqPricing();

		OptionsPricing optionsPricing = new OptionsPricing();

		if (null != request.getProfile() && request.getProfile().getMlifeNo() > 0) {
			Loyalty loyalty = new Loyalty();
			loyalty.setLoyaltyId(String.valueOf(request.getProfile().getMlifeNo()));
			optionsPricing.setLoyalty(loyalty);
		}

		RequestedDescriptionPricing requestedDescriptionPricing = new RequestedDescriptionPricing();
		requestedDescriptionPricing.setPackageDescFlag(true);
		requestedDescriptionPricing.setProductLongDescFlag(true);
		requestedDescriptionPricing.setRatePlanLongDescFlag(true);
		requestedDescriptionPricing.setTaxLongDescFlag(true);

		optionsPricing.setDescription(requestedDescriptionPricing);
		if(isPOFlow) {
			optionsPricing.setReservationContext(Optional.ofNullable(request.getConfirmationNumber()).orElse(ServiceConstant.DEFAULT_RESV_CONTEXT));
		}
		dataRqPricing.setOptions(optionsPricing);

		List<RequestedProductPricing> requestedProductPricingList = new ArrayList<>();
		RequestedProductPricing requestedProductPricing = new RequestedProductPricing();

		requestedProductPricing.setInventoryTypeCode(acrsInventoryCode);
		List<RequestedGuestCounts> guestCounts = new ArrayList<>();
		if (request.getNumAdults() > 0) {
			RequestedGuestCounts aqc10 = new RequestedGuestCounts();
			aqc10.setCount(request.getNumAdults());
			aqc10.setOtaCode(ServiceConstant.NUM_ADULTS_MAP);
			guestCounts.add(aqc10);
		}

		if (request.getNumChildren() > 0) {
			RequestedGuestCounts aqc8 = new RequestedGuestCounts();
			aqc8.setCount(request.getNumChildren());
			aqc8.setOtaCode(ServiceConstant.NUM_CHILD_MAP);
			guestCounts.add(aqc8);
		}
		requestedProductPricing.setGuestCounts(guestCounts);
		requestedProductPricing.setQuantity(request.getNumRooms());

		LocalDate tripStartDate = ReservationUtil.convertDateToLocalDate(request.getCheckInDate());
		LocalDate tripEndDate = ReservationUtil.convertDateToLocalDate(request.getCheckOutDate());
		String tripLength = String.valueOf(Duration.between(tripStartDate.atStartOfDay(), tripEndDate.atStartOfDay()).toDays());

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ServiceConstant.ISO_8601_DATE_FORMAT);
		String startDate = simpleDateFormat.format(request.getCheckInDate());
		requestedProductPricingList.add(requestedProductPricing);
		dataRqPricing.setProducts(requestedProductPricingList);
		dataRqPricing.setPromoCode(request.getPromo());
		bodyParameterPricing.setData(dataRqPricing);
		return acrsSearchOffers(bodyParameterPricing,
				acrsPropertyCode, ratePlanCode, startDate, tripLength, request.getSource(),
				request.isPerpetualPricing());
	}

	protected SuccessfulPricing acrsSearchOffers(BodyParameterPricing bodyParameterPricingRequest, String acrsPropertyCode,
												 String ratePlanCode, String startDate, String tripLength,
												 String source, boolean isPoFlow) {
		return roomPriceDAOStrategyACRSImpl.acrsSearchOffers(bodyParameterPricingRequest,
				acrsPropertyCode, ratePlanCode, startDate, tripLength, source,
				isPoFlow);
	}

	/**
	 * while calculating room charges id CNF is not available then this method
	 * will be called. It will call single availability API and return data in
	 * room reservation form.
	 *
	 * @param roomReservation
	 * @return
	 */
	protected RoomReservation getChargesFromSingleAvailability(RoomReservation roomReservation) {
		AuroraPriceRequest pricingRequest = AuroraPriceRequestTransformer.getAuroraPriceV2RequestForCharges(roomReservation);
		RoomReservation roomReservationResponse = RoomReservationChargesTransformer.singleAvailabilityToResvTransform(roomReservation,
				roomPriceDAOStrategyACRSImpl.getAcrsSingleAvailability(pricingRequest), acrsProperties);
		copyValuesFromRequestToResponse(roomReservation, roomReservationResponse);

		return roomReservationResponse;
	}

	protected List<AuroraPriceResponse> getRoomPrices(AuroraPriceRequest auroraPriceRequest) {
		return roomPriceDAOStrategyACRSImpl.getRoomPrices(auroraPriceRequest);
	}

	protected AuroraPricesResponse getRoomPricesV2(AuroraPriceRequest auroraPriceRequest) {
		return roomPriceDAOStrategyACRSImpl.getRoomPricesV2(auroraPriceRequest);
	}

	protected void copyValuesFromRequestToResponse(RoomReservation reservationRequest,
											   RoomReservation reservationResponse) {
		reservationResponse.setCustomerId(reservationRequest.getCustomerId());
		reservationResponse.setItineraryId(reservationRequest.getItineraryId());
		reservationResponse.setPerpetualPricing(reservationRequest.isPerpetualPricing());
		if ( null != reservationResponse.getProgramId() && !ACRSConversionUtil.isAcrsRatePlanGuid(reservationResponse.getProgramId()) ){
			if (reservationResponse.getIsGroupCode()) {
				reservationResponse.setProgramId(referenceDataDAOHelper.createFormatedGroupCode(reservationRequest.getPropertyId(),
						reservationRequest.getProgramId()));
			} else {
				reservationResponse.setProgramId(referenceDataDAOHelper.retrieveRatePlanDetail(reservationRequest.getPropertyId(), reservationRequest.getProgramId()));
			}
		}
		reservationResponse.setRoutingInstructions(reservationRequest.getRoutingInstructions());
		reservationResponse.setAlerts(reservationRequest.getAlerts());
		reservationResponse.setTraces(reservationRequest.getTraces());
		reservationResponse.setSpecialRequests(reservationRequest.getSpecialRequests());
		reservationResponse.setCreditCardCharges(reservationRequest.getCreditCardCharges());
		reservationResponse.setPerpetualPricing(reservationRequest.isPerpetualPricing());
		reservationResponse.setCheckInDate(reservationRequest.getCheckInDate());
		reservationResponse.setCheckOutDate(reservationRequest.getCheckOutDate());
	}

    /**
     * this will update the paymentInfo and payment transaction for the given reservation
     * @param cnfNumber
     * @param reservation
     * @param creditCardChargeAuthorizationMap
     * @param source
     * @param addOnsDeposits
     * @return
     */
    protected RoomReservation updateCardFormOfPayment(String cnfNumber, RoomReservation reservation,
													  Map<CreditCardCharge, String> creditCardChargeAuthorizationMap, String source, List<AddOnDeposit> addOnsDeposits) {
        ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
        ModificationChanges modificationChanges = new ModificationChanges();
        // 1. Create ModificationChange for PaymentInfo
        List<ModificationChangesItem> modificationChangeForPaymentInfo = getModificationChangeForPaymentInfo(reservation, addOnsDeposits);
        if (CollectionUtils.isNotEmpty(modificationChangeForPaymentInfo)) {
            modificationChanges.addAll(modificationChangeForPaymentInfo);
        }
        // 2. Create ModificationChange for PaymentTransactionReq.
        List<ModificationChangesItem> modificationChangesForPaymentTxns = getModificationChangesForPaymentTxns(reservation,
                creditCardChargeAuthorizationMap);
        if (CollectionUtils.isNotEmpty(modificationChangesForPaymentTxns)) {
            modificationChanges.addAll(modificationChangesForPaymentTxns);
        }
        // 3. Execute a partialModifyPendingRoomReservation with those
        // ModificationChanges.
        reservationPartialModifyReq.setData(modificationChanges);
		String iATAId = (null != reservation.getAgentInfo()) ? reservation.getAgentInfo().getAgentId() : null;
		if(reservation.isPerpetualPricing()) {
			return partialModifyPendingRoomReservationFromPaymentPO(reservationPartialModifyReq, cnfNumber, reservation.getPropertyId(),
					iATAId, reservation.getRrUpSell(), source);
		}

	    return invokePartialModifyPendingRoomReservation(reservationPartialModifyReq, cnfNumber, reservation.getPropertyId(),
			    iATAId, reservation.getRrUpSell(), source);
    }
    /**
     * this will return modification change for basic payment info
     * @param reservation
     * @param addOnsDeposits
	 * @return
     */
    protected List<ModificationChangesItem> getModificationChangeForPaymentInfo(RoomReservation reservation, List<AddOnDeposit> addOnsDeposits) {
        ModificationChanges modificationChanges = new ModificationChanges();
		double paymentInfoAmount = 0;
        if(CollectionUtils.isNotEmpty(reservation.getCreditCardCharges())){
			paymentInfoAmount = reservation.getCreditCardCharges().stream().mapToDouble(CreditCardCharge::getAmount).sum();
		}
        // Ice can provide 0.0 amount with card info even deposit >0. In that case we will be
		// storing card info with guarantee type.
		if(paymentInfoAmount != 0) {
			paymentInfoAmount = null != reservation.getDepositCalc() ? reservation.getDepositCalc().getAmount() : 0.0;
			//since reservation.getDepositCalc() will contain total deposit for all segments including main segment.
			if (CollectionUtils.isNotEmpty(addOnsDeposits)) {
				double totalAddonsDeposit = addOnsDeposits.stream().map(AddOnDeposit::getDepositAmount).map(Double::valueOf)
						.mapToDouble(x -> x).sum();
				paymentInfoAmount = paymentInfoAmount - totalAddonsDeposit; // main segment deposit amount
			}
		}
		final double finalPaymentAmount = BaseCommonUtil.round(paymentInfoAmount,ServiceConstant.THREE_DECIMAL_PLACES);
		CreditCardCharge cardCharge = null;
        if (CollectionUtils.isNotEmpty(reservation.getCreditCardCharges())) {
			cardCharge = reservation.getCreditCardCharges().get(0);
		}
		PaymentInfoReq paymentInfo = RoomReservationTransformer.createACRSPaymentInfoFromCreditCardCharge(
				cardCharge,finalPaymentAmount, reservation.getGuaranteeCode(),acrsProperties.getPaymentTypeGuaranteeCodeMap());
		modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT, acrsProperties.getModifyPaymentInfoPath(),
				paymentInfo));
		// add FOP changes for add-Ons
		addAddOnsFOPChanges(modificationChanges, addOnsDeposits, cardCharge, reservation.getGuaranteeCode());
		return modificationChanges;

	}

	public  void addAddOnsFOPChanges(ModificationChanges modificationChanges, List<AddOnDeposit> addOnsDeposits, CreditCardCharge cardCharge, String guaranteeCode){
		if (CollectionUtils.isNotEmpty(addOnsDeposits)) {
			final CreditCardCharge finalCreditCardCharge = cardCharge;
			//for all addons deposit might not be required so setting as GUARANTEE else DEPOSIT
			addOnsDeposits.forEach(addOn -> {
				double depositAmount = Double.parseDouble(addOn.getDepositAmount());
				PaymentInfoReq addOnPaymentInfo = RoomReservationTransformer.createACRSPaymentInfoFromCreditCardCharge(finalCreditCardCharge,depositAmount,guaranteeCode, acrsProperties.getPaymentTypeGuaranteeCodeMap());
				modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
						String.format(acrsProperties.getModifyAddOnPaymentInfoPath(), addOn.getId()),
						addOnPaymentInfo));
			});
		}
	}

	/**
     * this will return modification changes for payment transactions
     * @param reservation
     * @param creditCardChargeAuthorizationMap
     * @return
     */
    protected List<ModificationChangesItem> getModificationChangesForPaymentTxns(RoomReservation reservation,
            Map<CreditCardCharge, String> creditCardChargeAuthorizationMap) {
        ModificationChanges modificationChanges = new ModificationChanges();

       String txnDate = BaseCommonUtil.getDateStr(new Date(), ServiceConstant.DATE_FORMAT_WITH_TIME_SECONDS);
        if (null != creditCardChargeAuthorizationMap && !creditCardChargeAuthorizationMap.isEmpty()) {
            // deposit or refund card payment
			// Adding the filter again to stopping sending zero deposit to ACRS ,
			// as it is causing multiple  FINTX into Opera
			// Updated card will be stored into paymentinfo.
			// During find reservation we should take the updated card form payment info
			// to support modify card info with zero amount.
			modificationChanges.addAll(creditCardChargeAuthorizationMap.entrySet().stream()
					.filter(entry -> !entry.getValue().equalsIgnoreCase(ServiceConstant.COMP))
					.map(entry -> createACRSPaymentTransactionReqWithAuthDecorator(entry.getKey(), entry.getValue(), txnDate))
					.map(paymentTransaction -> createModificationChange(ModificationChangesItem.OpEnum.APPEND,
							acrsProperties.getModifyDepositPaymentsPath(), paymentTransaction))
					.collect(Collectors.toList()));

        } else if(null != reservation && CollectionUtils.isEmpty(reservation.getCreditCardCharges())){
            // cash payment
           PaymentTransactionReq cashPaymentTransactionReq = new PaymentTransactionReq();
            cashPaymentTransactionReq.setPaymentType(PaymentType.NUMBER_1);
            cashPaymentTransactionReq.setPaymentIntent(PaymentIntent.DEPOSIT);
			cashPaymentTransactionReq.setAmount("0.0");
            cashPaymentTransactionReq.setPaymentStatus(PaymentStatus.PAYMENT_RECEIVED);
            cashPaymentTransactionReq.setTransactionDate(txnDate);
           modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.APPEND,
                    acrsProperties.getModifyDepositPaymentsPath(), cashPaymentTransactionReq));
        }
        //for GUARANTEE no payment transaction is required
        return modificationChanges;
    }

	private PaymentTransactionReq createACRSPaymentTransactionReqWithAuthDecorator(CreditCardCharge creditCardCharge,
																				   String authId, String txnDate) {
		Optional<PaymentTransactionReq> depositPaymentTransactionReqOptional = Optional.ofNullable(
				RoomReservationTransformer.createACRSPaymentTransactionReqFromCreditCardCharge(creditCardCharge, txnDate));
		if (!depositPaymentTransactionReqOptional.isPresent()) {
			return null;
		}
		PaymentTransactionReq depositPaymentTransactionReq = depositPaymentTransactionReqOptional.get();
		depositPaymentTransactionReq.setPaymentStatus(PaymentStatus.PAYMENT_RECEIVED);
		depositPaymentTransactionReq.setPaymentRecordId(authId);
		return depositPaymentTransactionReq;
	}

	/**
	 * this will convert routing auth and routing code data by invoking the ref data api
	 *
	 * @param refDataRequest
	 * @return
	 */
	protected RoutingInfoResponseList getRoutingInfo(List<RoutingInfoRequest> refDataRequest) {
		final String refDataToken = idmsTokenDAO.generateToken().getAccessToken();
		HttpEntity<List<RoutingInfoRequest>> request = new HttpEntity<>(refDataRequest, CommonUtil.createHeaders(refDataToken));
		return refDataDAO.getRoutingInfo(request);
	}

	/**
	 * this will be used to form the ref data routinginfo api request and prepare the corresponding routing instruction object as response
	 *
	 * @param routingInsList
	 */
	protected void invokeRefDataRoutingInfo(List<ReservationRoutingInstruction> routingInsList, String propertyId, boolean rbsToAcrs) {
		String gsePropertyId = referenceDataDAOHelper.retrieveGsePropertyID(propertyId);
		if (CollectionUtils.isNotEmpty(routingInsList)) {
			List<RoutingInfoRequest> refDataRequest = RoomReservationTransformer.buildRefDataRoutingInfoRequest(routingInsList, gsePropertyId, rbsToAcrs);
			if (CollectionUtils.isNotEmpty(refDataRequest)) {
				RoutingInfoResponseList routingInfoResponseList = getRoutingInfo(refDataRequest);
				log.info("Ref Data Call response for RoutingInfo : {}", CommonUtil.convertObjectToJsonString(routingInfoResponseList));
				if (CollectionUtils.isNotEmpty(routingInfoResponseList)) {
					updateRoutingInstruction(routingInsList, routingInfoResponseList, rbsToAcrs);
				}
			}
		}
	}

    /**
     * it will set code from id of alerts and traces or reverse
     *
     * @param alerts
     * @param traces
     * @param splReqObjList
     * @param idToCodeConvert
     */
    protected void invokeRefDataEntities(String propertyId, List<RoomReservationAlert> alerts, List<RoomReservationTrace> traces,
            List<ReservationSplRequest> splReqObjList, boolean idToCodeConvert) {

    	String gsePropertyId = referenceDataDAOHelper.retrieveGsePropertyID(propertyId);
        // 1. create refData search request
        RefDataEntitySearchRefReq refDataEntitySearchRefReq = createAlertAndTraceSearchReq(gsePropertyId, alerts,
                traces, splReqObjList, idToCodeConvert);
        // 2. call ref data
        AlertAndTraceSearchRefDataRes alertAndTraceSearchRes = refDataDAO
                .searchRefDataEntity(refDataEntitySearchRefReq);
        // 3. parse response and set code from Id
        if (CollectionUtils.isNotEmpty(alerts)) {
            Map<String, String> alertAreaIdCodeMap = getRefDataIdCodeMap(alertAndTraceSearchRes,
                    ServiceConstant.ALERT_AREA_CODE, idToCodeConvert);
            Map<String, String> alertIdCodeMap = getRefDataIdCodeMap(alertAndTraceSearchRes, ServiceConstant.ALERT_CODE,
                    idToCodeConvert);
            alerts.forEach(alert -> {
                if (idToCodeConvert) {
                    alert.setArea(alertAreaIdCodeMap.get(alert.getAlertAreaId()));
                    alert.setCode(alertIdCodeMap.get(alert.getAlertCodeId()));
                } else {
                    alert.setAlertAreaId(alertAreaIdCodeMap.get(alert.getArea()));
                    alert.setAlertCodeId(alertIdCodeMap.get(alert.getCode()));
					alert.setCode(alert.getCode());
					alert.setArea(alert.getArea());
                }

            });

        }
        if (CollectionUtils.isNotEmpty(traces)) {
            Map<String, String> traceDeptIdCodeMap = getRefDataIdCodeMap(alertAndTraceSearchRes,
                    ServiceConstant.TRACE_DEPARTMENT, idToCodeConvert);

            traces.forEach(trace -> {
                if (idToCodeConvert) {
                    trace.setDepartmentCode(traceDeptIdCodeMap.get(trace.getDepartmentId()));
                } else {
                    trace.setDepartmentId(traceDeptIdCodeMap.get(trace.getDepartmentCode()));
                }

            });
        }
        // for SPECIAL_REQUEST & ROOMFEATURE
        if (CollectionUtils.isNotEmpty(splReqObjList)) {
            Map<String, String> splReqIdCodeMap = new HashMap<>();
            if (refDataEntitySearchRefReq.stream()
                    .anyMatch(x -> ServiceConstant.ROOM_FEATURE_SPECIAL_REQUEST.equalsIgnoreCase(x.getType()))) {
                splReqIdCodeMap.putAll(getRefDataIdCodeMap(alertAndTraceSearchRes,
                        ServiceConstant.ROOM_FEATURE_SPECIAL_REQUEST, idToCodeConvert));
            }
            splReqObjList.forEach(x -> {
                if (idToCodeConvert) {
                    x.setCode(splReqIdCodeMap.get(x.getId()));
                } else {
					if(x.getExactType().equalsIgnoreCase(ServiceConstant.ROOMFEATURE_FORMAT)) {
						x.setId(splReqIdCodeMap.get(x.getCode()+"|"+ServiceConstant.ROOMFEATURE_FORMAT));
					} else {
						x.setId(splReqIdCodeMap.get(x.getCode()+"|"+ServiceConstant.SPECIALREQUEST_FORMAT));
					}
				}
				// set remaining details
				String code = x.getCode();
				String splId = x.getId();
				Optional<RefDataEntityRes> splReqEntityRes = alertAndTraceSearchRes.stream()
						.filter(res -> ServiceConstant.ROOM_FEATURE_SPECIAL_REQUEST.equalsIgnoreCase(res.getType()))
						.findFirst();
				if (splReqEntityRes.isPresent()) {
					Optional<RefDataEntity> refData = splReqEntityRes.get().getElements().stream()
							.filter(y -> code.equalsIgnoreCase(y.getCode()) && (splId.equalsIgnoreCase(y.getId()) || splId.equalsIgnoreCase(y.getPhoenixId()))).findFirst();
					if (refData.isPresent()) {
						RefDataEntity data = refData.get();
						x.setShortDescription(data.getShortDescription());
						x.setDescription(data.getDescription());
						x.setPricingApplied(data.getPricingApplied());
						if(data.getId().contains(ServiceConstant.ROOMFEATURE_FORMAT)) {
							x.setExactType(ServiceConstant.ROOMFEATURE_FORMAT);
						} else {
							x.setExactType(ServiceConstant.SPECIALREQUEST_FORMAT);
						}
					}
				}
            });
        }
    }

	/**
	 * it will convert AlertAndTraceSearchRefDataRes to id-code or code-id map.
	 * @param alertAndTraceSearchRes
	 * @param type
	 * @param idToCodeConvert
	 * @return
	 */
    private Map<String, String> getRefDataIdCodeMap(AlertAndTraceSearchRefDataRes alertAndTraceSearchRes, String type,
            boolean idToCodeConvert) {

        Optional<RefDataEntityRes> redDataEntityRes = alertAndTraceSearchRes.stream()
                .filter(res -> type.equalsIgnoreCase(res.getType())).findFirst();
        if (redDataEntityRes.isPresent()) {
			if (ServiceConstant.ROOM_FEATURE_SPECIAL_REQUEST.equalsIgnoreCase(type)) {
				return redDataEntityRes.get().getElements().stream().collect(HashMap::new,
						(m, v) -> m.put(
								idToCodeConvert ? StringUtils.isNotEmpty(v.getPhoenixId()) ? v.getPhoenixId() : v.getId()
										: v.getCode()+"|"+(v.getId().contains(ServiceConstant.ROOMFEATURE_FORMAT) ? ServiceConstant.ROOMFEATURE_FORMAT : ServiceConstant.SPECIALREQUEST_FORMAT),
								idToCodeConvert ? v.getCode()
										: StringUtils.isNotEmpty(v.getPhoenixId()) ? v.getPhoenixId() : v.getId()),
						HashMap::putAll);
			} else {
				return redDataEntityRes.get().getElements().stream().collect(HashMap::new,
						(m, v) -> m.put(
								idToCodeConvert ? StringUtils.isNotEmpty(v.getPhoenixId()) ? v.getPhoenixId() : v.getId()
										: v.getCode(),
								idToCodeConvert ? v.getCode()
										: StringUtils.isNotEmpty(v.getPhoenixId()) ? v.getPhoenixId() : v.getId()),
						HashMap::putAll);
			}
        } else {
            log.error(String.format("Error while retrieving %s from refData API.", type));
			return new HashMap<>();
        }

    }


	/**
	 * It will create refDATA request object for alert and traces search by ids or codes
	 * @param alerts
	 * @param traces
	 * @param splReqObjList
	 * @param idToCodeConvert
	 * @return
	 */
    private RefDataEntitySearchRefReq createAlertAndTraceSearchReq(String propertyId, List<RoomReservationAlert> alerts,
            List<RoomReservationTrace> traces, List<ReservationSplRequest> splReqObjList, boolean idToCodeConvert) {
        RefDataEntitySearchRefReq refDataEntitySearchRefReq = new RefDataEntitySearchRefReq();
        if (CollectionUtils.isNotEmpty(traces)) {
            RefDataEntityReq tracesEntityReq = new RefDataEntityReq();
            tracesEntityReq.setType(ServiceConstant.TRACE_DEPARTMENT);
            tracesEntityReq.setPropertyId(propertyId);
            if (idToCodeConvert) {
                tracesEntityReq
                        .setIds(traces.stream().map(RoomReservationTrace::getDepartmentId).collect(Collectors.toList()));
            } else {
                tracesEntityReq
                        .setCodes(traces.stream().map(RoomReservationTrace::getDepartmentCode).collect(Collectors.toList()));
            }
            refDataEntitySearchRefReq.add(tracesEntityReq);
        }
        if(CollectionUtils.isNotEmpty(alerts)) {
            // alert code
            RefDataEntityReq alertEntityReq = new RefDataEntityReq();
            alertEntityReq.setType(ServiceConstant.ALERT_CODE);
            alertEntityReq.setPropertyId(propertyId);
            if (idToCodeConvert) {
                alertEntityReq
                        .setIds(alerts.stream().map(RoomReservationAlert::getAlertCodeId).collect(Collectors.toList()));
            } else {
                alertEntityReq
                        .setCodes(alerts.stream().map(RoomReservationAlert::getCode).collect(Collectors.toList()));
            }
            refDataEntitySearchRefReq.add(alertEntityReq);
         // alert area code
            RefDataEntityReq alertAreaEntityReq = new RefDataEntityReq();
            alertAreaEntityReq.setType(ServiceConstant.ALERT_AREA_CODE);
            alertAreaEntityReq.setPropertyId(propertyId);
            if (idToCodeConvert) {
                alertAreaEntityReq
                        .setIds(alerts.stream().map(RoomReservationAlert::getAlertAreaId).collect(Collectors.toList()));
            } else {
                alertAreaEntityReq
                        .setCodes(alerts.stream().map(RoomReservationAlert::getArea).collect(Collectors.toList()));
            }
            refDataEntitySearchRefReq.add(alertAreaEntityReq);
        }
        // special request and room features
        if (CollectionUtils.isNotEmpty(splReqObjList)) {
            // spl req
            List<ReservationSplRequest> splReqTypeList = splReqObjList.stream()
                    .filter(x -> ServiceConstant.ROOM_FEATURE_SPECIAL_REQUEST.equalsIgnoreCase(x.getType()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(splReqTypeList)) {
                RefDataEntityReq splEntityReq = new RefDataEntityReq();
                splEntityReq.setType(ServiceConstant.ROOM_FEATURE_SPECIAL_REQUEST);
                splEntityReq.setPropertyId(propertyId);
                if (idToCodeConvert) {
                    splEntityReq
                            .setIds(splReqTypeList.stream().map(ReservationSplRequest::getId).collect(Collectors.toList()));
                } else {
                    splEntityReq.setCodes(
                            splReqTypeList.stream().map(ReservationSplRequest::getCode).collect(Collectors.toList()));
                }
                refDataEntitySearchRefReq.add(splEntityReq);
            }
        }
        return refDataEntitySearchRefReq;
    }





    private void updateRoutingInstruction(List<ReservationRoutingInstruction> routingInsList, RoutingInfoResponseList routingInfoResponseList, boolean rbsToAcrs) {
		for (ReservationRoutingInstruction routingIns : routingInsList) {
			if (rbsToAcrs) {
				final String routingAuthorizerId = routingIns.getAuthorizerId();
				if (null != routingAuthorizerId) {
					Optional<RoutingInfoResponse> routingInfoResponse = routingInfoResponseList.stream()
							.filter(x -> null != x.getRoutingAuthDataEntity() && routingAuthorizerId.equals(x.getRoutingAuthDataEntity().getPhoenixId())).findFirst();
					if (routingInfoResponse.isPresent()) {
						routingIns.setHostAuthorizerAppUserId(routingInfoResponse.get().getRoutingAuthDataEntity().getAppUserId());
					} else {
						log.error("Routing Authorizer App User ID not found for Authorizer ID : {}", routingAuthorizerId);
					}
				}
				if (CollectionUtils.isNotEmpty(Arrays.asList(routingIns.getRoutingCodes()))) {
					String[] routingCodesResponse = new String[routingIns.getRoutingCodes().length];
					int counter = 0;
					for (String routingCodeArrangementId : routingIns.getRoutingCodes()) {
						Optional<RoutingInfoResponse> routingInfoResponse = routingInfoResponseList.stream()
								.filter(x -> null != x.getRoutingCodesDataEntity() && routingCodeArrangementId.equals(x.getRoutingCodesDataEntity().getArrangementId())).findFirst();
						if (routingInfoResponse.isPresent()) {
							routingCodesResponse[counter++] = routingInfoResponse.get().getRoutingCodesDataEntity().getCode();
						} else {
							log.error("Routing Code not found for Routing Code Arrangement ID : {}", routingCodeArrangementId);
						}
						routingIns.setHostRoutingCodes(routingCodesResponse);
					}
				}
			} else {
				final String routingAuthorizerAppUserId = routingIns.getHostAuthorizerAppUserId();
				if (null != routingAuthorizerAppUserId) {
					Optional<RoutingInfoResponse> routingInfoResponse = routingInfoResponseList.stream()
							.filter(x -> null != x.getRoutingAuthDataEntity() && StringUtils.isNotEmpty(routingAuthorizerAppUserId) &&
									routingAuthorizerAppUserId.equals(x.getRoutingAuthDataEntity().getAppUserId())).findFirst();
					if (routingInfoResponse.isPresent()) {
						routingIns.setAuthorizerId(routingInfoResponse.get().getRoutingAuthDataEntity().getPhoenixId());
					} else {
						log.error("Routing Authorizer ID not found for App User ID : {}", routingAuthorizerAppUserId);
					}
				}
				if (CollectionUtils.isNotEmpty(Arrays.asList(routingIns.getHostRoutingCodes()))) {
					String[] routingCodesArrangementIdResponse = new String[routingIns.getHostRoutingCodes().length];
					int counter = 0;
					for (String routingCode : routingIns.getHostRoutingCodes()) {
						Optional<RoutingInfoResponse> routingInfoResponse = routingInfoResponseList.stream()
								.filter(x -> null != x.getRoutingCodesDataEntity() && StringUtils.isNotEmpty(routingCode) &&
										routingCode.equals(x.getRoutingCodesDataEntity().getCode())).findFirst();
						if (routingInfoResponse.isPresent()) {
							routingCodesArrangementIdResponse[counter++] = routingInfoResponse.get().getRoutingCodesDataEntity().getArrangementId();
						} else {
							log.error("Routing Code Arrangement ID not found for Routing Code : {}", routingCode);
						}
						routingIns.setRoutingCodes(routingCodesArrangementIdResponse);
					}
				}
			}
		}
	}

	protected static RoomPrice prepareBookingFromRoomPrice(AuroraPriceResponse auroraPriceResponse) {
		RoomPrice booking = new RoomPrice();
		booking.setDate(auroraPriceResponse.getDate());
		booking.setBasePrice(auroraPriceResponse.getBasePrice());
		booking.setComp(auroraPriceResponse.isComp());
		booking.setCustomerPrice(auroraPriceResponse.getDiscountedPrice());
		booking.setPrice(auroraPriceResponse.getDiscountedPrice());
		booking.setOverridePrice(-1);
		booking.setProgramId(auroraPriceResponse.getProgramId());
		booking.setProgramIdIsRateTable(true);
		return booking;
	}

    protected RoomReservation createReservation(ReservationPendingReq reservationReq,
                                                RoomReservation reservation, boolean isCashPayment, String originalProgramId) {
		// if payment by cash or deposit is overwritten then create inn pending
        // with isForceSell = false
        boolean isPoFlow = reservation.isPerpetualPricing();
        if (isCashPayment) {
            IndicatorsReq indicator = new IndicatorsReq();
            indicator.setIsForcedSell(false);
			SegmentReqItem segment = reservationReq.getData().getHotelReservation().getSegments().get(0);
            segment.setIndicators(indicator);
        }
        ReservationPendingRes crsResponse = makeACRSPendingRoomReservation(reservationReq, reservation.getPropertyId(),
                reservation.getAgentInfo(), reservation.getRrUpSell(), reservation.getSource());
       RoomReservation pendingRoomReservation = reservationPendingResTransform(crsResponse);
        if (null == pendingRoomReservation) {
            // throw error
            throw new BusinessException(ErrorCode.RESERVATION_NOT_SUCCESSFUL);
        }

		// Update reservation object with latest DepositCalc from makePending
		reservation.setDepositCalc(pendingRoomReservation.getDepositCalc());

        String confirmationNumber = pendingRoomReservation.getConfirmationNumber();

        // modify pending for payment information
		Map<CreditCardCharge, String> creditCardChargeAuthorizationMap = new HashMap<>();
		boolean hasPaymentCardInfo = CollectionUtils.isNotEmpty(reservation.getCreditCardCharges());
		boolean paymentFailed = false;
		AuthorizationTransactionRequest authorizeRequest = null;
		// handle payment not having cardInfo
		if(!hasPaymentCardInfo) {
			if(validateAcrInventorysWarnings(pendingRoomReservation.getCrsWarnings())) {
				pendingRoomReservation = modifyFOPWithOutCardInfo(pendingRoomReservation, reservation);
			}
		}
		// handle payment having cardInfo
		else{
			//CBSR-1917- Payment widget will be doing afs check and authorise card
			if(!reservation.isSkipPaymentProcess()) {
				try {
					creditCardChargeAuthorizationMap = authorizeCreditCardsOnReservation(reservation, confirmationNumber,
							reservation.getPropertyId(),false);
					authorizeRequest = createBaseAuthorizeRequest(reservation);
				} catch (Exception e) {
					log.error("Payment authorization failed for reservation with confirmation number: '{}' because of  exception:", confirmationNumber, e);
					paymentFailed = true;
				}
			}else{
				creditCardChargeAuthorizationMap = createCreditCardChargeAuthorizationMap(reservation.getCreditCardCharges());
			}

			// update acrs formOfPayment information
			if (!paymentFailed) {
				pendingRoomReservation = updateCardFormOfPayment(confirmationNumber, reservation,
						creditCardChargeAuthorizationMap, reservation.getSource(),
						pendingRoomReservation.getAddOnsDeposits());
			}
		}
		

		String reservationStatus = null;
		String commitedConfNumber = null;
		String processResponseText = null;
        // If payment successes then commit the reservation
		// If isPreventCommit = true then don't commit
		try {
			if (!paymentFailed && validateAcrsWarnings(pendingRoomReservation.getCrsWarnings())) {

	            try {

	                final RoomReservation commitedReservation = commitPendingRoomReservation(confirmationNumber,
	                        reservation.getPropertyId(), reservation.getSource(), isPoFlow);

	                updatePatronPromo(null, reservation, confirmationNumber, acrsProperties);

					if (!isCashPayment) {
						if(!reservation.isSkipPaymentProcess()) {
							creditCardChargeAuthorizationMap.entrySet().stream()
									.filter(entry -> !entry.getValue().equalsIgnoreCase(ServiceConstant.COMP))
									.forEach(entry -> capturePayment(reservation.getCheckInDate(), reservation.getCheckOutDate(), entry, commitedReservation.getPropertyId(),
											confirmationNumber, reservation.getSource()));
						}
						commitedReservation.setPayments(reservation.getCreditCardCharges().stream()
																.map(BaseReservationDao::createPaymentFromCreditCardCharge)
																.collect(Collectors.toList()));
					}
	                commitedReservation.setItineraryId(reservation.getItineraryId());
	                commitedReservation.getProfile().setId(reservation.getProfile().getId());
	                commitedReservation.getProfile().setDateOfBirth(reservation.getProfile().getDateOfBirth());
	                commitedReservation.setCustomerId(reservation.getCustomerId());
	                commitedReservation.setId(reservation.getId());
	                commitedReservation.setGuaranteeCode(reservation.getGuaranteeCode());
	                // Setting the program id as is from the request.
	                // ICE wants program id to be passed in the response if it is
	                // only passed in the
	                // request.
					if(StringUtils.isNotEmpty(originalProgramId)) {
						commitedReservation.setProgramId(originalProgramId);
					}
	                // set routing appUserId to phoenixId
	                commitedReservation.setRoutingInstructions(reservation.getRoutingInstructions());
	                // set spl req from request
	                setReferenceData(commitedReservation);

					// Commit response will not contain credit card details so collecting from request
					// TODO do we need to set amount to 0.0 before returning?
					commitedReservation.setCreditCardCharges(reservation.getCreditCardCharges());

	                // INC-4 alerts and traces
	                commitedReservation.setAlerts(reservation.getAlerts());
	                commitedReservation.setTraces(reservation.getTraces());
					commitedReservation.setPkgComponents(reservation.getPkgComponents());
	                //CBSR-2481. Set Base price from create reservation req for comp create reservation
	                commitedReservation.getBookings().stream().forEach(commitBooking -> {
	                	reservation.getBookings().stream()
	                	.filter(reqBooking -> reqBooking.getDate().equals(commitBooking.getDate()))
	                	.findAny()
	                	.ifPresent(reqBooking -> commitBooking.setBasePrice(reqBooking.getBasePrice()));
	                });
	                if (log.isDebugEnabled()) {
	                    log.debug("Made Reservation with ACRS for confirmationNumber: {}", confirmationNumber);
	                    log.debug("Received committed Reservation Response after setting neccessary details {}.",
	                            CommonUtil.convertObjectToJsonString(commitedReservation));
	                }
	                
	                reservationStatus = ServiceConstant.COMPLETED;
	                processResponseText = ServiceConstant.COMPLETED;
	                commitedConfNumber = commitedReservation.getConfirmationNumber();
	                return commitedReservation;
	            } catch (ParseException parseException) {
	                log.error(parseException.getMessage());
					throw new SystemException(ErrorCode.RESERVATION_NOT_SUCCESSFUL, parseException);
	            }
	        } else {
	        	
	        	reservationStatus = ServiceConstant.CANCELLED_BY_MERCHANT;
	        	processResponseText = ServiceConstant.FAILED_TEXT;
	            // Failed to make deposit so cancel pending reservation
	            if (!ignorePendingRoomReservation(confirmationNumber, reservation.getPropertyId(),
	                    reservation.getSource(), isPoFlow)) {
	                // log system error
	                log.error("System error occurred while cancelling pending reservation with confirmation number: {}",
	                        confirmationNumber);
	            }
	            if (CollectionUtils.isNotEmpty(pendingRoomReservation.getCrsWarnings())) {
	                throw new BusinessException(ErrorCode.INVALID_BOOKINGS,
	                        pendingRoomReservation.getCrsWarnings().toString());
	            }
	            throw new BusinessException(ErrorCode.PAYMENT_AUTHORIZATION_FAILED);
	        }
		}catch (BusinessException | SystemException ex) {
			reservationStatus = ServiceConstant.CANCELLED_BY_MERCHANT;
			processResponseText = ex.getMessage();
			throw ex;
		} finally {
			if (null != authorizeRequest && !reservation.isSkipFraudCheck()) {
				accertifyInvokeHelper.confirmAsyncCall(setAuthorizeRequestStatus(authorizeRequest,reservationStatus,processResponseText),commitedConfNumber);
			}
		}
    }

	public Map<CreditCardCharge, String> createCreditCardChargeAuthorizationMap(List<CreditCardCharge> creditCardCharges) {
		Map<CreditCardCharge, String> cardRecordMap= new HashMap<>();
		if(CollectionUtils.isNotEmpty(creditCardCharges)){
			cardRecordMap = creditCardCharges.stream().filter(c -> StringUtils.isNotBlank(c.getAuthId()))
				.collect(Collectors.toMap(Function.identity(),CreditCardCharge::getAuthId));
	}
		return cardRecordMap;
	}

	public AuthorizationTransactionRequest setAuthorizeRequestStatus(AuthorizationTransactionRequest authorizeReq, String reservationStatus, String processResponseText) {
    	authorizeReq.getTransaction().setOrderStatus(reservationStatus);
    	authorizeReq.getTransaction().setProcessorResponseText(processResponseText);
		return authorizeReq;
    }

	private  RoomReservation modifyFOPWithOutCardInfo(RoomReservation pendingRoomReservation, RoomReservation reservationReq){
		String confirmationNumber = pendingRoomReservation.getConfirmationNumber();
			ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
			ModificationChanges modificationChanges = new ModificationChanges();
			// 1. Create ModificationChange for PaymentInfo
				List<ModificationChangesItem> modificationChangesForPaymentInfo = getModificationChangeForPaymentInfo(
						reservationReq, pendingRoomReservation.getAddOnsDeposits());
				if (CollectionUtils.isNotEmpty(modificationChangesForPaymentInfo)) {
					modificationChanges.addAll(modificationChangesForPaymentInfo);
				}
				//for cash, and guarantee payment we will not have any payment history

			reservationPartialModifyReq.setData(modificationChanges);

			return partialModifyPendingRoomReservation(reservationPartialModifyReq, confirmationNumber,
					reservationReq.getPropertyId(),
					(null != reservationReq.getAgentInfo()) ? reservationReq.getAgentInfo().getAgentId() : null,
					reservationReq.getRrUpSell(), reservationReq.getSource(), reservationReq.isPerpetualPricing());

	}

	public  boolean isCashPayment(String guaranteeCode, List<CreditCardCharge> creditCardCharges){
		// if guaranteeCode = DD and card details is not there then its cash Payment.
    	return ServiceConstant.DD_CASH_TYPE_STRING.equalsIgnoreCase(guaranteeCode) && CollectionUtils.isEmpty(creditCardCharges);

	}

	protected void updatePatronPromo(ReservationRetrieveResReservation existingReservation, RoomReservation newReservation, String confirmationNumber, AcrsProperties acrsProperties) {
		final Collection<UpdatedPromotion> promos =
				RoomReservationTransformer.getUpdatablePromos(existingReservation, newReservation, confirmationNumber, acrsProperties);

		// Updating Patron site id
		promos.forEach(x -> {
			x.setSiteId(referenceDataDAOHelper.getPatronSiteId(x.getPropertyId()));
			x.setPropertyId(null);
		});

		updatePatronPromo(promos, confirmationNumber);
	}

	protected void updatePatronPromo(Collection<UpdatedPromotion> promos, String confirmationNumber) {
		Runnable runnable = () -> {
			// Call Promo update
			try {
				loyaltyDao.updatePlayerPromo(promos);
			} catch (Exception e) {
				log.error("Unable to update patron promo status for confirmation #: {}", confirmationNumber, e);
			}

		};
		executor.execute(runnable);
	}

	protected String getProgramIdByPromo(String propertyId, String promoCode) {
		return roomProgramDao.getProgramByPromoCode(propertyId, promoCode);
	}

	private boolean validateAcrsWarnings(List<Warning> acrsWarnings) {
		boolean inValidWarning = true;
    	if(CollectionUtils.isNotEmpty(acrsWarnings)){
			inValidWarning = acrsWarnings.stream()
					.noneMatch(x -> Boolean.TRUE.equals(x.isIsPreventingCommit()));
		}
    	return inValidWarning;

    }

    public boolean validateAcrInventorysWarnings(List<Warning> acrsWarnings) {
        //TODO revisit check for inventory warning code. or for which warning we have to ignore the reservation.
        if(CollectionUtils.isNotEmpty(acrsWarnings)) {
           return acrsWarnings.stream().noneMatch(warning -> warning.getCode().equals(205));
        }else {
            return true;
        }

    }

    protected List<RoomReservation> createSecondaryReservations(ReservationPendingReq reservationReq,
            RoomReservation reservation, String originalProgramId) {
        List<RoomReservation> sharedReservations = new ArrayList<>();
        //change PolicyTypeCode CC to SH
		SegmentReqItem segment = reservationReq.getData().getHotelReservation().getSegments().get(0);
        segment.getOffer().setPolicyTypeCode("SH");
        // set payment by cash by setting billing to null
        reservation.setCreditCardCharges(null);

        // change guest count to 1
        List<GuestCountReq> requestedGuests = segment.getOffer().getProductUses().get(0).getGuestCounts();
        Optional<GuestCountReq> adultGuestCount = requestedGuests.stream()
                .filter(guest -> StringUtils.equalsIgnoreCase(guest.getOtaCode(), ServiceConstant.NUM_ADULTS_MAP))
                .findFirst();
		if (!adultGuestCount.isPresent()) {
			log.error("Unable To find guestCount in segment during creation of Secondary Reservation.");
			throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
		}
		// update adultCount=1 for all productUses.
		updateGuestCount(reservationReq,1);

        // get shareWithCustomers
        reservation.getShareWithCustomers().forEach(sharedProfile -> {
            // modify profile details in acrs pending req.
            GuestsPendingReqItem userProfile = RoomReservationTransformer.reservationProfileToACRSUserProfileReq(sharedProfile, 0, 0, null);
            GuestsPendingReq userProfileReqs = new GuestsPendingReq();
            userProfileReqs.add(userProfile);
            reservationReq.getData().getHotelReservation().setUserProfiles(userProfileReqs);
            reservation.setProfile(sharedProfile);
            RoomReservation secondaryReservation = createReservation(reservationReq, reservation, true,
                    originalProgramId);
			secondaryReservation.setCustomerId(sharedProfile.getId());
			sharedReservations.add(secondaryReservation);
        });
        return sharedReservations;
    }


	protected Map<CreditCardCharge, String> authorizeCreditCardsOnReservation(RoomReservation reservation,
			String cnfNumber, String propertyId, boolean isModifyFlow) {
		boolean isEnableZeroAmountAuth = !isModifyFlow && accertifyInvokeHelper.isEnableZeroAmountAuth();

		if (accertifyInvokeHelper.performAFSCheck(reservation)) {
			AuthorizationTransactionRequest authorizeRequest = createBaseAuthorizeRequest(reservation);
			makeAFSAuthorize(authorizeRequest, reservation.getSource());
		}

		final Map<CreditCardCharge, String> creditCardChargeAuthorizationMap = new HashMap<>();
		double minAuthorizeAmount = isEnableZeroAmountAuth ? 0.0 : 0.01;
		reservation.getCreditCardCharges().forEach(creditCardCharge -> {
			if (creditCardCharge.getAmount() >= minAuthorizeAmount) {
				creditCardChargeAuthorizationMap.put(creditCardCharge,
						makeAuthorizePayment(reservation.getCheckInDate(), reservation.getCheckOutDate(), creditCardCharge, propertyId, cnfNumber, reservation.getSource()));
			} else if (creditCardCharge.getAmount() <= -0.01) {
				creditCardChargeAuthorizationMap.put(creditCardCharge, ServiceConstant.REFUND);
			} else {
				creditCardChargeAuthorizationMap.put(creditCardCharge, ServiceConstant.COMP);
			}
		});
		return creditCardChargeAuthorizationMap;
	}

	protected List<String> getSharedReservationConfirmationNumbers(String cfNumber, String linkId, String source) {
		final ReservationSearchResPostSearchReservations reservationSearchRes = searchReservationByLink(cfNumber,linkId,source);
		return RoomReservationTransformer.collectSharedReservationConfirmationNumbers(reservationSearchRes.getData().getHotelReservations());
	}
	protected ReservationSearchResPostSearchReservations searchReservationByLink(String cfNumber, String linkId, String source){
		final ReservationSearchReq reservationSearchRequest = RoomReservationTransformer
				.buildSearchRequestfromRoomReservation(cfNumber, linkId);

		return searchReservationsByReservationSearchReq(
				reservationSearchRequest, source);
	}

	protected Optional<String> getSharedLinkId(List<ReservationLinks> links) {
		return CollectionUtils.isNotEmpty(links) ? links.stream().filter(link -> link.getType().equals(LinkType.SHARE))
				.findFirst().map(ReservationLinks::getId) : Optional.empty();
	}

	/**
     * Retrieve Reservation By Confirmation Number
     *
     * @param confirmationNumber
     * @return ReservationRetrieveRes
     */
    protected ReservationRetrieveResReservation retrieveReservationByConfirmationNumber(String confirmationNumber, String source) {
        return retrieveReservationByCnfNumber(confirmationNumber, source);
    }


	/**
	 * ACRS  retrieve Reservation
	 * @param confirmationNumber
	 * @param source
	 * @return
	 */
	protected ReservationRetrieveResReservation retrieveReservationByCnfNumber(String confirmationNumber,
 																				String source) {
		//Fetch reservation directly from ACRS
		if(referenceDataDAOHelper.isPETDisabled()){
			return retrieveReservationByCnfNumberDirectly(confirmationNumber, source);
		}else {
			return retrieveReservationByCnfNumberWithExchangeToken(confirmationNumber, source);
		}
	}

    /**
     * ACRS  retrieve Reservation via Payment Token
     * @param confirmationNumber
     * @param source
     * @return
     */
    protected ReservationRetrieveResReservation retrieveReservationByCnfNumberWithExchangeToken(String confirmationNumber,
            String source) {
        final String acrsVendor = referenceDataDAOHelper.getAcrsVendor(source);
        final Map<String, ACRSAuthTokenResponse> acrsAuthTokenResponseMap = acrsOAuthTokenDAOImpl.generateToken();
        final String tokenPath = "paymentCard.cardNumber.token";
        final DestinationHeader destinationHeader = CommonUtil.createDestinationHeader(null, acrsVendor,ServiceConstant.HTTP_METHOD_GET, null, null);
        destinationHeader.setXAuthorization(
                ServiceConstant.HEADER_AUTH_BEARER + acrsAuthTokenResponseMap.get(acrsVendor).getToken());
		ReservationRetrieveResReservation reservationRetrieveRes = paymentDao.sendRetrieveRequestToPaymentExchangeToken(tokenPath, destinationHeader, confirmationNumber,
				"$..paymentCard.expireDate");

		// merge subTotalServiceCharges into taxList
		SegmentResItem mainSegment = BaseAcrsTransformer.getMainSegment(reservationRetrieveRes.getData().getHotelReservation().getSegments());
		RoomReservationTransformer.mergeSubTotalServCrgIntoTaxList(mainSegment);
		return reservationRetrieveRes;

    }

    /**
     * This will set all the reference data like alert, traces, special request
     * @param roomReservation
     */

    protected void setReferenceData(RoomReservation roomReservation) {
        // convert id to code for alerts , traces and spl req from refData.
        if (CollectionUtils.isNotEmpty(roomReservation.getAlerts())
                || CollectionUtils.isNotEmpty(roomReservation.getTraces())
                || CollectionUtils.isNotEmpty(roomReservation.getSpecialRequestObjList())) {
            invokeRefDataEntities(roomReservation.getPropertyId(), roomReservation.getAlerts(),
                    roomReservation.getTraces(), roomReservation.getSpecialRequestObjList(), false);
        }
        // Set reservation special req/ room features and purchasedComponents.
        List<String> allSpecialReqs = new ArrayList<>();
        List<PurchasedComponent> purchasedComponents = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(roomReservation.getSpecialRequestObjList())) {
            List<String> splAndRoomFeatures = roomReservation.getSpecialRequestObjList().stream()
                    .map(ReservationSplRequest::getId).collect(Collectors.toList());
            allSpecialReqs.addAll(splAndRoomFeatures);
            purchasedComponents
                    .addAll(BaseAcrsTransformer.buildComponentDetails(roomReservation.getSpecialRequestObjList()));
        }
        if (CollectionUtils.isNotEmpty(roomReservation.getPurchasedComponents())) {
            purchasedComponents.addAll(roomReservation.getPurchasedComponents());
            List<String> addons = roomReservation.getPurchasedComponents().stream().map(PurchasedComponent::getId)
                    .collect(Collectors.toList());
            allSpecialReqs.addAll(addons);
        }
        roomReservation.setSpecialRequests(allSpecialReqs);
        roomReservation.setPurchasedComponents(purchasedComponents);

    }

	/**
	 * This method reset the PerpetualPricing flag if the applicable programs are not PO or
	 * the request has a promo input
	 * @param reservation
	 */
	protected void resetPerpetualPricing(RoomReservation reservation) {

		boolean perpetualPricing = reservation.isPerpetualPricing();
		if (perpetualPricing) {

			// In case of Promo make PO = false
			final String promo = reservation.getPromo();
			// In case of MyVegasPromoCode make PO = false
			final String myVegasPromoCode = reservation.getMyVegasPromoCode();
			String programId;
			if (StringUtils.isNotEmpty(promo) || StringUtils.isNotEmpty(myVegasPromoCode)) {
				perpetualPricing = false;
			} else {

				/*// In case parent program is not a PO then set PO = false , commenting this to fix PO issue
				if (!ACRSConversionUtil.isPORatePlan(ACRSConversionUtil.getRatePlanCode(programId))) {
					perpetualPricing = false;
				}*/
				// In case any booking program is a PO then set PO = true
				final List<RoomPrice> bookings = reservation.getBookings();
				if (CollectionUtils.isNotEmpty(bookings)) {
					for (RoomPrice booking : bookings) {
						programId = booking.getProgramId();
						final String overrideProgramId = booking.getOverrideProgramId();
						if (StringUtils.isNotEmpty(overrideProgramId)) {
							programId = overrideProgramId;
						}
						// If programId is PO then set perpetualFlag to true and break
						// For setting perpetualPricing flag to false, check all the program IDs in the bookings object
						if (StringUtils.isNotEmpty(programId)
								&& ACRSConversionUtil.isPORatePlan(ACRSConversionUtil.getRatePlanCode(programId))) {
							perpetualPricing = true;
							break;
						} else {
							perpetualPricing = false;
						}
					}
				}
			}

			reservation.setPerpetualPricing(perpetualPricing);
		}

	}

	/**
	 * This method reset the PerpetualPricing flag if the applicable programs are not PO or
	 * the request has a promo input
	 * @param reservation
	 */
	protected void resetPerpetualPricingForHold(RoomReservation reservation) {
		boolean perpetualPricing;

		// In case of Promo make PO = false
		final String promo = reservation.getPromo();
		String programId = reservation.getProgramId();
		if (StringUtils.isNotEmpty(promo)) {
			perpetualPricing = false;
		} else {
			//In case parent program is not a PO then set PO = false
			if (!ACRSConversionUtil.isPORatePlan(ACRSConversionUtil.getRatePlanCode(programId))) {
				perpetualPricing = false;
			} else {
				perpetualPricing = true;
			}
		}

		reservation.setPerpetualPricing(perpetualPricing);
	}

    private boolean hasModifiedCustomData(ModificationChangesRequest modificationChangesRequest,
            RoomReservation fetchedReservation) {
    	//checking to see if bookings are null as we have incase of profile modifications
    	if(null == modificationChangesRequest.getBookings()) {
    		return false;
    	}
        boolean isProgramModified = !StringUtils.equals(modificationChangesRequest.getProgramId(),
                fetchedReservation.getProgramId());

        List<RoomPrice> existingOverridenBookings = fetchedReservation.getBookings().stream()
                .filter(existing -> StringUtils.isNotEmpty(existing.getOverrideProgramId()))
                .collect(Collectors.toList());
        List<RoomPrice> modifiedOverridenBookings = modificationChangesRequest.getBookings().stream()
                .filter(existing -> StringUtils.isNotEmpty(existing.getOverrideProgramId()))
                .collect(Collectors.toList());
        boolean isOverridenBookingModified = !CollectionUtils.isEqualCollection(existingOverridenBookings,
                modifiedOverridenBookings);

        return (isProgramModified || isOverridenBookingModified);
    }
    
    public AuthorizationTransactionRequest createBaseAuthorizeRequest(RoomReservation reservation) {
    	AuthorizationTransactionRequest authorizeRequest = null;
    	if (accertifyInvokeHelper.performAFSCheck(reservation)) {
    		authorizeRequest = accertifyInvokeHelper.createAuthorizeRequest(reservation,
					reservation.getInSessionReservationId());
    	}
    	return authorizeRequest;
    }

    public void addComponentTaxAndCharges(RoomChargesAndTaxes chargesAndTaxesCalc,
            List<RoomBookingComponent> availableComponents, List<String> inputComponents) {
		if(CollectionUtils.isNotEmpty(availableComponents) && CollectionUtils.isNotEmpty(inputComponents)) {
			List<RoomBookingComponent> appliedComponentList = availableComponents.stream()
					.filter(splReq -> inputComponents.contains(splReq.getId())).collect(Collectors.toList());
			Map<String, Long> componentCountMap = inputComponents.stream()
					.collect(Collectors.groupingBy(component -> component, Collectors.counting()));
			// set charges
			chargesAndTaxesCalc.getCharges().forEach(charge -> {
				Date date = charge.getDate();
				appliedComponentList.forEach(appliedComponent -> {
					Optional<ComponentPrice> componentPrice = appliedComponent.getPrices().stream()
							.filter(price -> price.getDate().equals(date)).findFirst();
					if (componentPrice.isPresent()) {
						Long qty = componentCountMap.get(appliedComponent.getId());
						ItemizedChargeItem componentCharge = new ItemizedChargeItem();
						componentCharge.setAmount(componentPrice.get().getAmount() * qty);
						componentCharge.setItemType(RoomChargeItemType.ComponentCharge);
						componentCharge.setItem(appliedComponent.getCode());
						componentCharge.setPricingApplied(appliedComponent.getPricingApplied());
						componentCharge.setShortDescription(appliedComponent.getShortDescription());
						charge.getItemized().add(componentCharge);
						charge.setAmount(charge.getAmount() + componentCharge.getAmount());
					}

				});
			});

			// set taxes
			chargesAndTaxesCalc.getTaxesAndFees().forEach(charge -> {
				Date date = charge.getDate();
				appliedComponentList.forEach(appliedComponent -> {
					Long qty = componentCountMap.get(appliedComponent.getId());
					Optional<ComponentPrice> componentPrice = appliedComponent.getPrices().stream()
							.filter(price -> price.getDate().equals(date)).findFirst();
					if (componentPrice.isPresent()) {
						ItemizedChargeItem componentCharge = new ItemizedChargeItem();
						componentCharge.setAmount(componentPrice.get().getTax() * qty);
						componentCharge.setItemType(RoomChargeItemType.ComponentChargeTax);
						componentCharge.setItem(appliedComponent.getCode());
						componentCharge.setPricingApplied(appliedComponent.getPricingApplied());
						componentCharge.setShortDescription(appliedComponent.getShortDescription());
						charge.getItemized().add(componentCharge);
						charge.setAmount(charge.getAmount() + componentCharge.getAmount());
					}

				});
			});
		}

    }
    
    public double getComponentDeposit(List<RoomBookingComponent> availableComponents, List<String> inputComponents) {
		if(CollectionUtils.isNotEmpty(availableComponents) && CollectionUtils.isNotEmpty(inputComponents)) {
			Map<String, Long> componentCountMap = inputComponents.stream()
					.collect(Collectors.groupingBy(component -> component, Collectors.counting()));
			List<RoomBookingComponent> appliedComponentList = availableComponents.stream()
					.filter(RoomBookingComponent::getIsDespsitRequired)
					.filter(splReq -> inputComponents.contains(splReq.getId())).collect(Collectors.toList());
			Map<RoomBookingComponent, Long> map = new HashMap<>();
			appliedComponentList.forEach(e -> map.put(e, componentCountMap.get(e.getId())));
			return map.entrySet().stream()
					.mapToDouble(e -> e.getKey().getDepositAmount() * e.getValue()).sum();
		}
		else
			return 0;
    }

	public boolean isExistingPkgReservation(String propertyIdOrCode, List<PurchasedComponent> purchasedComponents) {
		return reservationDAOHelper.checkIfExistingPkgReservation(propertyIdOrCode, purchasedComponents);
	}

	public List<RoomRequest> getMissingPkgRoomRequests(String propertyId,
			List<RoomRequest> roomRequests, List<PurchasedComponent> purchasedComponents) {
		return reservationDAOHelper.getMissingPkgRoomRequests(propertyId, roomRequests, purchasedComponents);
	}

	public List<String> getMissingPkgComponents(String propertyId,
			List<String> specialRequests, List<PurchasedComponent> purchasedComponents) {
		return reservationDAOHelper.getMissingPkgComponents(propertyId, specialRequests, purchasedComponents);
	}
}
