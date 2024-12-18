package com.mgm.services.booking.room.service;

import com.mgm.services.booking.room.model.request.ActivateCustomerRequest;
import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.booking.room.model.response.ActivateCustomerResponse;
import com.mgm.services.booking.room.model.response.CreateCustomerResponse;

/**
 * Service class to provide Mlife profile related functionality.
 */
public interface ProfileManagementService {

    /**
     * Creates the customer.
     * 
     * @param createCustomerRequest
     *            the create customer request
     * @return the creates the customer response
     */
    CreateCustomerResponse createCustomer(CreateCustomerRequest createCustomerRequest);

    /**
     * Activate customer.
     *
     * @param activateCustomerRequest
     *            the activate customer request
     * @return the activate customer response
     */
    ActivateCustomerResponse activateCustomer(ActivateCustomerRequest activateCustomerRequest);

    /**
     * Deactivate customer.
     *
     * @param customerEmailId
     *              the customer email id
     */
    void deactivateCustomer(String customerEmailId);

    /**
     * Delete customer.
     *
     * @param customerEmailId
     *              the customer email id
     */
    void deleteCustomer(String customerEmailId);
}
