/**
 * 
 */
package com.mgm.services.booking.room.service.cache;

import com.mgm.services.booking.room.model.Email;

/**
 * Service interface exposing services for email template cache services.
 *
 */
public interface EmailCacheService {

    /**
     * Returns room confirmation email template for the requested property.
     * 
     * @param propertyId
     *            Property Id
     * @return Email template
     */
    Email getConfirmationEmailTemplate(String propertyId);

    /**
     * Returns room cancellation email template for the requested property.
     * 
     * @param propertyId
     *            Property Id
     * @return Email template
     */
    Email getCancellationEmailTemplate(String propertyId);
    /**
     * Returns HDE Package related room confirmation email template for the requested property.
     * 
     * @param propertyId
     *            Property Id
     * @return Email template
     */
    Email getHDEPackageConfirmationEmailTemplate(String propertyId);
    
}
