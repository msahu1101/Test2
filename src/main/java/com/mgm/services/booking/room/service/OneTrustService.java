package com.mgm.services.booking.room.service;

import com.mgm.services.common.model.Customer;

/**
 * OneTrust is a SaaS based solution to manage and track user preferences. It
 * provides various integration options such as APIs, SDKs to communicate with
 * the external systems. Each individual’s preferences are associated with an
 * identifier that uniquely identifies a user. Each user can have multiple
 * identifiers in Onetrust.
 * 
 * <p>
 * The service to connect to OneTrust for user preferences.
 * </p>
 */
public interface OneTrustService {

    /**
     * Creates user at OneTrust.
     * 
     * @param customer
     *                     The customer object created.
     */
    void createOneTrustUser(Customer customer);
}
