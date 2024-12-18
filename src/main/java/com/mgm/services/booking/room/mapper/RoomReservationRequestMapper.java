package com.mgm.services.booking.room.mapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.request.*;
import com.mgm.services.booking.room.model.request.dto.CommitPaymentDTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.event.RoomReservationEventRequest;
import com.mgm.services.booking.room.model.reservation.CardHolderProfile;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.model.ProfileAddress;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Interface used to map the room reservation request object to the model
 * 
 * @author swakulka
 *
 */
@Mapper(componentModel = "spring")
public abstract class RoomReservationRequestMapper {

    @Autowired
    private ApplicationProperties appProps;
    @Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;

    /**
     * Interface method to implement which converts room reservation request to the model class
     * 
     * @param request - RoomReservationRequest
     * @return RoomReservation
     */
    @Mapping(
            source = "request.tripDetails.checkInDate",
            target = "checkInDate",
            dateFormat = ServiceConstant.ISO_8601_DATE_FORMAT)
    @Mapping(
            source = "request.tripDetails.checkOutDate",
            target = "checkOutDate",
            dateFormat = ServiceConstant.ISO_8601_DATE_FORMAT)
    @Mapping(source = "request.tripDetails.numAdults", target = "numAdults")
    @Mapping(source = "request.tripDetails.numChildren", target = "numChildren")
    @Mapping(source = "request.tripDetails.numRooms", target = "numRooms")
    @Mapping(source = "request.chargesAndTaxes", target = "chargesAndTaxesCalc")
    @Mapping(source = "request.depositDetails", target = "depositCalc")
    @Mapping(source = "request.billing", target = "creditCardCharges")
    @Mapping(source = "request.payments", target = "payments")
    @Mapping(source = "request.markets", target = "markets")
    @Mapping(source = "request.depositPolicy", target = "depositPolicyCalc")
    @Mapping(source = "request.inAuthTransactionId", target = "inSessionReservationId")
    @Mapping(source = "request.skipFraudCheck", target = "skipFraudCheck")
    @Mapping(source = "request.skipPaymentProcess", target = "skipPaymentProcess")
    public abstract RoomReservation roomReservationRequestToModel(RoomReservationRequest request);
    public abstract CommitPaymentDTO paymentRoomReservationRequestToCommitPaymentDTO(PaymentRoomReservationRequest request);
    
    public abstract RoomReservationRequest roomReservationResponseToRequest(RoomReservationV2Response response);
    
    public abstract RoomReservationEventRequest roomReservationResponseToEventRequest(RoomReservationV2Response reservationResponse);

    /**
     * Default method to map PaymentDetails to CreditCardCharges
     * 
     * @param list - List of RoomPaymentDetailsRequest
     * @return - list of CreditCardCharge
     */
    public List<CreditCardCharge> roomPaymentDetailsRequestListToCreditCardChargeList(
            List<RoomPaymentDetailsRequest> list) {
    	
    	if(null == list) {
    		return null;
    	}
        List<CreditCardCharge> creditCardCharges = new ArrayList<>();
        list.stream().forEach(roomPaymentDetails -> 
            creditCardCharges.add(roomPaymentDetailsRequestToCreditCardCharge(roomPaymentDetails))
        );
        
        return creditCardCharges;
    }

