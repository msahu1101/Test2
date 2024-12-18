package com.mgm.services.booking.room.service.helper;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.dao.ProgramContentDAO;
import com.mgm.services.booking.room.model.ComponentPrice;
import com.mgm.services.booking.room.model.ComponentPrices;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.content.Program;
import com.mgm.services.booking.room.model.inventory.BookedItemList;
import com.mgm.services.booking.room.model.inventory.BookedItems;
import com.mgm.services.booking.room.model.inventory.ItemStatus;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.reservation.*;
import com.mgm.services.booking.room.model.response.MyVegasResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.AuroraProperties;
import com.mgm.services.booking.room.properties.AuroraProperties.AuroraCredential;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.MyVegasService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.cache.rediscache.service.PropertyPkgComponentCacheService;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.booking.room.util.TokenValidationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.util.DateUtil;
import com.mgmresorts.myvegas.jaxb.Customer;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper class for ModifyReservation service.
 * 
 * @author laknaray
 *
 */
@Component
@Log4j2
public class ReservationServiceHelper {

    @Autowired
    private MyVegasService myVegasService;
    
    @Autowired
    private ApplicationProperties appProperties;

    @Autowired
    private AuroraProperties auroraProperties;

    @Value("${myVegas.restClient.enabled}")
    private boolean isRestClientEnabled;
    @Value("${idms.token.validation.enabled}")
    private boolean validateTokenEnabled;
    @Autowired
    private ProgramContentDAO programContentDao;

    @Autowired
    private SecretsProperties secretProperties;

    @Autowired
    private RoomProgramCacheService roomProgramCacheService;
    @Autowired
    private ComponentDAO componentDAO;

    @Autowired
    private PropertyPkgComponentCacheService propertyPkgComponentCacheService;

    public boolean validateTokenOrServiceBasedRole(String tokenScope){
        if(validateTokenEnabled){
            return tokenContainsScope(tokenScope);
        }else{
            return tokenContainsServiceRole();
        }
    }

    /**
     * Gets the HttpServletRequest object and check for the existence of the
     * <code>scope</code> in JWT token.
     * 
     * @param scope
     *            scope
     * @return true, only if the JWT Token in the request has <code>scope</code>
     */
    public boolean tokenContainsScope(String scope) {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        return TokenValidationUtil.tokenContainsScope(TokenValidationUtil.extractJwtToken(httpRequest), scope);
    }

    /**
     * Gets the HttpServletRequest object and check for the existence of the
     * <code>mgm_service</code> in JWT token.
     *
     *            scope
     * @return true, only if the JWT Token in the request has <code>scope</code>
     */
    public boolean tokenContainsServiceRole() {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();

        return TokenValidationUtil.tokenContainsServiceRole(TokenValidationUtil.extractJwtToken(httpRequest));
    }
    /**
     * Anonymous user allowed to modify a non-loyalty reservation. Logged user
     * allowed to modify reservations which has user's mlife number and transient reservations as well. 
     * WEB channel is allowed to modify reservations which contain programs not part of HDE
     * package.
     * 
     * @param reservation
     *            room reservation object
     * @param token
     *            token string
     * @return true, when i) an anonymous user trying to modify non-loyalty
     *         reservation ii) logged in user trying to modify loyalty reservation
     *         of the same member or transient reservation (i OR ii), otherwise false.
     */
	/*
	 * public boolean isRequestAllowedToModifyCancelReservation(RoomReservation
	 * reservation, String token) { return ((!isLoggedInUser(token) &&
	 * !isLoyaltyReservation(reservation)) || (isLoggedInUser(token) &&
	 * isLoyaltyReservation(reservation) && isSameMember(reservation, token))) &&
	 * !isReservationHasHDEProgram(reservation); }
	 */    
    public boolean isRequestAllowedToModifyReservation(RoomReservation
    		reservation, String token) { 
    	return ((!isLoggedInUser(token) && !isLoyaltyReservation(reservation)) || (isLoggedInUser(token) && 
    			(!isLoyaltyReservation(reservation)||isSameMember(reservation, token)))); 
    }
    /**
     * A reservation is allowed to be canceled if the reservation is found and 
     * the first name and last name in the reservation is matching with the first and last name
     * in the request.
     * @param reservation
     *            room reservation object
     * @return true, Always return true, retaining this method to add any conditions in future.
     */
    public boolean isRequestAllowedToCancelReservation(RoomReservation
    		reservation) { 
    	return true; 
    }

    
    public boolean validateLoggedUserOrServiceToken(String token){
        if(validateTokenEnabled){
            return isLoggedInUser(token);
        }else{
            return isTokenAServiceToken(token);
        }
    }

    /**
     * Request made with guest jwt token is considered as logged in user.
     * 
     * @param token token string
     * @return true if the token in the request is a guest token otherwise false
     */
    public boolean isLoggedInUser(String token) {
        return TokenValidationUtil.isTokenAGuestToken(token);
    }
    
    /**
     * Request made with service jwt token is considered as service token.
     * 
     * @param token token string
     * @return true if the token in the request is a service token otherwise false
     */
    public boolean isTokenAServiceToken(String token) {
        return TokenValidationUtil.isTokenAServiceToken(token);
    }


