package com.mgm.services.booking.room.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.FailureReason;
import com.mgm.services.booking.room.model.PaymentInfo;
import com.mgm.services.booking.room.model.RoomCartItem;
import com.mgm.services.booking.room.model.UserAddress;
import com.mgm.services.booking.room.model.UserProfile;
import com.mgm.services.booking.room.model.authorization.TransactionMappingRequest;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.ActivateCustomerRequest;
import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.booking.room.model.request.MyVegasRequest;
import com.mgm.services.booking.room.model.request.PaymentTokenizeRequest;
import com.mgm.services.booking.room.model.request.ReservationRequest;
import com.mgm.services.booking.room.model.reservation.AgentInfo;
import com.mgm.services.booking.room.model.reservation.CardHolderProfile;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.ConsolidatedRoomReservationResponse;
import com.mgm.services.booking.room.model.response.CreateCustomerResponse;
import com.mgm.services.booking.room.model.response.FailedRoomReservationResponse;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.AccertifyService;
import com.mgm.services.booking.room.service.IATAV2Service;
import com.mgm.services.booking.room.service.MyVegasService;
import com.mgm.services.booking.room.service.OneTrustService;
import com.mgm.services.booking.room.service.PaymentTokenizationService;
import com.mgm.services.booking.room.service.ProfileEmailService;
import com.mgm.services.booking.room.service.ProfileManagementService;
import com.mgm.services.booking.room.service.ReservationEmailService;
import com.mgm.services.booking.room.service.ReservationService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.transformer.CustomerRequestTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.validator.ReservationRequestValidator;
import com.mgm.services.common.controller.BaseController;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.CartItem;
import com.mgm.services.common.model.Customer;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import com.mgm.services.common.model.RedemptionValidationResponse;
import com.mgm.services.common.model.ServicesSession;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;
import com.mgm.services.common.util.DateUtil;
import com.mgmresorts.aurora.service.EAuroraException;
import com.mgmresorts.myvegas.jaxb.Customer.Addresses;
import com.mgmresorts.myvegas.jaxb.Customer.Phones;

import lombok.extern.log4j.Log4j2;

/**
 * Controller to handle room booking confirmation service for checkout.
 *
 */
@RestController
@RequestMapping("/v1/reserve")
@Log4j2
public class ReservationController extends BaseController {

	private final Validator validator = new ReservationRequestValidator();

	@Autowired
	private ReservationService reservationService;

	@Autowired
	private AccertifyService transactionService;

	@Autowired
	private ReservationEmailService emailService;

	@Autowired
	private MyVegasService myVegasService;

	@Autowired
	private IATAV2Service iataService;

	@Autowired
	private RoomProgramCacheService roomProgramCacheService;

	@Autowired
	private ApplicationProperties appProperties;
	
	@Autowired
    private PaymentTokenizationService paymentTokenizationService;

	@Autowired
	private ProfileManagementService profileService;

	@Autowired
	private OneTrustService oneTrustService;

	@Autowired
	private ProfileEmailService profileEmailService;

	/**
	 * Service to be called on checkout to confirm the booking. Accepts users
	 * profile and billing information to process the reservation. Returns booking
	 * error or confirmation number. Service internally integrates with Accertify
	 * for anti-fraud protection. If an iata code is attached, then validate the
	 * same. If the code is not valid, then an exception will be thrown. If the code
	 * is valid, the reservation flow will continue
	 * 
	 * @param source             Source header
	 * @param skipMyVegasConfirm whether to skip myvegas confirmation mainly used
	 *                           for test cases to skip consuming a redemption code
	 * @param reservationRequest Reservation request
	 * @return Returns confirmed room reservation response
	 */
	@PostMapping("/room")
	public ConsolidatedRoomReservationResponse reserveRoom(@RequestHeader String source,
			@RequestHeader(defaultValue = "false") String skipMyVegasConfirm,
			@RequestBody ReservationRequest reservationRequest) {

		preprocess(source, reservationRequest, null);
		Errors errors = new BeanPropertyBindingResult(reservationRequest, "reservationRequest");
		reservationRequest.setEligibleForAccountCreation(CommonUtil.isEligibleForAccountCreation(sSession, source));
		validator.validate(reservationRequest, errors);
		handleValidationErrors(errors);

		if (StringUtils.isNotBlank(reservationRequest.getIata())) {
			iataService.validateCode(reservationRequest.getIata());
			return reserveAllRoomsInCart(source, reservationRequest, skipMyVegasConfirm);
		} else {
			return reserveAllRoomsInCart(source, reservationRequest, skipMyVegasConfirm);
		}
	}

