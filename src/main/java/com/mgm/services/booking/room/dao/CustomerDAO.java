/**
 * 
 */
package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.ProfileRequest;
import com.mgm.services.common.model.Customer;

/**
 * DAO interface to expose services for customer related functions.
 *
 */
public interface CustomerDAO {

    /**
     * Service finds customer information based on mlife number. Returns
     * Mono of Void if no customer record is found for given mlife number.
     * 
     * @param mlifeNumber
     *            Mlife Number
     * @return Customer object
     */
    Customer getCustomer(String mlifeNumber);

    /**
     * Service finds customer information based on customer id. Returns
     * Mono of Void if no customer record is found for given customer id.
     * 
     * @param id
     *            Customer id
     * @return Customer object
     */
    Customer getCustomerById(long id);

    /**
     * Service finds customer information based on customer emailId. Returns
     * Mono of Void if no customer record is found for given customer id.
     *
     * @param emailAddress
     *            email address
     * @return Customer object
     */
    Customer[] getCustomersByEmailAddress(String emailAddress);

    /**
     * This method is a multi-function method used to perform the
     * following functions:
     * 1. Add a new transient customer to the Aurora system
     * 2. Add an existing patron customer to the Aurora system
     *
     * @param profileRequest
     *            the profile request
     * @return the customer
     */
    Customer addCustomer(ProfileRequest profileRequest);
}
