package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.common.model.Customer;

/**
 * Services providing functionalities for sending account creation email.
 */
public interface ProfileEmailService {

    /**
     * Send signup confirmation email.
     * 
     * @param input
     *            abstract base request object
     * @param customer
     *            customer object
     */
    void sendAccountCreationMail(CreateCustomerRequest input, Customer customer);

}
