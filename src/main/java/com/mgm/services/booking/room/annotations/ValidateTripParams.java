package com.mgm.services.booking.room.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.mgm.services.booking.room.annotations.validator.TripParamsValidator;

/**
 * Validate the startDate & endDate with below conditions. 
 * 1. Start date < End date. 
 * 2. Either of Start date or End date should not be null. 
 * 3. Start date & End date should be in valid format.
 * 
 * @author jayveera
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TripParamsValidator.class)
public @interface ValidateTripParams {
    /**
     * Returns the error code in case of invalid input.
     * 
     * @return error code
     */
    String message() default "_invalid_dates";

    /**
     * Allows the specification of validation groups, to which this constraint
     * belongs.
     * 
     * @return validation group
     */
    Class<?>[] groups() default {};

    /**
     * Return payload that can be used by clients of the Bean Validation API to
     * assign custom payload objects to a constraint.
     * 
     * @return
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * return startDate
     * 
     * @return date - start date
     */
    String startDate();

    /**
     * return endDate
     * 
     * @return date - end date
     */
    String endDate();

    /**
     * return number of adults.
     * 
     * @return number of adults.
     */
    String numberOfAdults();
}