    /**
     * Reservation is considered as loyalty reservation, if reservation has mlife
     * number.
     * 
     * @return true only if the reservation has positive mlife number otherwise
     *         false
     */
    public boolean isLoyaltyReservation(RoomReservation reservation) {
        return null != reservation && null != reservation.getProfile() && reservation.getProfile().getMlifeNo() > 0;
    }

    /**
     * Compare reservation profile's mlifeNo and mlife claim present in the token.
     * 
     * @param reservation
     *            reservation object
     * @param token
     *            token string
     * @return true, only if reservation profile's mlifeNo and mlife number present
     *         in token are matching, otherwise false.
     */
    public boolean isSameMember(RoomReservation reservation, String token) {
        Map<String, String> tokenClaims = TokenValidationUtil.getClaimsFromToken(token,
                Arrays.asList(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM));
        
        if (tokenClaims.containsKey(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM)) {
            int mlifeNumber = Integer.parseInt(tokenClaims.get(ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM));
            log.info(
                    "JWT token is a guest token. Comparing token's mlifeNumber:: {} with reservation profile's mlifeNo:: {}",
                    mlifeNumber, reservation.getProfile().getMlifeNo());

            return null != reservation.getProfile() && reservation.getProfile().getMlifeNo() == mlifeNumber;
        }

        return false;
    }

    /**
     * Passed <code>firstName</code> and <code>lastName</code> will be compared with
     * the request.
     * 
     * @param firstName
     *            first name to compare
     * @param lastName
     *            last name to compare
     * @param preModifyRequest
     *            request object
     * @return true, only if the first and last names are matching, otherwise false.
     */
    public boolean requestHasMatchingFirstAndLastNames(String firstName, String lastName,
            PreModifyV2Request preModifyRequest) {
        String firstNameInRequest = preModifyRequest.getFirstName();
        String lastNameInRequest = preModifyRequest.getLastName();

        log.info("Comparing firstNameInRequest:: {}, lastNameInRequest:: {}, with firstName:: {}, lastName:: {}",
                firstNameInRequest, lastNameInRequest, firstName, lastName);

        return StringUtils.equalsIgnoreCase(firstName, firstNameInRequest)
                && StringUtils.equalsIgnoreCase(lastName, lastNameInRequest);
    }
    
    /**
     * Validate additional details like firstName and lastName.
     * 
     * @param preModifyV2Request
     *            pre modify request
     * @param reservation
     *            room reservation object
     * @return true, when request has matching first and last names, otherwise false
     */
    public boolean isRequestAllowedToFindReservation(PreModifyV2Request preModifyV2Request,
            RoomReservation reservation) {
        boolean isRequestAllowedToFindReservation = true;
        log.info("JWT token is not a service token");
        if (null != reservation.getProfile()) {
            String fNameInReservation = reservation.getProfile().getFirstName();
            String lNameInReservation = reservation.getProfile().getLastName();
            if (!requestHasMatchingFirstAndLastNames(fNameInReservation, lNameInReservation, preModifyV2Request)) {
                log.info("First name or last name didn't match to return the reservation");
                isRequestAllowedToFindReservation = false;
            }
        } else {
            log.info("Reservation does not have a profile object to return the reservation");
            isRequestAllowedToFindReservation = false;
        }
        return isRequestAllowedToFindReservation;
    }

    /**
     * Create <code>FindReservationV2Request</code> object from
     * <code>PreModifyV2Request</code> object.
     * 
     * @param preModifyV2Request
     *            pre modify request object
     * @return find reservation request object
     */
    public FindReservationV2Request createFindReservationV2Request(PreModifyV2Request preModifyV2Request) {
        FindReservationV2Request findReservationV2Request = new FindReservationV2Request();
        findReservationV2Request.setConfirmationNumber(preModifyV2Request.getConfirmationNumber());
        findReservationV2Request.setSource(preModifyV2Request.getSource());
        findReservationV2Request.setCacheOnly(true);
        return findReservationV2Request;
    }


    /**
     * Invokes <code>validateRedemptionCodeV2</code> method of
     * <code>MyVegasService</code> by creating for the object of
     * <code>MyVegasRequest</code>.
     * 
     * @param myVegasPromoCode
     *            myVegas promo code
     * @param checkInDate
     *            check in date
     * @param propertyId
     *            property id
     * @return MyVegasResponse received from <code>MyVegasService</code>
     */
    public MyVegasResponse validateRedemptionCode(String myVegasPromoCode, Date checkInDate, String propertyId) {
        MyVegasResponse redemptionResponse = null;
        if (StringUtils.isNoneBlank(myVegasPromoCode)) {
            MyVegasRequest myVegasRequest = new MyVegasRequest();
            myVegasRequest.setSkipCache(true);
            myVegasRequest.setRedemptionCode(myVegasPromoCode);
            redemptionResponse = myVegasService.validateRedemptionCodeV2(myVegasRequest, getAuthorizationHeader());
            if (redemptionResponse == null || StringUtils.isEmpty(redemptionResponse.getStatus())) {
                throw new BusinessException(ErrorCode.REDEMPTION_OFFER_NOT_AVAILABLE);
            }
            log.info("MyVegas RedemptionCode: {} status: {}, programId: {}", redemptionResponse.getRedemptionCode(),
                    redemptionResponse.getStatus(), redemptionResponse.getProgramId());
        }
        return redemptionResponse;
    }

