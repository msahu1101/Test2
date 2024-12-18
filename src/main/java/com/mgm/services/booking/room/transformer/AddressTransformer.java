/**
 * 
 */
package com.mgm.services.booking.room.transformer;

import com.mgm.services.booking.room.model.profile.Address;
import com.mgm.services.booking.room.model.profile.AddressType;
import com.mgmresorts.aurora.common.CustomerAddress;
import com.mgmresorts.aurora.common.CustomerAddressType;

import lombok.experimental.UtilityClass;

/**
 * Transformer class to create CustomerAddress from Address object.
 */
@UtilityClass
public class AddressTransformer {

    /**
     * Creates the to.
     * 
     * @param address
     *              address object
     * @return the customer address
     */
    public static CustomerAddress createTo(Address address) {
        final CustomerAddress customerAddress = CustomerAddress.create();
        customerAddress.setPreferred(true);
        customerAddress.setStreet1(address.getStreet1());
        customerAddress.setStreet2(address.getStreet2());
        customerAddress.setCity(address.getCity());
        customerAddress.setState(address.getState());
        customerAddress.setCountry(address.getCountry());
        customerAddress.setPostalCode(address.getPostalCode());
        if (null != address.getType()) {
            if (address.getType() == AddressType.HOME) {
                customerAddress.setType(CustomerAddressType.Home);
            } else if (address.getType() == AddressType.OFFICE) {
                customerAddress.setType(CustomerAddressType.Business);
            }
        }

        return customerAddress;
    }
}
