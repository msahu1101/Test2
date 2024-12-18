package com.mgm.services.booking.room.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.AccertifyDAO;
import com.mgm.services.booking.room.dao.ProgramContentDAO;
import com.mgm.services.booking.room.dao.RoomContentDAO;
import com.mgm.services.booking.room.model.authorization.TransactionMappingRequest;
import com.mgm.services.booking.room.model.content.Program;
import com.mgm.services.booking.room.model.content.Property;
import com.mgm.services.booking.room.model.content.Room;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.AccertifyService;
import com.mgm.services.booking.room.service.cache.PropertyContentCacheService;
import com.mgm.services.booking.room.util.CCTokenDecryptionClient;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.model.authorization.AuthorizationTransactionDetails;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;
import com.mgm.services.common.model.authorization.BillingDetails;
import com.mgm.services.common.model.authorization.BookingType;
import com.mgm.services.common.model.authorization.GuestDetails;
import com.mgm.services.common.model.authorization.PaymentMethod;
import com.mgm.services.common.model.authorization.Products;
import com.mgm.services.common.model.authorization.RoomDetail;

import io.jsonwebtoken.lang.Collections;
import lombok.extern.log4j.Log4j2;

/**
 * Service interface exposing service to authorize and confirm a transaction
 * 
 * @author nitpande0
 *
 */
@Component
@Log4j2
public class AccertifyServiceImpl implements AccertifyService {

	@Autowired
	private RoomContentDAO roomContentDao;

	@Autowired
	private ProgramContentDAO programContentDao;

	@Autowired
	private PropertyContentCacheService propertyCacheService;

	@Autowired
	private AccertifyDAO accertifyDao;

    @Autowired(required = false)
    private CCTokenDecryptionClient decryptionClient;
    
    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;

	@Override
	public void confirm(AuthorizationTransactionRequest req, HttpHeaders headers) {
		try {
			accertifyDao.confirm(req, headers);
		} catch (Exception ex) {
			// Unable to send accertify confirmation shouldn't be hard failure
			log.error("PostReservationErrors - Unable to send confirmation to accertify: ", ex);
		}
	}

	@Override
	public AuthorizationTransactionResponse authorize(AuthorizationTransactionRequest req) {

		return accertifyDao.authorize(req);
	}