    /**
     * Populates <code>roomReservation</code> object with myVegasProgramId, when
     * if it is neither empty nor null.
     * 
     * @param roomReservation
     *            room reservation object
     * @param redemptionResponse
     *            redemption response object
     */
    public void updateProgramId(RoomReservation roomReservation, MyVegasResponse redemptionResponse) {
        if (null != redemptionResponse && StringUtils.isNotBlank(redemptionResponse.getProgramId())) {
            roomReservation.getBookings().stream().forEach(t -> t.setProgramId(redemptionResponse.getProgramId()));
        }
    }

    /**
     * Invokes <code>confirmRedemptionCodeV2</code> method of
     * <code>MyVegasService</code> with the given details.
     * 
     * @param confirmationNumber
     *            confirmation number string
     * @param redemptionResponse
     *            remdeptonResponse object
     * @param reservationRequest
     *            room reservation object
     * @param reservationDate
     *            room reservation date
     */
    public void confirmRedemptionCode(RoomReservationRequest reservationRequest, MyVegasResponse redemptionResponse,
            String confirmationNumber, Date reservationDate, String propertyId) {
        MyVegasRequest myVegasRequest = new MyVegasRequest();
        myVegasRequest.setRedemptionCode(reservationRequest.getMyVegasPromoCode());
        myVegasRequest.setConfirmationNumber(confirmationNumber);
        myVegasRequest.setReservationDate(getReservationDate(reservationDate, propertyId));
        myVegasRequest.setCouponCode(redemptionResponse.getCouponCode());

        Customer customer = new Customer();
        UserProfileRequest profile = reservationRequest.getProfile();
        customer.setFirstName(profile.getFirstName());
        customer.setLastName(profile.getLastName());
        customer.setMembershipID(Integer.toString(profile.getMlifeNo()));
        customer.setDateOfBirth(DateUtil.convertDateToString(
                isRestClientEnabled ? ServiceConstant.ISO_8601_DATE_FORMAT : ServiceConstant.DEFAULT_DATE_FORMAT,
                profile.getDateOfBirth(), TimeZone.getTimeZone(appProperties.getTimezone(propertyId))));
        customer.setEmailID(profile.getEmailAddress1());
        customer.setTier(profile.getTier());
        customer.setDateOfEnrollment(DateUtil.convertDateToString(
                isRestClientEnabled ? ServiceConstant.ISO_8601_DATE_FORMAT : ServiceConstant.DEFAULT_DATE_FORMAT,
                profile.getDateOfEnrollment(), TimeZone.getTimeZone(appProperties.getTimezone(propertyId))));
        myVegasRequest.setCustomer(customer);

        myVegasService.confirmRedemptionCodeV2(myVegasRequest);
    }

    private String getReservationDate(Date checkInDate, String propertyId) {
        return DateUtil.convertDateToString(isRestClientEnabled ? ServiceConstant.ISO_8601_DATE_FORMAT : ServiceConstant.DEFAULT_DATE_FORMAT,
                checkInDate, TimeZone.getTimeZone(appProperties.getTimezone(propertyId)));
    }

    private String getAuthorizationHeader() {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        return TokenValidationUtil.extractJwtToken(httpRequest);
    }

    /**
     * Method checks if the date change is requested and performs all eligibility checks to perform dates changes.
     * Reservation dates are not modifiable in following scenarios:
     * <ul>
     * <li>If reservation has any of these: comp night, perpetual offer pricing,
     * multiple room programs</li>
     * <li>If reservation has no credit card on file</li>
     * <li>If reservation is in the cancellation penalty period</li>
     * </ul>
     * 
     * @param reservation
     *            Room Reservation object
     * @param request
     *            Preview modify request
     */
    public void isReservationDatesModifiable(RoomReservation reservation, PreModifyV2Request request) {

        TripDetailsRequest trip = request.getTripDetails();
        Instant checkInInstant1 = trip.getCheckInDate().toInstant().truncatedTo(ChronoUnit.DAYS);
        Instant checkInInstant2 = reservation.getCheckInDate().toInstant().truncatedTo(ChronoUnit.DAYS);
        Instant checkOutInstant1 = trip.getCheckOutDate().toInstant().truncatedTo(ChronoUnit.DAYS);
        Instant checkOutInstant2 = reservation.getCheckOutDate().toInstant().truncatedTo(ChronoUnit.DAYS);

        if (checkInInstant1.equals(checkInInstant2) && checkOutInstant1.equals(checkOutInstant2)) {
            return;
        }

        if (reservation.getBookings().stream().anyMatch(RoomPrice::isComp)) {
            throw new BusinessException(ErrorCode.MODIFY_VIOLATION_COMP);
        }

        if (reservation.getBookings().stream().filter(distinctByKey(RoomPrice::getProgramId))
                .collect(Collectors.toList()).size() > 1) {
            throw new BusinessException(ErrorCode.MODIFY_VIOLATION_MULTI_PROGRAM);
        }

        if (reservation.isPerpetualPricing()) {
            throw new BusinessException(ErrorCode.MODIFY_VIOLATION_PO);
        }

        if (reservation.getCreditCardCharges().stream().noneMatch(c -> StringUtils.isNotEmpty(c.getMaskedNumber()))) {
            throw new BusinessException(ErrorCode.MODIFY_VIOLATION_NO_CC);
        }

        if (ReservationUtil.isForfeit(reservation, appProperties)) {
            throw new BusinessException(ErrorCode.MODIFY_VIOLATION_FORFEIT);
        }

    }
    