	private ConsolidatedRoomReservationResponse reserveAllRoomsInCart(String source,
			ReservationRequest reservationRequest, String skipMyVegasConfirm) {

		List<RoomReservationResponse> reservationResponse = new ArrayList<>();
		ConsolidatedRoomReservationResponse combinedResponse = new ConsolidatedRoomReservationResponse();

		List<RoomCartItem> roomCartItems = CommonUtil.getRoomCartItems(sSession.getCartItems(), source);
		
		if (reservationRequest.isEligibleForAccountCreation()) {
			log.info("Invoking user sign-up to process JWB booking");
			createOrActivateAccount(source, reservationRequest);
			log.info("Successful user sign-up to process JWB booking");
		}

        roomCartItems.forEach(cartItem -> reservationResponse.add(
                makeIndividualRoomReservation(cartItem, source, reservationRequest, skipMyVegasConfirm, sSession)));

        reservationResponse.forEach(response -> {
            if (response != null && StringUtils.isNotEmpty(response.getConfirmationNumber())) {
                combinedResponse.getBooked().add(response);
            } else {
                FailedRoomReservationResponse failedResponse = new FailedRoomReservationResponse();
                failedResponse.setItemId(response.getItemId());
                failedResponse.setFailure(response.getFailureReason());
                combinedResponse.getFailed().add(failedResponse);
            }
            if (null != response.getMessages()) {
                combinedResponse.setMessages(response.getMessages());
            }

        });

        log.info(
                "Cart purchase summary: Number of items in cart: {}, Number of items failed: {}, Number of items booked: {}",
                roomCartItems.size(), combinedResponse.getFailed().size(), combinedResponse.getBooked().size());

		return combinedResponse;

	}

	private void createOrActivateAccount(String source, ReservationRequest reservationRequest) {
		final CreateCustomerRequest createCustomerRequest = CustomerRequestTransformer
				.getCreateCustomerRequest(reservationRequest, source);
		log.debug("invoking createCustomer with customerEmail {} .. .. .. .. .. .. .. .. .. .. .. .. .. .. .. ..",
				createCustomerRequest.getCustomerEmail());
		CreateCustomerResponse createCustomerResponse = profileService.createCustomer(createCustomerRequest);
		sSession.setCustomer(createCustomerResponse.getCustomer());

		if (createCustomerResponse.isInactiveWebProfile()) {
			final ActivateCustomerRequest activateCustReq = CustomerRequestTransformer
					.getActivateCustomerRequest(reservationRequest);
			profileService.activateCustomer(activateCustReq);
		}

		reservationRequest.setCustomerId(createCustomerResponse.getCustomer().getCustomerId());

		profileEmailService.sendAccountCreationMail(createCustomerRequest, createCustomerResponse.getCustomer());

		oneTrustService.createOneTrustUser(createCustomerResponse.getCustomer());
	}