    /**
     * Default method to conver a single RoomPaymentDetails object to
     * CredtiCardCharge
     * 
     * @param roomPaymentDetailsRequest request
     * @return creditCardCharge charges
     */
    public CreditCardCharge roomPaymentDetailsRequestToCreditCardCharge(
            RoomPaymentDetailsRequest roomPaymentDetailsRequest) {
        if (roomPaymentDetailsRequest == null) {
            return null;
        }

        CreditCardCharge creditCardCharge = new CreditCardCharge();
        CreditCardRequest ccRequest = roomPaymentDetailsRequest.getPayment();
        creditCardCharge.setAmount(ccRequest.getAmount());
        creditCardCharge.setCvv(ccRequest.getCvv());
        creditCardCharge.setCcToken(ccRequest.getCcToken());
        creditCardCharge.setMaskedNumber(ccRequest.getMaskedNumber());
        
        if(StringUtils.isNotBlank(ccRequest.getFxCurrencyCode())) {
            creditCardCharge.setCurrencyCode(ccRequest.getFxCurrencyCode());
            creditCardCharge.setFxAmount(ccRequest.getFxAmount());
            creditCardCharge.setFxCurrencyISOCode(ccRequest.getFxCurrencyISOCode());
            creditCardCharge.setFxExchangeRate(ccRequest.getFxExchangeRate());
            creditCardCharge.setFxFlag(ccRequest.getFxFlag());
        }
        // for cash payment expireDate is null
        if(null != ccRequest.getExpiry()) {
            boolean twoDigitYear = false;
            final Calendar expireDate = Calendar.getInstance();
            String[] expiry = ccRequest.getExpiry().split("/");
            String yearExpiry = expiry[1];
            if (yearExpiry.length() == 2) {
                yearExpiry = ServiceConstant.YEAR_PREFIX + yearExpiry;
                twoDigitYear = true;
            }
            expireDate.set(Calendar.YEAR, Integer.parseInt(yearExpiry));
            // subtracting one since Calendar month is 0 based (January corresponds
            // to 0)
            expireDate.set(Calendar.MONTH, Integer.parseInt(expiry[0]) - 1);
            expireDate.set(Calendar.DAY_OF_MONTH, getLastDayOfMonth(ccRequest.getExpiry(), twoDigitYear));
            expireDate.set(Calendar.HOUR_OF_DAY, 23);
            expireDate.set(Calendar.MINUTE, 59);
            expireDate.set(Calendar.SECOND, 59);

            creditCardCharge.setExpiry(expireDate.getTime());  
        }
        creditCardCharge.setTxnDateAndTime(ccRequest.getTxnDateAndTime());
        creditCardCharge.setType(ccRequest.getType());
        creditCardCharge.setHolder(ccRequest.getCardHolder());
        creditCardCharge.setNumber(ccRequest.getCcToken());
        creditCardCharge.setAuthId(ccRequest.getAuthId());
        creditCardCharge.setAuthIds(ccRequest.getAuthIds());
        CardHolderProfile holderProfile = new CardHolderProfile();
        holderProfile.setFirstName(ccRequest.getFirstName());
        holderProfile.setLastName(ccRequest.getLastName());

        ProfileAddress profileAddress = new ProfileAddress();
        BillingAddressRequest billingAddrRequest = roomPaymentDetailsRequest.getAddress();
        profileAddress.setType("Home");
        profileAddress.setPreferred(true);
        profileAddress.setStreet1(billingAddrRequest.getStreet1());
        profileAddress.setStreet2(billingAddrRequest.getStreet2());
        // Setting state only for US and Canada, since freedom pay only accepts
     	// 2 or 3 letter state code
        if (StringUtils.isNotBlank(billingAddrRequest.getCountry())
        		&& (billingAddrRequest.getCountry().equalsIgnoreCase(ServiceConstant.COUNTRY_US)
        				|| billingAddrRequest.getCountry().equalsIgnoreCase(ServiceConstant.COUNTRY_CANADA))) {
        	profileAddress.setState(billingAddrRequest.getState());
        } else {
        	profileAddress.setState(StringUtils.EMPTY);
        }
        profileAddress.setCity(billingAddrRequest.getCity());
        profileAddress.setCountry(billingAddrRequest.getCountry());
        profileAddress.setPostalCode(billingAddrRequest.getPostalCode());

        holderProfile.setAddress(profileAddress);
        creditCardCharge.setHolderProfile(holderProfile);
        return creditCardCharge;
    }

    private int getLastDayOfMonth(String expiry, boolean twoDigitYear) {
        int lastDay = 28;
        try {
            DateTimeFormatter formatter;
            if (twoDigitYear) {
                formatter = DateTimeFormatter.ofPattern("M/yy");
            } else {
                formatter = DateTimeFormatter.ofPattern("M/yyyy");
            }
            YearMonth yearMonth = YearMonth.parse(expiry, formatter);
            lastDay = yearMonth.atEndOfMonth().getDayOfMonth();
        } catch (Exception ex) {}
        return lastDay;
    }
	@AfterMapping
    public void setCreditCardCharges(@MappingTarget CommitPaymentDTO commitPaymentDTO, PaymentRoomReservationRequest request){
        commitPaymentDTO.setCreditCardCharges(roomPaymentDetailsRequestListToCreditCardChargeList(request.getBilling()));


    }
    @AfterMapping
    public void updateRoomReservation(@MappingTarget RoomReservation reservation, RoomReservationRequest request) {
        updateSpecialRequests(reservation);
        updateProfileAddress(reservation, request);

    }

    /**
     * Handling for borgata taxes. Add special requests which are required to
     * get additional tax elements
     * 
     * @param reservation
     *            - Room reservation
     */
    private void updateSpecialRequests(RoomReservation reservation) {
        if (!referenceDataDAOHelper.isPropertyManagedByAcrs(reservation.getPropertyId())) {
        ReservationUtil.addSpecialRequests(reservation, appProps);
        }
    }

    /**
     * Populate the profile's address for the transient reservations if it is empty.
     * 
     * @param reservation
     *            - Room reservation
     * @param request
     *            - Reservation request
     */
    private void updateProfileAddress(RoomReservation reservation, RoomReservationRequest request) {

        if (request.getProfile().getMlifeNo() <= 0 && CollectionUtils.isEmpty(request.getProfile().getAddresses())) {
            final List<ProfileAddress> addressList = new ArrayList<>();
            reservation.getCreditCardCharges().forEach(c -> addressList.add(c.getHolderProfile().getAddress()));
            reservation.getProfile().setAddresses(addressList);
        }

    }
}