    private <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    /**
     * Create <code>FindReservationV2Request</code> object from
     * <code>ReservationAssociateRequest</code> object.
     * 
     * @param request
     *            associate reservation request object
     * @return find reservation request object
     */
    public FindReservationV2Request createFindReservationV2Request(ReservationAssociateRequest request) {
        FindReservationV2Request findReservationV2Request = new FindReservationV2Request();
        findReservationV2Request.setConfirmationNumber(request.getConfirmationNumber());
        findReservationV2Request.setSource(request.getSource());
        findReservationV2Request.setCacheOnly(false);
        return findReservationV2Request;
    }

    public Map<String, String> getClaims(String token) {
        return TokenValidationUtil.getClaimsFromToken(token,
                Arrays.asList(ServiceConstant.IDMS_TOKEN_GIVEN_NAME_CLAIM, ServiceConstant.IDMS_TOKEN_FAMILY_NAME_CLAIM,
                        ServiceConstant.IDMS_TOKEN_MLIFE_CLAIM, ServiceConstant.IDMS_TOKEN_MGM_ID_CLAIM));
    }

    /**
     * Create <code>FindReservationV2Request</code> object from
     * <code>CancelV3Request</code> object.
     * 
     * @param request
     *            cancel v3 request object
     * @return find reservation request object
     */
    public FindReservationV2Request createFindReservationV2Request(CancelV3Request request) {
        FindReservationV2Request findReservationV2Request = new FindReservationV2Request();
        findReservationV2Request.setConfirmationNumber(request.getConfirmationNumber());
        findReservationV2Request.setSource(request.getSource());
        findReservationV2Request.setCacheOnly(true);
        return findReservationV2Request;
    }
    public FindReservationV2Request createFindReservationV2Request(CancelV4Request request) {
        FindReservationV2Request findReservationV2Request = new FindReservationV2Request();
        findReservationV2Request.setConfirmationNumber(request.getConfirmationNumber());
        findReservationV2Request.setSource(request.getSource());
        findReservationV2Request.setCacheOnly(true);
        return findReservationV2Request;
    }

    /**
     * Return the the request header.
     * 
     * @return header value as String
     */
    public String getRequestHeader(String header) {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        return httpRequest.getHeader(header);
    }

    public CancelV2Request createCancelV2Request(CancelV3Request cancelV3Request, RoomReservation reservation) {
        CancelV2Request cancelV2Request = new CancelV2Request();
        cancelV2Request.setConfirmationNumber(cancelV3Request.getConfirmationNumber());
        cancelV2Request.setOverrideDepositForfeit(cancelV3Request.isOverrideDepositForfeit());
        cancelV2Request.setCancellationReason(cancelV3Request.getCancellationReason());
        cancelV2Request.setPropertyId(cancelV3Request.getPropertyId());
        cancelV2Request.setSource(cancelV3Request.getSource());
        cancelV2Request.setPropertyId(reservation.getPropertyId());
        cancelV2Request.setItineraryId(reservation.getItineraryId());
        cancelV2Request.setReservationId(reservation.getId());
        cancelV2Request.setCustomerId(reservation.getCustomerId());
        cancelV2Request.setF1Package(reservation.isF1Package());
        cancelV2Request.setExistingReservation(reservation);
        cancelV2Request.setInAuthTransactionId(cancelV3Request.getInAuthTransactionId());
        cancelV2Request.setSkipPaymentProcess(cancelV3Request.isSkipPaymentProcess());
        cancelV2Request.setCancelPending(cancelV3Request.isCancelPending());
        cancelV2Request.setSkipCustomerNotification(cancelV3Request.isSkipCustomerNotification());
        return cancelV2Request;
    }
    public CancelV2Request createCancelV2Request(CancelV4Request cancelV4Request, RoomReservation reservation) {
        CancelV2Request cancelV2Request = new CancelV2Request();
        cancelV2Request.setConfirmationNumber(cancelV4Request.getConfirmationNumber());
        cancelV2Request.setOverrideDepositForfeit(cancelV4Request.isOverrideDepositForfeit());
        cancelV2Request.setCancellationReason(cancelV4Request.getCancellationReason());
        cancelV2Request.setPropertyId(cancelV4Request.getPropertyId());
        cancelV2Request.setSource(cancelV4Request.getSource());
        cancelV2Request.setPropertyId(reservation.getPropertyId());
        cancelV2Request.setItineraryId(reservation.getItineraryId());
        cancelV2Request.setReservationId(reservation.getId());
        cancelV2Request.setCustomerId(reservation.getCustomerId());
        cancelV2Request.setF1Package(reservation.isF1Package());
        cancelV2Request.setExistingReservation(reservation);
        cancelV2Request.setInAuthTransactionId(cancelV4Request.getInAuthTransactionId());
        cancelV2Request.setSkipCustomerNotification(cancelV4Request.isSkipCustomerNotification());
        return cancelV2Request;
    }