	private RoomReservationResponse makeIndividualRoomReservation(RoomCartItem cartItem, String source,
			ReservationRequest reservationRequest, String skipMyVegasConfirm, ServicesSession sSession) {
		RoomReservationResponse response = null;

		RoomReservation roomReservation = cartItem.getReservation();
		roomReservation.setSource(source);
		roomReservation.setProfile(getProfile(reservationRequest, sSession));
		roomReservation.setCreditCardCharges(getPayments(reservationRequest, roomReservation));
		List<RoomRequest> bookedRoomRequests = new ArrayList<>();
        cartItem.getAvailableComponents().forEach(request -> {
            if (request.isSelected()) {
                bookedRoomRequests.add(request);
            }
        });
        if (StringUtils.isNotBlank(reservationRequest.getIata())) {
            AgentInfo agentInfo = new AgentInfo();
            agentInfo.setAgentId(reservationRequest.getIata());
            agentInfo.setAgentType("IATA");
            roomReservation.setAgentInfo(agentInfo);
        }
        
		AuthorizationTransactionRequest authorizeRequest = createAuthorizeRequest(roomReservation, sSession,
				reservationRequest);
		log.debug("Request to Accertify: {}", authorizeRequest);

		RoomProgram roomProgram = roomProgramCacheService.getRoomProgram(roomReservation.getProgramId());
		// Sequence of calls are different for myvegas vs regular booking
		if (CommonUtil.isEligibleForMyVegasRedemption(sSession.getMyVegasRedemptionItems(), roomProgram,
				sSession.getCustomer())) {
			log.info("reserveRoomWithRedemption : Request for room reservation with redemption code {}::{}",
					roomReservation.getProgramId(), roomReservation);

			try {
				response = makeReservationWithRedemption(roomReservation, authorizeRequest, cartItem.getReservationId(),
						bookedRoomRequests, skipMyVegasConfirm);
			} catch (Exception ex) {
				response = processException(cartItem.getReservationId(), ex);
			}
		} else {
			log.info("reserveRoomWithoutRedemption : Request for room reservation without redemption code::{}",
					roomReservation);
			try {
				response = makeReservationWithoutRedemption(roomReservation, authorizeRequest, sSession,
						cartItem.getReservationId(), bookedRoomRequests);
				if (cartItem.isPromotedMlifePrice()) {
					log.info("Reservation {} made as part of JWB", response.getConfirmationNumber());
				}
			} catch (Exception ex) {
				response = processException(cartItem.getReservationId(), ex);
			}

		}

		return response;
	}

	private RoomReservationResponse processException(String reservationId, Throwable ex) {

		String exceptionType = ex.getClass().getName();
		ErrorCode errorCode = ErrorCode.SYSTEM_ERROR;

		RoomReservationResponse response = new RoomReservationResponse();
		response.setItemId(reservationId);
		FailureReason reason = new FailureReason();
		reason.setType(ServiceConstant.MESSAGE_TYPE_ERROR);

		if (exceptionType.equals(EAuroraException.class.getName())) {
			log.info("Aurora Exception while trying to reserve the room for reservationId:{}. Exception Details >> {}",
					reservationId, ex);

			if (isPaymentFailure(ex.getMessage())) {
				errorCode = ErrorCode.PAYMENT_FAILED;
				log.info("Checkout attempt resulted in payment failure for reservation id: {}", reservationId);
			} else {
				reason.setCode(EAuroraException.class.getSimpleName());
				reason.setReason(ex.getMessage());
				response.setFailureReason(reason);
				return response;
			}
		} else if (exceptionType.equals(BusinessException.class.getName())) {
			BusinessException exception = (BusinessException) ex;
			errorCode = exception.getErrorCode();
		} else if (exceptionType.equals(SystemException.class.getName())) {
			SystemException exception = (SystemException) ex;
			errorCode = exception.getErrorCode();
		} else if (exceptionType.equals(WebExchangeBindException.class.getName())) {
			WebExchangeBindException exception = (WebExchangeBindException) ex;
			String error = exception.getAllErrors().get(0).getDefaultMessage();
			errorCode = ErrorCode.getErrorCode(error);
		}
		log.info("Exception {}|{} while trying to reserve the room for reservationId::{}. Exception Details >> {}",
				errorCode.getErrorCode(), errorCode.getDescription(), reservationId, ex);

		reason.setCode(errorCode.getErrorCode());
		reason.setReason(errorCode.getDescription());
		response.setFailureReason(reason);
		return response;
	}

