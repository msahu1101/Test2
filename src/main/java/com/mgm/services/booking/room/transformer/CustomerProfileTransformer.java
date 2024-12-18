/**
 * 
 */
package com.mgm.services.booking.room.transformer;

import org.apache.commons.lang3.StringUtils;

import com.mgm.services.booking.room.model.profile.Address;
import com.mgm.services.booking.room.model.profile.AddressType;
import com.mgm.services.booking.room.model.profile.PhoneNumber;
import com.mgm.services.booking.room.model.profile.PhoneType;
import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.booking.room.model.request.UserProfileRequest;
import com.mgm.services.common.model.Customer;
import com.mgm.services.common.util.DateUtil;
import com.mgmresorts.aurora.common.CustomerAddress;
import com.mgmresorts.aurora.common.CustomerPhoneNumber;
import com.mgmresorts.aurora.common.CustomerProfile;
import com.mgmresorts.aurora.common.PatronType;

import lombok.experimental.UtilityClass;

/**
 * Transformer class to create CustomerProfile from createCustomerRequest
 * object.
 */
@UtilityClass
public class CustomerProfileTransformer {

    /**
     * Creates the CustomerProfile request object and set CustomerProfileVO values.
     *
     * @param createCustomerRequest
     *              create customer request object
     * @return the customer profile
     */
    public static CustomerProfile convert(CreateCustomerRequest createCustomerRequest) {
        final CustomerProfile customerProfile = CustomerProfile.create();
        customerProfile.setId(createCustomerRequest.getCustomerId());
        customerProfile.setFirstName(createCustomerRequest.getFirstName());
        customerProfile.setLastName(createCustomerRequest.getLastName());
        customerProfile.setEmailAddress1(createCustomerRequest.getCustomerEmail());
        customerProfile.setDateOfBirth(DateUtil.toDate(createCustomerRequest.getDateOfBirth()));
        customerProfile.setMlifeNo(createCustomerRequest.getMlifeNo() == null ? 0 : createCustomerRequest.getMlifeNo());
        customerProfile.setCaslOptin(createCustomerRequest.isCaslOptin());
        if (null != createCustomerRequest.getPatronType()) {
            customerProfile.setPatronType(PatronType.valueOf(createCustomerRequest.getPatronType()));
        }
        if (null != createCustomerRequest.getPhoneNumber()) {
            final PhoneNumber phoneNumberObj = new PhoneNumber();
            if (StringUtils.isNotEmpty(createCustomerRequest.getPhoneType())) {
                phoneNumberObj.setPhoneNumberType(PhoneType.valueOf(createCustomerRequest.getPhoneType()));
            } else {
                phoneNumberObj.setPhoneNumberType(PhoneType.RESIDENCE_LANDLINE);
            }
            final CustomerPhoneNumber customerPhoneNumber = CustomerPhoneNumberTransformer.createTo(phoneNumberObj);
            customerPhoneNumber.setNumber(createCustomerRequest.getPhoneNumber());
            CustomerPhoneNumber[] customerPhoneNumberArr = new CustomerPhoneNumber[1];
            customerPhoneNumberArr[0] = customerPhoneNumber;
            customerProfile.setPhoneNumbers(customerPhoneNumberArr);
        }

        if (null != createCustomerRequest.getStreet1()) {
            final Address address = new Address();
            if (StringUtils.isNotEmpty(createCustomerRequest.getAddressType())) {
                address.setType(AddressType.valueOf(createCustomerRequest.getAddressType()));
            } else {
                address.setType(AddressType.HOME);
            }

            final CustomerAddress customerAddress = AddressTransformer.createTo(address);
            customerAddress.setStreet1(createCustomerRequest.getStreet1());
            customerAddress.setStreet2(createCustomerRequest.getStreet2());
            customerAddress.setCity(createCustomerRequest.getCity());
            customerAddress.setState(createCustomerRequest.getState());
            customerAddress.setCountry(createCustomerRequest.getCountry());
            customerAddress.setPostalCode(createCustomerRequest.getPostalCode());
            CustomerAddress[] customerAddressArr = new CustomerAddress[1];
            customerAddressArr[0] = customerAddress;
            customerProfile.setAddresses(customerAddressArr);
        }

        return customerProfile;
    }

    public static UserProfileRequest convert(Customer customerProfile) {
        UserProfileRequest userProfileRequest = new UserProfileRequest();
        userProfileRequest.setId(customerProfile.getCustomerId());
        userProfileRequest.setMlifeNo(customerProfile.getMlifeNumber());
        userProfileRequest.setTitle(customerProfile.getTitle());

        userProfileRequest.setDateOfBirth(customerProfile.getDateOfBirth());
        userProfileRequest.setOperaId(customerProfile.getOperaId());
        userProfileRequest.setTier(customerProfile.getTier());
        userProfileRequest.setDateOfEnrollment(customerProfile.getDateOfEnrollment());
        userProfileRequest.setFirstName(customerProfile.getFirstName());
        userProfileRequest.setLastName(customerProfile.getLastName());
        userProfileRequest.setEmailAddress1(customerProfile.getEmailAddress());
        userProfileRequest.setHgpNo(customerProfile.getHgpNo());
        userProfileRequest.setSwrrNo(customerProfile.getSwrrNo());
        userProfileRequest.setPhoneNumbers(customerProfile.getPhoneNumbers());
        userProfileRequest.setAddresses(customerProfile.getAddresses());
        return userProfileRequest;
    }
}
