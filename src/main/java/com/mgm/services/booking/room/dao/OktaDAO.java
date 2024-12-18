package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.request.ActivateCustomerRequest;
import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.booking.room.model.response.ActivateCustomerResponse;
import com.mgm.services.booking.room.model.response.CustomerWebInfoResponse;

/**
 * DAO interface for creating and activating online/web profiles.
 *
 */
public interface OktaDAO {

    /**
     * Gets the customer web account details.
     *
     * @param createCustomerRequest
     *            create customer request
     * @return customer web information
     */
    CustomerWebInfoResponse getCustomerByWebCredentials(CreateCustomerRequest createCustomerRequest);

    /**
     * Creates customer web account in Okta.
     *
     * @param createCustomerRequest
     *            create customer request
     */
    void createCustomerWebCredentials(CreateCustomerRequest createCustomerRequest);

    /**
     * Activates the customer web account in Okta.
     *
     * @param activateCustomerRequest
     *            activate customer request
     * @return activate customer response
     */
    ActivateCustomerResponse activateCustomerWebCredentials(ActivateCustomerRequest activateCustomerRequest);

    /**
     * Deactivates customer web account in Okta.
     *
     * @param customerEmailId
     *              customer email id
     */
    void deactivateCustomerWebCredentials(String customerEmailId);

    /**
     * Deletes deactivated customer web account in Okta.
     *
     * @param customerEmailId
     *              customer email id
     */
    void deleteCustomerWebCredentials(String customerEmailId);
}
