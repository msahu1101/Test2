package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ModifyReservationDAOStrategy;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.exception.ACRSErrorDetails;
import com.mgm.services.booking.room.exception.ACRSErrorUtil;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.profile.Profile;
import com.mgm.services.booking.room.model.request.ModificationChangesRequest;
import com.mgm.services.booking.room.model.request.PreModifyRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.RoomRequest;
import com.mgm.services.booking.room.model.request.dto.CommitPaymentDTO;
import com.mgm.services.booking.room.model.request.dto.UpdateProfileInfoRequestDTO;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.AuroraPriceResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.transformer.AuroraPriceRequestTransformer;
import com.mgm.services.booking.room.transformer.BaseAcrsTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.PropertyConfig;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.model.ProfileAddress;
import com.mgm.services.common.model.ProfilePhone;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation class for ModifyReservationDAO for pre-modify and modify
 * reservation functions.
 *
 */
@Component
@Log4j2
public class ModifyReservationDAOStrategyACRSImpl extends BaseReservationDao implements ModifyReservationDAOStrategy {

    @Autowired
    private PropertyConfig propertyConfig;
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
    public ModifyReservationDAOStrategyACRSImpl(URLProperties urlProperties, DomainProperties domainProperties,
            ApplicationProperties applicationProperties, RestTemplateBuilder builder, AcrsProperties acrsProperties,
            ReferenceDataDAOHelper referenceDataDAOHelper, ACRSOAuthTokenDAOImpl acrsOAuthTokenDAOImpl,
                                                RoomPriceDAOStrategyACRSImpl roomPriceDAOStrategyACRSImpl)
            throws SSLException {
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
        this.client.setErrorHandler(new RestTemplateResponseErrorHandler());

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
        // This is abandoned as unsupported v1 operation
        throw new UnsupportedOperationException();
    }