    /**
     * Reverse lookup logic to get source from the origin.
     * 
     * @param origin origin value
     * @return booking source as String
     */
    public String getBookingSource(String origin) {
        String channelKey = null;
        if (StringUtils.isNotEmpty(origin)) {
            if (StringUtils.equalsIgnoreCase(origin, "mlife")) {
                channelKey = "mgmri";
            } else {
                List<AuroraCredential> s = auroraProperties.getChannelCredentials().stream()
                        .filter(credential -> StringUtils.equalsIgnoreCase(credential.getName(), origin))
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(s)) {
                    channelKey = s.get(0).getKey();
                }
            }
        }
        return channelKey;
    }

    /**
     * Return the booking channel from source.
     * 
     * @param bookingSource booking source
     * @return booking channel as String
     */
    public String getBookingChannel(String bookingSource) {
        String bookingChannel = null;
        if (StringUtils.isNotEmpty(bookingSource)) {
            bookingChannel = StringUtils.equalsIgnoreCase(bookingSource, "ice") ? "ice" : "web";
        }
        return bookingChannel;
    }

    /**
     * Create <code>FindReservationV2Request</code> object from
     * <code>UpdateProfileInfoRequest</code> object.
     * 
     * @param updateProfileInfoRequest
     *            update profile info request object
     * @return find reservation request object
     */
    public FindReservationV2Request createFindReservationV2Request(UpdateProfileInfoRequest updateProfileInfoRequest) {
        FindReservationV2Request findReservationV2Request = new FindReservationV2Request();
        findReservationV2Request.setConfirmationNumber(updateProfileInfoRequest.getConfirmationNumber());
        findReservationV2Request.setSource(updateProfileInfoRequest.getSource());
        findReservationV2Request.setCacheOnly(true);
        return findReservationV2Request;
    }

    /**
     * Identify whether the mlife number got changed or added between original
     * reservation and update profile info request.
     * 
     * @param originalReservation
     *            room reservation object
     * @param updateProfileInfoRequest
     *            update profile info request object
     * @return true, if update request has mlife and it's different from existing mlife.
     */
    public boolean isMlifeAddedOrChanged(RoomReservation originalReservation,
            UpdateProfileInfoRequest updateProfileInfoRequest) {
        
        int orgMlife = Optional.ofNullable(originalReservation.getProfile()).orElse(new ReservationProfile()).getMlifeNo();
        int newMlife = Optional.ofNullable(updateProfileInfoRequest.getUserProfile()).orElse(new UserProfileRequest()).getMlifeNo();
        
        return newMlife > 0 && orgMlife != newMlife;
    }

    /**
     * Identify whether the partner account number got changed or added between original
     * reservation and update profile info request.
     *
     * @param originalReservation
     *            room reservation object
     * @param updateProfileInfoRequest
     *            update profile info request object
     * @return true, if update request has partner account no and it's different from existing partner account no.
     */
    public boolean isPartnerAccountNoAddedOrChanged(RoomReservation originalReservation,
                                         UpdateProfileInfoRequest updateProfileInfoRequest) {

        String newPartnerAcc = CollectionUtils.isNotEmpty(updateProfileInfoRequest.getUserProfile().getPartnerAccounts())?
                updateProfileInfoRequest.getUserProfile().getPartnerAccounts().get(0).getPartnerAccountNo() : null;
        String orgPartnerAcc = CollectionUtils.isNotEmpty(originalReservation.getProfile().getPartnerAccounts())?
                originalReservation.getProfile().getPartnerAccounts().get(0).getPartnerAccountNo() : null;
        return !Objects.equals(orgPartnerAcc, newPartnerAcc);

    }

    /**
     * Calculates <i>isStayDateModifiable</i>.
     * 
     * @param reservation
     *            room reservation object
     * @param channel
     *            channel value
     * @return return false, if any of the below is true, otherwise return true
     *         <ul>
     *         <li>reservation has any of these: comp night, perpetual offer
     *         pricing, multiple room programs, no program id for a night</li>
     *         <li>reservation has no credit card on file</li>
     *         <li>reservation is in the cancellation penalty period</li>
     *         <li>reservation is cancelled or completed</li>
     *         <li>reservation booked through 3rd party channel (i.e. not through
     *         ICE or Web)</li>
     *         <li>reservation booked via a program which is part of a package</li>
     *         </ul>
     */
    public boolean isStayDateModifiable(RoomReservation reservation, String channel) {
        if (null != reservation) {

            if (null == reservation.getBookings() || reservation.getBookings()
                    .stream()
                    .anyMatch(RoomPrice::isComp)
                    || reservation.getBookings()
                            .stream()
                            .anyMatch(b -> StringUtils.isEmpty(b.getProgramId()))
                    || reservation.getBookings()
                            .stream()
                            .filter(distinctByKey(RoomPrice::getProgramId))
                            .collect(Collectors.toList())
                            .size() > 1
                    || reservation.isPerpetualPricing()) {
                return false;
            }
            if (null == reservation.getCreditCardCharges() || reservation.getCreditCardCharges()
                    .stream()
                    .noneMatch(c -> StringUtils.isNotEmpty(c.getMaskedNumber()))
                    || ReservationUtil.isForfeit(reservation, appProperties)
                    || ReservationState.Cancelled.equals(reservation.getState())) {
                return false;
            }
            if (!(StringUtils.equalsIgnoreCase(channel, "ice") || StringUtils.equalsIgnoreCase(channel, "web"))) {
                return false;
            }
            if (isReservationHasHDEProgram(reservation)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * It takes room reservation object and booking channel and returns whether a given reservation 
     * is cancellable.
     * @param reservation
     *            room reservation object
     * @param channel
     * 			  booking channel
     * @return true, if the reservation is booked through non third-party channels(ICE or WEB).
     * and if the check in date has not passed.
     */
    public boolean isReservationCancellable(RoomReservation reservation, String channel) {
    	String propertyId = reservation.getPropertyId();
    	String timeZone = appProperties.getTimezone(propertyId);
		if (ReservationUtil.hasCheckInDateElapsed(reservation.getCheckInDate(), timeZone)
				|| !(StringUtils.equalsIgnoreCase(channel, "ice") || StringUtils.equalsIgnoreCase(channel, "web"))) {
			return false;
		}
    	return true;
    }
	

    /**
     * It takes room reservation object and checks for the existence of programId at
     * reservation level and invokes Content API service to fetch hdePackage field.
     * Depending on that it returns whether the reservation has HDE program or not.
     * 
     * @param reservation
     *            room reservation object
     * @return true, iff the reservation object has programId and Content API
     *         program content service returns hdePackage as true, otherwise false.
     */
    public boolean isReservationHasHDEProgram(RoomReservation reservation) {
    	boolean hasHDEProgram = false;
    	//OTA reservations failing with NPE since reservation is not found in GSE/ACRS
    	if(reservation!=null) {
    		String programId = reservation.getProgramId();
    		String propertyId = reservation.getPropertyId();
    		if (StringUtils.isNotBlank(programId)) {
    			hasHDEProgram = isHDEPackageProgram(programId, propertyId);
    		} else {
    			// For HDE Packages GSE is not returning Program Id at reservation
    			// level in an edge case scenario
    			if(CollectionUtils.isNotEmpty(reservation.getBookings())){
    			Optional<String> hDEPackageProgram = reservation.getBookings().stream().map(RoomPrice::getProgramId).distinct()
    					.filter(id -> isHDEPackageProgram(id, propertyId)).findAny();
    			hasHDEProgram = hDEPackageProgram.isPresent();
    			}
    		}
    	}
        return hasHDEProgram;
    }

    /**
     * Determine whether the email has to be sent by RTC or not.
     * 
     * @param propertyId
     *            reservation property id
     * @param channelExcludedFromSendEmail
     *            channel excluded from sending email
     * @return true, if and only if the channel is not excluded from sending email
     *         and property is on boarded to send email via RTC, otherwise false.
     */
    public boolean isNotifyCustomerViaRTC(String propertyId, boolean channelExcludedFromSendEmail, boolean isHdePackageReservation) {
        boolean notifyCustomerViaRTC = false;
        if (!channelExcludedFromSendEmail) {
        	String rtcEnabledProperties;
        	if(isHdePackageReservation) {
        		rtcEnabledProperties = secretProperties.getSecretValue(
                        String.format(appProperties.getRtcHDEPackageEmailsOnboardedListSecretKey(), appProperties.getRbsEnv()));
        	} else {
             rtcEnabledProperties = secretProperties.getSecretValue(
                    String.format(appProperties.getRtcEmailsOnboardedListSecretKey(), appProperties.getRbsEnv()));
        	}
            notifyCustomerViaRTC = StringUtils.contains(rtcEnabledProperties, propertyId);
        }
        return notifyCustomerViaRTC;
    }
    
    /**
     * Get itemized component prices from room reservation charges and taxes
     * object.
     * 
     * @param chargesAndTaxesCalc
     *            Charges and taxes calc object
     * @param componentCode
     *            Room component code
     * @return
     */
    public ComponentPrices getComponentPrices(RoomChargesAndTaxes chargesAndTaxesCalc, String componentCode) {
        ComponentPrices componentPrices = new ComponentPrices();
        for (RoomChargeItem roomChargeItem : chargesAndTaxesCalc.getCharges()) {
            Optional<ItemizedChargeItem> itemizedChargeItem = roomChargeItem.getItemized().stream()
                    .filter(x -> componentCode.equals(x.getItem()))
                    .findFirst();
            if (itemizedChargeItem.isPresent()) {
                double amount = itemizedChargeItem.get().getAmount();
                List<RoomChargeItem> taxesFees = chargesAndTaxesCalc.getTaxesAndFees().stream()
                        .filter(x -> DateUtils.isSameDay(roomChargeItem.getDate(), x.getDate())).collect(Collectors.toList());
                double taxAmt = 0;
                for (RoomChargeItem taxChargeItem : taxesFees) {
                    Optional<ItemizedChargeItem> itemizedTaxItem = taxChargeItem.getItemized().stream()
                            .filter(x -> componentCode.equals(x.getItem())).findFirst();
                    if (itemizedTaxItem.isPresent()) {
                        taxAmt = itemizedTaxItem.get().getAmount();
                    }
                }
                ComponentPrice componentPrice = new ComponentPrice();
                componentPrice.setDate(roomChargeItem.getDate());
                componentPrice.setAmount(amount);
                componentPrice.setTax(taxAmt);
                componentPrices.add(componentPrice);
            }
        }
        return componentPrices;
    }
    /***
     * This method is used to return a flag to specify if a Deposit needs to be forfeited.
     * @param reservation
     * @return true if the forfeit date is in the past.
     */
    public boolean isDepositForfeit(RoomReservation reservation) {
    	final String timezone = appProperties.getTimezone(reservation.getPropertyId());
    	ZoneId propertyZone = ZoneId.of(timezone);
    	LocalDateTime propertyDate = LocalDateTime.now(propertyZone);
    	// Find if the reservation is within forfeit period
        if (reservation.getDepositCalc() != null && reservation.getDepositCalc().getForfeitDate() != null) {
            LocalDateTime forfeitDate = reservation.getDepositCalc().getForfeitDate().toInstant().atZone(propertyZone)
                    .toLocalDateTime();
            if (forfeitDate.isBefore(propertyDate)) {
                return true;
            }
        }
    	return false;
    }

    private boolean isHDEPackageProgram(String programId, String propertyId) {
        Program program = programContentDao.getProgramContent(propertyId, programId);
        if (null != program && BooleanUtils.toBoolean(program.getHdePackage())) {
            return true;
        } else if (StringUtils.isNotBlank(programId) && CommonUtil.isUuid(programId)) {
            Optional<RoomProgram> cacheProgram = Optional.ofNullable(roomProgramCacheService.getRoomProgram(programId));
            if (cacheProgram.isPresent()) {
                RoomProgram roomProgram = cacheProgram.get();
                return ReservationUtil.isBlockCodeHdePackage(roomProgram.getOperaBlockCode());
            }
        }
        return false;
    }

    public boolean isHDEPackageReservation(RoomReservation reservation ){
        boolean isHDEPackageReservation =false;
        // ACRS
        if(ACRSConversionUtil.isHDEPackage(reservation)) {
            isHDEPackageReservation =true;
        }else {
         //GSE
            isHDEPackageReservation = isReservationHasHDEProgram(reservation);
        }
        return isHDEPackageReservation;
    }

    public void addF1CasinoDefaultComponentPrices(RoomReservation roomReservation, RoomReservationV2Response response,
                                                  List<String> ratePlanTags, String source) {
        String componentCode = ReservationUtil.getF1DefaultCasinoComponentCode(ratePlanTags);
        if (org.apache.commons.lang.StringUtils.isNotEmpty(componentCode) && !componentCode.equalsIgnoreCase(ServiceConstant.F1_COMP_TAG)) {
            String propertyId;
            Date checkInDate;
            Date checkOutDate;
            String roomTypeId;
            String ratePlanId;
            String mlifeNumber;
            if (null != roomReservation) {
                propertyId = roomReservation.getPropertyId();
                checkInDate = roomReservation.getCheckInDate();
                checkOutDate = roomReservation.getCheckOutDate();
                roomTypeId = roomReservation.getRoomTypeId();
                ratePlanId = roomReservation.getProgramId();
                mlifeNumber = roomReservation.getProfile().getMlifeNo() == 0 ? null :
                        String.valueOf(roomReservation.getProfile().getMlifeNo());
            } else {
                propertyId = response.getPropertyId();
                checkInDate = response.getTripDetails().getCheckInDate();
                checkOutDate = response.getTripDetails().getCheckOutDate();
                roomTypeId = response.getRoomTypeId();
                ratePlanId = response.getProgramId();
                mlifeNumber = response.getProfile().getMlifeNo() == 0 ? null :
                        String.valueOf(response.getProfile().getMlifeNo());
            }
            RoomComponent roomComponent = componentDAO.getRoomComponentByCode(propertyId, componentCode,
                    roomTypeId, ratePlanId, checkInDate, checkOutDate, mlifeNumber, source);
            if (null != roomComponent && org.apache.commons.lang.StringUtils.isNotEmpty(roomComponent.getId())) {
                Float updatedPrice = ReservationUtil.getRoomComponentPrice(roomComponent,
                        checkInDate, checkOutDate);
                if (null != roomReservation) {
                    // update price in daily rates
                    roomReservation.getBookings().forEach(x -> {
                        BigDecimal bd = new BigDecimal(x.getPrice() - updatedPrice).setScale(2, RoundingMode.HALF_UP);
                        if (bd.signum() == 0) {
                            x.setComp(true);
                            x.setPrice(x.getCustomerPrice());
                            x.setCustomerPrice(0.0);
                        } else {
                            x.setPrice(bd.doubleValue());
                        }
                        BigDecimal bd1 = new BigDecimal(x.getBasePrice() - updatedPrice).setScale(2, RoundingMode.HALF_UP);
                        x.setBasePrice(bd1.doubleValue());
                    });
                    // add the default F1 component as special request
                    if (null != roomReservation.getSpecialRequests()) {
                        roomReservation.getSpecialRequests().add(roomComponent.getId());
                    } else {
                        List<String> specialRequestList = new ArrayList<>();
                        specialRequestList.add(roomComponent.getId());
                        roomReservation.setSpecialRequests(specialRequestList);
                    }
                } else {
                    // update price in daily rates
                    response.getBookings().forEach(x -> {
                        if (x.isComp()) {
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
                    response.getPurchasedComponents().removeIf(x -> (null != x.getId() && x.getId().equalsIgnoreCase(roomComponent.getId())));
                    //remove from special request
                    if (null != response.getSpecialRequests()) {
                        response.getSpecialRequests().removeIf(x -> x.equalsIgnoreCase(roomComponent.getId()));
                    }
                    RatesSummary ratesSummary = response.getRatesSummary();
                    ratesSummary.setDiscountedSubtotal(ratesSummary.getDiscountedSubtotal() + roomComponent.getPrice());
                    ratesSummary.setRoomSubtotal(ratesSummary.getRoomSubtotal() + roomComponent.getPrice());
                    ratesSummary.setDiscountedAveragePrice(ratesSummary.getDiscountedAveragePrice() + updatedPrice);
                    ratesSummary.setTripSubtotal(ratesSummary.getTripSubtotal() + roomComponent.getPrice());
                    ratesSummary.setRoomRequestsTotal(ratesSummary.getRoomRequestsTotal() - roomComponent.getPrice());
                }
            }
        }
    }

    public void validateTicketCount (RoomReservation roomReservation, List<String> rateplanTags) {
        boolean f1ComponentAbsent = true;
        if (CollectionUtils.isNotEmpty(roomReservation.getSpecialRequests())) {
            for (String specialRequestId : roomReservation.getSpecialRequests()) {
                RoomComponent roomComponent = new RoomComponent();
                if (!ACRSConversionUtil.isAcrsComponentCodeGuid(specialRequestId)) {
                    roomComponent = componentDAO.getRoomComponentById(specialRequestId, roomReservation.getPropertyId());
                    if (null != roomComponent && null != roomComponent.getPropertyId() &&
                            StringUtils.equalsIgnoreCase(appProperties.getTcolvPropertyId(), roomComponent.getPropertyId())) {
                        String componentCode = ReservationUtil.getTCOLVF1TicketComponentCode(new ArrayList<>(rateplanTags));
                        if (StringUtils.isNotEmpty(componentCode) && StringUtils.equalsIgnoreCase(componentCode, roomComponent.getName())) {
                            return;
                        }
                    }
                } else {
                    roomComponent.setName(ACRSConversionUtil.getComponentNRPlanCode(specialRequestId));
                }
                if (null != roomComponent && null != roomComponent.getName() &&
                        (roomComponent.getName().startsWith(ServiceConstant.F1_COMPONENT_START_F1) ||
                                roomComponent.getName().startsWith(ServiceConstant.F1_COMPONENT_START_HDN))) {
                    f1ComponentAbsent = false;
                }
            }
            if (f1ComponentAbsent) {
                throw new BusinessException(ErrorCode.F1_NON_EDITABLE_COMPONENTS_NOT_AVAILABLE);
            }
        } else {
            throw new BusinessException(ErrorCode.F1_NON_EDITABLE_COMPONENTS_NOT_AVAILABLE);
        }
    }

    public void validateF1InventoryStatus(BookedItemList bookedItemList) {
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(bookedItemList)) {
            BookedItems bookedItems = bookedItemList.get(0);
            boolean f1InvHeldStatus = null != bookedItems && StringUtils.isNotEmpty(bookedItems.getStatus()) &&
                    bookedItems.getStatus().equalsIgnoreCase(ItemStatus.HELD.toString());
            if (!f1InvHeldStatus) {
                throw new BusinessException(ErrorCode.F1_INVENTORY_NOT_HELD);
            }
        } else {
            throw new BusinessException(ErrorCode.F1_INVENTORY_NOT_HELD);
        }
    }

    public List<PurchasedComponent> updatePackageComponentsFlag(String propertyId, List<PurchasedComponent> purchasedComponents) {
        List<String> pkgComponentCodes = propertyPkgComponentCacheService.getPkgComponentCodeByPropertyId(propertyId);
        return ReservationUtil.setPkgComponentFlagForPurchasedComponents(purchasedComponents, pkgComponentCodes);
    }
    public List<String> getPkgComponentCodeByPropertyId(String propertyId){
        return propertyPkgComponentCacheService.getPkgComponentCodeByPropertyId(propertyId);
    }
}
