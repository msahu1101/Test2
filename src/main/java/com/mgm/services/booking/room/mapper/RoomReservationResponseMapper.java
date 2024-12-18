package com.mgm.services.booking.room.mapper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.model.ComponentPrice;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.reservation.CardHolderProfile;
import com.mgm.services.booking.room.model.reservation.CreditCardCharge;
import com.mgm.services.booking.room.model.reservation.PartyRoomReservation;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.BillingAddressResponse;
import com.mgm.services.booking.room.model.response.CreatePartyRoomReservationResponse;
import com.mgm.services.booking.room.model.response.CreditCardResponse;
import com.mgm.services.booking.room.model.response.RoomPaymentDetailsResponse;
import com.mgm.services.booking.room.model.response.RoomReservationV2Response;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.model.ProfileAddress;

/**
 * Interface to map the room reservation model to room reservation response
 * 
 * @author swakulka
 *
 */
@Mapper(componentModel = "spring")
public abstract class RoomReservationResponseMapper {
    
    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private ReservationServiceHelper reservationServiceHelper;
    
    @Autowired
    private ComponentDAO componentDao;
   

    /**
     * Interface method to be implement the transformation of room reservation object to response VO.
     * 
     * @param reservation reservation object.
     * @return RoomReservationResponseV2 response.
     */
    @Mapping(
            source = "checkInDate",
            target = "tripDetails.checkInDate",
            dateFormat = ServiceConstant.ISO_8601_DATE_FORMAT)
    @Mapping(
            source = "checkOutDate",
            target = "tripDetails.checkOutDate",
            dateFormat = ServiceConstant.ISO_8601_DATE_FORMAT)
    @Mapping(source = "numAdults", target = "tripDetails.numAdults")
    @Mapping(source = "numChildren", target = "tripDetails.numChildren")
    @Mapping(source = "numRooms", target = "tripDetails.numRooms")
    @Mapping(source = "chargesAndTaxesCalc", target = "chargesAndTaxes")
    @Mapping(source = "depositCalc", target = "depositDetails")
    @Mapping(source = "depositPolicyCalc", target = "depositPolicy")
    @Mapping(source = "creditCardCharges", target = "billing")    
    /**
     * Interface method to be implement the transformation of room reservation object to response VO.
     * 
     * @param reservation reservation object.
     * @return RoomReservationV2Response response.
     */
    public abstract RoomReservationV2Response roomReservationModelToResponse(RoomReservation reservation);

    /**
     * Interface method to be implement the transformation of party room reservation object to response VO.
     * 
     * @param partyRoomReservation party reservation object.
     * @return CreatePartyRoomReservationResponse response.
     */
    public abstract CreatePartyRoomReservationResponse partyRoomReservationsModelToResponse(PartyRoomReservation partyRoomReservation);

    /**
     * Handling for borgata taxes. Remove special requests which were added to
     * get additional tax elements. This will prevent client applications from
     * displaying internal components used for deriving additional taxes
     * 
     * @param reservation
     *            - Room reservation object
     */
    @BeforeMapping
    public void updateSpecialRequests(RoomReservation reservation) {

        if (null != reservation.getSpecialRequests()) {
            ReservationUtil.removeSpecialRequests(reservation, appProps);
        }
    }

    /**
     * Default method to convert a list of CreditCardCharge to
     * RoomPaymentDetailsResponse
     * 
     * @param list credit card detail list
     * @return List RoomPaymentDetailsList
     */
    public List<RoomPaymentDetailsResponse> roomCreditCardChargesToPaymentDetailsResponse(
            List<CreditCardCharge> list) {
        List<RoomPaymentDetailsResponse> roomPaymentdetailsList = new ArrayList<>();
        if (null != list) {
            list.forEach(roomPaymentDetail -> roomPaymentdetailsList
                    .add(roomCreditCardChargetoPaymentResponse(roomPaymentDetail)));
        }
        return roomPaymentdetailsList;
    }

    /**
     * Default method to convert a CredtiCardCharge to RoomPaymentDetailsResponse
     * 
     * @param creditCardCharge credit card charge
     * @return RoomPaymentDetailsResponse payment details
     */
    public RoomPaymentDetailsResponse roomCreditCardChargetoPaymentResponse(CreditCardCharge creditCardCharge) {
        if (creditCardCharge == null) {
            return null;
        }


        CreditCardResponse cardResponse = new CreditCardResponse();
        cardResponse.setAmount(creditCardCharge.getAmount());
        cardResponse.setMaskedNumber(creditCardCharge.getMaskedNumber());
        cardResponse.setCcToken(creditCardCharge.getDecryptedNumber()!= null ? creditCardCharge.getDecryptedNumber(): creditCardCharge.getNumber());
        cardResponse.setEncryptedccToken(creditCardCharge.getNumber());
        cardResponse.setCvv(creditCardCharge.getCvv());

        final Date expiryDate = creditCardCharge.getExpiry();
        if(null != expiryDate) {
            NumberFormat numberFormat = new DecimalFormat("00");

            Calendar cal = Calendar.getInstance();
            cal.setTime(expiryDate);
            // adding one to month since Calendar month is 0 based (January
            // corresponds to 0)
            cardResponse.setExpiry(numberFormat.format((long) cal.get(Calendar.MONTH) + 1) + "/"
                    + Integer.toString(cal.get(Calendar.YEAR)));  
        }
        cardResponse.setType(creditCardCharge.getType());
        cardResponse.setCardHolder(creditCardCharge.getHolder());

        RoomPaymentDetailsResponse paymentDetailsResponse = new RoomPaymentDetailsResponse();
        
        CardHolderProfile cardHolderProfile = creditCardCharge.getHolderProfile();
        if (null != cardHolderProfile) {
            
            cardResponse.setFirstName(cardHolderProfile.getFirstName());
            cardResponse.setLastName(cardHolderProfile.getLastName());
            ProfileAddress profileAddress = cardHolderProfile.getAddress();
            paymentDetailsResponse.setAddress(getBillingAddressResponse(profileAddress));
            
        }

        paymentDetailsResponse.setPayment(cardResponse);

        return paymentDetailsResponse;
    }
    