    public RoomReservation preModifyCharges(ModificationChangesRequest modificationChangesRequest) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for preModifyReservation: {}",
                    CommonUtil.convertObjectToJsonString(modificationChangesRequest));
        }

        // Step 1 - Fetch Reservation
        ReservationRetrieveResReservation ReservationRetrieveRes = retrieveReservationByConfirmationNumber(
                modificationChangesRequest.getConfirmationNumber(), modificationChangesRequest.getSource());
        RoomReservation fetchedReservation = RoomReservationTransformer.transform(ReservationRetrieveRes, acrsProperties);
        referenceDataDAOHelper.updateAcrsReferencesToGse(fetchedReservation);

        // Step 2 - Determine if Pricing must be called
        boolean checkInOrCheckOutDateChanged = false;
        if ( null != modificationChangesRequest.getTripDetails() ) {
            Date newCheckInDate = modificationChangesRequest.getTripDetails().getCheckInDate();
            if (!ReservationUtil.areDatesEqualExcludingTime(newCheckInDate, fetchedReservation.getCheckInDate())) {
                checkInOrCheckOutDateChanged = true;
            } else {
                Date newCheckOutDate = modificationChangesRequest.getTripDetails().getCheckOutDate();
                if (!ReservationUtil.areDatesEqualExcludingTime(newCheckOutDate, fetchedReservation.getCheckOutDate())) {
                    checkInOrCheckOutDateChanged = true;
                }
            }
        }

        if (checkInOrCheckOutDateChanged && isPricingNeededToUpdateBookings(modificationChangesRequest, fetchedReservation.getBookings())) {
            // If needed, make pricing call and update bookings on modificationChangesRequest before passing to next step
            List<AuroraPriceResponse> newPricing = getRoomPrices(AuroraPriceRequestTransformer.getAuroraRequest(modificationChangesRequest));
            modificationChangesRequest.setBookings(constructModifiedBookingsFromInputs(modificationChangesRequest,
					fetchedReservation.getBookings(), newPricing));
        } else if (!modificationChangesRequest.isPerpetualOffer()) {
            // Pricing is not needed so pass an empty list to updateBookingsFromPricing method in place of newPricing
            modificationChangesRequest.setBookings(constructModifiedBookingsFromInputs(modificationChangesRequest,
					fetchedReservation.getBookings(), Collections.emptyList()));
		}

		// Step 2.5 - Validate bookings don't have mixed ratePlans for non-po
		if (!modificationChangesRequest.isPerpetualOffer()) {
			long numDistinctRatePlans = modificationChangesRequest.getBookings().stream()
					.map(RoomPrice::getProgramId)
					.distinct()
					.count();
			if (numDistinctRatePlans > 1) {
				String errorMessage = "For non-po reservations there cannot be more than 1 ratePlan (programId) in " +
						"bookings objects.";
				log.error(errorMessage);
				throw new BusinessException(ErrorCode.INVALID_BOOKINGS, errorMessage);
			}
		}
        //packge2.0 changes- check if existing reservation is package reservation.
        //No required as we are not going to touch the pkg component
        //mergeExistingPkgComponents(modificationChangesRequest,fetchedReservation.getPropertyId(),fetchedReservation.getPurchasedComponents());
		// Step 3 - Create modification changes
        ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
        ModificationChanges modificationChanges = createModificationChangesFromModificationChangesRequest(
                modificationChangesRequest, ReservationRetrieveRes, false);

        // add changes for addOns
       List<ModificationChangesItem> addOnsChanges = getAddOnsComponentsChanges(modificationChangesRequest, ReservationRetrieveRes);
       if(CollectionUtils.isNotEmpty(addOnsChanges)) {
           modificationChanges.addAll(addOnsChanges); 
       }
       reservationPartialModifyReq.setData(modificationChanges);
        if (CollectionUtils.isNotEmpty(modificationChanges)) {
            // Step 4 - Apply modifications and return modified reservation then ignore pending changes
            RoomReservation modifiedReservation = getModifiedReservationFromPartialModifyIgnore(reservationPartialModifyReq, fetchedReservation.getPropertyId(), modificationChangesRequest.getConfirmationNumber(),modificationChangesRequest.getSource(), modificationChangesRequest.isPerpetualOffer());

            // Step 5 - return creditCardTokens to fetched values and return
            return updateCreditCardTokens(modifiedReservation, fetchedReservation.getCreditCardCharges(), fetchedReservation.getPayments());
        }else {
            return fetchedReservation;
        }
    }

	private RoomReservation updateCreditCardTokens(RoomReservation modifiedReservation, List<CreditCardCharge> fetchedCreditCardCharges, List<Payment> fetchedPayments) {
        RoomReservation updatedReservation = modifiedReservation; // TODO clone?

        // update creditCardCharge objects
        updatedReservation.setCreditCardCharges(fetchedCreditCardCharges);

        // update payment objects
        // because this is a modify/ignore I won't worry about payments that haven't happened until commit.
        updatedReservation.setPayments(fetchedPayments);
        return updatedReservation;
    }

    private List<RoomPrice> constructModifiedBookingsFromInputs(ModificationChangesRequest modificationChangesRequest, List<RoomPrice> fetchedBookings, List<AuroraPriceResponse> newPricing) {
        List<RoomPrice> newBookings = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(modificationChangesRequest.getTripDetails().getCheckInDate());
        // Step through each night in the trip and collect/create booking object into newBookings
        while ( ReservationUtil.isFirstDateBeforeSecondDateExcludingTime(cal.getTime(), modificationChangesRequest.getTripDetails().getCheckOutDate()) ) {
            // search modificationChangesRequest first for booking object
            Optional<RoomPrice> foundInModificationChangesRequest = modificationChangesRequest.getBookings().stream()
                    .filter(booking -> ReservationUtil.areDatesEqualExcludingTime(cal.getTime(), booking.getDate()))
                    .findFirst();
            if (foundInModificationChangesRequest.isPresent()) {
                newBookings.add(foundInModificationChangesRequest.get());
            } else {
                // search existing bookings second for booking object
                Optional<RoomPrice> foundInFetchedBookings = fetchedBookings.stream()
                        .filter(booking -> ReservationUtil.areDatesEqualExcludingTime(booking.getDate(), cal.getTime()))
                        .findFirst();
                if (foundInFetchedBookings.isPresent()) {
                    newBookings.add(foundInFetchedBookings.get());
                } else {
                    // if still not found from existing bookings then create booking from new pricing
                    Optional<AuroraPriceResponse> foundPricing = newPricing.stream()
                            .filter(auroraPriceResponse -> ReservationUtil.areDatesEqualExcludingTime(auroraPriceResponse.getDate(), cal.getTime()))
                            .findFirst();
                    if (foundPricing.isPresent()) {
                        newBookings.add(prepareBookingFromRoomPrice(foundPricing.get()));
                    } else {
                        // Unable to price trip
                        throw new BusinessException(ErrorCode.UNABLE_TO_PRICE);
                    }
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return newBookings;
    }

    private boolean isPricingNeededToUpdateBookings(ModificationChangesRequest modificationChangesRequest, List<RoomPrice> fetchedBookings) {
        if (!modificationChangesRequest.isPerpetualOffer()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(modificationChangesRequest.getTripDetails().getCheckInDate());
            while (ReservationUtil.isFirstDateBeforeSecondDateExcludingTime(calendar.getTime(), modificationChangesRequest.getTripDetails().getCheckOutDate())) {
                // Search for target date in fetched reservation's bookings List
                Optional<RoomPrice> foundInFetchedBookings = fetchedBookings.stream()
                        .filter(booking -> ReservationUtil.areDatesEqualExcludingTime(calendar.getTime(), booking.getDate()))
                        .findFirst();
                if (!foundInFetchedBookings.isPresent()) {
                    // If not in fetched, check modificationChangesRequest.getBookings()
                    Optional<RoomPrice> foundInModificationChangesRequest = modificationChangesRequest.getBookings().stream()
                            .filter(booking -> ReservationUtil.areDatesEqualExcludingTime(calendar.getTime(), booking.getDate()))
                            .findFirst();
                    if (!foundInModificationChangesRequest.isPresent()) {
                        // if not present in modificationChangesRequest then pricing is needed: return true
                        return true;
                    }
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        return false;
    }

    private List<ModificationChangesItem> getAddOnsComponentsChanges(ModificationChangesRequest modificationChangesRequest,
                                                                ReservationRetrieveResReservation reservationRetrieveRes){
        return createModificationChangesForAddOns(modificationChangesRequest, reservationRetrieveRes);
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
        throw new UnsupportedOperationException("v1 APIs are not supported for ACRS Properties.");
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
            if (httpResponse.getStatusCode().value() >= 500) {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            } else if (response.contains("<UnableToPriceTrip>")) {
                throw new BusinessException(ErrorCode.DATES_UNAVAILABLE);
            } else if (httpResponse.getStatusCode().equals(HttpStatus.NOT_FOUND)
                    || response.contains("<BookingNotFound>")) {
                throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
            } else if (response.contains("Reservation is already cancelled")) {
                throw new BusinessException(ErrorCode.UNMODIFIABLE_RESV_STATUS);
            } else {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, acrsError.getTitle());
            }
        }
    }

    private static ModificationChangesRequest populateProfilePreModifyRequest(UpdateProfileInfoRequestDTO requestDTO) {
        ModificationChangesRequest modificationChangesRequest = new ModificationChangesRequest();
        modificationChangesRequest.setConfirmationNumber(requestDTO.getConfirmationNumber());

        if (null != requestDTO.getUserProfile()) {
            Profile profile = new Profile();
            profile.setTitle(requestDTO.getUserProfile().getTitle());
            profile.setFirstName(requestDTO.getUserProfile().getFirstName());
            profile.setLastName(requestDTO.getUserProfile().getLastName());
            profile.setEmail(requestDTO.getUserProfile().getEmailAddress1());
            profile.setSecondEmail(requestDTO.getUserProfile().getEmailAddress2());
            profile.setMlifeNumber(Integer.toString(requestDTO.getUserProfile().getMlifeNo()));
            profile.setPartnerAccounts(requestDTO.getUserProfile().getPartnerAccounts());
            if (CollectionUtils.isNotEmpty(requestDTO.getUserProfile().getPhoneNumbers())) {
                profile.setPhoneNumbers(requestDTO.getUserProfile().getPhoneNumbers().stream()
                        .map(ModifyReservationDAOStrategyACRSImpl::acrsPhoneToProfilePhoneRequest)
                        .collect(Collectors.toList()));
            }

            if (CollectionUtils.isNotEmpty(requestDTO.getUserProfile().getAddresses())) {
                profile.setAddresses(requestDTO.getUserProfile().getAddresses().stream()
                        .map(ModifyReservationDAOStrategyACRSImpl::acrsAddressToProfileAddressRequest)
                        .collect(Collectors.toList()));
            }
            modificationChangesRequest.setProfile(profile);
        }
        return modificationChangesRequest;
    }

    private static ProfilePhone acrsPhoneToProfilePhoneRequest(ProfilePhone profilePhone) {
        ProfilePhone profilePhoneModifyRequest = new ProfilePhone();
        profilePhoneModifyRequest.setNumber(profilePhone.getNumber());
        profilePhoneModifyRequest.setType(profilePhone.getType());
        return profilePhoneModifyRequest;
    }

    private static ProfileAddress acrsAddressToProfileAddressRequest(ProfileAddress profileAddress) {
        ProfileAddress profileAddressModifyRequest = new ProfileAddress();
        profileAddressModifyRequest.setCity(profileAddress.getCity());
        profileAddressModifyRequest.setCountry(profileAddress.getCountry());
        profileAddressModifyRequest.setPostalCode(profileAddress.getPostalCode());
        profileAddressModifyRequest.setState(profileAddress.getState());
        profileAddressModifyRequest.setStreet1(profileAddress.getStreet1());
        if (profileAddress.getStreet2() != null) {
            profileAddressModifyRequest.setStreet2(profileAddress.getStreet2());
        }
        if (null != profileAddress.getType()) {
            if (AddressType.NUMBER_1.equals(profileAddress.getType())) {
                profileAddressModifyRequest.setType("Home");
            } else {
                profileAddressModifyRequest.setType("Business");
            }
        }
        profileAddress.setPreferred(true);
        return profileAddress;
    }

    @Override
    public RoomReservation updateProfileInfo(UpdateProfileInfoRequestDTO requestDTO) {

        log.info("Incoming Request for updateProfileInfo: {}", CommonUtil.convertObjectToJsonString(requestDTO));


        ModificationChangesRequest modificationChangesRequest = populateProfilePreModifyRequest(requestDTO);
        try {
            ReservationRetrieveResReservation existingResv = retrieveReservationByConfirmationNumber(
                    modificationChangesRequest.getConfirmationNumber(), requestDTO.getSource());
            RoomReservation fetchedReservation = RoomReservationTransformer.transform(existingResv, acrsProperties);
            referenceDataDAOHelper.updateAcrsReferencesToGse(fetchedReservation);

            // Step 2 - Apply modifications
            ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
            ModificationChanges modificationChanges = createModificationChangesFromModificationChangesRequest(
                    modificationChangesRequest, existingResv, true);

            // Temp fix for testing association issue
            if (CollectionUtils.isNotEmpty(modificationChanges)) {

                reservationPartialModifyReq.setData(modificationChanges);
                boolean isPoFlow = false;// get from existing reservation
                // Send pending partial modify to ACRS
                partialModifyPendingRoomReservation(reservationPartialModifyReq,
                        modificationChangesRequest.getConfirmationNumber(), fetchedReservation.getPropertyId(), null, null,requestDTO.getSource(), isPoFlow);

                if (null != requestDTO.getOriginalReservation()) {
                    if (StringUtils.isEmpty(requestDTO.getOriginalReservation().getSource())) {
                        requestDTO.getOriginalReservation().setSource(requestDTO.getSource());
                    }
                    if(CollectionUtils.isNotEmpty(requestDTO.getOriginalReservation().getRoutingInstructions())) {
                        invokeRefDataRoutingInfo(requestDTO.getOriginalReservation().getRoutingInstructions(),
                                requestDTO.getOriginalReservation().getPropertyId(), true);
                        // Modify pending for the RI changes
                        makeRoutingInstructionModification(requestDTO.getOriginalReservation(), existingResv);
                    }
                }

                // Commit pending Room reservation
             
                RoomReservation commitPendingRoomReservation = commitPendingRoomReservation(
                        modificationChangesRequest.getConfirmationNumber(), fetchedReservation.getPropertyId(),requestDTO.getSource(), isPoFlow);
                commitPendingRoomReservation.setItineraryId(requestDTO.getItineraryId());
                commitPendingRoomReservation.setId(requestDTO.getReservationId());
                commitPendingRoomReservation.getProfile().setId(requestDTO.getUserProfile().getId());

                // ACRS will not give us back credit card details so have to populate from fetch
                commitPendingRoomReservation.setCreditCardCharges(fetchedReservation.getCreditCardCharges());
                commitPendingRoomReservation.setPayments(fetchedReservation.getPayments());

                return commitPendingRoomReservation;
            } else {
                log.debug("Changes not found to modify the reservation {}", requestDTO.getConfirmationNumber());
                fetchedReservation.setItineraryId(requestDTO.getItineraryId());
                fetchedReservation.setId(requestDTO.getReservationId());
                fetchedReservation.getProfile().setId(requestDTO.getUserProfile().getId());
                return fetchedReservation;
            }
        } catch (ParseException e) {
            log.error("Failed to parse commit Reservation Response from ACRS.");
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
    }
    
    private void validateMissingWebSuppressComponents(List<PurchasedComponent> purchasedComponents,
                                                      List<String> specialRequests) {
		if (null != purchasedComponents && !specialRequests.isEmpty()) {

			List<String> existingCodesWithPatterns = purchasedComponents.stream()
					.filter(c -> ReservationUtil.isSuppressWebComponent(c.getId(), acrsProperties))
					.map(existingValues -> existingValues.getRatePlanCode()).collect(Collectors.toList());

			if (!existingCodesWithPatterns.isEmpty()) {

				List<String> requestCodesWithPatterns = specialRequests.stream()
						.filter(c -> ReservationUtil.isSuppressWebComponent(c, acrsProperties)).collect(Collectors.toList()).stream()
						.map(modifiedvalues -> ACRSConversionUtil.getComponentNRPlanCode(modifiedvalues))
						.collect(Collectors.toList());

				if (requestCodesWithPatterns.isEmpty()
						|| !requestCodesWithPatterns.containsAll(existingCodesWithPatterns)) {
					throw new BusinessException(ErrorCode.NON_EDITABLE_COMPONENTS_MISSING);
				}
			}
		}

	}

    @Override
    public RoomReservation modifyRoomReservationV2(RoomReservation reservation) {

        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for modifyRoomReservationV2: {}",
                    CommonUtil.convertObjectToJsonString(reservation));
        }
        //1. get existing committed or pending reservation
        ReservationRetrieveResReservation existingResv = null;
        if (reservation.isPendingReservation()) {
            existingResv = retrievePendingReservationByConfirmationNumber(reservation.getConfirmationNumber(),
                    reservation.getSource());
            //During hold from web if modify pending with FOP passed but commit get failed in that case
            //Payment will be updated in ACRS. So when user retry for the next time then
            //existing logic will be authorized the amount again.Which will be causing duplicate payment
            //Fix-adjustBillingAmount
            adjustBillingAmount(existingResv,reservation);
        } else {
            existingResv = retrieveReservationByConfirmationNumber(reservation.getConfirmationNumber(),
                    reservation.getSource());
        }
        RoomReservation existingReservation = RoomReservationTransformer.transform(existingResv, acrsProperties);
        // add missing pkg components in request
        // its not required as we are not going to touch the pkg component
        //mergeExistingPkgComponents(reservation,existingReservation.getPurchasedComponents());
        //2. set existing shared info in the request if any.
        setExistingSharedInfo(reservation,existingResv);
        //3. Invoke modify pending for the changes of date,room,rate, RI, guest count etc.
        RoomReservation pendingRoomReservation = null;
        boolean isCashPayment = isCashPayment(reservation.getGuaranteeCode(),reservation.getCreditCardCharges());
        boolean isModified = false;
        boolean doIgnoreReservation = true;
        ReservationModifyPendingRes modifyPendingRes = invokeModifyPending(reservation, existingResv,isCashPayment);
        if(null != modifyPendingRes) {
            pendingRoomReservation = transformReservationModifyPendingRes(modifyPendingRes);
            isModified = true;
        }
        //4. check if reservation has no modification but still need to commit due to some
        //other modifications like override deposit,checkout old resv etc.
        // In that case we have set pendingRoomReservation by existing pending one.
        if (null == pendingRoomReservation && isOthersModificationRequired(reservation,existingReservation)) {
            pendingRoomReservation = existingReservation;
        }
        // get ACRS propertyCode for the existing reservation.(This is required for exception pseudo properties-MV305,MV931MV002 and MV003)
        String existingAcrsPropertyCode = existingResv.getData().getHotelReservation().getHotels().get(0).getPropertyCode();
        //5. If anything modified then update form of payment
        if (null != pendingRoomReservation) {
            boolean hasRefundInPaymentWidgetFlow = false;
            //5.1 if payment by cash then modify pending reservation with
            boolean hasPaymentCardInfo = CollectionUtils.isNotEmpty(reservation.getCreditCardCharges());
            Map<CreditCardCharge, String> creditCardChargeAuthorizationMap = new HashMap<>();
            AuthorizationTransactionRequest authorizeRequest = createBaseAuthorizeRequest(reservation);
            String reservationStatus = null;
            String processResponseText = null;
            // isFourceSell
            // = true
            try {
	            if(!hasPaymentCardInfo){
	                updateNonCardFormOfPayment(reservation,pendingRoomReservation);
	            }
	           //5.2 update form of payment if card payment or
	            // This map holds authorized creditCardCharges' authRequestId for
	            // charges and simply a REFUND constant
	            // for refund creditCardCharges.
	            else {
                    if(!reservation.isSkipFraudCheck()) {
                        creditCardChargeAuthorizationMap = authorizeCreditCardsOnReservation(reservation,
                                reservation.getConfirmationNumber(), reservation.getPropertyId(),true);
                    }else{
                        creditCardChargeAuthorizationMap = createCreditCardChargeAuthorizationMap(reservation.getCreditCardCharges()) ;
                    }
                    // update acrs formOfPayment information
                    //CBSR-2129 - for payment widget integration
                    // if there is any refund  in payment widget flow then skip updating FOP.
                    double existingDeposit = 0.0;
                    if(CollectionUtils.isNotEmpty(existingReservation.getPayments())){
                        existingDeposit = existingReservation.getPayments().stream().
                                filter(x -> ServiceConstant.PAYMENT_STATUS_SETTLED.equalsIgnoreCase(x.getStatus()))
                                .mapToDouble(Payment::getChargeAmount).sum();
                    }
                    double modifiedDeposit = pendingRoomReservation.getDepositCalc().getAmount();
                    hasRefundInPaymentWidgetFlow = reservation.isSkipPaymentProcess() && existingDeposit > modifiedDeposit;
                    // update acrs formOfPayment information
                    //update deposit amount with updated deposit as for web reservation during checkout reservation.deposit is not the actual deposit
                    // if there is an addons during checkout
                    if(null != reservation.getDepositCalc() && null != pendingRoomReservation.getDepositCalc()){
                        reservation.getDepositCalc().setAmount(pendingRoomReservation.getDepositCalc().getAmount());
                    }
                    if(!hasRefundInPaymentWidgetFlow) {
                       updateCardFormOfPayment(reservation.getConfirmationNumber(), reservation,
                               creditCardChargeAuthorizationMap, reservation.getSource(),
                               pendingRoomReservation.getAddOnsDeposits());
                   }
	            }
	            //6. handle existing shared reservation(modify) before commit the primary reservation.
	            // As per ACRS we have to do modify pending for shared reservation before committing the primary reservation.
	            List<RoomReservation> modifiedSharedResvs = new ArrayList<>();
	            if (isProductUsesModified(referenceDataDAOHelper.updateAcrsReferencesFromGse(populatePreModifyRequest(reservation, existingReservation)),
	                            existingReservation) &&
	                    CollectionUtils.isNotEmpty(reservation.getSharedConfirmationNumbers())) {
	                modifiedSharedResvs = invokeModifySharedReservation(reservation);
	            }

                    //CBSR-2129 - for payment widget integration
                    // If there is a refund in payment widget flow then we will be skipping the commit.
                     RoomReservation commitPendingRoomReservation = pendingRoomReservation;
	                //7. commit the reservation
                    if(!hasRefundInPaymentWidgetFlow){
                        commitPendingRoomReservation = commitPendingRoomReservation(
                                reservation.getConfirmationNumber(), reservation.getPropertyId(), reservation.getSource(),
                                reservation.isPerpetualPricing());
                    }
                     doIgnoreReservation = false;
	                if (!isCashPayment && !reservation.isSkipPaymentProcess()) {
	                    captureOrRefund(creditCardChargeAuthorizationMap, commitPendingRoomReservation,
	                            reservation.getSource());
	                }
	
	                //8. Update Patron promo status
	                updatePatronPromo(existingResv, reservation, reservation.getConfirmationNumber(), acrsProperties);
	                //9. update committed response by ref data, input request
	                updateCommittedResponse(commitPendingRoomReservation, reservation, existingResv, isCashPayment);
	
	                //10.if it is partyReservation the break the party reservation If
	                // required
	                if (StringUtils.isNotEmpty(commitPendingRoomReservation.getPartyConfirmationNumber())) {
	                    if (breakPartyReservation(reservation, existingResv)) {
	                        commitPendingRoomReservation.setPartyConfirmationNumber(null);
	                    }
	                }
	                // Handle modify shared reservation modification
	                // create new shared reservation If any or modified
	                boolean hasSharedWithResv = CollectionUtils.isNotEmpty(reservation.getSharedConfirmationNumbers())
	                        || CollectionUtils.isNotEmpty(reservation.getShareWithCustomers());
	                if (hasSharedWithResv) {
	                    // If new sharedWith added with existing shared reservation
	                    if(CollectionUtils.isNotEmpty(reservation.getShareWithCustomers())){
	
	                        List<RoomReservation> newSharedResvs = invokeNewSharedReservation(reservation,existingAcrsPropertyCode);
	                        modifiedSharedResvs.addAll(newSharedResvs);
	                    }
	                    // set links and shared information in
	                    setShareWithInfoIntoComittedReservation(commitPendingRoomReservation,modifiedSharedResvs,reservation);
	                }
	                // TODO: This OperaState fix is temporary, so adding here and
	                // easy to
	                // remove as well.
	                if (ReservationState.Booked.equals(commitPendingRoomReservation.getState())
	                        && StringUtils.isNotBlank(commitPendingRoomReservation.getOperaConfirmationNumber())) {
	                    commitPendingRoomReservation.setOperaState(ServiceConstant.RESERVED_STRING);
	                }
	                reservationStatus = ServiceConstant.COMPLETED;
	                processResponseText = ServiceConstant.COMPLETED;
	                return commitPendingRoomReservation;

            } catch (ParseException | BusinessException e) {
                log.error("Failed to commit Reservation Response from ACRS.");
                reservationStatus = ServiceConstant.CANCELLED_BY_MERCHANT;
                processResponseText = e.getMessage();
                if(e instanceof BusinessException){
                    throw new BusinessException(((BusinessException) e).getErrorCode(),((BusinessException) e).getMessage());
                }else{
                    throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
                }
            }finally {
                //If this is not reservation checkout flow and commit got failed, then ignore reservation.
                if(doIgnoreReservation && !reservation.isPendingReservation()){
                    ignorePendingRoomReservation(reservation.getConfirmationNumber(),
                            reservation.getPropertyId(), reservation.getSource(),
                            reservation.isPerpetualPricing());
                }
            	if (null != authorizeRequest && !reservation.isSkipFraudCheck()) {
    				accertifyInvokeHelper.confirmAsyncCall(setAuthorizeRequestStatus(authorizeRequest, reservationStatus, processResponseText),
    						reservation.getConfirmationNumber());
    			}
    		}
        } else if (CollectionUtils.isNotEmpty(reservation.getShareWithCustomers())) {
            List<RoomReservation> modifiedSharedWiths = invokeNewSharedReservation(reservation,existingAcrsPropertyCode);
            List<String> modifiedSharedCnfs = modifiedSharedWiths.stream().map(RoomReservation::getConfirmationNumber).collect(Collectors.toList());
            reservation.setShareWiths(modifiedSharedCnfs.toArray(new String[0]));
            reservation.setShareWithReservations(modifiedSharedWiths);
            return reservation;
        } else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"No change for modification.");
        }
    }

    private void adjustBillingAmount(ReservationRetrieveResReservation existingResv, RoomReservation reservation) {
        if(CollectionUtils.isNotEmpty(reservation.getCreditCardCharges())) {
            SegmentRes segmentRes = existingResv.getData().getHotelReservation().getSegments();
            SegmentResItem segment = BaseAcrsTransformer.getMainSegment(segmentRes);
            if (null != segment.getFormOfPayment()) {
                List<Payment> existingPayments = BaseAcrsTransformer.getPaymentsFromAcrs(segment.getFormOfPayment().getDepositPayments());
                if (CollectionUtils.isNotEmpty(existingPayments)) {
                    double paidAmount = existingPayments.stream().mapToDouble(Payment::getChargeAmount).sum();
                    double totalDepositAmountRequired = segmentRes.stream().map(BaseAcrsTransformer::createAcrsDepositCal).mapToDouble(Deposit::getAmount).sum();
                    reservation.getCreditCardCharges().get(0).setAmount(totalDepositAmountRequired - paidAmount);
                }
            }
        }
    }

    private void updateCommittedResponse(RoomReservation commitPendingRoomReservation, RoomReservation modifiedReservationRequest,
                                         ReservationRetrieveResReservation existingResv, boolean isCashPayment) {
        commitPendingRoomReservation.setItineraryId(modifiedReservationRequest.getItineraryId());
        commitPendingRoomReservation.getProfile().setId(modifiedReservationRequest.getProfile().getId());
        commitPendingRoomReservation.getProfile().setDateOfBirth(modifiedReservationRequest.getProfile().getDateOfBirth());
        commitPendingRoomReservation.setCustomerId(modifiedReservationRequest.getCustomerId());
        commitPendingRoomReservation.setId(modifiedReservationRequest.getId());
        commitPendingRoomReservation.setGuaranteeCode(modifiedReservationRequest.getGuaranteeCode());
        // Setting the program id as is from the request.
        // ICE wants program id to be passed in the response if it is
        // only
        // passed in the request.
        commitPendingRoomReservation.setProgramId(modifiedReservationRequest.getProgramId());

        // Response from ACRS will not have any credit card details so
        // need to populate from elsewhere.
        if (!isCashPayment) {
            commitPendingRoomReservation.setCreditCardCharges(modifiedReservationRequest.getCreditCardCharges());

            // Seed payments from fetched modifiedReservationRequest
            SegmentResItem mainSegment = BaseAcrsTransformer
                    .getMainSegment(existingResv.getData().getHotelReservation().getSegments());
            List<PaymentTransactionRes> depositPayments = new ArrayList<>();
            if (null != mainSegment.getFormOfPayment()) {
                depositPayments = mainSegment.getFormOfPayment().getDepositPayments();
            }
            List<Payment> postCommitPayments = BaseAcrsTransformer.getPaymentsFromAcrs(depositPayments);

            // Add any new payments from inputted creditCardCharges

			postCommitPayments.addAll(getNewPaymentsFromModificationRequest(modifiedReservationRequest));

            commitPendingRoomReservation.setPayments(postCommitPayments);
        }

        // convert acrs to refData guid
        commitPendingRoomReservation.setRoutingInstructions(modifiedReservationRequest.getRoutingInstructions());
        commitPendingRoomReservation.setAlerts(modifiedReservationRequest.getAlerts());
        commitPendingRoomReservation.setTraces(modifiedReservationRequest.getTraces());
        //CBSR-2481. Set Base price from req
        commitPendingRoomReservation.getBookings().stream().forEach(commitBooking -> {
        	modifiedReservationRequest.getBookings().stream()
        	.filter(reqBooking -> reqBooking.getDate().equals(commitBooking.getDate()))
        	.findAny()
        	.ifPresent(reqBooking -> commitBooking.setBasePrice(reqBooking.getBasePrice()));
        });
        setReferenceData(commitPendingRoomReservation);

    }

	private static List<Payment> getNewPaymentsFromModificationRequest(RoomReservation modifiedReservationRequest) {
		if (CollectionUtils.isEmpty(modifiedReservationRequest.getCreditCardCharges())) {
			return Collections.emptyList();
		}
		return modifiedReservationRequest.getCreditCardCharges().stream()
				.map(BaseReservationDao::createPaymentFromCreditCardCharge).collect(Collectors.toList());
	}

    private void updateNonCardFormOfPayment(RoomReservation reservation, RoomReservation pendingRoomReservation) {
        if (validateAcrInventorysWarnings(pendingRoomReservation.getCrsWarnings())) {
            Map<String, PaymentType> paymentTypeGuaranteeCodeMap = acrsProperties.getPaymentTypeGuaranteeCodeMap();

            ModificationChanges modificationChanges = new ModificationChanges();
            // For  non card payment(30 and 44) we will not be doing any change in existing fop.
            // Other than those we will update fop for GuaranteeCode - DD,CC.
            if(null == paymentTypeGuaranteeCodeMap.get(reservation.getGuaranteeCode())) {
                // 1. Create ModificationChange for PaymentInfo for main and addOns segments
                List<ModificationChangesItem> modificationChangeForPaymentInfo = getModificationChangeForPaymentInfo(
                        reservation, pendingRoomReservation.getAddOnsDeposits());
                if (null != modificationChangeForPaymentInfo) {
                    modificationChanges.addAll(modificationChangeForPaymentInfo);
                }
            }else{
                // for 30 and 44 add FOP for only addOns if any
                addAddOnsFOPChanges(modificationChanges,pendingRoomReservation.getAddOnsDeposits(),null,pendingRoomReservation.getGuaranteeCode());
            }

			if (CollectionUtils.isNotEmpty(modificationChanges)) {
				ReservationPartialModifyReq resvPartialModifyReq = new ReservationPartialModifyReq();
				resvPartialModifyReq.setData(modificationChanges);
				partialModifyPendingRoomReservation(resvPartialModifyReq,
						pendingRoomReservation.getConfirmationNumber(), reservation.getPropertyId(),
						(null != reservation.getAgentInfo()) ? reservation.getAgentInfo().getAgentId() : null,
						reservation.getRrUpSell(), reservation.getSource(), reservation.isPerpetualPricing());
			}
        } else if (!validateAcrInventorysWarnings(pendingRoomReservation.getCrsWarnings())) {
            // if payment by cash and inventory warning there then ignore .
            if (!ignorePendingRoomReservation(reservation.getConfirmationNumber(), reservation.getPropertyId(),
                    reservation.getSource(), reservation.isPerpetualPricing())) {
                // log system error
                log.error("System error occurred while ignore pending reservation with confirmation number: {}",
                        reservation.getConfirmationNumber());
                throw new BusinessException(ErrorCode.RESERVATION_NOT_SUCCESSFUL);
            }
            log.error("Modify Room Reservation was unsuccessful");
            throw new BusinessException(ErrorCode.RESERVATION_NOT_SUCCESSFUL);
        }
    }

    private ReservationModifyPendingRes invokeModifyPending(RoomReservation reservation, ReservationRetrieveResReservation existingResv, boolean isCashPayment) {
        ReservationModifyPendingRes reservationRes = null;
        ReservationPartialModifyReq reservationPartialModifyReq = createPendingModifyReq(reservation, existingResv);
        ModificationChanges modificationChanges = reservationPartialModifyReq.getData();
        //If no modification is there don't call acrs modify partial
        if (CollectionUtils.isNotEmpty(modificationChanges)) {
            // if payment by cash then create inn pending with isFourceSell =
            // false to not skip all non-deposit checks
            if (isCashPayment) {
                modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                        acrsProperties.getModifyForcedSellPath(), false));

            }
            if(RoomReservationTransformer.hasZeroOverriddenPrice(reservation.getBookings())) {
                modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                        acrsProperties.getModifyForcedSellPath(), true));
            }
            reservationPartialModifyReq.setData(modificationChanges);

            // Send pending partial modify to ACRS
             reservationRes = invokePartialModifyPendingRoomReservation(reservationPartialModifyReq,
                    reservation.getConfirmationNumber(), reservation.getPropertyId(),
                    (null != reservation.getAgentInfo()) ? reservation.getAgentInfo().getAgentId() : null,
                    reservation.getRrUpSell(), reservation.getSource(), reservation.isPerpetualPricing());

        }
        // Modify pending for the RI changes
        ReservationModifyPendingRes routingChangeRoomReservation = makeRoutingInstructionModification(reservation, existingResv);
        if (null != routingChangeRoomReservation) {
            reservationRes = routingChangeRoomReservation;
        }
        return reservationRes;
    }

    private void setExistingSharedInfo(RoomReservation reservation, ReservationRetrieveResReservation existingResv) {
        String sharedId = BaseAcrsTransformer.getLinkedId(existingResv.getData().getHotelReservation().getLinks());
        if(StringUtils.isNotBlank(sharedId)){
            reservation.setShareId(sharedId);
            final List<String> reservationIds = getSharedReservationConfirmationNumbers(
                    reservation.getConfirmationNumber(), sharedId, reservation.getSource());
            List<String> shareWiths = reservationIds.stream().filter(x -> !StringUtils.equals(reservation.getConfirmationNumber(), x)).collect(Collectors.toList());
            reservation.setSharedConfirmationNumbers(shareWiths);
            //filter out existing profile from reservation.getShareWithCustomers().ICE does sent the existing.
            if(CollectionUtils.isNotEmpty(reservation.getShareWithCustomers())){
                //call acrs search reservation
                ReservationSearchResPostSearchReservations sharedReservations = searchReservationByLink(reservation.getConfirmationNumber(), sharedId, reservation.getSource());
                sharedReservations.getData().getHotelReservations().stream()
                        .filter(hr -> reservation.getConfirmationNumber() != hr.getReservationIds().getCfNumber())
                        .map(hr -> hr.getUserProfiles())
                        .forEach(user ->{
                            PersonName existingName = user.get(0).getPersonName();
                            reservation.getShareWithCustomers().removeIf(pr -> StringUtils.equalsIgnoreCase(existingName.getGivenName(),pr.getFirstName()) &&  StringUtils.equalsIgnoreCase(existingName.getSurname(),pr.getLastName()));
                        });
            }

        }
    }

    private void setShareWithInfoIntoComittedReservation(RoomReservation commitPendingRoomReservation,List<RoomReservation> modifiedSharedResvs, RoomReservation reservation) {
        List<String> modifiedSharedCnfs = modifiedSharedResvs.stream().map(RoomReservation::getConfirmationNumber).collect(Collectors.toList());
        commitPendingRoomReservation.setShareWiths(modifiedSharedCnfs.toArray(new String[0]));
        commitPendingRoomReservation.setShareWithReservations(modifiedSharedResvs);
        if (StringUtils.isEmpty(commitPendingRoomReservation.getShareId())) {
            commitPendingRoomReservation.setShareId(reservation.getShareId());
        }
        if (StringUtils.isEmpty(commitPendingRoomReservation.getPrimarySharerConfirmationNumber())) {
            commitPendingRoomReservation.setShareId(reservation.getPrimarySharerConfirmationNumber());
        }
        commitPendingRoomReservation.setShareWithCustomers(reservation.getShareWithCustomers());
    }

	private boolean isOthersModificationRequired(RoomReservation reservationModified,RoomReservation existingReservation) {
        // Fix for -CQ-14532
        // After holding PO reservation while checkout if there is not any
        // modification then pendingRoomReservation will be null.
        if (reservationModified.isPendingReservation()) {
            return true;
        }	        //change in billing
        else if(hasChangeInBilling(reservationModified.getCreditCardCharges(),existingReservation.getCreditCardCharges())){
            return true;
        }
        // fix for ICEUCP-633
        // send only override deposit amount.
        else if (null != reservationModified.getDepositCalc() && reservationModified.getDepositCalc().getOverrideAmount() > -1.0) {
            return true;
        }
        else if(hasAgentInfoChanged(reservationModified.getAgentInfo(),existingReservation.getAgentInfo())){
            return true;
        }
        else {
            return false;
        }

    }

    private boolean hasAgentInfoChanged(AgentInfo modifiedAgentInfo, AgentInfo existingAgentInfo) {
        if (null != modifiedAgentInfo && null != modifiedAgentInfo.getAgentId() && null == existingAgentInfo) {
            return true;
        } else if (null != modifiedAgentInfo && null != existingAgentInfo) {
            if (null != modifiedAgentInfo.getAgentId() && null != existingAgentInfo.getAgentId()) {
                return !existingAgentInfo.getAgentId().equals(modifiedAgentInfo.getAgentId());
            }
        }
        return false;
    }

    private boolean hasChangeInBilling(List<CreditCardCharge> modifiedCharges, List<CreditCardCharge> existingCharges) {
        boolean hasChangeInBilling = false;
        if(null != modifiedCharges && null != existingCharges){
            hasChangeInBilling =  modifiedCharges.size() != existingCharges.size();
            if(!hasChangeInBilling){
                List<String> modifiedTokens = modifiedCharges.stream().map(CreditCardCharge::getCcToken).filter(StringUtils::isNotBlank).collect(Collectors.toList());
                List<String> existingTokens = modifiedCharges.stream().map(CreditCardCharge::getCcToken).filter(StringUtils::isNotBlank).collect(Collectors.toList());
                hasChangeInBilling = !Objects.equals(modifiedTokens,existingTokens);
            }
        }
        return hasChangeInBilling;
    }
    private List<RoomReservation> invokeModifySharedReservation(RoomReservation reservation) {
         //modify all shared reservations.
        List<RoomReservation> modifiedShareResvs = new ArrayList<>();
        reservation.getSharedConfirmationNumbers().forEach(sharedCnfNumber -> {
            //get existing shared reservation
            ReservationRetrieveResReservation existingSharedResv = retrieveReservationByConfirmationNumber(sharedCnfNumber,
                    reservation.getSource());
            RoomReservation existingReservation = RoomReservationTransformer.transform(existingSharedResv, acrsProperties);
            //copy modified data from request
            RoomReservation modifiedSharedResv = RoomReservationTransformer.copyRoomReservationForShare(reservation);
            // set override price=0 for SH reservations as while create ACRS set those to 0.0 for SH.
            modifiedSharedResv.getBookings().forEach(booking->{
                booking.setOverridePrice(0.0);
            });
            //update with existing data
            modifiedSharedResv.setConfirmationNumber(sharedCnfNumber);
            modifiedSharedResv.setProfile(existingReservation.getProfile());
            modifiedSharedResv.setGuaranteeCode(existingReservation.getGuaranteeCode());
            modifiedSharedResv.setSharedConfirmationNumbers(null);
            modifiedSharedResv.setItineraryId(null);
            modifiedSharedResv.setCustomerId(0);
            modifiedSharedResv.setShareWithReservations(null);
            modifiedSharedResv.setShareWiths(null);
            modifiedSharedResv.setNumAdults(existingReservation.getNumAdults());
            modifiedSharedResv.setNumChildren(existingReservation.getNumChildren());
            //invoke modify pending
            ReservationModifyPendingRes sharedModifyPendingRes = invokeModifyPending(modifiedSharedResv, existingSharedResv, true);
            modifiedShareResvs.add(transformReservationModifyPendingRes(sharedModifyPendingRes));
        });
        return modifiedShareResvs;
    }

    private List<RoomReservation> invokeNewSharedReservation(RoomReservation reservation, String existingPropertyCode) {
        List<RoomReservation> newSharedWithResvs = new ArrayList<>();
         //If ShareWithCustomers is there in the request then Create new
        // shared reservation.
        if (CollectionUtils.isNotEmpty(reservation.getShareWithCustomers())) {
            newSharedWithResvs.addAll(createNewSharedReservations(reservation,existingPropertyCode));
            List<String> newSharedCnfs = newSharedWithResvs.stream().map(RoomReservation::getConfirmationNumber).collect(Collectors.toList());
            // 3. create or update link
            if (StringUtils.isNotEmpty(reservation.getShareId())) {
                modifyReservationLink(reservation.getShareId(), newSharedCnfs,
                        reservation.getPropertyId(), reservation.getSource(), LinkModificationChangesItem.OpEnum.LINK, reservation.isPerpetualPricing());
            } else {
                LinkReservationRes linkRes = createSharedReservationLink(reservation.getConfirmationNumber(),
                        newSharedCnfs, reservation.getPropertyId(), reservation.getSource(),reservation.isPerpetualPricing());
                String linkedId = linkRes.getData().getHotelLinkReservation().getLink().getId();
                reservation.setShareId(linkedId);
                reservation.setPrimarySharerConfirmationNumber(reservation.getConfirmationNumber());
            }
        }

        return newSharedWithResvs;
    }
    private LinkReservationRes modifyReservationLink(String shareId, List<String> newSharedCnfs,
            String acrsPropertyCode, String source, LinkModificationChangesItem.OpEnum operation, boolean isPOFlow) {
        LinkReservationModifyReq req = new LinkReservationModifyReq();
        LinkModificationChanges chnages = new LinkModificationChanges();
        LinkModificationChangesItem change = new LinkModificationChangesItem();
        change.setOp(operation);
        change.setValue(new ArrayList<>(newSharedCnfs));
        chnages.add(change);
        req.setData(chnages);
        return modifySharedReservationLink(shareId, acrsPropertyCode, source, req, isPOFlow);
    }

    /**
     * 
     * @param reservation
     * @return
     */
    private List<RoomReservation> createNewSharedReservations(RoomReservation reservation, String existingAcrsPropertyCode) {

        // create acrs pending request without form of payment
        String originalProgramId = reservation.getProgramId();
        ReservationPendingReq reservationReq = createPendingReservationReq(reservation);
        //update propertyCode from the existing reservation if existing propertyCode is in MV305,MV002,MV003 and MV931
        if(StringUtils.isNotEmpty(existingAcrsPropertyCode) && acrsProperties.getPseudoExceptionProperties().contains(existingAcrsPropertyCode)){
            reservationReq.getData().getHotelReservation().getHotels().get(0).setPropertyCode(existingAcrsPropertyCode);
            reservationReq.getData().getHotelReservation().getSegments().get(0).setPropertyCode(existingAcrsPropertyCode);
        }
        //update adultCount=1 of all productUses for shared reservation
        updateGuestCount(reservationReq,1);
        // override price to 0.0
        ReservationUtil.createOverrideShareWithPricing(reservationReq);
        return createSecondaryReservations(reservationReq, reservation, originalProgramId);
    }
    private boolean breakPartyReservation(RoomReservation updatedRoomReservation,
            ReservationRetrieveResReservation existingRoomReservation) {
        //breakPartyReservation pending
        String cnfNumber = breakPartyReservationPending(updatedRoomReservation, existingRoomReservation);
        //breakPartyReservation commit
        try {
            String propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(updatedRoomReservation.getPropertyId());
            commitPendingRoomReservation(cnfNumber, propertyCode, updatedRoomReservation.getSource(), updatedRoomReservation.isPerpetualPricing());
        } catch (ParseException e) {
            log.error("Failed to parse commit Reservation Response from ACRS.");
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
        return StringUtils.isNotBlank(cnfNumber);
    }

    private String breakPartyReservationPending(RoomReservation updatedRoomReservation,
                                              ReservationRetrieveResReservation existingRoomReservation) {

        RoomReservation existingReservation = RoomReservationTransformer.transform(existingRoomReservation, acrsProperties);
        String cnfNumber = null;
        if (!CommonUtil.isDateRangInADateRange(updatedRoomReservation.getCheckInDate(),
                updatedRoomReservation.getCheckOutDate(), existingReservation.getCheckInDate(),
                existingReservation.getCheckOutDate())) {
            ReservationIdsRes resvIdRes = existingRoomReservation.getData().getHotelReservation().getReservationIds();
            resvIdRes.setExtCfNumber(null);
            removePartyCnfNumber(updatedRoomReservation.getConfirmationNumber(), updatedRoomReservation.getPropertyId(),
                    resvIdRes, null,updatedRoomReservation.getSource(),updatedRoomReservation.isPerpetualPricing());
            ReservationSearchReq reservationSearchReq = new ReservationSearchReq();
            ReservationSearchReqData reservationSearchReqData = new ReservationSearchReqData();
            reservationSearchReqData.setCfNumber(existingReservation.getPartyConfirmationNumber());
            reservationSearchReq.setData(reservationSearchReqData);
            ReservationSearchResPostSearchReservations serachRes = searchReservationsByReservationSearchReq(
                    reservationSearchReq, updatedRoomReservation.getSource());
            if (serachRes.getData().getReservationCount().intValue() == 1) {
                ReservationIdsSearchRes resvSeachIdRes = serachRes.getData().getHotelReservations().get(0)
                        .getReservationIds();
                resvSeachIdRes.setExtCfNumber(null);
                cnfNumber = resvSeachIdRes.getCfNumber();
                removePartyCnfNumber(cnfNumber, updatedRoomReservation.getPropertyId(), null,
                        resvSeachIdRes,updatedRoomReservation.getSource(), updatedRoomReservation.isPerpetualPricing());
            }
        }
        return cnfNumber;
    }

    private void removePartyCnfNumber(String cnfNumber, String propertyId, ReservationIdsRes resvIdRes,
            ReservationIdsSearchRes resvSeachIdRes, String source, boolean isPoFlow) {
        String propertyCode = referenceDataDAOHelper.retrieveAcrsPropertyID(propertyId);
        ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();
        ModificationChanges modificationChanges = new ModificationChanges();

        modificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                acrsProperties.getModifyExtConfirmationNumberPath(), null != resvIdRes ? resvIdRes : resvSeachIdRes));
        reservationPartialModifyReq.setData(modificationChanges);
        partialModifyPendingRoomReservation(reservationPartialModifyReq, cnfNumber, propertyCode, null, null, source, isPoFlow);
    }

    private void captureOrRefund(Map<CreditCardCharge, String> creditCardChargeAuthorizationMap,
            RoomReservation commitPendingRoomReservation, String source) {
        creditCardChargeAuthorizationMap.entrySet().stream()
                .filter(entry -> !entry.getValue().equalsIgnoreCase(ServiceConstant.COMP)).forEach(entry -> {
                    if (!ServiceConstant.REFUND.equalsIgnoreCase(entry.getValue())) {
                        capturePayment(commitPendingRoomReservation.getCheckInDate(),
                                commitPendingRoomReservation.getCheckOutDate(), entry,
                                commitPendingRoomReservation.getPropertyId(),
                                commitPendingRoomReservation.getConfirmationNumber(), source);
                    } else {
                        refundPayments(Collections.singletonList(entry.getKey()), source,
                                commitPendingRoomReservation.getConfirmationNumber(),
                                commitPendingRoomReservation.getPropertyId());
                    }
                });
    }

    @Override
    public RoomReservation preModifyReservation(PreModifyV2Request preModifyRequest) {
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for preModifyReservationV2: {}",
                    CommonUtil.convertObjectToJsonString(preModifyRequest));
        }
		// Step 1 - Fetch Reservation
        RoomReservation fetchedReservation = preModifyRequest.getFindResvResponse();
        
		if (null != fetchedReservation) {
			validateMissingWebSuppressComponents(fetchedReservation.getPurchasedComponents(),
					preModifyRequest.getRoomRequests());
		}

		// Step 2 - Convert to ModificationChangesRequest
		ModificationChangesRequest modificationChangesRequest =
				createModificationChangesRequestFromPreModifyRequest(preModifyRequest, fetchedReservation);

		// Step 3 - Pass into PreModifyCharges and return
		return preModifyChargesDecoratorForPreModifyReservation(modificationChangesRequest,
																				preModifyRequest, fetchedReservation);
    }
    @Override
    public RoomReservation commitPaymentReservation(CommitPaymentDTO refundRoomReservationRequest) {
        // get CRS cnf# from Search reservation
        String acrsConfNo = getAcrsConfirmationBySearchAPI(refundRoomReservationRequest);
        //if no acrs no returned then use conf no from request
        if (null == acrsConfNo) {
            acrsConfNo = refundRoomReservationRequest.getConfirmationNumber();
        }
        // 2. get pending reservation
        //get reservation frm pending status
        ReservationRetrieveResReservation reservationRetrieveResReservation = retrievePendingReservationByConfirmationNumber(acrsConfNo,
                refundRoomReservationRequest.getSource());
        RoomReservation roomReservation = RoomReservationTransformer.transform(reservationRetrieveResReservation, acrsProperties);
        roomReservation.setCreditCardCharges(refundRoomReservationRequest.getCreditCardCharges());
        // 3. if resv status pending then create payment txn and call modify pending
         if(roomReservation.getProfile().getFirstName().equalsIgnoreCase(refundRoomReservationRequest.getFirstName()) &&
                roomReservation.getProfile().getLastName().equalsIgnoreCase(refundRoomReservationRequest.getLastName())) {

             Map<CreditCardCharge, String> creditCardChargeAuthorizationMap = createCreditCardChargeAuthorizationMap(refundRoomReservationRequest.getCreditCardCharges());
             updateCardFormOfPayment(roomReservation.getConfirmationNumber(), roomReservation,
                    creditCardChargeAuthorizationMap, refundRoomReservationRequest.getSource(),
                    roomReservation.getAddOnsDeposits());
            RoomReservation commitReservation;

            // 4. call commit reservation
            try {
                commitReservation = commitPendingRoomReservation(roomReservation.getConfirmationNumber(), roomReservation.getPropertyId(),
                        refundRoomReservationRequest.getSource(), false);
            } catch (ParseException e) {
                log.error("Failed to parse ACRS reservation during modify commit while adding refund to reservation.");
                throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
            }
            //5. update promo
            updatePatronPromo(reservationRetrieveResReservation, roomReservation, roomReservation.getConfirmationNumber(), acrsProperties);
            // Set payments information from fetched before response
            List<Payment> allPayments = new ArrayList<>(roomReservation.getPayments());
            //Add refund payment
            creditCardChargeAuthorizationMap.forEach((k,v)->{
                // If refund is successful create payment object from the creditCardCharge object
                Payment refundPayment = createPaymentFromCreditCardCharge(k);
                // Set DCC transDate to now as ICE reads this as transDate even if DCC not completed.
                refundPayment.setDccTransDate(new Date());
                allPayments.add(refundPayment);
            });
            commitReservation.setPayments(allPayments);
            commitReservation.setCreditCardCharges(roomReservation.getCreditCardCharges());
            commitReservation.setAmountDue(commitReservation.getAmountDue());
            commitReservation.setCustomerId(refundRoomReservationRequest.getCustomerId());
            commitReservation.setItineraryId(refundRoomReservationRequest.getItineraryId());
            commitReservation.setId(refundRoomReservationRequest.getId());
            // convert acrs to refData guid
            invokeRefDataRoutingInfo(commitReservation.getRoutingInstructions(), commitReservation.getPropertyId(), false);
            setReferenceData(commitReservation);
             if (ReservationState.Booked.equals(commitReservation.getState())
                     && StringUtils.isNotBlank(commitReservation.getOperaConfirmationNumber())) {
                 commitReservation.setOperaState(ServiceConstant.RESERVED_STRING);
             }
            return commitReservation;
        } else {
            throw new SystemException(ErrorCode.INVALID_NAME, null);
        }
    }

    @Override
    public RoomReservation modifyPendingRoomReservationV2(RoomReservation reservation){

        if (log.isDebugEnabled()) {
            log.debug("Incoming Request for modifyRoomReservationV2: {}",
                    CommonUtil.convertObjectToJsonString(reservation));
        }

        //1. get existing committed or pending reservation
        ReservationRetrieveResReservation existingResv = retrieveReservationByConfirmationNumber(reservation.getConfirmationNumber(),
                    reservation.getSource());
        RoomReservation existingReservation = RoomReservationTransformer.transform(existingResv, acrsProperties);
        // add missing pkg components in request
        //Its not required as we are not going to touch the existing pkg component
        //mergeExistingPkgComponents(reservation,existingReservation.getPurchasedComponents());
        //2. set existing shared info in the request if any.
        setExistingSharedInfo(reservation,existingResv);
        //3. Invoke modify pending for the changes of date,room,rate, RI, guest count etc.
        RoomReservation pendingRoomReservation = null;
        boolean isCashPayment = isCashPayment(reservation.getGuaranteeCode(),reservation.getCreditCardCharges());
        ReservationModifyPendingRes modifyPendingRes = invokeModifyPending(reservation, existingResv,isCashPayment);
        if(null != modifyPendingRes) {
            pendingRoomReservation = transformReservationModifyPendingRes(modifyPendingRes);
        }
        //4. check if reservation has no modification but still need to commit due to some
        // only override deposit need to be taken care in /commit/payment API
        // get ACRS propertyCode for the existing reservation.(This is required for exception pseudo properties-MV305,MV931MV002 and MV003)
        String existingAcrsPropertyCode = existingResv.getData().getHotelReservation().getHotels().get(0).getPropertyCode();
        //5. If anything modified then update form of payment
        if (null != pendingRoomReservation) {
                //6. handle existing shared reservation(modify) before commit the primary reservation.
                // As per ACRS we have to do modify pending for shared reservation before committing the primary reservation.
                List<RoomReservation> modifiedSharedResvs = new ArrayList<>();
                if (isProductUsesModified(referenceDataDAOHelper.updateAcrsReferencesFromGse(populatePreModifyRequest(reservation, existingReservation)),
                        existingReservation) &&
                        CollectionUtils.isNotEmpty(reservation.getSharedConfirmationNumbers())) {
                    modifiedSharedResvs = invokeModifySharedReservation(reservation);
                }
                     //8. Update Patron promo status will be done when reservation will be /commit/payment API
                     //9. update committed response by ref data, input request
                    updateCommittedResponse(pendingRoomReservation, reservation, existingResv, isCashPayment);

                    //10.if it is partyReservation the break the party reservation If
                    // required
                    if (StringUtils.isNotEmpty(pendingRoomReservation.getPartyConfirmationNumber())) {
                        if (null != breakPartyReservationPending(reservation, existingResv)) {
                            pendingRoomReservation.setPartyConfirmationNumber(null);
                        }
                    }
                    // Handle modify shared reservation modification
                    // create new shared reservation If any or modified
                    boolean hasSharedWithResv = CollectionUtils.isNotEmpty(reservation.getSharedConfirmationNumbers())
                            || CollectionUtils.isNotEmpty(reservation.getShareWithCustomers());
                    if (hasSharedWithResv) {
                        // If new sharedWith added with existing shared reservation
                        if(CollectionUtils.isNotEmpty(reservation.getShareWithCustomers())){
                            // We need to commit primary before  creating new shared
                            try {
                                commitPendingRoomReservation(
                                        reservation.getConfirmationNumber(), reservation.getPropertyId(), reservation.getSource(),
                                        reservation.isPerpetualPricing());
                            } catch(ParseException px){
                                log.error("Error while parsing the Commit Response in modify pending. {}",px.getMessage());
                            }catch (SystemException | BusinessException e) {
                                    ignoreReservation(
                                            reservation.getConfirmationNumber(), reservation.getPropertyId(), reservation.getSource(),
                                            reservation.isPerpetualPricing());
                                throw e;
                            }
                            List<RoomReservation> newSharedResvs = invokeNewSharedReservation(reservation,existingAcrsPropertyCode);
                            modifiedSharedResvs.addAll(newSharedResvs);
                        }
                        // set links and shared information in
                        setShareWithInfoIntoComittedReservation(pendingRoomReservation,modifiedSharedResvs,reservation);
                    }
                    // TODO: This OperaState fix is temporary, so adding here and
                    // easy to
                    // remove as well.
                    if (ReservationState.Booked.equals(pendingRoomReservation.getState())
                            && StringUtils.isNotBlank(pendingRoomReservation.getOperaConfirmationNumber())) {
                        pendingRoomReservation.setOperaState(ServiceConstant.RESERVED_STRING);
                    }
                    return pendingRoomReservation;
        } else if(CollectionUtils.isNotEmpty(reservation.getShareWithCustomers())){
            // Only share with added during modification
            List<RoomReservation> modifiedSharedWiths = invokeNewSharedReservation(reservation,existingAcrsPropertyCode);
            List<String> modifiedSharedCnfs = modifiedSharedWiths.stream().map(RoomReservation::getConfirmationNumber).collect(Collectors.toList());
            reservation.setShareWiths(modifiedSharedCnfs.toArray(new String[0]));
            reservation.setShareWithReservations(modifiedSharedWiths);
            return reservation;
        }else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"No change for modification.");
        }
    }

    private void ignoreReservation(String cnfNumber,String propertyId, String source,boolean isPoFlow){
        try {
            ignorePendingRoomReservation(cnfNumber, propertyId, source,isPoFlow);
        }catch (Exception ex){
            log.error("Error while ignoring the reservation {}",ex.getMessage());
        }
    }

    private String getAcrsConfirmationBySearchAPI(CommitPaymentDTO paymentRoomReservationRequest){
        ReservationSearchReq reservationSearchRequest =
                RoomReservationTransformer.buildSearchRequestWithOperaConfirmationNumber(paymentRoomReservationRequest.getConfirmationNumber());
        ReservationSearchResPostSearchReservations searchResponse = searchReservationsByReservationSearchReq(reservationSearchRequest, paymentRoomReservationRequest.getSource());
        List<HotelReservationSearchResPostSearchReservations> response = searchResponse.getData().getHotelReservations();
        if(null != response) {
            List<ReservationIdsSearchRes> confirmationNumbers = response.stream().map(HotelReservationSearchResPostSearchReservations::getReservationIds).collect(Collectors.toList());
            Optional<String> acrsNo = confirmationNumbers.stream().map(e -> e.getCfNumber()).findFirst();
            return acrsNo.isPresent()? acrsNo.get() : null;
        }
        return null;
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
	private RoomReservation preModifyChargesDecoratorForPreModifyReservation(ModificationChangesRequest modificationChangesRequest,
																			 PreModifyV2Request preModifyRequest,
																			 RoomReservation fetchedReservation) {
		// Call preModifyCharges
		RoomReservation modifiedReservation = preModifyCharges(modificationChangesRequest);

		// Decorator work specific to preModifyReservation:
		// CustomerId not returned from ACRS response
		modifiedReservation.setCustomerId(preModifyRequest.getCustomerId());

		// ACRS will only return credit card details in fetchReservation call, so need to populate
		//  creditCardCharges & Payments from there.
		modifiedReservation.setCreditCardCharges(fetchedReservation.getCreditCardCharges());
		modifiedReservation.setPayments(fetchedReservation.getPayments());
		// setRefData for spl req, room feature, etc
        setReferenceData(modifiedReservation);
		return modifiedReservation;
	}

	private ModificationChangesRequest createModificationChangesRequestFromPreModifyRequest(PreModifyV2Request preModifyRequest, RoomReservation fetchedReservation) {
		ModificationChangesRequest modificationChangesRequest = populatePreModifyRequest(fetchedReservation);
		// Make modifications to modificationChangesRequest based on preModifyRequest
		modificationChangesRequest.setSource(preModifyRequest.getSource());
		modificationChangesRequest.setChannel(preModifyRequest.getChannel());

		// Note: changing number of guests is not supported by /v2/reservation/preview

		// If either checkInDate or checkOutDate is changed update modificationChangesRequest
		if ( null != preModifyRequest.getTripDetails() ) {
			if (!ReservationUtil.areDatesEqualExcludingTime(preModifyRequest.getTripDetails().getCheckInDate(), fetchedReservation.getCheckInDate()) ) {
				modificationChangesRequest.getTripDetails().setCheckInDate(preModifyRequest.getTripDetails().getCheckInDate());
			}
			if (!ReservationUtil.areDatesEqualExcludingTime(preModifyRequest.getTripDetails().getCheckOutDate(), fetchedReservation.getCheckOutDate()) ) {
				modificationChangesRequest.getTripDetails().setCheckOutDate(preModifyRequest.getTripDetails().getCheckOutDate());
			}
		}

		// RoomRequest modifications
		modificationChangesRequest.setRoomRequests(populateRoomRequests(preModifyRequest.getRoomRequests(), fetchedReservation.getSpecialRequests()));

		return modificationChangesRequest;
	}

	private List<String> getSpecialRequestsIdListFromPurchasedComponents(List<PurchasedComponent> purchasedComponents) {
        if (null == purchasedComponents) {
            return new ArrayList<>();
        } else {
            return purchasedComponents.stream()
                    .map(PurchasedComponent::getId)
                    .collect(Collectors.toList());
        }
    }

    private RoomReservation getModifiedReservationFromPartialModifyIgnore(ReservationPartialModifyReq reservationPartialModifyReq, String propertyId, String confirmationNumber,String source, boolean isPoFlow) {
        // Send pending partial modify to ACRS
        RoomReservation pendingRoomReservation = partialModifyPendingRoomReservation(reservationPartialModifyReq,
                                                                                     confirmationNumber,
                                                                                     propertyId, null, null,source, isPoFlow);
        // Send ignore pending
        if (!ignorePendingRoomReservation(confirmationNumber, propertyId, source, isPoFlow)) {
            log.warn("Ignore pending RoomReservation failed for confirmation number: {}",
                    pendingRoomReservation.getConfirmationNumber());
        }

        return pendingRoomReservation;
    }
    //@uttam
    // At this point all productUses are modified and it will be adding productUse wise RI based on date.
    private ReservationModifyPendingRes makeRoutingInstructionModification(RoomReservation reservation,
            ReservationRetrieveResReservation existingResv) {
        ReservationModifyPendingRes reservationRes = null;
        // check any routing changes are there or not
        boolean hasRoutingInsChanged = false;
        // get existing manual RIs only
        SegmentResItem existingMainSegment = BaseAcrsTransformer
                .getMainSegment(existingResv.getData().getHotelReservation().getSegments());
        ProductUseRes existingProductUses = existingMainSegment.getOffer().getProductUses();
        List<RoutingInstruction> existingRIs = existingProductUses.stream()
                .filter(p -> null != p.getRoutingInstructions())
                .flatMap(ris -> ris.getRoutingInstructions().stream()
                        .filter(ri -> !ri.getSource().equals(RoutingInstructionWithoutId.SourceEnum.SYSTEM)))
                .collect(Collectors.toList());
        List<ReservationRoutingInstruction> modifiedRIs = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(reservation.getRoutingInstructions())) {
            modifiedRIs = reservation.getRoutingInstructions().stream().filter(x-> !x.getIsSystemRouting()).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(modifiedRIs)
                || (CollectionUtils.isNotEmpty(existingRIs) && CollectionUtils.isEmpty(modifiedRIs))) {
            hasRoutingInsChanged = true;
        }
        if (hasRoutingInsChanged) {
            ReservationRetrieveResReservation modifiedPendingResv = retrievePendingReservationByConfirmationNumber(
                    reservation.getConfirmationNumber(), reservation.getSource());
            SegmentResItem pendingMainSegment = BaseAcrsTransformer
                    .getMainSegment(modifiedPendingResv.getData().getHotelReservation().getSegments());

            // get RI changes
            ReservationPartialModifyReq routingChanges = createRoutingInstructionChnages(existingProductUses,
                    pendingMainSegment.getOffer().getProductUses(), modifiedRIs, ReservationUtil.convertDateToLocalDate(reservation.getCheckOutDate()));
            if (CollectionUtils.isNotEmpty(routingChanges.getData())) {
                reservationRes = invokePartialModifyPendingRoomReservation(routingChanges,
                        reservation.getConfirmationNumber(), reservation.getPropertyId(),
                        (null != reservation.getAgentInfo()) ? reservation.getAgentInfo().getAgentId() : null,
                        reservation.getRrUpSell(), reservation.getSource(), reservation.isPerpetualPricing());

            }
        }
        return reservationRes;
    }
    
    /**
     * This will build the RIs changes for modify flow 
     * It will consider only overlapped product uses.
     * @param existingProductUseRes
     * @param modifiedProductUseRes
     * @param routingInstructions
     * @return
     */
    public  ReservationPartialModifyReq createRoutingInstructionChnages(ProductUseRes existingProductUseRes,
            ProductUseRes modifiedProductUseRes, List<ReservationRoutingInstruction> routingInstructions, LocalDate checkOutDate) {
        ReservationPartialModifyReq reservationPartialModifyReq = new ReservationPartialModifyReq();

        ModificationChanges routingModificationChanges = new ModificationChanges();
        reservationPartialModifyReq.setData(routingModificationChanges);
        // Modify or add Manual routing instruction into overlapped productUses

        List<RoutingInstruction> modifiedAcrsRoutingList = BaseAcrsTransformer
                .buildACRSRoutingInstructions(routingInstructions);
        //filter only main product
        List<ProductUseResItem> modifiedSleepingProductUseRes = modifiedProductUseRes.stream().filter(pu ->Boolean.TRUE == pu.getIsMainProduct()).collect(Collectors.toList());
        List<ProductUseResItem> existingSleepingProductUseRes = existingProductUseRes.stream().filter(pu ->Boolean.TRUE == pu.getIsMainProduct()).collect(Collectors.toList());

                modifiedSleepingProductUseRes.forEach(modifiedProductUse -> {
            addRoutingInstructionChanges(modifiedAcrsRoutingList,modifiedProductUse,
                    routingModificationChanges, existingSleepingProductUseRes, checkOutDate);
        });

        return reservationPartialModifyReq;
    }

    public void addRoutingInstructionChanges(List<RoutingInstruction> modifiedAcrsRoutingList,
                                             ProductUseResItem modifiedProductUse, ModificationChanges routingModificationChanges, List<ProductUseResItem> existingProductUses, LocalDate checkOutDate) {
        Optional<ProductUseResItem> existingProductUse = existingProductUses.stream().filter(x -> x.getId() == modifiedProductUse.getId()).findFirst();
        if (existingProductUse.isPresent()) {
            boolean isRoutingExists = CollectionUtils.isNotEmpty(existingProductUse.get().getRoutingInstructions());
            boolean isModifiedRoutingExists = CollectionUtils.isNotEmpty(modifiedAcrsRoutingList);
            List<RoutingInstruction> acrsProductUseRIs = new ArrayList<>();
            // build Manual RIs list to be added
            if (isModifiedRoutingExists) {
                modifiedAcrsRoutingList.forEach(modifiedRI -> {
                    acrsProductUseRIs.addAll(RoomReservationTransformer
                            .buildProductRoutingInstructions(modifiedRI, modifiedProductUse, checkOutDate));
                });
            }
            if (isRoutingExists) {
                // remove all existing Manual RI
                routingModificationChanges.add(createRemoveModificationChange(String.format(acrsProperties.getDeleteAllManualRIProductUsePath(), modifiedProductUse.getId())));
                //Append all updated Manual RI for the productUse
                acrsProductUseRIs.forEach(productRI -> {
                    routingModificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.APPEND,
                            String.format(acrsProperties.getModifyRoutingInstructionsPath(), modifiedProductUse.getId()),
                            productRI));
                });
            } else if (isModifiedRoutingExists) {
                //Upsert all updated Manual RI for the productUse
                routingModificationChanges.add(createModificationChange(ModificationChangesItem.OpEnum.UPSERT,
                        String.format(acrsProperties.getModifyRoutingInstructionsPath(), modifiedProductUse.getId()),
                        acrsProductUseRIs));
            }
        }
    }

    private void mergeExistingPkgComponents(RoomReservation reservation, List<PurchasedComponent> existingComponents){
        if(isExistingPkgReservation(reservation.getPropertyId(), existingComponents)) {
            List<String> missingPkgComponents =
                    getMissingPkgComponents(reservation.getPropertyId(), reservation.getSpecialRequests(),
                            existingComponents);
            if(null == reservation.getSpecialRequests()){
                reservation.setSpecialRequests(missingPkgComponents);
            }else {
                reservation.getSpecialRequests().addAll(missingPkgComponents);
            }
        }
    }

    private void mergeExistingPkgComponents(ModificationChangesRequest modificationChangesRequest,String propertyId, List<PurchasedComponent> existingComponents){
        //packge2.0 changes- check if existing reservation is package reservation.
        if(isExistingPkgReservation(propertyId, existingComponents)) {
            List<RoomRequest> missingPkgRoomRequests =
                    getMissingPkgRoomRequests(propertyId,
                            modificationChangesRequest.getRoomRequests(), existingComponents);
            if(CollectionUtils.isNotEmpty(missingPkgRoomRequests)) {
                if (modificationChangesRequest.getRoomRequests() == null) {
                    modificationChangesRequest.setRoomRequests(missingPkgRoomRequests);
                } else {
                    modificationChangesRequest.getRoomRequests().addAll(missingPkgRoomRequests);
                }
            }
        }
    }
}
