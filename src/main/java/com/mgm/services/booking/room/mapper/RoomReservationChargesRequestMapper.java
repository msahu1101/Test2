package com.mgm.services.booking.room.mapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.util.PropertyConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import com.mgm.services.booking.room.model.request.BillingAddressRequest;
import com.mgm.services.booking.room.model.request.CreditCardRequest;
import com.mgm.services.booking.room.model.request.RoomPaymentDetailsRequest;
import com.mgm.services.booking.room.model.request.RoomReservationChargesRequest;
import com.mgm.services.booking.room.model.reservation.CardHolderProfile;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.model.ProfileAddress;

/**
 * Mapper interface for room reservation request object conversion to model.
 * @author swakulka
 *
 */
@Mapper(componentModel = "spring")
public abstract class RoomReservationChargesRequestMapper {

    @Autowired
    private ApplicationProperties appProps;
    @Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;

    /**
     * Method to implement for conversion of room reservation request object to the model object.
     * 
     * @param request - RoomReservationChargesRequest
     * @return RoomReservation object
     */
    @Mapping(source = "request.tripDetails.checkInDate", target = "checkInDate", dateFormat = "mm/DD/yyyy")
    @Mapping(source = "request.tripDetails.checkOutDate", target = "checkOutDate", dateFormat = "mm/DD/yyyy")
    @Mapping(source = "request.tripDetails.numAdults", target = "numAdults")
    @Mapping(source = "request.tripDetails.numChildren", target = "numChildren")
    @Mapping(source = "request.tripDetails.numRooms", target = "numRooms")
    @Mapping(source = "request.depositDetails", target = "depositCalc")
    @Mapping(source = "request.billing", target = "creditCardCharges")
    @Mapping(source = "request.perpetualPricing", target = "perpetualPricing")
    public abstract RoomReservation roomReservationChargesRequestToModel(RoomReservationChargesRequest request);

    /**
     * Default method to convert list of RoomPaymentDetailsRequest to list of CreditCardCharge.
     * 
     * @param list - List of RoomPaymentDetailsRequest 
     * @return List of CreditCardCharge
     */
    public List<CreditCardCharge> roomPaymentDetailsRequestListToCreditCardChargeList(
            List<RoomPaymentDetailsRequest> list) {

        if (list == null) {
            return new ArrayList<>();
        }

        List<CreditCardCharge> list1 = new ArrayList<>(list.size());

        for (RoomPaymentDetailsRequest roomPaymentDetailsRequest : list) {
            list1.add(roomPaymentDetailsRequestToCreditCardCharge(roomPaymentDetailsRequest));
        }

        return list1;
    }

    /**
     * Default method to convert a RoomPaymentDetailsRequest to CreditCardCharge
     * @param roomPaymentDetailsRequest - RoomPaymentDetailsRequest object
     * @return CreditCardCharge object
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
        if(null != ccRequest.getExpiry()) {
	        final Calendar expireDate = Calendar.getInstance();
	        String[] expiry = ccRequest.getExpiry().split("/");
	        expireDate.set(Calendar.YEAR, Integer.parseInt(expiry[1]));
	        expireDate.set(Calendar.MONTH, Integer.parseInt(expiry[0]) - 1);
	
	        creditCardCharge.setExpiry(expireDate.getTime());
        }
        creditCardCharge.setType(ccRequest.getType());
        creditCardCharge.setHolder(ccRequest.getCardHolder());

        CardHolderProfile holderProfile = new CardHolderProfile();
        holderProfile.setFirstName(ccRequest.getFirstName());
        holderProfile.setLastName(ccRequest.getLastName());

        ProfileAddress profileAddress = new ProfileAddress();
        BillingAddressRequest billingAddrRequest = roomPaymentDetailsRequest.getAddress();
        profileAddress.setType("Home");
        profileAddress.setPreferred(true);
        profileAddress.setStreet1(billingAddrRequest.getStreet1());
        profileAddress.setStreet2(billingAddrRequest.getStreet2());
        profileAddress.setCity(billingAddrRequest.getCity());
        profileAddress.setCountry(billingAddrRequest.getCountry());

        holderProfile.setAddress(profileAddress);
        creditCardCharge.setHolderProfile(holderProfile);

        return creditCardCharge;
    }
    
    /**
     * Handling for borgata taxes. Add special requests which are required to
     * get additional tax elements
     * 
     * @param reservation
     *            - Room reservation
     * @param request
     *            - Charges request
     */
    @AfterMapping
    public void updateSpecialRequests(@MappingTarget RoomReservation reservation,
            RoomReservationChargesRequest request) {

        if (null != reservation.getSpecialRequests() && !referenceDataDAOHelper.isPropertyManagedByAcrs(reservation.getPropertyId())) {
            ReservationUtil.addSpecialRequests(reservation, appProps);
        }
    }
}
