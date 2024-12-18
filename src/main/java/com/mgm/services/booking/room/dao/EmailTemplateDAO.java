/**
 * 
 */
package com.mgm.services.booking.room.dao;

/**
 * DAO interface to retrieve email templates from AEM end points.
 *
 */
public interface EmailTemplateDAO {

    /**
     * Fetches and returns the email template from AEM public end point.
     * 
     * @param propertyId
     *            Property identifier
     * @return Email template body
     */
    String getRoomConfirmationTemplate(String propertyId);

    /**
     * Fetches and returns the email template from AEM public end point.
     * 
     * @param propertyId
     *            Property identifier
     * @return Email template body
     */
    String getRoomCancellationTemplate(String propertyId);

    /**
     * Fetches and returns the email template from AEM public end point.
     *
     * @param propertyId
     *            Property identifier
     * @return Email template body
     */
    String getSignupCompletionTemplate(String propertyId);
    /**
     * Fetches and returns the email template from AEM public end point.
     * 
     * @param propertyId
     *            Property identifier
     * @return Email template body
     */
    String getHDEPackageConfirmationTemplate(String propertyId);
    
    
}