	private RoomReservationResponse makeReservationWithoutRedemption(final RoomReservation roomReservationObj,
			AuthorizationTransactionRequest authorizeRequest, ServicesSession sSession, String reservationId,
			List<RoomRequest> bookedRoomRequests) {
		// Authorize with anti-fraud, make reservation and send transaction
		// confirmation
		boolean authorized = true;
		try {
			AuthorizationTransactionResponse txResponse = transactionService.authorize(authorizeRequest);
			log.info("reserveRoomWithRedemption::Response from Accertify:{}", txResponse);
			authorized = txResponse.isAuthorized();
		} catch (Exception ex) {
			log.error("Error from anti-fraud service: ", ex);
			throw new BusinessException(ErrorCode.AFS_FAILURE);
		}

		if (authorized) {
			RoomReservation reservation = reservationService.makeRoomReservation(roomReservationObj);

			RoomReservationResponse response = processRoomReservationResponse(reservation, sSession, reservationId,
					bookedRoomRequests);
			log.info("Financial Impact:{}", ReservationUtil.getReservationFinancialImpact(response));
			sendTransactionConfirmation(authorizeRequest, reservation);

			return response;
		} else {
			throw new BusinessException(ErrorCode.TRANSACTION_NOT_AUTHORIZED);
		}

	}

	private RoomReservationResponse makeReservationWithRedemption(final RoomReservation roomReservationObj,
			AuthorizationTransactionRequest authorizeRequest, String reservationId,
			List<RoomRequest> bookedRoomRequests, String skipMyVegasConfirm) {
		Map<String, RedemptionValidationResponse> myVegasRedemptionItems = sSession.getMyVegasRedemptionItems();
		MyVegasRequest myVegasRequest = new MyVegasRequest();
		myVegasRequest
				.setRedemptionCode(myVegasRedemptionItems.get(roomReservationObj.getProgramId()).getRedemptionCode());
		// If reservation is for myvegas, validate the redemption code and
		// authorize the transaction with anti-fraud simultaneously. Once both
		// calls done, proceed with booking. Once booking is complete, send
		// redemption code confirmation to myvegas.
		RedemptionValidationResponse redemptionResponse = myVegasService.validateRedemptionCode(myVegasRequest);
		if (redemptionResponse == null || StringUtils.isEmpty(redemptionResponse.getStatus())) {
			throw new BusinessException(ErrorCode.REDEMPTION_OFFER_NOT_AVAILABLE);
		}
		if (!CommonUtil.isMatchingMyVegasProfile(sSession.getCustomer(), redemptionResponse.getCustomer())) {
			throw new BusinessException(ErrorCode.OFFER_NOT_ELIGIBLE);
		}

		boolean authorized = true;
		try {
			AuthorizationTransactionResponse txResponse = transactionService.authorize(authorizeRequest);
			log.info("reserveRoomWithRedemption::Response from Accertify:{}", txResponse);
			authorized = txResponse.isAuthorized();
		} catch (Exception ex) {
			log.error("Error from anti-fraud service: ", ex);
			throw new BusinessException(ErrorCode.AFS_FAILURE);
		}

		if (authorized) {

			String comment = "MyVegasData -" + sSession.getCustomer().getMlifeNumber() + "_"
					+ redemptionResponse.getRedemptionCode() + "_" + appProperties.getMyvegasAuthCode() + "_";
			roomReservationObj.setAdditionalComments(Collections.singletonList(comment));

			RoomReservation reservation = reservationService.makeRoomReservation(roomReservationObj);

			RoomReservationResponse response = processRoomReservationResponse(reservation, sSession, reservationId,
					bookedRoomRequests);
			log.info("Reservation Details: {}", reservation);
			log.info("Reservation Response: {}", response);
			log.info("Financial Impact:{}", ReservationUtil.getReservationFinancialImpact(response));

			confirmMyvegasRedemption(myVegasRequest, response, redemptionResponse, skipMyVegasConfirm);
			removeRedemptionItemFromSession(roomReservationObj.getProgramId());
			sendTransactionConfirmation(authorizeRequest, reservation);
			return response;
		} else {
			throw new BusinessException(ErrorCode.TRANSACTION_NOT_AUTHORIZED);
		}

	}

	private void removeRedemptionItemFromSession(String programId) {
		sSession.getMyVegasRedemptionItems().remove(programId);

	}

