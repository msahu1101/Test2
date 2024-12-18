package com.mgm.services.booking.room.service;

import com.mgm.services.common.model.Customer;

/**
 * Service class to provide aurora customer functionality
 */
public interface CustomerService {

    /**
     * Get customer based on mlife number
     * 
     * @param mlifeNumber
     *            Mlife number
     * @return Customer information from patron
     */
    Customer getCustomer(String mlifeNumber);

    /**
     * Get customer based on customer id
     * 
     * @param id
     *            Customer id
     * @return Customer information from patron
     */
    Customer getCustomerById(long id);
}