    private BillingAddressResponse getBillingAddressResponse(ProfileAddress profileAddress) {

        if (null != profileAddress) {
            BillingAddressResponse billingAddrResponse = new BillingAddressResponse();
            billingAddrResponse.setStreet1(profileAddress.getStreet1());
            billingAddrResponse.setStreet2(profileAddress.getStreet2());
            billingAddrResponse.setCity(profileAddress.getCity());
            billingAddrResponse.setState(profileAddress.getState());
            billingAddrResponse.setPostalCode(profileAddress.getPostalCode());
            billingAddrResponse.setCountry(profileAddress.getCountry());
            return billingAddrResponse;
        }

        return null;
    }

    @AfterMapping
    public void populateAdditionalDetails(@MappingTarget RoomReservationV2Response roomReservationV2Response,
            RoomReservation reservation) {
        roomReservationV2Response.setRatesSummary(ReservationUtil.getRateSummary(reservation));

        roomReservationV2Response.getBilling().forEach(billing -> {
            // If card holder billing address is empty, get from reservation profile
            if (null == billing.getAddress() && null != reservation.getProfile().getAddresses()) {
                billing.setAddress(getBillingAddressResponse(reservation.getProfile().getAddresses().get(0)));
            }
            // If card holder first and last name is empty, get from reservation profile
            if (StringUtils.isBlank(billing.getPayment().getFirstName())) {
                billing.getPayment().setFirstName(reservation.getProfile().getFirstName());
            }
            if (StringUtils.isBlank(billing.getPayment().getLastName())) {
                billing.getPayment().setLastName(reservation.getProfile().getLastName());
            }
        });
        // populating bookingSource and bookingChannel
        String bookingSource = reservationServiceHelper.getBookingSource(reservation.getOrigin());
        roomReservationV2Response.setBookingSource(bookingSource);
        roomReservationV2Response.setBookingChannel(reservationServiceHelper.getBookingChannel(bookingSource));
        // populating isCreditCardExpired & isStayDateModifiable
        roomReservationV2Response.setIsCreditCardExpired(ReservationUtil.isCreditCardExpired(reservation));
        roomReservationV2Response.setIsStayDateModifiable(reservationServiceHelper.isStayDateModifiable(reservation,
                roomReservationV2Response.getBookingChannel()));
        //Populating is cancellable flag
		roomReservationV2Response.setCancellable(reservationServiceHelper.isReservationCancellable(reservation,
				roomReservationV2Response.getBookingChannel()));
        roomReservationV2Response.setOperaHotelCode(appProps.getHotelCode(reservation.getPropertyId()));
        // this will be calling only for GSE. purchasedComponents will be set from DAO impl for ACRS flow.
        if (CollectionUtils.isEmpty(roomReservationV2Response.getPurchasedComponents())) {
            setPurchasedComponents(roomReservationV2Response, reservation);
        }
        //CBSR-747 Set the hdePackage and forfeitDeposit Flags to Find room reservation response
        boolean hdePackage  =reservationServiceHelper.isHDEPackageReservation(reservation);
        boolean depositForfeit = hdePackage || reservationServiceHelper.isDepositForfeit(reservation);
        roomReservationV2Response.setHdePackage(hdePackage);
        roomReservationV2Response.setDepositForfeit(depositForfeit);
        //CBSR-932 Returning operaRoomCode from OCRS for OTA reservations to correctly resolve propertyDetails and RoomType Details 
        //On GraphQL side.
        roomReservationV2Response.setOperaRoomCode(reservation.getOperaRoomCode());
    }
    
    private void setPurchasedComponents(RoomReservationV2Response roomReservationV2Response,
            RoomReservation reservation) {

        if (null == reservation.getSpecialRequests()) {
            return;
        }

        // Add information about purchased components
        List<PurchasedComponent> purchasedComponents = new ArrayList<>();
        reservation.getSpecialRequests()
                .forEach(r -> {
                    PurchasedComponent component = new PurchasedComponent();

                    RoomRequest roomRequest = componentDao.getRoomComponentById(r, reservation.getRoomTypeId(),
                            reservation.getPropertyId());

                    if (null != roomRequest) {
                        component.setId(r);
                        component.setCode(roomRequest.getCode());
                        component.setActive(roomRequest.isActive());
                        component.setShortDescription(roomRequest.getShortDescription());
                        component.setLongDescription(roomRequest.getLongDescription());
                        component.setPricingApplied(roomRequest.getPricingApplied());
                        component.setPrice(roomRequest.getPrice());
                        component.setPrices(reservationServiceHelper
                                .getComponentPrices(reservation.getChargesAndTaxesCalc(), component.getCode()));

                        component.setTripPrice(component.getPrices()
                                .stream()
                                .collect(Collectors.summingDouble(ComponentPrice::getAmount)));
                        component.setTripTax(component.getPrices()
                                .stream()
                                .collect(Collectors.summingDouble(ComponentPrice::getTax)));

                        purchasedComponents.add(component);
                    }
                });

        roomReservationV2Response.setPurchasedComponents(purchasedComponents);
    }
}