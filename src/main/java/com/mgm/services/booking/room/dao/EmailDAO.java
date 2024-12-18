package com.mgm.services.booking.room.dao;

import com.mgm.services.booking.room.model.Email;

/**
 * DAO interface to expose services for email functionalities.
 *
 */
public interface EmailDAO {

    /**
     * Sends email to the user.
     * 
     * @param email
     *            Email contents
     */
    void sendEmail(Email email);
}