	/**
	 * Transform the room request into a format which can be passed to the
	 * Authorization service
	 * 
	 * @param req the request
	 * @return the transformed response
	 */
	public AuthorizationTransactionRequest transform(TransactionMappingRequest req) {

		RoomReservation reservation = req.getReservation();
		RoomReservationResponse preresponse = req.getPreresponse();

		AuthorizationTransactionDetails authDetails = new AuthorizationTransactionDetails();

		authDetails.setTransactionId(req.getTransactionId());
		if(StringUtils.isEmpty(req.getTransactionId())) {
		    log.info("Generating a transaction id since it's not provided");
		    authDetails.setTransactionId(UUID.randomUUID().toString());
		}
		authDetails.setInauthTransactionId(req.getInAuthTransactionId());
		authDetails.setConfirmationNumbers(ServiceConstant.CONFIRMATION_PENDING);
		authDetails.setBookingType(BookingType.ROOM.getValue());
		authDetails.setTransactionType(ServiceConstant.TRANSACTION_TYPE_ONLINE);
		authDetails.setTransactionDateTime(
				CommonUtil.getDateStr(new Date(), ServiceConstant.TIME_ZONE_FORMAT_WITH_LOCALE));
		authDetails.setTransactionTotalAmount(preresponse.getRates().getReservationTotal());
		authDetails.setSalesChannel(reservation.getSource());

		GuestDetails guest = new GuestDetails();
		ReservationProfile profile = reservation.getProfile();
		guest.setLoggedIn(profile.getMlifeNo() > 0 ? "Yes" : "No");
		guest.setMemberId(Integer.toString(profile.getMlifeNo()));
		guest.setFirstName(profile.getFirstName());
		guest.setLastName(profile.getLastName());
		guest.setEmailAddress(profile.getEmailAddress1());
		// Handling the scenario for v2 services, where request may not contain phoneNumber
        if (!Collections.isEmpty(profile.getPhoneNumbers())) {
            guest.setPhone(profile.getPhoneNumbers().get(0).getNumber());
        }
		guest.setCreatedDate(CommonUtil.getDateStr(new Date(TimeUnit.SECONDS.toMillis(profile.getCreatedAt())),
				ServiceConstant.TIME_ZONE_FORMAT_WITH_LOCALE));
		guest.setLastModifiedDate(CommonUtil.getDateStr(new Date(TimeUnit.SECONDS.toMillis(profile.getUpdatedAt())),
				ServiceConstant.TIME_ZONE_FORMAT_WITH_LOCALE));
        // Handling the scenario for v2 services, where request may not contain address
        if (!Collections.isEmpty(profile.getAddresses())) {
            guest.setAddress1(profile.getAddresses().get(0).getStreet1());
            guest.setAddress2(profile.getAddresses().get(0).getStreet2());
            guest.setCity(profile.getAddresses().get(0).getCity());
            guest.setState(profile.getAddresses().get(0).getState());
            guest.setPostalCode(profile.getAddresses().get(0).getPostalCode());
            guest.setCountry(profile.getAddresses().get(0).getCountry());
        }

        // Populate Payment method
        List<PaymentMethod> paymentMethods = new ArrayList<>();
        if (reservation.getCreditCardCharges() != null && !reservation.getCreditCardCharges().isEmpty()) {
            List<CreditCardCharge> creditCardCharges = reservation.getCreditCardCharges();
            creditCardCharges.stream().forEach(creditCardCharge -> {
                PaymentMethod paymentMethod = new PaymentMethod();
                paymentMethod.setPaymentType(ServiceConstant.PAYMENT_TYPE_CREDIT_CARD);
                if(null != creditCardCharge.getHolderProfile()) {
                    StringBuilder profileName = new StringBuilder();
                    if (StringUtils.isNotBlank(creditCardCharge.getHolderProfile().getFirstName() )) {  
                        profileName.append(creditCardCharge.getHolderProfile().getFirstName());
                    }
                    if(StringUtils.isNotBlank(creditCardCharge.getHolderProfile().getLastName())) {
                        profileName.append(ServiceConstant.WHITESPACE_STRING);
                        profileName.append(creditCardCharge.getHolderProfile().getLastName());
                    }
                    if(0 != profileName.length()) {
                        paymentMethod.setCardHolderName(profileName.toString());
                    }
                    if(null != creditCardCharge.getHolderProfile().getAddress()) {
                        paymentMethod.setBillingAddress1(creditCardCharge.getHolderProfile().getAddress().getStreet1());
                        paymentMethod.setBillingAddress2(creditCardCharge.getHolderProfile().getAddress().getStreet2());
                        paymentMethod.setBillingCity(creditCardCharge.getHolderProfile().getAddress().getCity());
                        paymentMethod.setBillingState(creditCardCharge.getHolderProfile().getAddress().getState());
                        paymentMethod.setBillingPostalCode(
                                creditCardCharge.getHolderProfile().getAddress().getPostalCode());
                        paymentMethod.setBillingCountry(creditCardCharge.getHolderProfile().getAddress().getCountry());
                    }
                }
                paymentMethod.setCreditCardType(creditCardCharge.getType());
                paymentMethod.setPaymentToken(getPaymentToken(creditCardCharge.getNumber()));
                Calendar cal = Calendar.getInstance();
                cal.setTime(creditCardCharge.getExpiry());
                paymentMethod.setCreditCardExpireMonth(Integer.toString(cal.get(Calendar.MONTH) + 1));
                paymentMethod.setCreditCardExpireYear(Integer.toString(cal.get(Calendar.YEAR)));
                paymentMethod.setCurrencyCode(ServiceConstant.CURRENCY_USD);
                paymentMethod.setTransactionChargeAmount(preresponse.getRates().getDepositDue());
                paymentMethods.add(paymentMethod);
            });
        }

        List<RoomDetail> rooms = new ArrayList<>();
        RoomDetail roomDetail = new RoomDetail();
        
        roomDetail.setConfirmationNumber(ServiceConstant.CONFIRMATION_PENDING);
        roomDetail.setGuestName(reservation.getProfile().getFirstName() + reservation.getProfile().getLastName());
        String propertyGuid = reservation.getPropertyId();
        if(!CommonUtil.isUuid(propertyGuid)){
            propertyGuid = referenceDataDAOHelper.retrieveGsePropertyID(propertyGuid);
        }
        roomDetail.setPropertyId(propertyGuid);
        roomDetail.setHotelName(getHotelName(reservation.getPropertyId()));
        roomDetail.setCheckInDate(CommonUtil.getDateStr(reservation.getCheckInDate(), "yyyy-MM-dd"));
        roomDetail.setCheckOutDate(CommonUtil.getDateStr(reservation.getCheckOutDate(), "yyyy-MM-dd"));
        roomDetail.setGuests(reservation.getNumAdults());
        roomDetail.setOfferId(reservation.getProgramId());
        roomDetail.setRoomTotal(reservation.getBookings().get(0).getPrice());
        roomDetail.setDepositDue(preresponse.getRates().getDepositDue());
        roomDetail.setTotalRoomCharges(preresponse.getRates().getRoomSubtotal());
        roomDetail.setTaxes(preresponse.getRates().getRoomChargeTax());
        roomDetail.setResortFeeAndTaxes(preresponse.getRates().getResortFeeAndTax());
        roomDetail.setAdditionalCharges(preresponse.getRates().getRoomRequestsTotal());

        roomDetail.setRoomName(getRoomName(reservation.getRoomTypeId()));
        roomDetail.setRoomId(reservation.getRoomTypeId());
        roomDetail.setOfferName(getOfferName(reservation.getPropertyId(), reservation.getProgramId()));

        int numRooms = reservation.getNumRooms();
        for (int i = 0; i < numRooms; i++) {
            rooms.add(roomDetail);
        }

		BillingDetails billingDetails = new BillingDetails();
		billingDetails.setPaymentMethods(paymentMethods);

		Products products = new Products();
		products.setRooms(rooms);

		authDetails.setGuest(guest);
		authDetails.setBilling(billingDetails);
		authDetails.setProducts(products);

		AuthorizationTransactionRequest transactionRequest = new AuthorizationTransactionRequest();
		transactionRequest.setTransaction(authDetails);

		return transactionRequest;

	}
	