	private void confirmMyvegasRedemption(MyVegasRequest myVegasRequest, RoomReservationResponse response,
			RedemptionValidationResponse redemptionResponse, String skipMyVegasConfirm) {

		myVegasRequest.setConfirmationNumber(response.getConfirmationNumber());

		log.info("Myvegas Booking Details: {}, {}", response.getTripDetails().getCheckInDate(),
				response.getPropertyId());
		log.info("Book Date: {}",
				DateUtil.convertDateToString(ServiceConstant.DEFAULT_DATE_FORMAT,
						response.getTripDetails().getCheckInDate(),
						TimeZone.getTimeZone(appProperties.getTimezone(response.getPropertyId()))));
		myVegasRequest.setReservationDate(DateUtil.convertDateToString(ServiceConstant.DEFAULT_DATE_FORMAT,
				response.getTripDetails().getCheckInDate(),
				TimeZone.getTimeZone(appProperties.getTimezone(response.getPropertyId()))));
		myVegasRequest.setCouponCode(redemptionResponse.getCouponCode());
		com.mgmresorts.myvegas.jaxb.Customer myvegasCust = new com.mgmresorts.myvegas.jaxb.Customer();
		Customer customer = sSession.getCustomer();
		myvegasCust.setFirstName(customer.getFirstName());
		myvegasCust.setLastName(customer.getLastName());
		myvegasCust.setEmailID(customer.getEmailAddress());
		myvegasCust.setMembershipID(String.valueOf(customer.getMlifeNumber()));
		myvegasCust.setDateOfBirth(DateUtil.convertDateToString(ServiceConstant.DEFAULT_DATE_FORMAT,
				customer.getDateOfBirth(), TimeZone.getDefault()));
		myvegasCust.setDateOfEnrollment(DateUtil.convertDateToString(ServiceConstant.DEFAULT_DATE_FORMAT,
				customer.getDateOfEnroll(), TimeZone.getDefault()));
		myvegasCust.setTier(customer.getTier());

		if (!CollectionUtils.isEmpty(customer.getPhoneNumbers())) {
			Phones phone = new Phones();
			phone.setType(customer.getPhoneNumbers().get(0).getType());
			phone.setNumber(customer.getPhoneNumbers().get(0).getNumber());
			myvegasCust.getPhones().add(phone);
		}

		if (!CollectionUtils.isEmpty(customer.getAddresses())) {
			ProfileAddress pAddress = customer.getAddresses().get(0);
			Addresses address = new Addresses();
			address.setType(pAddress.getType());
			address.setStreet1(pAddress.getStreet1());
			address.setStreet2(pAddress.getStreet2());
			address.setCity(pAddress.getCity());
			address.setState(pAddress.getState());
			address.setPostalCode(pAddress.getPostalCode());
			address.setCountry(pAddress.getCountry());

			myvegasCust.getAddresses().add(address);

		}

		myVegasRequest.setCustomer(myvegasCust);
		if (skipMyVegasConfirm == null || !skipMyVegasConfirm.equalsIgnoreCase(ServiceConstant.TRUE)) {
			myVegasService.confirmRedemptionCode(myVegasRequest);
		}
	}

	private RoomReservationResponse processRoomReservationResponse(RoomReservation reservation,
			ServicesSession sSession, String reservationId, List<RoomRequest> bookedRoomRequests) {
		log.info("reserveRoom::Recieved response from the Room Booking service");
		// Setting InSessionReservationId to retain cart item id
		reservation.setInSessionReservationId(reservationId);
		RoomReservationResponse response = RoomReservationTransformer.transform(reservation, appProperties);
		emailService.sendConfirmationEmail(reservation, response, reservation.getPropertyId());
		// Remove the booked item from the cart items
		List<CartItem> roomCartItems = new ArrayList<>();
		List<RoomCartItem> allRoomCartItems = CommonUtil.getRoomCartItems(sSession.getCartItems(), null);
		for (CartItem cartItem : allRoomCartItems) {
			if (!cartItem.getReservationId().equals(reservationId)) {
				roomCartItems.add(cartItem);
			}
		}
		response.setRoomRequests(bookedRoomRequests);
		sSession.setCartItems(roomCartItems);
		return response;

	}

