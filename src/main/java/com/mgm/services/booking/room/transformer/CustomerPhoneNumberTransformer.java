package com.mgm.services.booking.room.transformer;

import com.mgm.services.booking.room.model.profile.PhoneNumber;
import com.mgmresorts.aurora.common.CustomerPhoneNumber;
import com.mgmresorts.aurora.common.CustomerPhoneType;

import lombok.experimental.UtilityClass;

/**
 * Transformer class to create CustomerPhoneNumber from PhoneNumber object.
 */
@UtilityClass
public class CustomerPhoneNumberTransformer {

    /**
     * Creates the CustomerPhoneNumber request object and set the
     * CustomerPhoneNumberVO values in it.
     *
     * @param phoneNumber
     *              phone number object
     * @return the customer phone number
     */
    public static CustomerPhoneNumber createTo(PhoneNumber phoneNumber) {
        final CustomerPhoneNumber customerPhoneNumber = CustomerPhoneNumber.create();
        customerPhoneNumber.setNumber(phoneNumber.getNumber());
        if (null != phoneNumber.getPhoneNumberType()) {
            switch (phoneNumber.getPhoneNumberType()) {
            case RESIDENCE_LANDLINE:
                    customerPhoneNumber.setType(CustomerPhoneType.Home);
                    break;
            case OFFICE_LANDLINE:
                    customerPhoneNumber.setType(CustomerPhoneType.Business);
                    break;
            case MOBILE:
                    customerPhoneNumber.setType(CustomerPhoneType.Mobile);
                    break;
            case PAGER:
                    customerPhoneNumber.setType(CustomerPhoneType.Pager);
                    break;
            default:
                    break;
            }
        }
        return customerPhoneNumber;
    }
}