    private String getRoomName(String roomTypeId) {

        if (appProps.isSkipCapiLookupsForAFS()) {
            return StringUtils.EMPTY;
        }
        try {
            Room room = roomContentDao.getRoomContent(roomTypeId);
            return room.getName();
        } catch (Exception e) {
            // Room name for anti-fraud is not mandatory
            log.warn("Failed to get room content from Content API", e);
        }
        return StringUtils.EMPTY;
    }
    
    private String getHotelName(String propertyId) {

        Property property = propertyCacheService.getProperty(propertyId);

        log.info("Retriving Hotel data from the cache");
        if (property != null) {
            return property.getName();
        } else {
            log.info("Hotel data retrieved from the cache is null");
            return StringUtils.EMPTY;
        }
    }
    
    private String getOfferName(String propertyId, String programId) {
        if (appProps.isSkipCapiLookupsForAFS()) {
            return StringUtils.EMPTY;
        }
        
        if (StringUtils.isNotEmpty(programId)) {
            try {
                Program program = programContentDao.getProgramContent(propertyId, programId);
                if(null != program) {
                	return program.getName();
                }
            } catch (Exception e) {
                // Offer name for anti-fraud is not mandatory
                log.warn("Failed to get program content from Content API", e);
            }
        }
        return StringUtils.EMPTY;
    }

    private String getPaymentToken(String number) {

        // If the number string is greater than 28, it's not a token. It could
        // be encrypted token which needs to be decrypted
        if (StringUtils.isNotEmpty(number) && number.length() > 28 && null != decryptionClient) {
            try {
                return decryptionClient.decrypt(number);
            } catch (Exception e) {
                log.error("Failed to decrypt the payment token: ", e);
            }

        }

        return number;
    }

}
