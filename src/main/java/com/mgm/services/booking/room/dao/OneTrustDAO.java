package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.UserAttributeList;

/**
 *DAO to connect to OneTrust server.
 */
public interface OneTrustDAO {

    /**
     * Method to create OneTrustUser.
     * @param userAttributes The user attributes
     * @param userIdentifier The user identifier.
     */
    void createOneTrustUser(UserAttributeList userAttributes, String userIdentifier);
}
