package com.mgm.services.booking.room.model.profile;

import java.io.Serializable;

import com.mgmresorts.aurora.common.CustomerPhoneNumber;

import lombok.Data;

public @Data class PhoneNumber implements Serializable {

    private static final long serialVersionUID = -8687450320475248229L;

    private PhoneType phoneNumberType;
    private String number;
    
    /**
     * Convert from CustomerPhoneNumber response to CustomerPhoneNumberVO.
     *
     * @param customerPhoneNumber
     *            the customer phone number
     */
    public void convertFrom(final CustomerPhoneNumber customerPhoneNumber) {
        setNumber(customerPhoneNumber.getNumber());
        if (null != customerPhoneNumber.getType()) {

            switch (customerPhoneNumber.getType()) {
            case Home:
                    this.setPhoneNumberType(PhoneType.RESIDENCE_LANDLINE);
                    break;
            case Business:
                    this.setPhoneNumberType(PhoneType.OFFICE_LANDLINE);
                    break;
            case Mobile:
                    this.setPhoneNumberType(PhoneType.MOBILE);
                    break;
            case Pager:
                    this.setPhoneNumberType(PhoneType.PAGER);
                    break;
            default:
                    break;
            }
        }
    }
}
