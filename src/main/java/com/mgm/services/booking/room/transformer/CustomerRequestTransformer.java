/**
 * 
 */
package com.mgm.services.booking.room.transformer;

import org.apache.commons.lang3.BooleanUtils;

import com.mgm.services.booking.room.model.UserAddress;
import com.mgm.services.booking.room.model.UserProfile;
import com.mgm.services.booking.room.model.request.ActivateCustomerRequest;
import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.booking.room.model.request.ReservationRequest;
import com.mgmresorts.aurora.common.PatronType;

import lombok.experimental.UtilityClass;

/**
 * Transformer class to create CreateCustomerRequest from ReservationRequest
 * object.
 */
@UtilityClass
public class CustomerRequestTransformer {

    /**
     * Takes reservationRequest, source and build CreateCustomerRequest.
     * 
     * @param reservationRequest
     *            reservation request
     * @param source
     *            source string
     * @return CreateCustomerRequest object
     */
    public static CreateCustomerRequest getCreateCustomerRequest(ReservationRequest reservationRequest, String source) {
        UserProfile userProfile = reservationRequest.getProfile();
        UserAddress userAddress = reservationRequest.getBilling().getAddress();
        return CreateCustomerRequest.builder().firstName(userProfile.getFirstName()).lastName(userProfile.getLastName())
                .secretQuestionId(Integer.parseInt(userProfile.getSecurityQuestionId()))
                .secretAnswer(userProfile.getSecurityAnswer()).dateOfBirth(userProfile.getDateOfBirth())
                .phoneNumber(userProfile.getPhone()).password(userProfile.getPassword())
                .customerEmail(userProfile.getEmail())
                .isCaslOptin(BooleanUtils.toBoolean(userProfile.getCanadianUser())).country(userAddress.getCountry())
                .state(userAddress.getState()).postalCode(userAddress.getPostalCode()).city(userAddress.getCity())
                .street1(userAddress.getStreet1()).street2(userAddress.getStreet2())
                .patronType(PatronType.Mlife.toString()).source(source).build();
    }
    
    /**
     * Takes reservationRequest object and builds AuroraCustomerRequest object.
     * 
     * @param reservationRequest
     *            reservationRequest object
     * @return ActivateCustomerRequest object
     */
    public ActivateCustomerRequest getActivateCustomerRequest(ReservationRequest reservationRequest) {
        return ActivateCustomerRequest.builder().customerEmail(reservationRequest.getProfile().getEmail()).build();
    }
}