	private AuthorizationTransactionRequest createAuthorizeRequest(RoomReservation roomReservation,
			ServicesSession sSession, ReservationRequest reservationRequest) {
		TransactionMappingRequest req = new TransactionMappingRequest();
		req.setPreresponse(RoomReservationTransformer.transform(roomReservation, appProperties));
		req.setReservation(roomReservation);
		req.setSession(sSession);
		req.setInAuthTransactionId(reservationRequest.getInAuthTransactionId());
		req.setTransactionId(ThreadContext.get("API_GTWYTRACEID"));
		return transactionService.transform(req);
	}

	private void sendTransactionConfirmation(AuthorizationTransactionRequest authReq, RoomReservation reservation) {

		if (reservation != null) {
			authReq.getTransaction().setConfirmationNumbers(reservation.getConfirmationNumber());
			//transactionService.confirm(authReq);
		}
	}

	/**
	 * Creates and returns profile information to be used for booking. If the user
	 * information is available in session, reservation profile is created with
	 * information from session. If not, profile is created with billing and guest
	 * information received as input.
	 * 
	 * @param reservationRequest Reservation request
	 * @param session            Services session
	 * @return Reservation profile
	 */
	private ReservationProfile getProfile(ReservationRequest reservationRequest, ServicesSession session) {
		ReservationProfile profile = new ReservationProfile();
		UserProfile userProfile = reservationRequest.getProfile();
		if (reservationRequest.getCustomerId() > 0) {
			Customer customer = session.getCustomer();
			profile.setId(customer.getCustomerId());
			profile.setMlifeNo(customer.getMlifeNumber());
			profile.setFirstName(customer.getFirstName());
			profile.setLastName(customer.getLastName());

		} else {
			profile.setId(-1);
			profile.setFirstName(userProfile.getFirstName());
			profile.setLastName(userProfile.getLastName());

		}
		
        profile.setEmailAddress1(userProfile.getEmail());

        List<ProfilePhone> phoneList = new ArrayList<>();
        ProfilePhone phone = new ProfilePhone();
        phone.setNumber(userProfile.getPhone());
        phone.setType("Home");
        phoneList.add(phone);

        profile.setPhoneNumbers(phoneList);

        profile.setAddresses(getAddresses(reservationRequest));


		return profile;
	}

	/**
	 * Populates and returns profile address information based on billing
	 * information received as input.
	 * 
	 * @param reservationRequest Reservation request
	 * @return Returns profile address
	 */
	private List<ProfileAddress> getAddresses(ReservationRequest reservationRequest) {

		UserAddress userAddress = reservationRequest.getBilling().getAddress();

		ProfileAddress address = new ProfileAddress();
		address.setType("Home");
		address.setPreferred(true);
		address.setStreet1(userAddress.getStreet1());
		address.setStreet2(userAddress.getStreet2());
		address.setCity(userAddress.getCity());
		address.setState(userAddress.getState());
		address.setCountry(userAddress.getCountry());
		address.setPostalCode(userAddress.getPostalCode());
		List<ProfileAddress> addressList = new ArrayList<>();
		addressList.add(address);

		return addressList;
	}

