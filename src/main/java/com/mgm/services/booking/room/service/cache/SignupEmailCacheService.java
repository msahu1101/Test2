/**
 * 
 */
package com.mgm.services.booking.room.service.cache;

import com.mgm.services.booking.room.model.Email;

/**
 * Service interface exposing services for email template cache services.
 *
 */
public interface SignupEmailCacheService {

    /**
     * Returns signup confirmation email template for the requested property.
     *
     * @param propertyId
     *            Property Id
     * @return Email template
     */
    Email getSignupEmailTemplate(String propertyId);
}
