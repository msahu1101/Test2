/**
 * Helper class to keep the supporting methods of ReservationDAO class.
 */
package com.mgm.services.booking.room.dao.helper;

import com.mgm.services.common.exception.SystemException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mgm.services.common.util.ValidationUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.PurchasedComponent;
import com.mgm.services.booking.room.model.request.ModificationChangesRequest;
import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.RoomRequest;
import com.mgm.services.booking.room.model.reservation.RoomPrice;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.service.cache.rediscache.service.PropertyPkgComponentCacheService;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgmresorts.aurora.common.CustomerPhoneNumber;
import com.mgmresorts.aurora.common.CustomerPhoneType;
import com.mgmresorts.aurora.common.CustomerProfile;
import com.mgmresorts.aurora.common.TripParams;
import com.mgmresorts.aurora.messages.AddCustomerRequest;
import com.mgmresorts.aurora.messages.CreateCustomerItineraryRequest;
import com.mgmresorts.aurora.messages.MessageFactory;

/**
 * Helper class to keep the supporting methods of ReservationDAO class.
 * 
 * @author laknaray
 *
 */
@Component
public class ReservationDAOHelper {

    private static final String TRANSIENT_USER_FIRSTNAME = "Transient-F-";

    private static final String TRANSIENT_USER_LASTNAME = "Transient-L-";
    
    @Autowired
    private PropertyPkgComponentCacheService propertyPkgComponentCacheService;

    @Autowired
    private ReferenceDataDAOHelper referenceDataDAOHelper;

    /**
     * Filter programs with rate table as true and summarize program id by count,
     * fetch the max entry and map to value.
     * 
     * @param bookings
     *            list of RoomPrice objects
     * @return dominant programId
     */
    public String findDominantProgram(List<RoomPrice> bookings) {

        return bookings.stream()
                // filter out programs with rate table as true
                .filter(r -> StringUtils.isNotEmpty(r.getProgramId()) &&
                        !r.isProgramIdIsRateTable())
                // summarize program id by count
                .collect(Collectors.groupingBy(RoomPrice::getProgramId, Collectors.counting()))
                // fetch the max entry
                .entrySet().stream().max(Map.Entry.comparingByValue())
                // map to value
                .map(Map.Entry::getKey).orElse(null);
    }

    /**
     * Prepares AddCustomerRequest object from the given reservation object.
     * 
     * @param reservation
     *            room reservation object
     * @return AddCustomerRequest object
     */
    public AddCustomerRequest prepareAddCustomerRequest(RoomReservation reservation) {
        CustomerProfile customerProfile = CustomerProfile.create();
        customerProfile.setFirstName(TRANSIENT_USER_FIRSTNAME + System.currentTimeMillis());
        customerProfile.setLastName(TRANSIENT_USER_LASTNAME + System.currentTimeMillis());
        CustomerPhoneNumber phoneNumber = new CustomerPhoneNumber();
        if (reservation.getProfile().getPhoneNumbers() != null) {
            phoneNumber.setNumber(reservation.getProfile().getPhoneNumbers().get(0).getNumber());
        }
        phoneNumber.setType(CustomerPhoneType.Home);
        CustomerPhoneNumber[] phoneNumbers = new CustomerPhoneNumber[] { phoneNumber };

        customerProfile.setPhoneNumbers(phoneNumbers);

        AddCustomerRequest addCustomerRequest = MessageFactory.createAddCustomerRequest();
        addCustomerRequest.setCustomer(customerProfile);
        addCustomerRequest.setEnroll(false);
        return addCustomerRequest;
    }

    /**
     * Prepares CreateCustomerItineraryRequest object from the given reservation
     * object and customerId.
     * 
     * @param reservation
     *            room reservation object
     * @param customerId
     *            customerId as string
     * @return CreateCustomerItineraryRequest object
     */
    public CreateCustomerItineraryRequest prepareCreateCustomerItineraryRequest(RoomReservation reservation,
            long customerId) {
        CreateCustomerItineraryRequest createCustomerItineraryRequest = MessageFactory
                .createCreateCustomerItineraryRequest();
        createCustomerItineraryRequest.setCustomerId(customerId);

        final TripParams tripParams = TripParams.create();
        tripParams.setArrivalDate(reservation.getCheckInDate());
        tripParams.setDepartureDate(reservation.getCheckOutDate());
        tripParams.setNumAdults(reservation.getNumAdults());

        createCustomerItineraryRequest.setTripParams(tripParams);
        return createCustomerItineraryRequest;
    }

    public boolean checkIfExistingPkgReservation(String propertyIdOrCode, List<PurchasedComponent> purchasedComponents) {
        // if property code, then get propertyId
        String propertyId = referenceDataDAOHelper.retrieveGsePropertyID(propertyIdOrCode);
        if(CollectionUtils.isNotEmpty(purchasedComponents)) {
            List<String> pkgComponentCodes = propertyPkgComponentCacheService.getPkgComponentCodeByPropertyId(propertyId);
            List<PurchasedComponent> existingPkgComponents =
                    purchasedComponents.stream()
                            .filter(component -> ReservationUtil.isPkgComponent(component.getId(), pkgComponentCodes))
                            .collect(Collectors.toList());
            return CollectionUtils.isNotEmpty(existingPkgComponents);
        }
        return false;
    }

    public List<String> getMissingPkgComponents(String propertyId,
            List<String> specialRequests, List<PurchasedComponent> purchasedComponents) {
        List<String> pkgComponentCodes = propertyPkgComponentCacheService.getPkgComponentCodeByPropertyId(propertyId);
        return ReservationUtil.getMissingPkgComponentsInRequest(specialRequests,pkgComponentCodes, purchasedComponents);
    }

    public List<RoomRequest> getMissingPkgRoomRequests(String propertyId,
            List<RoomRequest> roomRequests, List<PurchasedComponent> purchasedComponents) {
        if(roomRequests == null) {
            roomRequests = new ArrayList<>();
        }
        List<String> roomRequestIds = filterOutACRSComponentIds(roomRequests);
        List<String> missingPkgComponents = getMissingPkgComponents(propertyId, roomRequestIds, purchasedComponents);
        return makeRoomRequestFromIds(missingPkgComponents);
    }

    private List<String> filterOutACRSComponentIds(List<RoomRequest> roomRequests) {
        return roomRequests.stream()
                .map(RoomRequest::getId)
                .filter(id -> !ValidationUtil.isUuid(id))
                .filter(ACRSConversionUtil::isAcrsComponentCodeGuid)
                .collect(Collectors.toList());
    }

    private List<RoomRequest> makeRoomRequestFromIds(List<String> roomRequestIds) {
        return roomRequestIds.stream()
                .map(roomId -> {
                    RoomRequest roomReq  = new RoomRequest(roomId);
                    roomReq.setSelected(true);
                    return roomReq;
                }).collect(Collectors.toList());
    }

    public List<String> getPkgComponentCodesByPropertyId(String propertyId){
        return propertyPkgComponentCacheService.getPkgComponentCodeByPropertyId(propertyId);
    }

}