	/**
	 * Creates and returns profile address object which can be used as address for
	 * card holder profile. This information will be used by GSE to perform AVS
	 * checks with freedom pay.
	 * 
	 * @param reservationRequest Reservation request
	 * @return Card holder profile address
	 */
	private ProfileAddress getCardHolderAddress(ReservationRequest reservationRequest) {

		UserAddress userAddress = reservationRequest.getBilling().getAddress();

		ProfileAddress address = new ProfileAddress();
		address.setType("Home");
		address.setPreferred(true);
		address.setStreet1(userAddress.getStreet1());
		address.setStreet2(userAddress.getStreet2());
		address.setCity(userAddress.getCity());
		address.setCountry(userAddress.getCountry());

		// Setting state only for US and Canada, since freedom pay only accepts
		// 2 or 3 letter state code
		if (StringUtils.isNotBlank(userAddress.getCountry())
				&& (userAddress.getCountry().equalsIgnoreCase(ServiceConstant.COUNTRY_US)
						|| userAddress.getCountry().equalsIgnoreCase(ServiceConstant.COUNTRY_CANADA))) {
			address.setState(userAddress.getState());
		} else {
			address.setState(StringUtils.EMPTY);
		}

		// Remove space and hyphens in postal since freedompay doesn't accept
		String postalCode = StringUtils.trimToEmpty(userAddress.getPostalCode());
		postalCode = postalCode.replaceAll("\\s", StringUtils.EMPTY);
		postalCode = postalCode.replaceAll("-", StringUtils.EMPTY);

		address.setPostalCode(postalCode);

		return address;
	}
	
    private String getPaymentToken(PaymentInfo paymentInfo) {

        // Tokenize card information
        PaymentTokenizeRequest tokenizeReq = new PaymentTokenizeRequest();
        tokenizeReq.setCreditCard(paymentInfo.getCardNumber());
        tokenizeReq.setExpirationMonth(paymentInfo.getExpiry().split("/")[0]);
        // extract last 2 digits for the year
		String expiryYear = paymentInfo.getExpiry().split("/")[1];
		if (expiryYear.length() > 2) {
			expiryYear = expiryYear.substring(expiryYear.length() - 2);
		}
        tokenizeReq.setExpirationYear(expiryYear);
        tokenizeReq.setCvv(paymentInfo.getCvv());
        tokenizeReq.setCardType(paymentInfo.getType());
        return paymentTokenizationService.tokenize(tokenizeReq);
    }

	/**
	 * Returns payments information received as input into room reservation object
	 * along with address information for AVS validation.
	 * 
	 * @param reservationRequest Reservation request
	 * @param reservation        Room reservation object from session
	 * @return Returns populated credit card charges
	 */
	private List<CreditCardCharge> getPayments(ReservationRequest reservationRequest, RoomReservation reservation) {

		CreditCardCharge cardCharge = new CreditCardCharge();

		PaymentInfo payment = reservationRequest.getBilling().getPayment();
		if (reservationRequest.isFullPayment()) {
			double fullAmount = RoomReservationTransformer.transform(reservation, appProperties).getRates()
					.getReservationTotal();
			cardCharge.setAmount(fullAmount);
			reservation.getDepositCalc().setOverrideAmount(fullAmount);
		} else {
			cardCharge.setAmount(reservation.getDepositCalc().getAmount());
		}
		cardCharge.setCvv(payment.getCvv());
		final Calendar expireDate = Calendar.getInstance();
		String[] expiry = payment.getExpiry().split("/");
		expireDate.set(Calendar.YEAR, Integer.parseInt(expiry[1]));
		expireDate.set(Calendar.MONTH, Integer.parseInt(expiry[0]) - 1);

		cardCharge.setExpiry(expireDate.getTime());
		cardCharge.setType(payment.getType());
		cardCharge.setHolder(payment.getCardHolder());
		// Tokenize card information
		cardCharge.setNumber(getPaymentToken(payment));

		CardHolderProfile holderProfile = new CardHolderProfile();
		UserProfile profile = reservationRequest.getProfile();
		holderProfile.setFirstName(profile.getFirstName());
		holderProfile.setLastName(profile.getLastName());
		holderProfile.setAddress(getCardHolderAddress(reservationRequest));

		cardCharge.setHolderProfile(holderProfile);

		List<CreditCardCharge> paymentMethods = new ArrayList<>();
		paymentMethods.add(cardCharge);

		return paymentMethods;
	}

	/**
	 * Checks if the received error message is due to payment failure.
	 * 
	 * @param message Exception message
	 * @return Returns true if exception is related to payment
	 */
	private boolean isPaymentFailure(String message) {

		for (String error : appProperties.getPaymentErrorCodes()) {
			if (message.contains(error)) {
				return true;
			}
		}
		return false;

	}
}
